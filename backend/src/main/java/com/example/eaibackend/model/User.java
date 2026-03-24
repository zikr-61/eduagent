package com.example.eaibackend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Table;
import lombok.Data;  // 使用 Lombok 注解

@Entity
@Table(name = "users")
@Data  // Lombok 自动为所有字段生成 getter、setter、toString、equals 和 hashCode 方法
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String username;

    private String password;
    
    private String userType; // student 或 teacher
    
    private String name; // 姓名
    
    // 学生特有字段
    private String grade; // 年级
    
    // 教师特有字段
    private String subject; // 学科
    private String school; // 学校
}
