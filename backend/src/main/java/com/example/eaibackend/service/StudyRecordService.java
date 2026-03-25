package com.example.eaibackend.service;

import com.example.eaibackend.model.StudyRecord;
import com.example.eaibackend.model.User;
import com.example.eaibackend.repository.StudyRecordRepository;
import com.example.eaibackend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StudyRecordService {

    @Autowired
    private StudyRecordRepository studyRecordRepository;
    
    @Autowired
    private UserRepository userRepository;

    // 开始学习活动
    public Map<String, Object> startLearning(Integer userId, String activityType, Integer activityId) {
        Map<String, Object> response = new HashMap<>();
        response.put("startTime", LocalDateTime.now());
        response.put("userId", userId);
        response.put("activityType", activityType);
        response.put("activityId", activityId);
        return response;
    }

    // 结束学习活动并保存记录
    public StudyRecord endLearning(Integer userId, String activityType, Integer activityId, 
                                  LocalDateTime startTime, LocalDateTime endTime) {
        StudyRecord record = new StudyRecord();
        record.setUserId(userId);
        record.setActivityType(activityType);
        record.setActivityId(activityId);
        record.setStartTime(startTime);
        record.setEndTime(endTime);
        
        // 计算时长（分钟）
        long minutes = ChronoUnit.MINUTES.between(startTime, endTime);
        record.setDurationMinutes((int) minutes);
        record.setRecordDate(LocalDate.now());
        
        return studyRecordRepository.save(record);
    }

    public StudyRecord addStudyRecord(Integer userId, Integer durationMinutes) {
        StudyRecord record = new StudyRecord();
        record.setUserId(userId);
        record.setActivityType("manual");
        record.setDurationMinutes(durationMinutes);
        record.setStartTime(LocalDateTime.now().minusMinutes(durationMinutes));
        record.setEndTime(LocalDateTime.now());
        record.setRecordDate(LocalDate.now());
        return studyRecordRepository.save(record);
    }

    /** 学生完成作业时记录学习时长，计入学习成果统计 */
    public StudyRecord addHomeworkStudyRecord(Integer userId, Integer homeworkId, Integer durationMinutes) {
        if (durationMinutes == null || durationMinutes <= 0) {
            durationMinutes = 1;
        }
        StudyRecord record = new StudyRecord();
        record.setUserId(userId);
        record.setActivityType("homework");
        record.setActivityId(homeworkId);
        LocalDateTime end = LocalDateTime.now();
        record.setEndTime(end);
        record.setStartTime(end.minusMinutes(durationMinutes));
        record.setDurationMinutes(durationMinutes);
        record.setRecordDate(LocalDate.now());
        return studyRecordRepository.save(record);
    }

    public List<StudyRecord> getUserStudyRecords(Integer userId) {
        return studyRecordRepository.findByUserId(userId);
    }

    public Integer getTotalStudyDuration(Integer userId) {
        Integer totalMinutes = studyRecordRepository.getTotalDurationByUserId(userId);
        return totalMinutes != null ? totalMinutes : 0;
    }

    public Map<String, Object> getWeeklyReport(Integer userId) {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.minusDays(6);

        List<StudyRecord> weeklyRecords = studyRecordRepository.findByUserIdAndRecordDateBetween(userId, weekStart, today);

        int totalMinutes = 0;
        Map<String, Integer> dailyStudyMap = new HashMap<>();

        for (LocalDate date = weekStart; !date.isAfter(today); date = date.plusDays(1)) {
            dailyStudyMap.put(date.toString(), 0);
        }

        for (StudyRecord record : weeklyRecords) {
            // 只统计完成作业后的用时
            if (!"homework".equals(record.getActivityType())) continue;
            totalMinutes += record.getDurationMinutes();
            dailyStudyMap.put(record.getRecordDate().toString(),
                dailyStudyMap.getOrDefault(record.getRecordDate().toString(), 0) + record.getDurationMinutes());
        }

        Map<String, Object> report = new HashMap<>();
        report.put("startDate", weekStart.toString());
        report.put("endDate", today.toString());
        report.put("totalMinutes", totalMinutes);
        report.put("totalHours", Math.round(totalMinutes / 60.0 * 10) / 10.0);
        report.put("dailyStudy", dailyStudyMap);

        return report;
    }

    public Map<String, Object> getMonthlyReport(Integer userId) {
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);

        List<StudyRecord> monthlyRecords = studyRecordRepository.findByUserIdAndRecordDateBetween(userId, monthStart, today);

        int totalMinutes = 0;
        Map<String, Integer> weeklyStudyMap = new HashMap<>();

        for (StudyRecord record : monthlyRecords) {
            // 只统计完成作业后的用时
            if (!"homework".equals(record.getActivityType())) continue;
            totalMinutes += record.getDurationMinutes();
        }

        LocalDate week1Start = monthStart;
        LocalDate week1End = monthStart.plusDays(6);
        LocalDate week2Start = week1End.plusDays(1);
        LocalDate week2End = week2Start.plusDays(6);
        LocalDate week3Start = week2End.plusDays(1);
        LocalDate week3End = week3Start.plusDays(6);
        LocalDate week4Start = week3End.plusDays(1);
        LocalDate week4End = today.isBefore(week4Start) ? today : week4Start.plusDays(6);

        weeklyStudyMap.put("week1", calculateWeekMinutes(monthlyRecords, week1Start, week1End));
        weeklyStudyMap.put("week2", calculateWeekMinutes(monthlyRecords, week2Start, week2End));
        weeklyStudyMap.put("week3", calculateWeekMinutes(monthlyRecords, week3Start, week3End));
        weeklyStudyMap.put("week4", calculateWeekMinutes(monthlyRecords, week4Start, week4End));

        Map<String, Object> report = new HashMap<>();
        report.put("startDate", monthStart.toString());
        report.put("endDate", today.toString());
        report.put("totalMinutes", totalMinutes);
        report.put("totalHours", Math.round(totalMinutes / 60.0 * 10) / 10.0);
        report.put("weeklyStudy", weeklyStudyMap);

        return report;
    }

    private int calculateWeekMinutes(List<StudyRecord> records, LocalDate start, LocalDate end) {
        int minutes = 0;
        for (StudyRecord record : records) {
            if (!"homework".equals(record.getActivityType())) continue;
            if (!record.getRecordDate().isBefore(start) && !record.getRecordDate().isAfter(end)) {
                minutes += record.getDurationMinutes();
            }
        }
        return minutes;
    }

    public List<Map<String, Object>> getAllStudentsStudyStats() {
        List<User> students = userRepository.findByUserType("student");
        List<Map<String, Object>> studentsStats = new ArrayList<>();
        
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.minusDays(6);
        LocalDate monthStart = today.withDayOfMonth(1);
        
        for (User student : students) {
            Map<String, Object> stats = new HashMap<>();
            stats.put("id", student.getId());
            stats.put("name", student.getName());
            stats.put("grade", student.getGrade());
            
            // 计算总学习时长
            int totalMinutes = getTotalStudyDuration(student.getId());
            double totalHours = Math.round(totalMinutes / 60.0 * 10) / 10.0;
            stats.put("totalHours", totalHours);
            
            // 计算本周学习时长
            List<StudyRecord> weeklyRecords = studyRecordRepository.findByUserIdAndRecordDateBetween(
                student.getId(), weekStart, today);
            int weeklyMinutes = weeklyRecords.stream()
                .filter(r -> "homework".equals(r.getActivityType()))
                .mapToInt(StudyRecord::getDurationMinutes)
                .sum();
            double weeklyHours = Math.round(weeklyMinutes / 60.0 * 10) / 10.0;
            stats.put("weeklyHours", weeklyHours);
            
            // 计算本月学习时长
            List<StudyRecord> monthlyRecords = studyRecordRepository.findByUserIdAndRecordDateBetween(
                student.getId(), monthStart, today);
            int monthlyMinutes = monthlyRecords.stream()
                .filter(r -> "homework".equals(r.getActivityType()))
                .mapToInt(StudyRecord::getDurationMinutes)
                .sum();
            double monthlyHours = Math.round(monthlyMinutes / 60.0 * 10) / 10.0;
            stats.put("monthlyHours", monthlyHours);
            
            studentsStats.add(stats);
        }
        
        return studentsStats;
    }
}
