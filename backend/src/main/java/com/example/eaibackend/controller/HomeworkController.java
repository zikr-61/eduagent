package com.example.eaibackend.controller;

import com.example.eaibackend.service.HomeworkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/homework")
public class HomeworkController {

    @Autowired
    private HomeworkService homeworkService;

    @PostMapping("/create")
    public Map<String, Object> createHomework(
            @RequestParam Integer userId,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) Integer priority,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueDate) {
        return Map.of("data", homeworkService.createHomework(userId, title, description, priority, dueDate));
    }

    @GetMapping("/list")
    public Map<String, Object> getHomeworkList(@RequestParam Integer userId) {
        return Map.of("data", homeworkService.getUserHomeworkList(userId));
    }

    @GetMapping("/pending")
    public Map<String, Object> getPendingHomework(@RequestParam Integer userId) {
        return Map.of("data", homeworkService.getPendingHomework(userId));
    }

    @GetMapping("/{id}")
    public Map<String, Object> getHomework(@PathVariable Integer id) {
        return Map.of("data", homeworkService.getHomeworkById(id));
    }

    @PutMapping("/update/{id}")
    public Map<String, Object> updateHomework(
            @PathVariable Integer id,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) Integer priority,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueDate) {
        return Map.of("data", homeworkService.updateHomework(id, title, description, priority, dueDate));
    }

    @DeleteMapping("/delete/{id}")
    public Map<String, Object> deleteHomework(@PathVariable Integer id) {
        boolean deleted = homeworkService.deleteHomework(id);
        return Map.of("success", deleted, "message", deleted ? "删除成功" : "删除失败");
    }

    @PutMapping("/complete/{id}")
    public Map<String, Object> markAsCompleted(@PathVariable Integer id) {
        return Map.of("data", homeworkService.markAsCompleted(id));
    }

    @PutMapping("/incomplete/{id}")
    public Map<String, Object> markAsIncomplete(@PathVariable Integer id) {
        return Map.of("data", homeworkService.markAsIncomplete(id));
    }

    @PutMapping("/reminder/{id}")
    public Map<String, Object> setReminder(
            @PathVariable Integer id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate reminderTime) {
        return Map.of("data", homeworkService.setReminder(id, reminderTime));
    }
}
