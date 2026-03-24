package com.example.eaibackend.service;

import com.example.eaibackend.model.ErrorQuestion;
import com.example.eaibackend.model.KnowledgePoint;
import com.example.eaibackend.model.Question;
import com.example.eaibackend.repository.ErrorQuestionRepository;
import com.example.eaibackend.repository.KnowledgePointRepository;
import com.example.eaibackend.repository.QuestionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ErrorQuestionService {

    @Autowired
    private ErrorQuestionRepository errorQuestionRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private KnowledgePointRepository knowledgePointRepository;

    private boolean canStudentAccessQuestion(Integer studentId, Integer questionId) {
        Question q = questionRepository.findById(questionId).orElse(null);
        if (q == null) {
            return false;
        }
        KnowledgePoint kp = knowledgePointRepository.findById(q.getKnowledgePointId()).orElse(null);
        return kp != null && studentId.equals(kp.getUserId());
    }

    @Transactional
    public ErrorQuestion recordError(Integer studentId, Integer questionId) {
        if (!canStudentAccessQuestion(studentId, questionId)) {
            return null;
        }
        ErrorQuestion existing = errorQuestionRepository.findByStudentIdAndQuestionId(studentId, questionId);
        LocalDateTime now = LocalDateTime.now();
        if (existing != null) {
            existing.setErrorCount(existing.getErrorCount() + 1);
            existing.setLastErrorTime(now);
            return errorQuestionRepository.save(existing);
        }
        ErrorQuestion eq = new ErrorQuestion();
        eq.setStudentId(studentId);
        eq.setQuestionId(questionId);
        eq.setErrorTime(now);
        eq.setErrorCount(1);
        eq.setLastErrorTime(now);
        return errorQuestionRepository.save(eq);
    }

    public List<Map<String, Object>> listForStudent(Integer studentId) {
        List<ErrorQuestion> errors = errorQuestionRepository.findByStudentId(studentId);
        List<Map<String, Object>> out = new ArrayList<>();
        for (ErrorQuestion eq : errors) {
            Question q = questionRepository.findById(eq.getQuestionId()).orElse(null);
            if (q == null) {
                continue;
            }
            if (!canStudentAccessQuestion(studentId, q.getId())) {
                continue;
            }
            KnowledgePoint kp = knowledgePointRepository.findById(q.getKnowledgePointId()).orElse(null);
            Map<String, Object> row = new HashMap<>();
            row.put("id", eq.getId());
            row.put("questionId", q.getId());
            row.put("questionText", q.getQuestionText());
            row.put("options", q.getOptions());
            row.put("answer", q.getAnswer());
            row.put("knowledgePointId", q.getKnowledgePointId());
            row.put("knowledgeTitle", kp != null ? kp.getTitle() : "");
            row.put("errorCount", eq.getErrorCount());
            row.put("errorTime", eq.getErrorTime());
            row.put("lastErrorTime", eq.getLastErrorTime());
            out.add(row);
        }
        return out;
    }

    @Transactional
    public boolean removeByStudentAndQuestion(Integer studentId, Integer questionId) {
        if (!canStudentAccessQuestion(studentId, questionId)) {
            return false;
        }
        ErrorQuestion existing = errorQuestionRepository.findByStudentIdAndQuestionId(studentId, questionId);
        if (existing == null) {
            return false;
        }
        errorQuestionRepository.delete(existing);
        return true;
    }
}
