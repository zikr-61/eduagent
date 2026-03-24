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