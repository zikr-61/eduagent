package com.example.eaibackend.service;

import com.example.eaibackend.model.KnowledgePoint;
import com.example.eaibackend.model.Question;
import com.example.eaibackend.repository.KnowledgePointRepository;
import com.example.eaibackend.repository.QuestionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class KnowledgePointService {

    @Autowired
    private KnowledgePointRepository knowledgePointRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Value("${qwen.api.key:EMPTY}")
    private String qwenApiKey;

    @Value("${qwen.api.base-url:https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation}")
    private String qwenBaseUrl;

    @Value("${qwen.model:qwen-turbo}")
    private String qwenModel;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Transactional
    public KnowledgePoint createKnowledgePoint(Integer userId, String title, String content, String summary, String fileName) {
        KnowledgePoint knowledgePoint = new KnowledgePoint();
        knowledgePoint.setUserId(userId);
        knowledgePoint.setTitle(title);
        knowledgePoint.setContent(content);
        knowledgePoint.setSummary(summary);
        knowledgePoint.setFileName(fileName);
        return knowledgePointRepository.save(knowledgePoint);
    }

    public List<KnowledgePoint> getUserKnowledgePoints(Integer userId) {
        return knowledgePointRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public KnowledgePoint getKnowledgePointById(Integer id) {
        return knowledgePointRepository.findById(id).orElse(null);
    }

    @Transactional
    public Question addQuestion(Integer knowledgePointId, String questionText, String questionType, String options, String answer, Integer difficulty) {
        Question question = new Question();
        question.setKnowledgePointId(knowledgePointId);
        question.setQuestionText(questionText);
        question.setQuestionType(questionType);
        question.setOptions(options);
        question.setAnswer(answer);
        question.setDifficulty(difficulty != null ? difficulty : 1);
        return questionRepository.save(question);
    }

    public List<Question> getQuestionsByKnowledgePointId(Integer knowledgePointId) {
        return questionRepository.findByKnowledgePointIdOrderByCreatedAtDesc(knowledgePointId);
    }

    @Transactional
    public Map<String, Object> generateSummaryAndQuestions(Integer userId, String title, String content, String fileName) {
        List<Map<String, Object>> executionSteps = new ArrayList<>();
        
        Map<String, Object> step1 = new HashMap<>();
        step1.put("stepId", 1);
        step1.put("stepName", "任务分解");
        step1.put("description", "解析用户输入，分解为子任务");
        step1.put("status", "completed");
        step1.put("details", Map.of(
            "input", Map.of("title", title, "contentLength", content != null ? content.length() : 0),
            "subTasks", List.of("生成知识点摘要", "生成选择题习题")
        ));
        step1.put("timestamp", System.currentTimeMillis());
        executionSteps.add(step1);
        
        Map<String, Object> step2 = new HashMap<>();
        step2.put("stepId", 2);
        step2.put("stepName", "工具调用 - 摘要生成");
        step2.put("description", "调用AI模型生成知识点摘要");
        step2.put("status", "running");
        step2.put("timestamp", System.currentTimeMillis());
        executionSteps.add(step2);
        
        long summaryStartTime = System.currentTimeMillis();
        String summary = generateSummaryWithAI(content);
        long summaryDuration = System.currentTimeMillis() - summaryStartTime;
        
        step2.put("status", "completed");
        step2.put("duration", summaryDuration + "ms");
        step2.put("details", Map.of(
            "tool", "Qwen AI API",
            "model", qwenModel,
            "outputLength", summary != null ? summary.length() : 0,
            "result", summary
        ));
        
        Map<String, Object> step3 = new HashMap<>();
        step3.put("stepId", 3);
        step3.put("stepName", "工具调用 - 习题生成");
        step3.put("description", "调用AI模型生成选择题");
        step3.put("status", "running");
        step3.put("timestamp", System.currentTimeMillis());
        executionSteps.add(step3);
        
        long questionsStartTime = System.currentTimeMillis();
        List<Map<String, String>> questions = generateQuestionsWithAI(content, 5);
        long questionsDuration = System.currentTimeMillis() - questionsStartTime;
        
        step3.put("status", "completed");
        step3.put("duration", questionsDuration + "ms");
        step3.put("details", Map.of(
            "tool", "Qwen AI API",
            "model", qwenModel,
            "questionCount", questions.size(),
            "questions", questions
        ));
        
        Map<String, Object> step4 = new HashMap<>();
        step4.put("stepId", 4);
        step4.put("stepName", "数据存储");
        step4.put("description", "保存知识点和习题到数据库");
        step4.put("status", "running");
        step4.put("timestamp", System.currentTimeMillis());
        executionSteps.add(step4);
        
        KnowledgePoint knowledgePoint = createKnowledgePoint(userId, title, content, summary, fileName);

        List<Question> savedQuestions = new ArrayList<>();
        for (Map<String, String> q : questions) {
            Question question = new Question();
            question.setKnowledgePointId(knowledgePoint.getId());
            question.setQuestionText(q.get("question"));
            question.setQuestionType(q.get("type") != null ? q.get("type") : "choice");
            question.setOptions(q.get("options"));
            question.setAnswer(q.get("answer"));
            question.setDifficulty(1);
            savedQuestions.add(questionRepository.save(question));
        }
        
        step4.put("status", "completed");
        step4.put("details", Map.of(
            "knowledgePointId", knowledgePoint.getId(),
            "savedQuestions", savedQuestions.size()
        ));
        
        Map<String, Object> step5 = new HashMap<>();
        step5.put("stepId", 5);
        step5.put("stepName", "结果输出");
        step5.put("description", "整合结果并返回给用户");
        step5.put("status", "completed");
        step5.put("timestamp", System.currentTimeMillis());
        step5.put("details", Map.of(
            "summaryLength", summary != null ? summary.length() : 0,
            "questionCount", savedQuestions.size()
        ));
        executionSteps.add(step5);

        // 知识点已保存，通知 Python Agent 服务清除该用户的向量缓存
        notifyAgentCacheInvalidate(userId);

        Map<String, Object> result = new HashMap<>();
        result.put("knowledgePoint", knowledgePoint);
        result.put("summary", summary);
        result.put("questions", savedQuestions);
        result.put("executionSteps", executionSteps);
        result.put("totalDuration", executionSteps.stream()
            .filter(s -> s.containsKey("duration"))
            .mapToLong(s -> Long.parseLong(s.get("duration").toString().replace("ms", "")))
            .sum() + "ms");
        return result;
    }

    private void notifyAgentCacheInvalidate(Integer userId) {
        try {
            String body = "{\"user_id\":" + userId + "}";
            java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create("http://localhost:8000/agent/knowledge/invalidate"))
                    .header("Content-Type", "application/json")
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(body))
                    .build();
            httpClient.sendAsync(req, java.net.http.HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            // 非阻塞，Python 服务不在线时不影响知识点保存
        }
    }

    private String generateSummaryWithAI(String content) {
        if (content == null || content.isEmpty()) {
            return "无内容";
        }

        try {
            String prompt = "请根据以下内容生成一个简洁的摘要（不超过100字）：\n\n" + content;
            String response = callQwenAPI(prompt);
            if (response != null && !response.trim().isEmpty()) {
                return response.trim();
            }
        } catch (Exception e) {
            System.err.println("AI生成摘要失败，使用备用方法: " + e.getMessage());
        }

        return generateSummaryFallback(content);
    }

    private List<Map<String, String>> generateQuestionsWithAI(String content, int count) {
        List<Map<String, String>> questions = new ArrayList<>();

        try {
            String prompt = "请根据以下内容生成" + count + "道选择题。每道题必须有4个选项（A、B、C、D），并给出正确答案。\n" +
                    "请严格按照以下JSON格式返回，不要有其他内容：\n" +
                    "[{\"question\":\"题目内容\",\"options\":\"A: 选项1\\nB: 选项2\\nC: 选项3\\nD: 选项4\",\"answer\":\"正确答案（如A）\"}]\n\n" +
                    "内容：\n" + content;

            String response = callQwenAPI(prompt);
            if (response != null) {
                questions = parseQuestionsFromAIResponse(response);
                if (!questions.isEmpty()) {
                    return questions;
                }
            }
        } catch (Exception e) {
            System.err.println("AI生成习题失败，使用备用方法: " + e.getMessage());
        }

        return generateQuestionsFallback(content, count);
    }

    private String callQwenAPI(String prompt) {
        try {
            if ("EMPTY".equals(qwenApiKey) || qwenApiKey.isEmpty()) {
                System.err.println("Qwen API Key未配置");
                return null;
            }

            Map<String, Object> requestBodyMap = new HashMap<>();
            requestBodyMap.put("model", qwenModel);
            
            List<Map<String, String>> messages = new ArrayList<>();
            Map<String, String> message = new HashMap<>();
            message.put("role", "user");
            message.put("content", prompt);
            messages.add(message);
            requestBodyMap.put("messages", messages);
            
            requestBodyMap.put("temperature", 0.7);
            requestBodyMap.put("max_tokens", 2000);

            String requestBody = objectMapper.writeValueAsString(requestBodyMap);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(qwenBaseUrl + "/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + qwenApiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("Qwen API请求: " + requestBody);
            System.out.println("Qwen API响应: " + response.statusCode() + " - " + response.body());

            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                JsonNode choices = root.path("choices");
                if (choices.isArray() && choices.size() > 0) {
                    JsonNode msg = choices.get(0).path("message");
                    if (msg.isObject()) {
                        return msg.path("content").asText();
                    }
                }
            } else {
                System.err.println("Qwen API调用失败: " + response.statusCode() + " - " + response.body());
            }
        } catch (Exception e) {
            System.err.println("调用Qwen API异常: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    private List<Map<String, String>> parseQuestionsFromAIResponse(String response) {
        List<Map<String, String>> questions = new ArrayList<>();
        try {
            String cleanedResponse = response;
            if (response.contains("```json")) {
                cleanedResponse = response.replaceAll("```json\\s*", "").replaceAll("```\\s*", "");
            } else if (response.contains("```")) {
                cleanedResponse = response.replaceAll("```\\s*", "");
            }
            
            cleanedResponse = cleanedResponse.trim();
            if (cleanedResponse.startsWith("[")) {
                JsonNode array = objectMapper.readTree(cleanedResponse);
                if (array.isArray()) {
                    for (JsonNode item : array) {
                        Map<String, String> q = new HashMap<>();
                        q.put("type", "choice");
                        
                        if (item.has("question")) {
                            q.put("question", item.get("question").asText());
                        }
                        if (item.has("options")) {
                            q.put("options", item.get("options").asText());
                        }
                        if (item.has("answer")) {
                            q.put("answer", item.get("answer").asText());
                        }
                        
                        if (q.containsKey("question") && q.containsKey("options") && q.containsKey("answer")) {
                            questions.add(q);
                        }
                    }
                }
            }
            
            if (questions.isEmpty()) {
                Pattern pattern = Pattern.compile("\\{[^{}]*\"question\"[^{}]*\"answer\"[^{}]*\\}", Pattern.DOTALL);
                Matcher matcher = pattern.matcher(cleanedResponse);

                while (matcher.find()) {
                    String jsonStr = matcher.group();
                    try {
                        JsonNode item = objectMapper.readTree(jsonStr);
                        Map<String, String> q = new HashMap<>();
                        q.put("type", "choice");
                        
                        if (item.has("question")) {
                            q.put("question", item.get("question").asText());
                        }
                        if (item.has("options")) {
                            q.put("options", item.get("options").asText());
                        }
                        if (item.has("answer")) {
                            q.put("answer", item.get("answer").asText());
                        }
                        
                        if (q.containsKey("question") && q.containsKey("options") && q.containsKey("answer")) {
                            questions.add(q);
                        }
                    } catch (Exception e) {
                        System.err.println("解析单个题目失败: " + e.getMessage());
                    }
                }
            }
            
            System.out.println("解析到的题目数量: " + questions.size());
        } catch (Exception e) {
            System.err.println("解析AI响应失败: " + e.getMessage());
            e.printStackTrace();
        }
        return questions;
    }

    private String generateSummaryFallback(String content) {
        if (content == null || content.isEmpty()) {
            return "无内容";
        }
        String[] sentences = content.split("[。！？]");
        if (sentences.length <= 3) {
            return content;
        }
        StringBuilder summary = new StringBuilder();
        for (int i = 0; i < Math.min(3, sentences.length); i++) {
            if (!sentences[i].trim().isEmpty()) {
                summary.append(sentences[i].trim()).append("。");
            }
        }
        return summary.toString();
    }

    private List<Map<String, String>> generateQuestionsFallback(String content, int count) {
        List<Map<String, String>> questions = new ArrayList<>();
        String[] lines = content.split("[\n，。、；：]");

        for (int i = 0; i < Math.min(count, lines.length); i++) {
            if (lines[i].trim().length() < 5) continue;

            Map<String, String> question = new HashMap<>();
            question.put("type", "choice");
            question.put("question", "关于本文内容，以下说法正确的是？");
            question.put("options", "A: " + lines[i].trim().substring(0, Math.min(20, lines[i].trim().length())) + "\nB: 与A相反的说法\nC: 与内容无关的说法\nD: 部分正确的说法");
            question.put("answer", "A");
            questions.add(question);
        }

        if (questions.isEmpty()) {
            Map<String, String> defaultQ = new HashMap<>();
            defaultQ.put("type", "choice");
            defaultQ.put("question", "本文主要讨论的内容是什么？");
            defaultQ.put("options", "A: 学习方法\nB: 生活常识\nC: 科学知识\nD: 历史事件");
            defaultQ.put("answer", "A");
            questions.add(defaultQ);
        }

        return questions;
    }
}
