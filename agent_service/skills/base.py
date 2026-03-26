"""
Skill 系统核心
- 技能基类定义
- 技能注册器
- 技能管理
"""
from abc import ABC, abstractmethod
from typing import Dict, Any, List, Optional
from langchain_core.tools import BaseTool
from pydantic import BaseModel

class Skill(ABC):
    """技能基类"""
    name: str
    description: str
    args_schema: Optional[type[BaseModel]] = None
    
    @abstractmethod
    def run(self, **kwargs) -> str:
        """执行技能"""
        pass
    
    async def arun(self, **kwargs) -> str:
        """异步执行技能"""
        return self.run(**kwargs)

class SkillRegistry:
    """技能注册器"""
    def __init__(self):
        self.skills: Dict[str, Skill] = {}
    
    def register(self, skill: Skill):
        """注册技能"""
        self.skills[skill.name] = skill
    
    def get(self, name: str) -> Optional[Skill]:
        """获取技能"""
        return self.skills.get(name)
    
    def list(self) -> List[str]:
        """列出所有技能"""
        return list(self.skills.keys())
    
    def tools(self) -> List[BaseTool]:
        """将技能转换为 LangChain 工具"""
        from langchain_core.tools import BaseTool, Tool
        tools = []
        
        for skill_name, skill in self.skills.items():
            def create_tool_func(**kwargs):
                nonlocal skill
                return skill.run(**kwargs)
            # 使用Tool类直接创建，而不是装饰器
            tool_instance = Tool(
                name=skill_name,
                func=create_tool_func,
                description=skill.description
            )
            tools.append(tool_instance)
        
        return tools

# 全局技能注册器
skill_registry = SkillRegistry()
