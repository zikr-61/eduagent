"""
工具2：ErrorAnalysisTool
作用：分析学生错题模式，找出薄弱知识点，用 LLM 做二次推理
与简单API调用的区别：不只是查数据库返回原始数据，还用 LLM 做模式识别和原因分析
"""
import sys, os, json
sys.path.insert(0, os.path.dirname(os.path.dirname(__file__)))

from langchain.tools import BaseTool
from langchain_openai import ChatOpenAI
from pydantic import BaseModel, Field
from db import query
from config import QWEN_API_KEY, QWEN_BASE_URL, QWEN_CHAT_MODEL


class ErrorAnalysisInput(BaseModel):
    student_id: int = Field(description="学生ID")


class ErrorAnalysisTool(BaseTool):
    name: str = "error_analysis"
    description: str = (
        "分析学生的错题本，找出薄弱知识点和错误模式。"
        "返回结构化分析：错误集中领域、可能原因、最需要加强的方向。"
        "当学生问'我哪里需要加强'或'帮我制定复习计划'时使用。"
    )
    args_schema: type[BaseModel] = ErrorAnalysisInput

    def _run(self, student_id: int) -> str:
        # 查询错题，关联题目和知识点信息
        rows = query(
            """
            SELECT eq.error_count, eq.last_error_time,
                   q.question_text, q.options, q.answer, q.difficulty,
                   kp.title as kp_title
            FROM error_questions eq
            JOIN questions q ON eq.question_id = q.id
            JOIN knowledge_points kp ON q.knowledge_point_id = kp.id
            WHERE eq.student_id = %s
            ORDER BY eq.error_count DESC, eq.last_error_time DESC
            LIMIT 20
            """,
            (student_id,),
        )

        if not rows:
            return "该学生暂无错题记录，表现不错！"

        # 构建 LLM 分析 Prompt（工具内部自己调用 LLM，体现 Agent 工具的智能性）
        llm = ChatOpenAI(
            model=QWEN_CHAT_MODEL,
            openai_api_key=QWEN_API_KEY,
            openai_api_base=QWEN_BASE_URL,
            temperature=0.3,
        )

        error_summary = []
        for r in rows:
            error_summary.append({
                "knowledge_area": r["kp_title"],
                "question": r["question_text"][:80],
                "error_count": r["error_count"],
                "difficulty": r["difficulty"],
            })

        prompt = f"""
以下是学生的错题数据（共{len(rows)}道错题）：
{json.dumps(error_summary, ensure_ascii=False, indent=2)}

请分析：
1. 错误主要集中在哪些知识点领域
2. 是概念理解问题（同一领域反复错）还是粗心问题（难度低但错）
3. 列出最需要加强的3个具体方向
4. 给出简短的复习建议

请用简洁的中文回答，分点列出。
"""
        result = llm.invoke(prompt)
        return f"【错题分析报告】\n共有{len(rows)}道错题\n\n{result.content}"

    async def _arun(self, student_id: int) -> str:
        return self._run(student_id)
