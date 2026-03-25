"""
作业完成情况分析技能
"""
from .base import Skill, skill_registry
from pydantic import BaseModel, Field
from mcp.client import MCPClient

class HomeworkAnalysisInput(BaseModel):
    student_id: int = Field(description="学生ID")

class HomeworkAnalysisSkill(Skill):
    name = "homework_completion_analysis"
    description = "分析学生的作业完成情况"
    args_schema = HomeworkAnalysisInput
    
    def run(self, student_id: int) -> str:
        """分析作业完成情况"""
        try:
            mcp_client = MCPClient()
            homework_status = mcp_client.get_homework_status(student_id)
            
            # 分析作业数据
            total_homework = 0
            completed_homework = 0
            pending_homework = 0
            
            if "data" in homework_status:
                for homework in homework_status["data"]:
                    total_homework += 1
                    status = homework.get("status", "")
                    if status == "completed":
                        completed_homework += 1
                    elif status == "pending":
                        pending_homework += 1
            
            # 计算完成率
            completion_rate = (completed_homework / total_homework * 100) if total_homework > 0 else 0
            
            # 生成分析报告
            report = f"【作业完成情况分析】\n"
            report += f"学生ID：{student_id}\n"
            report += f"总作业数：{total_homework}\n"
            report += f"已完成：{completed_homework}\n"
            report += f"未完成：{pending_homework}\n"
            report += f"完成率：{completion_rate:.1f}%\n"
            
            # 根据完成情况给出建议
            if completion_rate == 100:
                report += "\n【建议】\n"
                report += "作业完成情况优秀，继续保持！"
            elif completion_rate >= 80:
                report += "\n【建议】\n"
                report += "作业完成情况良好，建议继续保持当前状态。"
            elif completion_rate >= 60:
                report += "\n【建议】\n"
                report += "作业完成情况一般，建议提高作业完成效率，按时完成作业。"
            else:
                report += "\n【建议】\n"
                report += "作业完成情况较差，建议制定合理的学习计划，优先完成作业任务。"
            
            return report
        except Exception as e:
            return f"获取作业数据失败：{str(e)}"

# 注册技能
skill_registry.register(HomeworkAnalysisSkill())
