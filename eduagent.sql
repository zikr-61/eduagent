CREATE DATABASE education_ai_db;
USE education_ai_db;
CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,     -- 用户唯一标识符
    username VARCHAR(100) NOT NULL UNIQUE,  -- 用户名，必须唯一
    password VARCHAR(255) NOT NULL,         -- 密码
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP  -- 注册时间
);
INSERT INTO users (username, password)
VALUES ('admin', '123456');