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

    @Transactional
    public Map<String, Object> generateLessonPlan(Integer userId, String title, String gradeLevel, String content) {
        List<Map<String, Object>> executionSteps = new ArrayList<>();

        Map<String, Object> step1 = new HashMap<>();
        step1.put("stepId", 1);
        step1.put("stepName", "任务分析");
        step1.put("description", "解析教学主题，准备生成教案");
        step1.put("status", "completed");
        step1.put("details", Map.of("title", title, "gradeLevel", gradeLevel));
        step1.put("timestamp", System.currentTimeMillis());
        executionSteps.add(step1);

        Map<String, Object> step2 = new HashMap<>();
        step2.put("stepId", 2);
        step2.put("stepName", "AI 生成教案");
        step2.put("description", "调用AI模型生成结构化教案");
        step2.put("status", "running");
        step2.put("timestamp", System.currentTimeMillis());
        executionSteps.add(step2);

        long startTime = System.currentTimeMillis();
        String grade = (gradeLevel != null && !gradeLevel.isEmpty()) ? gradeLevel : "通用";
        String prompt = "你是一位有15年教学经验的中学" + grade + "教师，精通教案设计。" +
                "请根据以下信息生成一份内容翔实、可直接用于课堂教学的完整教案。\n\n" +
                "教学主题：" + title + "\n" +
                "目标年级：" + grade + "\n" +
                "内容要点：" + content + "\n\n" +
                "要求：每个字段都必须内容详细完整，不得使用省略号或\"...\"代替实际内容，总字数不少于800字。\n" +
                "请严格按照以下JSON格式返回，不要有其他内容（字段值中换行用\\n表示）：\n" +
                "{" +
                "\"objectives\":\"【教学目标】\\n1.知识与技能目标：...（具体描述学生学完后能掌握什么知识、形成什么技能）\\n2.过程与方法目标：...（描述通过什么学习过程培养什么能力）\\n3.情感态度目标：...（描述培养什么情感或学科素养）\"," +
                "\"keyPoints\":\"【重点难点】\\n教学重点：...（列出2-3个核心知识点，说明重要性）\\n教学难点：...（列出1-2个易混淆或理解困难的点，并说明突破策略）\"," +
                "\"framework\":\"【知识讲解框架】\\n1.课堂导入（5分钟）：...（具体描述用什么情境或问题引入，激发学生兴趣）\\n2.新知讲解（15分钟）：...（详细说明概念定义、公式推导或原理分析的讲解步骤）\\n3.例题讲解（10分钟）：...（说明例题类型和讲解思路）\\n4.学生练习（10分钟）：...（说明练习内容和组织方式）\\n5.总结归纳（5分钟）：...（说明如何帮学生构建知识体系）\"," +
                "\"examples\":\"【例题（含解析）】\\n例题一（基础题）：\\n题目：...（完整题目）\\n解题思路：...（分析题目条件）\\n解题步骤：...（逐步写出完整解题过程）\\n答案：...\\n\\n例题二（提高题）：\\n题目：...（完整题目）\\n解题思路：...\\n解题步骤：...（逐步写出完整解题过程）\\n答案：...\\n常见错误提示：...\"," +
                "\"homework\":\"【课后作业建议】\\n（★基础）第1题：...（完整题目内容）\\n（★基础）第2题：...\\n（★★提高）第3题：...（完整题目内容）\\n（★★提高）第4题：...\\n（★★★拓展）第5题：...（完整题目内容）\\n作业说明：...（说明完成要求和评分标准）\"" +
                "}";

        String aiResponse = callQwenAPI(prompt);
        long duration = System.currentTimeMillis() - startTime;

        Map<String, Object> lessonPlan = new HashMap<>();
        if (aiResponse != null) {
            try {
                String cleaned = aiResponse;
                if (cleaned.contains("```json")) cleaned = cleaned.replaceAll("```json\\s*", "").replaceAll("```\\s*", "");
                else if (cleaned.contains("```")) cleaned = cleaned.replaceAll("```\\s*", "");
                cleaned = cleaned.trim();
                int start = cleaned.indexOf('{');
                int end = cleaned.lastIndexOf('}');
                if (start >= 0 && end > start) cleaned = cleaned.substring(start, end + 1);
                JsonNode node = objectMapper.readTree(cleaned);
                lessonPlan.put("objectives", node.path("objectives").asText());
                lessonPlan.put("keyPoints", node.path("keyPoints").asText());
                lessonPlan.put("framework", node.path("framework").asText());
                lessonPlan.put("examples", node.path("examples").asText());
                lessonPlan.put("homework", node.path("homework").asText());
            } catch (Exception e) {
                lessonPlan.put("objectives", aiResponse);
                lessonPlan.put("keyPoints", "");
                lessonPlan.put("framework", "");
                lessonPlan.put("examples", "");
                lessonPlan.put("homework", "");
            }
        } else {
            lessonPlan.put("objectives", "掌握" + title + "的基本概念和方法");
            lessonPlan.put("keyPoints", title + "的核心知识点");
            lessonPlan.put("framework", "1. 引入 → 2. 概念讲解 → 3. 例题演示 → 4. 练习巩固");
            lessonPlan.put("examples", "例1：基础应用题\n例2：综合提高题");
            lessonPlan.put("homework", "完成教材相关练习题");
        }

        step2.put("status", "completed");
        step2.put("duration", duration + "ms");
        step2.put("details", Map.of("tool", "Qwen AI API", "model", qwenModel));

        Map<String, Object> step3 = new HashMap<>();
        step3.put("stepId", 3);
        step3.put("stepName", "保存教案");
        step3.put("description", "将教案保存到数据库");
        step3.put("status", "running");
        step3.put("timestamp", System.currentTimeMillis());
        executionSteps.add(step3);

        String summaryJson;
        try {
            summaryJson = objectMapper.writeValueAsString(lessonPlan);
        } catch (Exception e) {
            summaryJson = lessonPlan.toString();
        }

        KnowledgePoint kp = createKnowledgePoint(userId, title, content, summaryJson, "LESSON_PLAN");

        step3.put("status", "completed");
        step3.put("details", Map.of("knowledgePointId", kp.getId()));

        Map<String, Object> result = new HashMap<>();
        result.put("knowledgePointId", kp.getId());
        result.put("title", title);
        result.put("gradeLevel", gradeLevel);
        result.put("lessonPlan", lessonPlan);
        result.put("executionSteps", executionSteps);
        result.put("totalDuration", duration + "ms");
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
            requestBodyMap.put("max_tokens", 4000);

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
