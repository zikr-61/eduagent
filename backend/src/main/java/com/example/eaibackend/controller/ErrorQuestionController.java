package com.example.eaibackend.controller;

import com.example.eaibackend.model.ErrorQuestion;
import com.example.eaibackend.service.ErrorQuestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/error-questions")
public class ErrorQuestionController {

    @Autowired
    private ErrorQuestionService errorQuestionService;

    @PostMapping("/record")
    public Map<String, Object> recordError(@RequestParam Integer studentId, @RequestParam Integer questionId) {
        ErrorQuestion eq = errorQuestionService.recordError(studentId, questionId);
        if (eq == null) {
            return Map.of("success", false, "message", "无权记录或题目不存在");
        }
        return Map.of("success", true, "data", eq);
    }

    @GetMapping("/list")
    public Map<String, Object> list(@RequestParam Integer studentId) {
        return Map.of("data", errorQuestionService.listForStudent(studentId));
    }

    @DeleteMapping("/remove")
    public Map<String, Object> remove(@RequestParam Integer studentId, @RequestParam Integer questionId) {
        boolean ok = errorQuestionService.removeByStudentAndQuestion(studentId, questionId);
        return Map.of("success", ok);
    }
}
