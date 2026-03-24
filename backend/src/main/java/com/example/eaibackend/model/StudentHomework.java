package com.example.eaibackend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "student_homework")
@Data
public class StudentHomework {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "student_id", nullable = false)
    private Integer studentId;

    @Column(name = "homework_id", nullable = false)
    private Integer homeworkId;

    @Column(name = "status", nullable = false)
    private String status; // pending, completed

    @Column(name = "submit_time")
    private LocalDateTime submitTime;

    @Column(name = "score")
    private Integer score;

    @Column(name = "completion_time")
    private Integer completionTime; // 完成时长（分钟）

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}