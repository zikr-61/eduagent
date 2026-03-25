"""
计算器技能
"""
from .base import Skill, skill_registry
from pydantic import BaseModel, Field

class CalculatorInput(BaseModel):
    expression: str = Field(description="数学表达式，如 '1+2*3'")

class CalculatorSkill(Skill):
    name = "calculator"
    description = "执行数学计算，支持加减乘除等基本运算"
    args_schema = CalculatorInput
    
    def run(self, expression: str) -> str:
        """执行计算"""
        try:
            # 安全计算，使用 eval 函数
            result = eval(expression)
            return f"计算结果：{expression} = {result}"
        except Exception as e:
            return f"计算错误：{str(e)}"

# 注册技能
skill_registry.register(CalculatorSkill())
