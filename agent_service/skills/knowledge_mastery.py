"""
知识点掌握度评估技能
"""
from .base import Skill, skill_registry
from pydantic import BaseModel, Field
from mcp.client import MCPClient

class KnowledgeMasteryInput(BaseModel):
    student_id: int = Field(description="学生ID")

class KnowledgeMasterySkill(Skill):
    name = "knowledge_mastery_analysis"
    description = "评估学生对知识点的掌握程度"
    args_schema = KnowledgeMasteryInput
    
    def run(self, student_id: int) -> str:
        """评估知识点掌握度"""
        try:
            mcp_client = MCPClient()
            error_questions = mcp_client.get_error_questions(student_id)
            
            # 分析错题数据，统计各知识点的错误次数
            knowledge_errors = {}
            
            if "data" in error_questions:
                for error in error_questions["data"]:
                    knowledge_point = error.get("knowledge_point", "未知知识点")
                    if knowledge_point in knowledge_errors:
                        knowledge_errors[knowledge_point] += 1
                    else:
                        knowledge_errors[knowledge_point] = 1
            
            # 生成掌握度报告
            report = f"【知识点掌握度评估】\n"
            report += f"学生ID：{student_id}\n"
            
            if knowledge_errors:
                report += "\n【薄弱知识点】\n"
                # 按错误次数排序
                sorted_errors = sorted(knowledge_errors.items(), key=lambda x: x[1], reverse=True)
                
                for i, (knowledge, count) in enumerate(sorted_errors[:5], 1):
                    report += f"{i}. {knowledge}：错误{count}次\n"
                
                report += "\n【建议】\n"
                report += "1. 重点复习上述薄弱知识点\n"
                report += "2. 针对每个薄弱知识点进行专项练习\n"
                report += "3. 定期回顾错题，加深理解\n"
            else:
                report += "\n【评估结果】\n"
                report += "暂无错题记录，知识点掌握情况良好！\n"
                report += "\n【建议】\n"
                report += "1. 继续保持当前的学习状态\n"
                report += "2. 定期进行知识巩固和拓展\n"
            
            return report
        except Exception as e:
            return f"获取错题数据失败：{str(e)}"

# 注册技能
skill_registry.register(KnowledgeMasterySkill())
