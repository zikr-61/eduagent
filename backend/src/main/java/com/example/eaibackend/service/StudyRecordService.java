package com.example.eaibackend.service;

import com.example.eaibackend.model.StudyRecord;
import com.example.eaibackend.repository.StudyRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StudyRecordService {

    @Autowired
    private StudyRecordRepository studyRecordRepository;

    public StudyRecord addStudyRecord(Integer userId, Integer durationMinutes) {
        StudyRecord record = new StudyRecord();
        record.setUserId(userId);
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
            if (!record.getRecordDate().isBefore(start) && !record.getRecordDate().isAfter(end)) {
                minutes += record.getDurationMinutes();
            }
        }
        return minutes;
    }
}
