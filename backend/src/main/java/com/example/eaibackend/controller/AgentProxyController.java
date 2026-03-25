package com.example.eaibackend.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
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
     * 代理：流式对话（SSE），转发 Python 的 text/event-stream。
     *
     * 根本原因分析：Python/uvicorn 使用 HTTP/1.1 keep-alive，发完最后一个 SSE 事件后
     * 不立即关闭 TCP 连接，导致 InputStream.read() 阻塞直到超时，从而抛出异常，
     * Spring 来不及发送 chunked 结束标记（0\r\n\r\n），浏览器报 ERR_INCOMPLETE_CHUNKED_ENCODING。
     *
     * 修复方案：
     * 1. 直接写入 HttpServletResponse.getOutputStream()，由 Tomcat 负责 HTTP 生命周期
     * 2. 逐行读取 SSE，每个完整事件（空行分隔）后立即 flush
     * 3. 检测到 "type":"done" 或 "type":"error" 后主动退出循环，不依赖 Python 关闭连接
     */
    @PostMapping(value = "/proxy/chat/stream", consumes = MediaType.APPLICATION_JSON_VALUE)
    public void proxyChatStream(@RequestBody Map<String, Object> payload,
                                HttpServletResponse response) {
        response.setContentType("text/event-stream;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.addHeader("Cache-Control", "no-cache");
        response.addHeader("X-Accel-Buffering", "no");
        response.addHeader("Connection", "keep-alive");

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

            OutputStream out = response.getOutputStream();

            if (resp.statusCode() != 200) {
                String detail = "";
                try (InputStream errIn = resp.body()) {
                    byte[] bytes = errIn.readAllBytes();
                    if (bytes.length > 0) {
                        detail = new String(bytes, StandardCharsets.UTF_8).replace("\n", " ").trim();
                        if (detail.length() > 300) detail = detail.substring(0, 300) + "...";
                    }
                }
                String msg = "Agent 返回 HTTP " + resp.statusCode()
                        + (detail.isBlank() ? "" : "，详情: " + detail);
                String json = objectMapper.writeValueAsString(Map.of("type", "error", "message", msg));
                out.write(("data: " + json + "\n\n").getBytes(StandardCharsets.UTF_8));
                out.flush();
                return;
            }

            // 逐行读取 SSE；检测到 done/error 后主动退出，不等 Python 关闭连接
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resp.body(), StandardCharsets.UTF_8))) {
                String line;
                boolean finished = false;
                while (!finished && (line = reader.readLine()) != null) {
                    out.write((line + "\n").getBytes(StandardCharsets.UTF_8));
                    if (line.isEmpty()) {
                        // 空行 = SSE 事件边界，立即 flush 让浏览器能立刻收到
                        out.flush();
                    } else if (line.startsWith("data:")) {
                        // 检测流结束标志，主动停止而不依赖 TCP 关闭
                        String data = line.length() > 5 ? line.substring(5).trim() : "";
                        if (data.contains("\"type\":\"done\"") || data.contains("\"type\":\"error\"")) {
                            finished = true;
                        }
                    }
                }
                // 确保最后一个事件有完整的 \n\n 结尾
                out.write("\n".getBytes(StandardCharsets.UTF_8));
            }
            out.flush();

        } catch (Exception e) {
            try {
                String msg = e.getMessage() == null ? "连接 Agent 失败"
                        : e.getMessage().replace("\"", "'");
                if (msg.length() > 200) msg = msg.substring(0, 200);
                String json = "{\"type\":\"error\",\"message\":\"" + msg
                        + " — 请在 IDEA 运行 agent_service/main.py 或双击 start.bat\"}";
                response.getOutputStream().write(("data: " + json + "\n\n")
                        .getBytes(StandardCharsets.UTF_8));
                response.getOutputStream().flush();
            } catch (Exception ignored) {
            }
        }
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
