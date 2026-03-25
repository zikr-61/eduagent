package com.example.eaibackend.controller;

import com.example.eaibackend.service.StudyRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/study")
public class StudyRecordController {

    @Autowired
    private StudyRecordService studyRecordService;

    @PostMapping("/start")
    public Map<String, Object> startLearning(@RequestParam Integer userId, 
                                           @RequestParam String activityType, 
                                           @RequestParam(required = false) Integer activityId) {
        return studyRecordService.startLearning(userId, activityType, activityId);
    }

    @PostMapping("/end")
    public Map<String, Object> endLearning(@RequestParam Integer userId, 
                                         @RequestParam String activityType, 
                                         @RequestParam(required = false) Integer activityId, 
                                         @RequestParam String startTime) {
        LocalDateTime start = LocalDateTime.parse(startTime);
        LocalDateTime end = LocalDateTime.now();
        return Map.of("record", studyRecordService.endLearning(userId, activityType, activityId, start, end));
    }

    @PostMapping("/add")
    public Map<String, Object> addStudyRecord(@RequestParam Integer userId, @RequestParam Integer durationMinutes) {
        return Map.of("record", studyRecordService.addStudyRecord(userId, durationMinutes));
    }

    @GetMapping("/list")
    public Map<String, Object> getStudyRecords(@RequestParam Integer userId) {
        return Map.of("records", studyRecordService.getUserStudyRecords(userId));
    }

    @GetMapping("/total")
    public Map<String, Object> getTotalDuration(@RequestParam Integer userId) {
        int totalMinutes = studyRecordService.getTotalStudyDuration(userId);
        double totalHours = Math.round(totalMinutes / 60.0 * 10) / 10.0;
        return Map.of("totalMinutes", totalMinutes, "totalHours", totalHours);
    }

    @GetMapping("/weekly-report")
    public Map<String, Object> getWeeklyReport(@RequestParam Integer userId) {
        return studyRecordService.getWeeklyReport(userId);
    }

    @GetMapping("/monthly-report")
    public Map<String, Object> getMonthlyReport(@RequestParam Integer userId) {
        return studyRecordService.getMonthlyReport(userId);
    }

    @GetMapping("/all-students")
    public Map<String, Object> getAllStudentsStudyStats() {
        return Map.of("studentsStats", studyRecordService.getAllStudentsStudyStats());
    }
}
