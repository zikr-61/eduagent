"""
FAISS 向量知识库
- 基于学生/教师自己的知识点内容构建本地向量索引
- 不需要外部向量数据库服务，faiss-cpu 是纯 Python 库
"""
import sys, os
sys.path.insert(0, os.path.dirname(os.path.dirname(__file__)))

from langchain_openai import OpenAIEmbeddings
from langchain_community.vectorstores import FAISS
from langchain.text_splitter import RecursiveCharacterTextSplitter
from langchain.schema import Document
from db import query
from config import QWEN_API_KEY, QWEN_BASE_URL, QWEN_EMBED_MODEL

# 每个用户一个内存中的向量索引（进程级缓存）
_index_cache: dict[int, FAISS] = {}


def _get_embeddings():
    return OpenAIEmbeddings(
        model=QWEN_EMBED_MODEL,
        openai_api_key=QWEN_API_KEY,
        openai_api_base=QWEN_BASE_URL,
    )


def build_user_index(user_id: int) -> FAISS | None:
    """从数据库拉取该用户的知识点，构建 FAISS 向量索引"""
    rows = query(
        "SELECT id, title, content, summary FROM knowledge_points WHERE user_id = %s",
        (user_id,),
    )
    if not rows:
        return None

    splitter = RecursiveCharacterTextSplitter(chunk_size=400, chunk_overlap=40)
    docs: list[Document] = []
    for kp in rows:
        text = f"【{kp['title']}】\n{kp['content'] or ''}"
        chunks = splitter.create_documents(
            [text],
            metadatas=[{"kp_id": kp["id"], "title": kp["title"]}],
        )
        docs.extend(chunks)

    if not docs:
        return None

    embeddings = _get_embeddings()
    store = FAISS.from_documents(docs, embeddings)
    _index_cache[user_id] = store
    return store


def get_or_build(user_id: int) -> FAISS | None:
    if user_id not in _index_cache:
        build_user_index(user_id)
    return _index_cache.get(user_id)


def invalidate(user_id: int):
    """知识点更新后调用，清除缓存"""
    _index_cache.pop(user_id, None)


def search(user_id: int, query_text: str, k: int = 3) -> list[dict]:
    """
    语义检索，返回 [{content, title, kp_id, score}]
    使用向量相似度（不是 SQL LIKE 关键词匹配）
    """
    store = get_or_build(user_id)
    if store is None:
        return []
    results = store.similarity_search_with_score(query_text, k=k)
    out = []
    for doc, score in results:
        out.append({
            "content": doc.page_content,
            "title": doc.metadata.get("title", ""),
            "kp_id": doc.metadata.get("kp_id"),
            "score": float(score),
        })
    return out
