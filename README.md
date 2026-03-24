# 教育AI助手项目

## 项目简介

教育AI助手是一个前后端分离的Web应用，使用Java Spring Boot作为后端，Vue.js作为前端，提供用户登录和注册功能。

## 技术栈

### 后端

- Java 17
- Spring Boot 3.5.11
- Spring Security
- Spring Data JPA
- MySQL
- Lombok

### 前端

- Vue 3
- Element Plus
- Vue Router
- Axios

## 项目结构

```
eduagent/
├── backend/            # 后端代码
│   ├── src/main/java/com/example/eaibackend/
│   │   ├── config/     # 配置类
│   │   ├── controller/ # 控制器
│   │   ├── model/      # 数据模型
│   │   ├── repository/ # 数据访问层
│   │   ├── service/    # 业务逻辑层
│   │   └── EAiBackendApplication.java # 应用入口
│   ├── src/main/resources/
│   │   └── application.properties # 配置文件
│   ├── pom.xml         # Maven依赖配置
│   └── mvnw.cmd        # Maven wrapper
├── frontend/           # 前端代码
│   ├── src/
│   │   ├── assets/     # 静态资源
│   │   ├── components/ # 组件
│   │   ├── views/      # 页面
│   │   ├── router/     # 路由配置
│   │   ├── api/        # API请求封装
│   │   ├── App.vue     # 根组件
│   │   └── main.js     # 前端入口
│   ├── package.json    # 依赖配置
│   └── vue.config.js   # Vue配置
├── eduagent.sql        # 数据库初始化脚本
└── README.md           # 项目说明
```

## 环境要求

### 后端环境

- JDK 17或更高版本
- Maven 3.6或更高版本
- MySQL 5.7或更高版本

### 前端环境

- Node.js 14或更高版本
- npm 6或更高版本

## 启动流程

### 1. 数据库初始化

1. 打开MySQL命令行或客户端工具
2. 执行以下命令创建数据库和表结构：

   替换为自己电脑里安装的项目的具体路径！！！！
   ```sql
   source d:\eduagent\eduagent\eduagent.sql
   ```

替换为自己电脑里安装的项目的具体路径
或手动执行`eduagent.sql`文件中的SQL语句：

```sql
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

……………………………………省略
```

### 2. 启动后端服务（idea）

1. 进入后端目录：
   ```bash
   cd d:\eduagent\eduagent\backend
   ```
2. 使用Maven启动服务：
   - 方法1：使用系统安装的Maven
     ```bash
     mvn spring-boot:run
     ```
   - 方法2：使用项目提供的Maven wrapper
     ```bash
     .\mvnw.cmd spring-boot:run
     ```
3. 后端服务将在 `http://localhost:8080` 启动

### 3. 启动前端服务

1. 进入前端目录：
   ```bash
   cd d:\eduagent\eduagent\frontend
   ```
2. 安装依赖：
   ```bash
   npm install
   ```
3. 启动前端服务：
   ```bash
   npm run serve
   ```
4. 前端服务将在 `http://localhost:8081` 启动（默认端口）

## 测试流程

1. 打开浏览器，访问 `http://localhost:8081`
2. 点击"去注册"链接，进入注册页面
3. 输入用户名和密码（用户名至少3个字符，密码至少6个字符）
4. 点击"Register"按钮，注册成功后会跳转到登录页面
5. 输入刚注册的用户名和密码，或使用默认账号：
   - 用户名：admin
   - 密码：123456
6. 点击"登录"按钮，登录成功后会显示成功提示

## 主要功能

- 用户注册：创建新用户账号
- 用户登录：验证用户身份
- 表单验证：确保用户输入符合要求
- 错误处理：提供友好的错误提示

## 注意事项

1. 确保MySQL服务已启动
2. 确保数据库连接配置正确（用户名、密码）
3. 后端服务和前端服务需要同时运行
4. 前端默认连接后端地址为 `http://localhost:8080/api`，如果后端服务端口有变化，需要修改前端 `src/api/index.js` 文件中的 `baseURL`

## 故障排查

1. **后端启动失败**：
   - 检查MySQL服务是否运行
   - 检查数据库连接配置是否正确
   - 检查端口8080是否被占用
2. **前端启动失败**：
   - 检查Node.js和npm是否正确安装
   - 检查依赖是否安装成功
   - 检查端口8081是否被占用
3. **前后端通信失败**：
   - 检查后端服务是否正常运行
   - 检查前端API配置中的 `baseURL` 是否正确
   - 检查浏览器控制台是否有错误信息

#
