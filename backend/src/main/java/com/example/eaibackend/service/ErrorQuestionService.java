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
        if (q == null) return false;
        KnowledgePoint kp = knowledgePointRepository.findById(q.getKnowledgePointId()).orElse(null);
        return kp != null && studentId.equals(kp.getUserId());
    }

    @Transactional
    public ErrorQuestion recordError(Integer studentId, Integer questionId) {
        if (!canStudentAccessQuestion(studentId, questionId)) {
            return null;
        }
        // 获取题目和知识点快照
        Question q = questionRepository.findById(questionId).orElse(null);
        KnowledgePoint kp = q != null ? knowledgePointRepository.findById(q.getKnowledgePointId()).orElse(null) : null;

        LocalDateTime now = LocalDateTime.now();
        ErrorQuestion existing = errorQuestionRepository.findByStudentIdAndQuestionId(studentId, questionId);
        if (existing != null) {
            existing.setErrorCount(existing.getErrorCount() + 1);
            existing.setLastErrorTime(now);
            // 更新快照（题目内容可能变化）
            if (q != null) {
                existing.setQuestionTextSnapshot(q.getQuestionText());
                existing.setOptionsSnapshot(q.getOptions());
                existing.setCorrectAnswerSnapshot(q.getAnswer());
            }
            if (kp != null) existing.setKnowledgeTitleSnapshot(kp.getTitle());
            return errorQuestionRepository.save(existing);
        }
        ErrorQuestion eq = new ErrorQuestion();
        eq.setStudentId(studentId);
        eq.setQuestionId(questionId);
        eq.setErrorTime(now);
        eq.setErrorCount(1);
        eq.setLastErrorTime(now);
        // 保存快照
        if (q != null) {
            eq.setQuestionTextSnapshot(q.getQuestionText());
            eq.setOptionsSnapshot(q.getOptions());
            eq.setCorrectAnswerSnapshot(q.getAnswer());
        }
        if (kp != null) eq.setKnowledgeTitleSnapshot(kp.getTitle());
        return errorQuestionRepository.save(eq);
    }

    public List<Map<String, Object>> listForStudent(Integer studentId) {
        List<ErrorQuestion> errors = errorQuestionRepository.findByStudentId(studentId);
        List<Map<String, Object>> out = new ArrayList<>();
        for (ErrorQuestion eq : errors) {
            // 优先从数据库获取最新题目，不存在时用快照
            Question q = questionRepository.findById(eq.getQuestionId()).orElse(null);
            KnowledgePoint kp = q != null
                    ? knowledgePointRepository.findById(q.getKnowledgePointId()).orElse(null)
                    : null;

            // 如果题目存在但不属于该学生，跳过
            if (q != null && !canStudentAccessQuestion(studentId, q.getId())) continue;

            Map<String, Object> row = new HashMap<>();
            row.put("id", eq.getId());
            row.put("questionId", eq.getQuestionId());
            // 题目内容：优先实时数据，降级到快照
            row.put("questionText", q != null ? q.getQuestionText() : eq.getQuestionTextSnapshot());
            row.put("options", q != null ? q.getOptions() : eq.getOptionsSnapshot());
            row.put("answer", q != null ? q.getAnswer() : eq.getCorrectAnswerSnapshot());
            row.put("knowledgePointId", q != null ? q.getKnowledgePointId() : null);
            row.put("knowledgeTitle", kp != null ? kp.getTitle() : eq.getKnowledgeTitleSnapshot());
            row.put("errorCount", eq.getErrorCount());
            row.put("errorTime", eq.getErrorTime());
            row.put("lastErrorTime", eq.getLastErrorTime());
            out.add(row);
        }
        return out;
    }

    @Transactional
    public boolean removeByStudentAndQuestion(Integer studentId, Integer questionId) {
        // 如果题目已被删除，仍允许学生移除错题记录（通过快照验证）
        ErrorQuestion existing = errorQuestionRepository.findByStudentIdAndQuestionId(studentId, questionId);
        if (existing == null) return false;
        // 验证：若题目仍存在，检查归属权；若题目已删除，直接允许移除
        Question q = questionRepository.findById(questionId).orElse(null);
        if (q != null && !canStudentAccessQuestion(studentId, questionId)) return false;
        errorQuestionRepository.delete(existing);
        return true;
    }
}
