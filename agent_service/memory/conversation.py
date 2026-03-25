"""
对话历史管理
- 存储多轮对话到 MySQL chat_messages 表
- 支持按 session_id 加载历史
"""
import sys, os
sys.path.insert(0, os.path.dirname(os.path.dirname(__file__)))

import uuid
from datetime import datetime
from langchain_core.messages import HumanMessage, AIMessage, BaseMessage
from db import query, get_conn


def new_session(user_id: int) -> str:
    """创建新的对话会话，返回 session_id"""
    session_id = str(uuid.uuid4())
    conn = get_conn()
    try:
        with conn.cursor() as cur:
            cur.execute(
                "INSERT INTO chat_sessions (session_id, user_id, created_at) VALUES (%s, %s, %s)",
                (session_id, user_id, datetime.now()),
            )
        conn.commit()
    finally:
        conn.close()
    return session_id


def get_or_create_session(user_id: int, session_id: str | None) -> str:
    if session_id:
        rows = query("SELECT session_id FROM chat_sessions WHERE session_id=%s AND user_id=%s",
                     (session_id, user_id))
        if rows:
            return session_id
    return new_session(user_id)


def save_message(session_id: str, role: str, content: str):
    """保存一条消息（role: human / ai）"""
    conn = get_conn()
    try:
        with conn.cursor() as cur:
            cur.execute(
                "INSERT INTO chat_messages (session_id, role, content, created_at) VALUES (%s, %s, %s, %s)",
                (session_id, role, content, datetime.now()),
            )
        conn.commit()
    finally:
        conn.close()


def load_history(session_id: str, limit: int = 20) -> list[BaseMessage]:
    """加载对话历史，转换为 LangChain 消息格式"""
    rows = query(
        "SELECT role, content FROM chat_messages WHERE session_id=%s ORDER BY created_at DESC LIMIT %s",
        (session_id, limit),
    )
    rows = list(reversed(rows))
    messages: list[BaseMessage] = []
    for r in rows:
        if r["role"] == "human":
            messages.append(HumanMessage(content=r["content"]))
        else:
            messages.append(AIMessage(content=r["content"]))
    return messages


def list_sessions(user_id: int) -> list[dict]:
    """列出用户所有历史会话"""
    return query(
        """
        SELECT cs.session_id, cs.created_at,
               (SELECT content FROM chat_messages cm WHERE cm.session_id=cs.session_id
                ORDER BY cm.created_at LIMIT 1) as first_message
        FROM chat_sessions cs
        WHERE cs.user_id = %s
        ORDER BY cs.created_at DESC
        LIMIT 20
        """,
        (user_id,),
    )
