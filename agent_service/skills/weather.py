"""
天气查询技能
"""
from .base import Skill, skill_registry
from pydantic import BaseModel, Field

class WeatherInput(BaseModel):
    city: str = Field(description="城市名称")

class WeatherSkill(Skill):
    name = "weather_query"
    description = "查询指定城市的天气信息"
    args_schema = WeatherInput
    
    def run(self, city: str) -> str:
        """查询天气"""
        # 这里使用模拟数据，实际项目中可以调用天气API
        weather_data = {
            "北京": "晴，15-25℃",
            "上海": "多云，18-28℃",
            "广州": "阴，22-30℃",
            "深圳": "小雨，20-26℃"
        }
        
        if city in weather_data:
            return f"{city}的天气：{weather_data[city]}"
        else:
            return f"抱歉，暂无法查询{city}的天气信息"

# 注册技能
skill_registry.register(WeatherSkill())
