package com.example.eaibackend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;  // 使用 Lombok 注解

@Entity
@Data  // Lombok 自动为所有字段生成 getter、setter、toString、equals 和 hashCode 方法
public class User {

    @Id
    private Long id;

    private String username;

    private String password;
}
