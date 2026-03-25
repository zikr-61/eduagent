"""
LangGraph ReAct Agent：工具由 LLM 选择，但「当前登录用户 ID」由服务端注入闭包，
避免模型猜错 user_id 导致工具失败、长时间无输出。
"""
import sys, os

sys.path.insert(0, os.path.dirname(os.path.dirname(__file__)))

from langchain_openai import ChatOpenAI
from langchain_core.messages import SystemMessage
from langchain_core.tools import tool
from langgraph.prebuilt import create_react_agent

from tools.knowledge_search import KnowledgeSearchTool
from tools.error_analysis import ErrorAnalysisTool
from tools.question_generator import QuestionGeneratorTool
from tools.study_plan import StudyPlanTool
from tools.teacher_analysis import TeacherAnalysisTool
from config import QWEN_API_KEY, QWEN_BASE_URL, QWEN_CHAT_MODEL

STUDENT_SYSTEM_PROMPT = """你是一位专注、耐心的AI学习助手，服务于当前学生用户。

可用工具（不要传入用户ID，系统已绑定当前学生）：
- knowledge_search：检索该学生自己的知识点笔记。参数：query（检索词）、top_k（可选，默认3）
- analyze_my_errors：分析该学生的错题本与薄弱点。无参数。
- generate_targeted_questions：根据知识片段与薄弱方向出题。参数：context、focus、count、difficulty
- generate_study_plan：根据该学生的学习/作业/错题数据给个性化建议。无参数。

规则简要：问知识先 knowledge_search；问薄弱/复习先 analyze_my_errors；要计划用 generate_study_plan；出题先检索再 generate_targeted_questions。
回答要亲切、具体。"""

TEACHER_SYSTEM_PROMPT = """你是 AI 教学助手，服务于当前教师用户。

可用工具（班级分析已绑定当前教师账号，不要传 teacher_id）：
- teacher_class_analysis：班级学情。参数 analysis_type：overview / at_risk / homework
- knowledge_search：检索该教师自己的知识点材料。参数：query、top_k
- generate_targeted_questions：生成练习题。参数：context、focus、count、difficulty
- analyze_student_errors：查看某学生错题。参数：student_id（整数）

重要约束（防止编造数据）：
1) 你在最终回答中出现的学生姓名/学生ID/具体数值/名单，必须严格来自工具输出；
2) 如果工具输出没有提供某信息（例如具体次数、变化幅度、正确率等），必须明确说明“数据不足”，不得擅自补全；
3) 不要在工具未返回该学生或该数值时，仍给出“针对某学生”的具体结论。
4) 如果 teacher_class_analysis（overview/at_risk/homework）返回了“【数据不足】”开头的内容，你只能原样复述/回答“需要先补齐班级数据”，不得继续生成具体学生名单/百分比/正确率等。

回答要专业、结合数据。"""


def _student_tools(uid: int):
    @tool
    def knowledge_search(query: str, top_k: int = 3) -> str:
        """从当前学生自己的知识点笔记中做语义检索。query：想问或检索的内容；top_k：返回条数 1-5。"""
        return KnowledgeSearchTool()._run(query, uid, top_k)

    @tool
    def analyze_my_errors() -> str:
        """分析当前登录学生的错题本，找出薄弱知识点与复习建议。"""
        return ErrorAnalysisTool()._run(uid)

    @tool
    def generate_study_plan() -> str:
        """根据当前学生的学习时长、作业、错题数据生成个性化学习建议。"""
        return StudyPlanTool()._run(uid)

    gen = QuestionGeneratorTool()

    @tool
    def generate_targeted_questions(
        context: str, focus: str, count: int = 3, difficulty: str = "medium"
    ) -> str:
        """根据知识点文本 context 与考察重点 focus 生成选择题。count 题目数量；difficulty：easy/medium/hard。"""
        return gen._run(context, focus, count, difficulty)

    return [knowledge_search, analyze_my_errors, generate_study_plan, generate_targeted_questions]


def _teacher_tools(tid: int):
    @tool
    def teacher_class_analysis(analysis_type: str = "overview") -> str:
        """全班学情。analysis_type 取 overview（概览）、at_risk（需关注学生）、homework（作业完成）。"""
        return TeacherAnalysisTool()._run(tid, analysis_type)

    @tool
    def knowledge_search(query: str, top_k: int = 3) -> str:
        """从当前教师自己的知识点材料中语义检索。"""
        return KnowledgeSearchTool()._run(query, tid, top_k)

    @tool
    def analyze_student_errors(student_id: int) -> str:
        """查看指定学生（student_id）的错题分析。"""
        return ErrorAnalysisTool()._run(int(student_id))

    gen = QuestionGeneratorTool()

    @tool
    def generate_targeted_questions(
        context: str, focus: str, count: int = 3, difficulty: str = "medium"
    ) -> str:
        """根据材料 context 与重点 focus 生成选择题。"""
        return gen._run(context, focus, count, difficulty)

    return [
        teacher_class_analysis,
        knowledge_search,
        analyze_student_errors,
        generate_targeted_questions,
    ]


def build_agent(user_type: str, user_id: int):
    # 让教师场景更“确定性”，降低模型在数据不足时的编造概率。
    temperature = 0.2 if user_type == "teacher" else 0.6
    llm = ChatOpenAI(
        model=QWEN_CHAT_MODEL,
        openai_api_key=QWEN_API_KEY,
        openai_api_base=QWEN_BASE_URL,
        temperature=temperature,
        streaming=True,
    )

    if user_type == "teacher":
        tools = _teacher_tools(user_id)
        system_prompt = TEACHER_SYSTEM_PROMPT
    else:
        tools = _student_tools(user_id)
        system_prompt = STUDENT_SYSTEM_PROMPT

    return create_react_agent(
        llm,
        tools,
        state_modifier=SystemMessage(content=system_prompt),
    )
