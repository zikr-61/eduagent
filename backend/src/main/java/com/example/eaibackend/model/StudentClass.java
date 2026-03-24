package com.example.eaibackend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "student_class")
@Data
public class StudentClass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "student_id", nullable = false)
    private Integer studentId;

    @Column(name = "class_id", nullable = false)
    private Integer classId;

    @Column(name = "join_time")
    private LocalDateTime joinTime;

    @PrePersist
    protected void onCreate() {
        joinTime = LocalDateTime.now();
    }
}