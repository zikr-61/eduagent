"""
工具1：KnowledgeSearchTool
作用：从学生/教师自己的知识点笔记中进行向量语义检索
与简单API调用的区别：使用 FAISS 向量相似度，而非关键词匹配
"""
import sys, os
sys.path.insert(0, os.path.dirname(os.path.dirname(__file__)))

from langchain.tools import BaseTool
from pydantic import BaseModel, Field
from rag.knowledge_base import search


class KnowledgeSearchInput(BaseModel):
    query: str = Field(description="检索查询词，描述你想找的知识点内容")
    user_id: int = Field(description="用户ID")
    top_k: int = Field(default=3, description="返回最相关的前k条")


class KnowledgeSearchTool(BaseTool):
    name: str = "knowledge_search"
    description: str = (
        "从用户自己的知识点笔记中语义检索相关内容。"
        "当需要了解学生学过什么、找相关背景知识、或者针对已学内容出题时使用。"
        "输入查询词和用户ID，返回最相关的知识点片段。"
    )
    args_schema: type[BaseModel] = KnowledgeSearchInput

    def _run(self, query: str, user_id: int, top_k: int = 3) -> str:
        results = search(user_id, query, k=top_k)
        if not results:
            return "该用户暂无相关知识点记录，建议先在「知识点总结」中添加学习内容。"

        lines = [f"检索到 {len(results)} 条相关知识点："]
        for i, r in enumerate(results, 1):
            lines.append(
                f"\n[{i}] 知识点《{r['title']}》(相关度: {1 - r['score']:.2f})\n"
                f"内容片段: {r['content'][:200]}..."
            )
        return "\n".join(lines)

    async def _arun(self, query: str, user_id: int, top_k: int = 3) -> str:
        return self._run(query, user_id, top_k)
