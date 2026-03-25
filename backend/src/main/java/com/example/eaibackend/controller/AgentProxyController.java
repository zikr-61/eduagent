package com.example.eaibackend.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Agent 相关接口：
 * 1) 通知 Python 清除向量缓存、健康检查
 * 2) 反向代理到 Python :8000，使前端只访问 8080（避免浏览器直连 8000 被拒绝连接）
 */
@RestController
@RequestMapping("/api/agent")
public class AgentProxyController {

    private static final String PYTHON_BASE = "http://localhost:8000";
    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 代理：会话列表 GET /agent/sessions?user_id=
     */
    @GetMapping("/proxy/sessions")
    public Map<String, Object> proxySessions(@RequestParam("user_id") Integer userId) {
        try {
            URI uri = URI.create(PYTHON_BASE + "/agent/sessions?user_id=" + userId);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(uri)
                    .GET()
                    .timeout(Duration.ofSeconds(20))
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200 && resp.body() != null && !resp.body().isBlank()) {
                Map<String, Object> body = objectMapper.readValue(resp.body(), new TypeReference<>() {});
                body.put("agentOnline", true);
                return body;
            }
        } catch (Exception ignored) {
        }
        return Map.of(
                "sessions", List.of(),
                "agentOnline", false,
                "message", "Python Agent 未启动：在 IDEA 中运行 agent_service/main.py，或执行 agent_service/start.bat（端口 8000）"
        );
    }

    /**
     * 代理：某会话消息
     */
    @GetMapping("/proxy/sessions/{sessionId}/messages")
    public Map<String, Object> proxySessionMessages(
            @PathVariable String sessionId,
            @RequestParam("user_id") Integer userId) {
        try {
            String url = PYTHON_BASE + "/agent/sessions/" + sessionId + "/messages?user_id=" + userId;
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .timeout(Duration.ofSeconds(20))
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200 && resp.body() != null) {
                Map<String, Object> body = objectMapper.readValue(resp.body(), new TypeReference<>() {});
                body.put("agentOnline", true);
                return body;
            }
        } catch (Exception ignored) {
        }
        return Map.of("messages", List.of(), "agentOnline", false);
    }

    /**
     * 代理：流式对话（SSE），转发 Python 的 text/event-stream
     * 使用 Map 接收 JSON 再序列化：避免 @RequestBody String 在部分环境下拿到空串，导致 Python 422
     */
    @PostMapping(value = "/proxy/chat/stream", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<StreamingResponseBody> proxyChatStream(@RequestBody Map<String, Object> payload) {
        StreamingResponseBody stream = outputStream -> {
            try {
                String rawBody = objectMapper.writeValueAsString(payload);
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(PYTHON_BASE + "/agent/chat/stream"))
                        .version(HttpClient.Version.HTTP_1_1)
                        .timeout(Duration.ofMinutes(8))
                        .header("Content-Type", "application/json; charset=UTF-8")
                        .POST(HttpRequest.BodyPublishers.ofString(rawBody, StandardCharsets.UTF_8))
                        .build();
                HttpResponse<InputStream> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofInputStream());
                if (resp.statusCode() != 200) {
                    String detail = "";
                    try (InputStream errIn = resp.body()) {
                        byte[] bytes = errIn.readAllBytes();
                        if (bytes.length > 0) {
                            detail = new String(bytes, StandardCharsets.UTF_8).replace("\n", " ").trim();
                            if (detail.length() > 300) {
                                detail = detail.substring(0, 300) + "...";
                            }
                        }
                    }
                    String msg = "Agent 返回 HTTP " + resp.statusCode()
                            + (detail.isBlank() ? "" : ("，详情: " + detail));
                    String json = objectMapper.writeValueAsString(Map.of(
                            "type", "error",
                            "message", msg
                    ));
                    outputStream.write(("data: " + json + "\n\n").getBytes(StandardCharsets.UTF_8));
                    outputStream.flush();
                    return;
                }
                // 逐块读取并立即 flush，确保每个 SSE event 及时送达
                // 避免 transferTo 在流结束时未发送 chunked 结束标记导致 ERR_INCOMPLETE_CHUNKED_ENCODING
                try (InputStream in = resp.body()) {
                    byte[] buf = new byte[4096];
                    int n;
                    while ((n = in.read(buf)) != -1) {
                        outputStream.write(buf, 0, n);
                        outputStream.flush();
                    }
                } finally {
                    try { outputStream.flush(); } catch (Exception ignored) {}
                }
            } catch (Exception e) {
                try {
                    String msg = e.getMessage() == null ? "连接 Agent 失败" : e.getMessage().replace("\"", "'");
                    String json = "{\"type\":\"error\",\"message\":\"" + msg
                            + " — 请在 IDEA 运行 agent_service/main.py 或双击 start.bat\"}";
                    outputStream.write(("data: " + json + "\n\n").getBytes(StandardCharsets.UTF_8));
                    outputStream.flush();
                } catch (Exception ignored) {
                }
            }
        };
        return ResponseEntity.ok()
                .header("Cache-Control", "no-cache")
                .header("X-Accel-Buffering", "no")
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body(stream);
    }

    /**
     * 知识点保存后调用，清除向量缓存
     * POST /api/agent/knowledge/invalidate?userId=xxx
     */
    @PostMapping("/knowledge/invalidate")
    public Map<String, Object> invalidateKnowledgeCache(@RequestParam Integer userId) {
        try {
            String body = "{\"user_id\":" + userId + "}";
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(PYTHON_BASE + "/agent/knowledge/invalidate"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            return Map.of("success", resp.statusCode() == 200, "status", resp.statusCode());
        } catch (Exception e) {
            // Python 服务未启动时不影响正常业务
            return Map.of("success", false, "message", "Agent服务暂不可用: " + e.getMessage());
        }
    }

    /**
     * 健康检查 Python 服务是否在线
     */
    @GetMapping("/health")
    public Map<String, Object> checkAgentHealth() {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(PYTHON_BASE + "/health"))
                    .GET()
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            return Map.of("online", resp.statusCode() == 200, "response", resp.body());
        } catch (Exception e) {
            return Map.of("online", false, "message", "Agent服务离线");
        }
    }
}
