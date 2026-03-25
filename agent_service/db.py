"""MySQL 直连工具（Python 侧只读查询，写操作仍走 Spring Boot API）"""
import pymysql
from config import DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASS


def get_conn():
    return pymysql.connect(
        host=DB_HOST,
        port=DB_PORT,
        db=DB_NAME,
        user=DB_USER,
        password=DB_PASS,
        charset="utf8mb4",
        cursorclass=pymysql.cursors.DictCursor,
    )


def query(sql: str, args=None) -> list[dict]:
    conn = get_conn()
    try:
        with conn.cursor() as cur:
            cur.execute(sql, args or ())
            return cur.fetchall()
    finally:
        conn.close()


def query_one(sql: str, args=None) -> dict | None:
    rows = query(sql, args)
    return rows[0] if rows else None
