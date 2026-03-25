"""
工具3：QuestionGeneratorTool
作用：根据知识上下文和薄弱点，生成有针对性的题目
与简单API调用的区别：
  - context 来自 RAG 检索（而非用户原始输入）
  - weak_points 来自 ErrorAnalysisTool 的推理结果
  - 两个工具的输出组合后才调用此工具，是多步 Agent 协作的产物
"""
import sys, os, json
sys.path.insert(0, os.path.dirname(os.path.dirname(__file__)))

from langchain.tools import BaseTool
from langchain_openai import ChatOpenAI
from pydantic import BaseModel, Field
from config import QWEN_API_KEY, QWEN_BASE_URL, QWEN_CHAT_MODEL


class QuestionGeneratorInput(BaseModel):
    context: str = Field(description="相关知识点内容（来自RAG检索结果）")
    focus: str = Field(description="重点考察方向或薄弱点描述")
    count: int = Field(default=3, description="生成题目数量，默认3道")
    difficulty: str = Field(default="medium", description="难度: easy/medium/hard")


class QuestionGeneratorTool(BaseTool):
    name: str = "generate_targeted_questions"
    description: str = (
        "根据知识点内容和学生薄弱点，生成有针对性的练习题。"
        "与泛化出题不同：会专门针对薄弱领域，避免出已掌握的内容。"
        "需要先调用 knowledge_search 获取知识点内容，再调用此工具。"
    )
    args_schema: type[BaseModel] = QuestionGeneratorInput

    def _run(self, context: str, focus: str, count: int = 3, difficulty: str = "medium") -> str:
        diff_map = {"easy": "简单（基础概念）", "medium": "中等（综合应用）", "hard": "困难（深度理解）"}
        diff_text = diff_map.get(difficulty, "中等")

        llm = ChatOpenAI(
            model=QWEN_CHAT_MODEL,
            openai_api_key=QWEN_API_KEY,
            openai_api_base=QWEN_BASE_URL,
            temperature=0.7,
        )

        prompt = f"""
基于以下知识点内容，专门针对【{focus}】这个方向，生成{count}道{diff_text}的选择题。

知识点内容：
{context[:1500]}

要求：
1. 每道题必须有4个选项（A/B/C/D）
2. 重点考察【{focus}】，不要出与此无关的题目
3. 给出正确答案和简短解析（1句话）

严格按照以下JSON格式返回，不要有其他文字：
[
  {{
    "question": "题目内容",
    "options": "A: 选项1\\nB: 选项2\\nC: 选项3\\nD: 选项4",
    "answer": "A",
    "explanation": "解析：为什么选A"
  }}
]
"""
        result = llm.invoke(prompt)
        raw = result.content.strip()

        # 清理 markdown 代码块
        if "```json" in raw:
            raw = raw.split("```json")[1].split("```")[0].strip()
        elif "```" in raw:
            raw = raw.split("```")[1].split("```")[0].strip()

        try:
            questions = json.loads(raw)
            lines = [f"为你生成了 {len(questions)} 道针对【{focus}】的练习题：\n"]
            for i, q in enumerate(questions, 1):
                lines.append(f"第{i}题：{q.get('question', '')}")
                lines.append(q.get("options", ""))
                lines.append(f"正确答案：{q.get('answer', '')}")
                lines.append(f"{q.get('explanation', '')}\n")
            return "\n".join(lines)
        except json.JSONDecodeError:
            return f"生成的题目：\n{raw}"

    async def _arun(self, context: str, focus: str, count: int = 3, difficulty: str = "medium") -> str:
        return self._run(context, focus, count, difficulty)
