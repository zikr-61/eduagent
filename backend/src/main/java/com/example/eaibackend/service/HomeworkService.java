package com.example.eaibackend.service;

import com.example.eaibackend.model.Homework;
import com.example.eaibackend.model.StudentHomework;
import com.example.eaibackend.model.User;
import com.example.eaibackend.repository.HomeworkRepository;
import com.example.eaibackend.repository.StudentHomeworkRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class HomeworkService {

    @Autowired
    private HomeworkRepository homeworkRepository;
    
    @Autowired
    private StudentHomeworkRepository studentHomeworkRepository;

    @Autowired
    private com.example.eaibackend.repository.UserRepository userRepository;

    @Autowired
    private StudyRecordService studyRecordService;

    public Homework createHomework(Integer teacherId, String title, String description, Integer difficulty, LocalDate dueDate) {
        Homework homework = new Homework();
        homework.setTeacherId(teacherId);
        homework.setTitle(title);
        homework.setDescription(description);
        homework.setDifficulty(difficulty != null ? difficulty : 1);
        homework.setDueDate(dueDate);
        return homeworkRepository.save(homework);
    }

    public List<Homework> getTeacherHomeworkList(Integer teacherId) {
        return homeworkRepository.findByTeacherIdOrderByCreatedAtDesc(teacherId);
    }

    public Homework getHomeworkById(Integer id) {
        return homeworkRepository.findById(id).orElse(null);
    }

    @Transactional
    public Homework updateHomework(Integer id, String title, String description, Integer difficulty, LocalDate dueDate) {
        Homework homework = homeworkRepository.findById(id).orElse(null);
        if (homework != null) {
            if (title != null) homework.setTitle(title);
            if (description != null) homework.setDescription(description);
            if (difficulty != null) homework.setDifficulty(difficulty);
            if (dueDate != null) homework.setDueDate(dueDate);
            return homeworkRepository.save(homework);
        }
        return null;
    }

    @Transactional
    public boolean deleteHomework(Integer id) {
        if (homeworkRepository.existsById(id)) {
            homeworkRepository.deleteById(id);
            return true;
        }
        return false;
    }

    // 布置作业给学生
    @Transactional
    public StudentHomework assignHomework(Integer homeworkId, Integer studentId) {
        StudentHomework existing = studentHomeworkRepository.findByStudentIdAndHomeworkId(studentId, homeworkId);
        if (existing != null) {
            return existing;
        }
        StudentHomework studentHomework = new StudentHomework();
        studentHomework.setHomeworkId(homeworkId);
        studentHomework.setStudentId(studentId);
        studentHomework.setStatus("pending");
        return studentHomeworkRepository.save(studentHomework);
    }

    /** 布置给系统中全部学生 */
    @Transactional
    public Homework assignHomeworkToAllStudents(Integer homeworkId, Integer teacherId) {
        Homework homework = homeworkRepository.findById(homeworkId).orElse(null);
        if (homework == null || !homework.getTeacherId().equals(teacherId)) {
            return null;
        }
        List<User> students = userRepository.findByUserType("student");
        for (User u : students) {
            assignHomework(homeworkId, u.getId());
        }
        return homework;
    }

    /** 教师查看某作业下各学生完成情况 */
    public List<Map<String, Object>> getHomeworkCompletionDetail(Integer homeworkId, Integer teacherId) {
        Homework homework = homeworkRepository.findById(homeworkId).orElse(null);
        if (homework == null || !homework.getTeacherId().equals(teacherId)) {
            return null;
        }
        List<StudentHomework> rows = studentHomeworkRepository.findByHomeworkId(homeworkId);
        List<Map<String, Object>> out = new ArrayList<>();
        for (StudentHomework sh : rows) {
            User u = userRepository.findById(sh.getStudentId()).orElse(null);
            Map<String, Object> row = new HashMap<>();
            row.put("studentId", sh.getStudentId());
            row.put("studentName", u != null ? u.getName() : "");
            row.put("username", u != null ? u.getUsername() : "");
            row.put("status", sh.getStatus());
            row.put("completionTime", sh.getCompletionTime());
            row.put("submitTime", sh.getSubmitTime());
            out.add(row);
        }
        return out;
    }

    private List<Map<String, Object>> enrichStudentHomeworkRows(List<StudentHomework> rows) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (StudentHomework sh : rows) {
            Homework h = homeworkRepository.findById(sh.getHomeworkId()).orElse(null);
            Map<String, Object> m = new HashMap<>();
            m.put("id", sh.getId());
            m.put("studentId", sh.getStudentId());
            m.put("homeworkId", sh.getHomeworkId());
            m.put("status", sh.getStatus());
            m.put("completionTime", sh.getCompletionTime());
            m.put("submitTime", sh.getSubmitTime());
            m.put("homework", h);
            out.add(m);
        }
        return out;
    }

    /** 学生端列表（含作业详情） */
    public List<Map<String, Object>> getStudentHomeworkList(Integer studentId) {
        return enrichStudentHomeworkRows(studentHomeworkRepository.findByStudentId(studentId));
    }

    public List<Map<String, Object>> getPendingStudentHomework(Integer studentId) {
        return enrichStudentHomeworkRows(studentHomeworkRepository.findByStudentIdAndStatus(studentId, "pending"));
    }

    // 学生完成作业：记录状态并写入学习时长统计
    @Transactional
    public StudentHomework submitHomework(Integer studentId, Integer homeworkId, Integer completionTime) {
        StudentHomework studentHomework = studentHomeworkRepository.findByStudentIdAndHomeworkId(studentId, homeworkId);
        if (studentHomework != null) {
            studentHomework.setStatus("completed");
            studentHomework.setSubmitTime(java.time.LocalDateTime.now());
            studentHomework.setCompletionTime(completionTime);
            StudentHomework saved = studentHomeworkRepository.save(studentHomework);
            studyRecordService.addHomeworkStudyRecord(studentId, homeworkId, completionTime);
            return saved;
        }
        return null;
    }

    // 计算作业完成率
    public double calculateCompletionRate(Integer homeworkId) {
        List<StudentHomework> studentHomeworks = studentHomeworkRepository.findByHomeworkId(homeworkId);
        if (studentHomeworks.isEmpty()) {
            return 0;
        }
        long completedCount = studentHomeworks.stream()
                .filter(sh -> "completed".equals(sh.getStatus()))
                .count();
        return (double) completedCount / studentHomeworks.size() * 100;
    }
}

