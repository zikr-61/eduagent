package com.example.eaibackend.service;

import com.example.eaibackend.model.Homework;
import com.example.eaibackend.repository.HomeworkRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;

@Service
public class HomeworkService {

    @Autowired
    private HomeworkRepository homeworkRepository;

    public Homework createHomework(Integer userId, String title, String description, Integer priority, LocalDate dueDate) {
        Homework homework = new Homework();
        homework.setUserId(userId);
        homework.setTitle(title);
        homework.setDescription(description);
        homework.setPriority(priority != null ? priority : 1);
        homework.setDueDate(dueDate);
        homework.setIsCompleted(false);
        return homeworkRepository.save(homework);
    }

    public List<Homework> getUserHomeworkList(Integer userId) {
        return homeworkRepository.findByUserIdOrderByPriorityDescCreatedAtDesc(userId);
    }

    public List<Homework> getPendingHomework(Integer userId) {
        return homeworkRepository.findByUserIdAndIsCompleted(userId, false);
    }

    public Homework getHomeworkById(Integer id) {
        return homeworkRepository.findById(id).orElse(null);
    }

    @Transactional
    public Homework updateHomework(Integer id, String title, String description, Integer priority, LocalDate dueDate) {
        Homework homework = homeworkRepository.findById(id).orElse(null);
        if (homework != null) {
            if (title != null) homework.setTitle(title);
            if (description != null) homework.setDescription(description);
            if (priority != null) homework.setPriority(priority);
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

    @Transactional
    public Homework markAsCompleted(Integer id) {
        Homework homework = homeworkRepository.findById(id).orElse(null);
        if (homework != null) {
            homework.setIsCompleted(true);
            return homeworkRepository.save(homework);
        }
        return null;
    }

    @Transactional
    public Homework markAsIncomplete(Integer id) {
        Homework homework = homeworkRepository.findById(id).orElse(null);
        if (homework != null) {
            homework.setIsCompleted(false);
            return homeworkRepository.save(homework);
        }
        return null;
    }

    @Transactional
    public Homework setReminder(Integer id, LocalDate reminderTime) {
        Homework homework = homeworkRepository.findById(id).orElse(null);
        if (homework != null) {
            homework.setReminderTime(reminderTime.atStartOfDay());
            return homeworkRepository.save(homework);
        }
        return null;
    }
}
