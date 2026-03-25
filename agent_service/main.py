"""
EduAgent - Python FastAPI 服务
端口：8000
Spring Boot：8080

接口列表：
POST /agent/chat/stream  - 流式对话（SSE）
POST /agent/chat         - 普通对话（非流式）
GET  /agent/sessions     - 历史会话列表
POST /agent/knowledge/invalidate - 清除向量缓存（知识点更新后调用）
GET  /health             - 健康检查
"""
import sys, os, json, asyncio
sys.path.insert(0, os.path.dirname(__file__))

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import StreamingResponse
from pydantic import BaseModel
from langchain_core.messages import HumanMessage

from graph.agent_graph import build_agent
from memory.conversation import (
    get_or_create_session, save_message, load_history, list_sessions
)
from rag.knowledge_base import invalidate as invalidate_kb

app = FastAPI(title="EduAgent Service", version="1.0.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:8080", "http://localhost:8081", "http://localhost:3000", "*"],
    allow_methods=["*"],
    allow_headers=["*"],
)


# ── 请求模型 ───────────────────────────────────────────────────────────────────

class ChatRequest(BaseModel):
    message: str
    user_id: int
    user_type: str = "student"   # student / teacher
    session_id: str | None = None


class InvalidateRequest(BaseModel):
    user_id: int


def _extract_stream_text(chunk) -> str:
    """兼容 str / list[dict] 等 chunk 格式。"""
    if chunk is None:
        return ""
    c = getattr(chunk, "content", None)
    if c is None and isinstance(chunk, str):
        return chunk
    if c is None:
        return ""
    if isinstance(c, str):
        return c
    if isinstance(c, list):
        parts = []
        for block in c:
            if isinstance(block, dict):
                parts.append(block.get("text", "") or str(block.get("content", "")))
            else:
                parts.append(str(block))
        return "".join(parts)
    return str(c)


def _last_ai_text(messages) -> str:
    """取最后一条 AI 文本回复。"""
    for m in reversed(messages or []):
        name = m.__class__.__name__
        if name == "AIMessage" or getattr(m, "type", None) == "ai":
            t = _extract_stream_text(m)
            if t and t.strip():
                return t.strip()
    return ""


# ── 流式对话（SSE）─────────────────────────────────────────────────────────────

@app.post("/agent/chat/stream")
async def chat_stream(req: ChatRequest):
    """
    流式对话接口，使用 SSE 推送以下事件：
    - {"type": "step_start", "step": {...}}  Agent 开始某一步骤
    - {"type": "tool_call", "tool": "...", "input": {...}}  调用工具
    - {"type": "tool_result", "tool": "...", "output": "..."}  工具返回
    - {"type": "text_chunk", "content": "..."}  LLM 生成文字（逐字）
    - {"type": "step_done", "step": {...}}  步骤完成
    - {"type": "done"}  全部完成
    - {"type": "error", "message": "..."}  错误
    """
    async def event_generator():
        try:
            session_id = get_or_create_session(req.user_id, req.session_id)

            # 保存用户消息
            save_message(session_id, "human", req.message)

            # 加载历史并构建输入
            history = load_history(session_id, limit=10)
            # 最后一条是刚刚保存的，history 包含它；去掉最后一条再加 HumanMessage
            if history and hasattr(history[-1], "content") and history[-1].content == req.message:
                history = history[:-1]
            
            messages = history + [HumanMessage(content=req.message)]

            # 发送 session_id（前端需要保存用于多轮）
            yield f"data: {json.dumps({'type': 'session_id', 'session_id': session_id}, ensure_ascii=False)}\n\n"
            yield f"data: {json.dumps({'type': 'status', 'message': '正在调用 Agent（可能需多次访问大模型与数据库，请稍候）'}, ensure_ascii=False)}\n\n"

            agent = build_agent(req.user_type, req.user_id)

            full_ai_response: list[str] = []
            step_counter = [0]
            active_steps: dict[str, dict] = {}
            last_teacher_class_analysis_output: str | None = None
            force_tool_only_response = False

            async for event in agent.astream_events(
                {"messages": messages},
                version="v2",
                config={"recursion_limit": 15},
            ):
                event_type = event.get("event", "")
                event_name = event.get("name", "")

                # LangGraph 节点开始
                if event_type == "on_chain_start" and "agent" in event_name.lower():
                    step_counter[0] += 1
                    step = {
                        "stepId": step_counter[0],
                        "stepName": "Agent 思考中",
                        "description": "分析问题，决定下一步行动",
                        "status": "running",
                        "timestamp": asyncio.get_event_loop().time() * 1000,
                    }
                    active_steps[event_name] = step
                    yield f"data: {json.dumps({'type': 'step_start', 'step': step}, ensure_ascii=False)}\n\n"

                # 工具开始调用
                elif event_type == "on_tool_start":
                    step_counter[0] += 1
                    tool_input = event.get("data", {}).get("input", {})
                    step = {
                        "stepId": step_counter[0],
                        "stepName": f"调用工具: {_tool_display_name(event_name)}",
                        "description": _tool_description(event_name),
                        "status": "running",
                        "timestamp": asyncio.get_event_loop().time() * 1000,
                        "details": {"tool": event_name, "input": str(tool_input)[:200]},
                    }
                    active_steps[event_name] = step
                    yield f"data: {json.dumps({'type': 'tool_call', 'tool': event_name, 'step': step}, ensure_ascii=False)}\n\n"

                # 工具返回结果
                elif event_type == "on_tool_end":
                    output = str(event.get("data", {}).get("output", ""))
                    step = active_steps.get(event_name, {})
                    step["status"] = "completed"
                    if "details" not in step:
                        step["details"] = {}
                    step["details"]["output"] = output[:300]
                    yield f"data: {json.dumps({'type': 'tool_result', 'tool': event_name, 'step': step}, ensure_ascii=False)}\n\n"
                    # 如果 teacher_class_analysis 明确返回“数据不足”，则禁止模型继续编造回复：
                    # 直接用工具输出作为最终回答。
                    if event_name == "teacher_class_analysis" and output.strip().startswith("【数据不足】"):
                        last_teacher_class_analysis_output = output.strip()
                        force_tool_only_response = True

                # LLM 流式 token（不同版本事件名可能不同，尽量都收）
                elif event_type in ("on_chat_model_stream", "on_llm_stream"):
                    chunk = event.get("data", {}).get("chunk")
                    piece = _extract_stream_text(chunk)
                    if piece and not force_tool_only_response:
                        full_ai_response.append(piece)
                        yield f"data: {json.dumps({'type': 'text_chunk', 'content': piece}, ensure_ascii=False)}\n\n"

                # 部分版本在 end 事件里带完整 assistant 文本
                elif event_type == "on_chat_model_end":
                    chunk = event.get("data", {}).get("output")
                    piece = _extract_stream_text(chunk)
                    if piece and not full_ai_response and not force_tool_only_response:
                        full_ai_response.append(piece)
                        yield f"data: {json.dumps({'type': 'text_chunk', 'content': piece}, ensure_ascii=False)}\n\n"

            # LangGraph 常不把最终回复拆成 token 流：若上面没收齐，再同步跑一次拿最终 AIMessage（仅兜底，不重复写库逻辑）
            complete_response = "".join(full_ai_response)
            if force_tool_only_response and last_teacher_class_analysis_output:
                complete_response = last_teacher_class_analysis_output
            elif not complete_response.strip():
                try:
                    yield f"data: {json.dumps({'type': 'status', 'message': '正在汇总最终回答…'}, ensure_ascii=False)}\n\n"
                    final_state = await agent.ainvoke(
                        {"messages": messages},
                        config={"recursion_limit": 15},
                    )
                    complete_response = _last_ai_text(final_state.get("messages", []))
                    if complete_response:
                        yield f"data: {json.dumps({'type': 'text_chunk', 'content': complete_response}, ensure_ascii=False)}\n\n"
                except Exception as ex:
                    yield f"data: {json.dumps({'type': 'error', 'message': f'Agent 执行失败: {ex}'}, ensure_ascii=False)}\n\n"

            if complete_response.strip():
                save_message(session_id, "ai", complete_response.strip())

            yield f"data: {json.dumps({'type': 'done', 'session_id': session_id}, ensure_ascii=False)}\n\n"

        except Exception as e:
            yield f"data: {json.dumps({'type': 'error', 'message': str(e)}, ensure_ascii=False)}\n\n"

    return StreamingResponse(
        event_generator(),
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache",
            "X-Accel-Buffering": "no",
        },
    )


# ── 普通对话（非流式，兜底）────────────────────────────────────────────────────

@app.post("/agent/chat")
async def chat(req: ChatRequest):
    try:
        session_id = get_or_create_session(req.user_id, req.session_id)
        save_message(session_id, "human", req.message)
        history = load_history(session_id, limit=10)
        if history and history[-1].content == req.message:
            history = history[:-1]
        messages = history + [HumanMessage(content=req.message)]

        agent = build_agent(req.user_type, req.user_id)
        result = await agent.ainvoke({"messages": messages})

        last_msg = result["messages"][-1]
        response_text = last_msg.content if hasattr(last_msg, "content") else str(last_msg)
        save_message(session_id, "ai", response_text)

        return {"session_id": session_id, "response": response_text}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


# ── 历史会话 ───────────────────────────────────────────────────────────────────

@app.get("/agent/sessions")
def get_sessions(user_id: int):
    sessions = list_sessions(user_id)
    return {"sessions": sessions}


@app.get("/agent/sessions/{session_id}/messages")
def get_session_messages(session_id: str, user_id: int):
    from memory.conversation import load_history
    history = load_history(session_id, limit=50)
    messages = []
    for msg in history:
        role = "human" if msg.__class__.__name__ == "HumanMessage" else "ai"
        messages.append({"role": role, "content": msg.content})
    return {"messages": messages}


# ── 向量缓存管理 ───────────────────────────────────────────────────────────────

@app.post("/agent/knowledge/invalidate")
def knowledge_invalidate(req: InvalidateRequest):
    """知识点更新后调用，清除该用户的 FAISS 向量缓存"""
    invalidate_kb(req.user_id)
    return {"success": True}


# ── 健康检查 ───────────────────────────────────────────────────────────────────

@app.get("/health")
def health():
    return {"status": "ok", "service": "EduAgent"}


# ── 工具名称映射 ───────────────────────────────────────────────────────────────

_TOOL_NAMES = {
    "knowledge_search": "知识库检索",
    "analyze_my_errors": "错题分析",
    "analyze_student_errors": "学生错题分析",
    "error_analysis": "错题分析",
    "generate_targeted_questions": "针对性出题",
    "generate_study_plan": "学习计划生成",
    "teacher_class_analysis": "班级学情分析",
}

_TOOL_DESCS = {
    "knowledge_search": "正在从你的知识点笔记中语义检索相关内容...",
    "analyze_my_errors": "正在分析你的错题本，寻找薄弱点...",
    "analyze_student_errors": "正在分析该学生的错题...",
    "error_analysis": "正在分析错题本...",
    "generate_targeted_questions": "正在根据薄弱点生成针对性练习题...",
    "generate_study_plan": "正在综合你的学习数据，生成个性化建议...",
    "teacher_class_analysis": "正在分析全班学生学习数据...",
}


def _tool_display_name(tool_name: str) -> str:
    return _TOOL_NAMES.get(tool_name, tool_name)


def _tool_description(tool_name: str) -> str:
    return _TOOL_DESCS.get(tool_name, f"正在调用 {tool_name}...")


if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)
