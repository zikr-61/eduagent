CREATE DATABASE IF NOT EXISTS education_ai_db;
USE education_ai_db;

-- 用户表
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    user_type VARCHAR(20) NOT NULL, -- student or teacher
    name VARCHAR(100) NOT NULL,
    grade VARCHAR(20), -- 学生年级
    subject VARCHAR(50), -- 教师学科
    school VARCHAR(100), -- 教师学校
    phone VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 学习记录表
CREATE TABLE IF NOT EXISTS study_records (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    activity_type VARCHAR(50) NOT NULL,
    activity_id INT,
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    duration_minutes INT NOT NULL DEFAULT 0,
    record_date DATE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 知识点表
CREATE TABLE IF NOT EXISTS knowledge_points (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT,
    summary TEXT,
    file_name VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 习题表
CREATE TABLE IF NOT EXISTS questions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    knowledge_point_id INT NOT NULL,
    question_text TEXT NOT NULL,
    question_type VARCHAR(50) NOT NULL,
    options TEXT,
    answer VARCHAR(255),
    difficulty INT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (knowledge_point_id) REFERENCES knowledge_points(id) ON DELETE CASCADE
);

-- 作业表
CREATE TABLE IF NOT EXISTS homework (
    id INT AUTO_INCREMENT PRIMARY KEY,
    teacher_id INT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    difficulty INT DEFAULT 1, -- 难度级别：1-简单，2-中等，3-困难
    due_date DATE,
    average_duration INT, -- 平均完成时长（分钟）
    completion_rate DOUBLE, -- 完成率
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (teacher_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 用户答题记录表
CREATE TABLE IF NOT EXISTS user_answers (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    question_id INT NOT NULL,
    user_answer VARCHAR(255),
    is_correct BOOLEAN DEFAULT FALSE,
    answered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE
);

-- 学生作业表（与 JPA 实体字段一致）
CREATE TABLE IF NOT EXISTS student_homework (
    id INT AUTO_INCREMENT PRIMARY KEY,
    student_id INT NOT NULL,
    homework_id INT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'pending', -- pending, completed
    completion_time INT, -- 完成时长（分钟）
    score INT,
    submit_time TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (homework_id) REFERENCES homework(id) ON DELETE CASCADE,
    UNIQUE KEY unique_student_homework (student_id, homework_id)
);

-- 错题本：知识点生成习题中答错的题目，供学生复习
CREATE TABLE IF NOT EXISTS error_questions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    student_id INT NOT NULL,
    question_id INT NOT NULL,
    error_time DATETIME NOT NULL,
    error_count INT NOT NULL DEFAULT 1,
    last_error_time DATETIME,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE,
    UNIQUE KEY unique_student_question (student_id, question_id)
);

-- 插入默认管理员用户
INSERT INTO users (username, password, user_type, name, phone)
VALUES ('admin', '123456', 'teacher', '管理员', '13800138000');

-- 插入测试学生用户
INSERT INTO users (username, password, user_type, name, grade, phone)
VALUES ('student1', '123456', 'student', '张三', '高一', '13800138001'),
       ('student2', '123456', 'student', '李四', '高二', '13800138002');

-- 插入测试教师用户
INSERT INTO users (username, password, user_type, name, subject, school, phone)
VALUES ('teacher1', '123456', 'teacher', '王老师', '数学', '第一中学', '13800138003'),
       ('teacher2', '123456', 'teacher', '李老师', '英语', '第二中学', '13800138004');
