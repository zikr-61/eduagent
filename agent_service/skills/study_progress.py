"""
学习进度分析技能
"""
from .base import Skill, skill_registry
from pydantic import BaseModel, Field
from mcp.client import MCPClient

class StudyProgressInput(BaseModel):
    user_id: int = Field(description="学生ID")
    days: int = Field(default=7, description="统计天数")

class StudyProgressSkill(Skill):
    name = "study_progress_analysis"
    description = "分析学生的学习进度和学习时长"
    args_schema = StudyProgressInput
    
    def run(self, user_id: int, days: int = 7) -> str:
        """分析学习进度"""
        try:
            mcp_client = MCPClient()
            study_records = mcp_client.get_study_records(user_id, days)
            
            # 分析学习数据
            total_minutes = 0
            session_count = 0
            
            if "data" in study_records:
                for record in study_records["data"]:
                    total_minutes += record.get("duration_minutes", 0)
                    session_count += 1
            
            # 计算平均每天学习时长
            avg_daily_minutes = total_minutes / days if days > 0 else 0
            
            # 生成分析报告
            report = f"【学习进度分析】\n"
            report += f"统计周期：最近{days}天\n"
            report += f"总学习时长：{total_minutes}分钟\n"
            report += f"学习次数：{session_count}次\n"
            report += f"平均每天学习：{avg_daily_minutes:.1f}分钟\n"
            
            # 根据学习时长给出建议
            if total_minutes < 300:
                report += "\n【建议】学习时长较少，建议增加学习时间，每天至少保持30分钟的学习。"
            elif total_minutes < 600:
                report += "\n【建议】学习时长适中，建议保持当前学习节奏。"
            else:
                report += "\n【建议】学习时长充足，建议注重学习效率和方法。"
            
            return report
        except Exception as e:
            return f"获取学习数据失败：{str(e)}"

# 注册技能
skill_registry.register(StudyProgressSkill())
