package com.example.eaibackend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "error_questions")
@Data
public class ErrorQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "student_id", nullable = false)
    private Integer studentId;

    @Column(name = "question_id", nullable = false)
    private Integer questionId;

    // 快照字段：记录时保存题目内容，防止原题被删除后丢失
    @Column(name = "question_text_snapshot", columnDefinition = "TEXT")
    private String questionTextSnapshot;

    @Column(name = "options_snapshot", columnDefinition = "TEXT")
    private String optionsSnapshot;

    @Column(name = "correct_answer_snapshot")
    private String correctAnswerSnapshot;

    @Column(name = "knowledge_title_snapshot")
    private String knowledgeTitleSnapshot;

    @Column(name = "error_time", nullable = false)
    private LocalDateTime errorTime;

    @Column(name = "error_count", nullable = false)
    private Integer errorCount = 1;

    @Column(name = "last_error_time")
    private LocalDateTime lastErrorTime;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (errorTime == null) {
            errorTime = LocalDateTime.now();
        }
        if (lastErrorTime == null) {
            lastErrorTime = errorTime;
        }
    }
}
