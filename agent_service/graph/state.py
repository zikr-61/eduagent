"""LangGraph AgentState 定义"""
from typing import Annotated, TypedDict
from langgraph.graph.message import add_messages


class AgentState(TypedDict):
    """Agent 运行时状态，在节点间传递"""
    messages: Annotated[list, add_messages]   # 对话消息列表
    user_id: int                               # 当前用户ID
    user_type: str                             # student / teacher
    session_id: str                            # 对话会话ID
