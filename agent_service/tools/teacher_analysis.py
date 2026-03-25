"""
工具5：TeacherAnalysisTool（教师专属）
作用：分析全班学生学习情况，找出需要重点关注的学生，给教学建议
"""
import sys, os, json
sys.path.insert(0, os.path.dirname(os.path.dirname(__file__)))

from langchain.tools import BaseTool
from langchain_openai import ChatOpenAI
from pydantic import BaseModel, Field
from db import query
from config import QWEN_API_KEY, QWEN_BASE_URL, QWEN_CHAT_MODEL


class TeacherAnalysisInput(BaseModel):
    teacher_id: int = Field(description="教师ID")
    analysis_type: str = Field(
        default="overview",
        description="分析类型: overview（班级概览）/ at_risk（需关注学生）/ homework（作业情况）",
    )


class TeacherAnalysisTool(BaseTool):
    name: str = "teacher_class_analysis"
    description: str = (
        "教师专用：分析全班学生学习情况。"
        "analysis_type='overview' 给出班级整体学习状况；"
        "analysis_type='at_risk' 找出学习时长不足或错题多的学生；"
        "analysis_type='homework' 分析各作业的完成情况。"
        "教师提问时使用，学生不应使用此工具。"
    )
    args_schema: type[BaseModel] = TeacherAnalysisInput

    def _run(self, teacher_id: int, analysis_type: str = "overview") -> str:
        llm = ChatOpenAI(
            model=QWEN_CHAT_MODEL,
            openai_api_key=QWEN_API_KEY,
            openai_api_base=QWEN_BASE_URL,
            temperature=0.3,
        )

        if analysis_type == "at_risk":
            return self._at_risk_analysis(llm)
        elif analysis_type == "homework":
            return self._homework_analysis(llm, teacher_id)
        else:
            return self._overview_analysis(llm)

    def _overview_analysis(self, llm) -> str:
        stats = query(
            """
            SELECT u.name, u.grade,
                COALESCE((SELECT SUM(duration_minutes) FROM study_records sr WHERE sr.user_id=u.id
                          AND sr.record_date >= DATE_SUB(CURDATE(),INTERVAL 7 DAY)), 0) as week_minutes,
                COALESCE((SELECT COUNT(*) FROM error_questions eq WHERE eq.student_id=u.id), 0) as error_count,
                COALESCE((SELECT COUNT(*) FROM student_homework sh WHERE sh.student_id=u.id AND sh.status='completed'), 0) as done_hw
            FROM users u WHERE u.user_type='student'
            ORDER BY week_minutes DESC
            """,
        )
        if not stats:
            return "暂无学生数据。"

        summary = [{"name": r["name"], "grade": r["grade"],
                    "week_study_min": r["week_minutes"],
                    "error_count": r["error_count"],
                    "completed_hw": r["done_hw"]} for r in stats]

        prompt = f"""
以下是全班{len(stats)}名学生的本周学习数据：
{json.dumps(summary, ensure_ascii=False, indent=2)}

请给出：
1. 班级整体学习情况评价（2-3句）
2. 学习最积极的3名学生
3. 需要关注的学生（学习时间少或错题多）
4. 对教师的教学建议（1-2条）

简洁、具体，不要泛泛而谈。
"""
        result = llm.invoke(prompt)
        return f"【班级学习概览】\n\n{result.content}"

    def _at_risk_analysis(self, llm) -> str:
        at_risk = query(
            """
            SELECT u.name, u.grade,
                COALESCE((SELECT SUM(duration_minutes) FROM study_records sr WHERE sr.user_id=u.id
                          AND sr.record_date >= DATE_SUB(CURDATE(),INTERVAL 7 DAY)), 0) as week_minutes,
                COALESCE((SELECT COUNT(*) FROM error_questions eq WHERE eq.student_id=u.id), 0) as error_count,
                COALESCE((SELECT COUNT(*) FROM student_homework sh WHERE sh.student_id=u.id AND sh.status='pending'), 0) as pending_hw
            FROM users u WHERE u.user_type='student'
            HAVING week_minutes < 60 OR error_count > 5 OR pending_hw > 2
            ORDER BY week_minutes ASC
            """,
        )
        if not at_risk:
            return "目前没有明显需要重点关注的学生，班级整体情况良好！"

        data = [{"name": r["name"], "week_study_min": r["week_minutes"],
                 "error_count": r["error_count"], "pending_hw": r["pending_hw"]} for r in at_risk]

        prompt = f"""
以下是需要关注的学生数据：
{json.dumps(data, ensure_ascii=False, indent=2)}

对每位学生给出：
1. 主要问题（学习时间少/错题多/作业未完成）
2. 建议采取的干预措施（具体到行动）

用表格或列表格式，简洁明了。
"""
        result = llm.invoke(prompt)
        return f"【需重点关注的学生】\n\n{result.content}"

    def _homework_analysis(self, llm, teacher_id: int) -> str:
        hw_stats = query(
            """
            SELECT h.title, h.due_date,
                COUNT(sh.id) as assigned_count,
                SUM(CASE WHEN sh.status='completed' THEN 1 ELSE 0 END) as completed_count,
                AVG(sh.completion_time) as avg_time
            FROM homework h
            LEFT JOIN student_homework sh ON h.id = sh.homework_id
            WHERE h.teacher_id = %s
            GROUP BY h.id ORDER BY h.created_at DESC LIMIT 10
            """,
            (teacher_id,),
        )
        if not hw_stats:
            return "暂无作业数据。"

        data = []
        for r in hw_stats:
            assigned = r["assigned_count"] or 0
            completed = r["completed_count"] or 0
            rate = round(completed / assigned * 100, 1) if assigned > 0 else 0
            data.append({"title": r["title"], "due": str(r["due_date"]),
                         "rate": f"{rate}%", "avg_time_min": round(float(r["avg_time"] or 0), 1)})

        prompt = f"""
以下是教师布置的作业完成情况：
{json.dumps(data, ensure_ascii=False, indent=2)}

请分析：
1. 哪些作业完成率低（<60%），可能原因是什么
2. 平均用时最长的作业说明了什么
3. 给教师的布置建议

简洁，结合数据说明。
"""
        result = llm.invoke(prompt)
        return f"【作业完成情况分析】\n\n{result.content}"

    async def _arun(self, teacher_id: int, analysis_type: str = "overview") -> str:
        return self._run(teacher_id, analysis_type)
