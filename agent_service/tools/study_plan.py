"""
工具4：StudyPlanTool
作用：综合学习时长、错题分布、作业完成情况，生成个性化学习建议
与简单API调用的区别：跨数据源（study_records + error_questions + student_homework）联合推理
"""
import sys, os, json
sys.path.insert(0, os.path.dirname(os.path.dirname(__file__)))

from langchain.tools import BaseTool
from langchain_openai import ChatOpenAI
from pydantic import BaseModel, Field
from db import query
from config import QWEN_API_KEY, QWEN_BASE_URL, QWEN_CHAT_MODEL


class StudyPlanInput(BaseModel):
    student_id: int = Field(description="学生ID")


class StudyPlanTool(BaseTool):
    name: str = "generate_study_plan"
    description: str = (
        "综合学生的学习时长、错题数量、作业完成情况，生成个性化学习建议。"
        "不同于通用建议，会结合该学生的真实数据。"
        "当学生问'我应该怎么学'、'给我制定计划'时使用。"
    )
    args_schema: type[BaseModel] = StudyPlanInput

    def _run(self, student_id: int) -> str:
        # 1. 近7天学习时长
        study = query(
            """
            SELECT COALESCE(SUM(duration_minutes), 0) as week_minutes,
                   COUNT(*) as session_count
            FROM study_records
            WHERE user_id = %s AND record_date >= DATE_SUB(CURDATE(), INTERVAL 7 DAY)
            """,
            (student_id,),
        )
        week_minutes = study[0]["week_minutes"] if study else 0
        session_count = study[0]["session_count"] if study else 0

        # 2. 错题统计（各知识领域错题数量）
        errors = query(
            """
            SELECT kp.title as area, COUNT(*) as cnt, SUM(eq.error_count) as total_errors
            FROM error_questions eq
            JOIN questions q ON eq.question_id = q.id
            JOIN knowledge_points kp ON q.knowledge_point_id = kp.id
            WHERE eq.student_id = %s
            GROUP BY kp.title
            ORDER BY total_errors DESC
            LIMIT 5
            """,
            (student_id,),
        )

        # 3. 作业完成情况
        homework = query(
            """
            SELECT
                COUNT(*) as total,
                SUM(CASE WHEN status='completed' THEN 1 ELSE 0 END) as done,
                AVG(CASE WHEN completion_time IS NOT NULL THEN completion_time END) as avg_time
            FROM student_homework
            WHERE student_id = %s
            """,
            (student_id,),
        )
        hw = homework[0] if homework else {}
        total_hw = hw.get("total") or 0
        done_hw = hw.get("done") or 0
        avg_hw_time = round(float(hw.get("avg_time") or 0), 1)

        # 4. 用 LLM 综合推理生成个性化建议
        llm = ChatOpenAI(
            model=QWEN_CHAT_MODEL,
            openai_api_key=QWEN_API_KEY,
            openai_api_base=QWEN_BASE_URL,
            temperature=0.5,
        )

        error_areas = [f"{e['area']}（错{e['total_errors']}次）" for e in errors] if errors else ["无错题记录"]
        hw_rate = round(done_hw / total_hw * 100, 1) if total_hw > 0 else 0

        prompt = f"""
以下是该学生的真实学习数据：
- 近7天学习时长：{week_minutes}分钟（共{session_count}次学习记录）
- 作业完成情况：{done_hw}/{total_hw}份（完成率{hw_rate}%，平均用时{avg_hw_time}分钟/份）
- 错题集中领域：{', '.join(error_areas)}

请根据以上真实数据（不要给通用建议），生成：
1. 对当前学习状态的评价（2句话，具体）
2. 本周最优先要做的3件事（结合薄弱点）
3. 每日建议学习时长（基于现有习惯）
4. 一句鼓励的话

语气要像一位了解这个学生的老师，亲切、具体。
"""
        result = llm.invoke(prompt)
        return f"【个性化学习建议】\n\n{result.content}"

    async def _arun(self, student_id: int) -> str:
        return self._run(student_id)
