import os
from dotenv import load_dotenv

load_dotenv()

# Qwen / DashScope（OpenAI 兼容模式）
QWEN_API_KEY = os.getenv("QWEN_API_KEY", "sk-4850e30b4912474ba141fb44be819ed5")
QWEN_BASE_URL = os.getenv("QWEN_BASE_URL", "https://dashscope.aliyuncs.com/compatible-mode/v1")
QWEN_CHAT_MODEL = os.getenv("QWEN_CHAT_MODEL", "qwen-plus")
QWEN_EMBED_MODEL = os.getenv("QWEN_EMBED_MODEL", "text-embedding-v3")

# MySQL（与 Spring Boot 共用同一数据库）
DB_HOST = os.getenv("DB_HOST", "localhost")
DB_PORT = int(os.getenv("DB_PORT", "3306"))
DB_NAME = os.getenv("DB_NAME", "education_ai_db")
DB_USER = os.getenv("DB_USER", "root")
DB_PASS = os.getenv("DB_PASS", "123456")

# Spring Boot API 地址（工具需要调用部分业务接口）
SPRING_BASE_URL = os.getenv("SPRING_BASE_URL", "http://localhost:8080/api")
