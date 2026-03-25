"""
MCP (Micro-service Communication Protocol) 客户端
用于与 Spring Boot 后端进行通信，支持同步和异步调用
"""
import httpx
import json
from typing import Dict, Any, Optional
from config import SPRING_BASE_URL

class MCPClient:
    def __init__(self, base_url: str = SPRING_BASE_URL):
        self.base_url = base_url
        self.client = httpx.Client(timeout=30.0)
        
    def get_user_info(self, user_id: int) -> Dict[str, Any]:
        """获取用户信息"""
        url = f"{self.base_url}/user/{user_id}"
        response = self.client.get(url)
        response.raise_for_status()
        return response.json()
    
    def get_study_records(self, user_id: int, days: int = 7) -> Dict[str, Any]:
        """获取学习记录"""
        url = f"{self.base_url}/study-record/user/{user_id}?days={days}"
        response = self.client.get(url)
        response.raise_for_status()
        return response.json()
    
    def get_error_questions(self, student_id: int) -> Dict[str, Any]:
        """获取错题记录"""
        url = f"{self.base_url}/error-question/student/{student_id}"
        response = self.client.get(url)
        response.raise_for_status()
        return response.json()
    
    def get_homework_status(self, student_id: int) -> Dict[str, Any]:
        """获取作业状态"""
        url = f"{self.base_url}/homework/student/{student_id}"
        response = self.client.get(url)
        response.raise_for_status()
        return response.json()
    
    def call(self, service: str, method: str, params: Dict[str, Any]) -> Dict[str, Any]:
        """同步调用微服务"""
        url = f"{self.base_url}/{service}/{method}"
        response = self.client.post(url, json=params)
        response.raise_for_status()
        return response.json()
    
    async def acall(self, service: str, method: str, params: Dict[str, Any]) -> Dict[str, Any]:
        """异步调用微服务"""
        url = f"{self.base_url}/{service}/{method}"
        async with httpx.AsyncClient(timeout=30.0) as client:
            response = await client.post(url, json=params)
            response.raise_for_status()
            return response.json()
    
    def close(self):
        """关闭客户端连接"""
        self.client.close()
