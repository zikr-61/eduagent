package com.example.eaibackend.controller;

import com.example.eaibackend.model.Homework;
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
            @RequestParam Integer teacherId,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) Integer difficulty,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueDate) {
        return Map.of("data", homeworkService.createHomework(teacherId, title, description, difficulty, dueDate));
    }

    @GetMapping("/teacher/list")
    public Map<String, Object> getTeacherHomeworkList(@RequestParam Integer teacherId) {
        return Map.of("data", homeworkService.getTeacherHomeworkList(teacherId));
    }

    @GetMapping("/student/list")
    public Map<String, Object> getStudentHomeworkList(@RequestParam Integer studentId) {
        return Map.of("data", homeworkService.getStudentHomeworkList(studentId));
    }

    @GetMapping("/student/pending")
    public Map<String, Object> getPendingStudentHomework(@RequestParam Integer studentId) {
        return Map.of("data", homeworkService.getPendingStudentHomework(studentId));
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
            @RequestParam(required = false) Integer difficulty,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueDate) {
        return Map.of("data", homeworkService.updateHomework(id, title, description, difficulty, dueDate));
    }

    @DeleteMapping("/delete/{id}")
    public Map<String, Object> deleteHomework(@PathVariable Integer id) {
        boolean deleted = homeworkService.deleteHomework(id);
        return Map.of("success", deleted, "message", deleted ? "删除成功" : "删除失败");
    }

    @PostMapping("/assign")
    public Map<String, Object> assignHomework(
            @RequestParam Integer homeworkId,
            @RequestParam Integer studentId) {
        return Map.of("data", homeworkService.assignHomework(homeworkId, studentId));
    }

    /** 发布给全部学生（仅创建该作业的教师可操作） */
    @PostMapping("/assign-all")
    public Map<String, Object> assignHomeworkToAll(
            @RequestParam Integer homeworkId,
            @RequestParam Integer teacherId) {
        Homework h = homeworkService.assignHomeworkToAllStudents(homeworkId, teacherId);
        if (h == null) {
            return Map.of("success", false, "message", "作业不存在或无权限");
        }
        return Map.of("success", true, "data", h);
    }

    /** 某作业下各学生完成情况 */
    @GetMapping("/{id}/completion")
    public Map<String, Object> getHomeworkCompletion(
            @PathVariable Integer id,
            @RequestParam Integer teacherId) {
        List<Map<String, Object>> rows = homeworkService.getHomeworkCompletionDetail(id, teacherId);
        if (rows == null) {
            return Map.of("success", false, "message", "作业不存在或无权限", "data", List.of());
        }
        return Map.of("success", true, "data", rows);
    }

    @PostMapping("/submit")
    public Map<String, Object> submitHomework(
            @RequestParam Integer studentId,
            @RequestParam Integer homeworkId,
            @RequestParam Integer completionTime) {
        return Map.of("data", homeworkService.submitHomework(studentId, homeworkId, completionTime));
    }

    @GetMapping("/completion-rate/{id}")
    public Map<String, Object> getCompletionRate(@PathVariable Integer id) {
        return Map.of("completionRate", homeworkService.calculateCompletionRate(id));
    }
}

