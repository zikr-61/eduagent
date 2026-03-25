package com.example.eaibackend.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.ColumnDefault;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "homework")
@Data
public class Homework {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "teacher_id", nullable = false)
    private Integer teacherId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ColumnDefault("1")
    private Integer difficulty = 1; // 1-简单, 2-中等, 3-困难

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "average_duration")
    private Integer averageDuration; // 平均完成时长（分钟）

    @Column(name = "completion_rate")
    private Double completionRate; // 完成率

    @Column(name = "knowledge_point_id")
    private Integer knowledgePointId; // 可选关联知识点（方案B轻量挂载）

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
