package com.example.eaibackend.controller;

import com.example.eaibackend.service.KnowledgePointService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/knowledge")
public class KnowledgePointController {

    @Autowired
    private KnowledgePointService knowledgePointService;

    @PostMapping("/create")
    public Map<String, Object> createKnowledgePoint(
            @RequestParam Integer userId,
            @RequestParam String title,
            @RequestParam String content,
            @RequestParam(required = false) String summary,
            @RequestParam(required = false) String fileName) {
        return Map.of("data", knowledgePointService.createKnowledgePoint(userId, title, content, summary, fileName));
    }

    @GetMapping("/list")
    public Map<String, Object> getKnowledgePoints(@RequestParam Integer userId) {
        return Map.of("data", knowledgePointService.getUserKnowledgePoints(userId));
    }

    @GetMapping("/{id}")
    public Map<String, Object> getKnowledgePoint(@PathVariable Integer id) {
        return Map.of("data", knowledgePointService.getKnowledgePointById(id));
    }

    @GetMapping("/{id}/questions")
    public Map<String, Object> getQuestions(@PathVariable Integer id) {
        return Map.of("data", knowledgePointService.getQuestionsByKnowledgePointId(id));
    }

    @PostMapping("/generate")
    public Map<String, Object> generateSummaryAndQuestions(
            @RequestParam Integer userId,
            @RequestParam String title,
            @RequestParam String content,
            @RequestParam(required = false) String fileName) {
        return Map.of("data", knowledgePointService.generateSummaryAndQuestions(userId, title, content, fileName));
    }

    @PostMapping("/generate-plan")
    public Map<String, Object> generateLessonPlan(
            @RequestParam Integer userId,
            @RequestParam String title,
            @RequestParam(required = false) String gradeLevel,
            @RequestParam String content) {
        return Map.of("data", knowledgePointService.generateLessonPlan(userId, title, gradeLevel, content));
    }
}
