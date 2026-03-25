"""
工具5：TeacherAnalysisTool（教师专属）
作用：分析全班学生学习情况，找出需要重点关注的学生，给教学建议
"""
import sys, os
sys.path.insert(0, os.path.dirname(os.path.dirname(__file__)))

from langchain.tools import BaseTool
from pydantic import BaseModel, Field
from db import query


class TeacherAnalysisInput(BaseModel):
    teacher_id: int = Field(description="教师ID")
    analysis_type: str = Field(
        default="overview",
        description="分析类型: overview（全体学生概览）/ at_risk（需关注学生）/ homework（作业情况）",
    )


class TeacherAnalysisTool(BaseTool):
    name: str = "teacher_class_analysis"
    description: str = (
        "教师专用：分析全班学生学习情况。"
        "analysis_type='overview' 给出班级整体学习状况；"
        "analysis_type='at_risk' 找出学习时长不足或错题多的学生；"
        "analysis_type='homework' 分析各作业的完成情况。"
        "教师提问时使用，学生不应使用此工具。"
    )
    args_schema: type[BaseModel] = TeacherAnalysisInput

    def _run(self, teacher_id: int, analysis_type: str = "overview") -> str:
        if analysis_type == "at_risk":
            return self._at_risk_analysis(teacher_id)
        elif analysis_type == "homework":
            return self._homework_analysis(teacher_id)
        else:
            return self._overview_analysis(teacher_id)

    def _overview_analysis(self, teacher_id: int) -> str:
        # 老师权限：默认管理所有学生，不依赖 classes/student_class/年级/学科。
        # 同时补充 student_id，避免模型在后续回答时编造/猜测。
        stats = query(
            """
            SELECT
              u.id as student_id,
              u.name,
              COALESCE((SELECT SUM(sr.duration_minutes) FROM study_records sr
                        WHERE sr.user_id=u.id AND sr.record_date >= DATE_SUB(CURDATE(), INTERVAL 7 DAY)
                       ), 0) as week_minutes,
              COALESCE((SELECT COUNT(*) FROM error_questions eq WHERE eq.student_id=u.id), 0) as error_count,
              COALESCE((SELECT COUNT(*) FROM student_homework sh
                        WHERE sh.student_id=u.id AND sh.status='completed'
                       ), 0) as done_hw,
              COALESCE((SELECT COUNT(*) FROM student_homework sh
                        WHERE sh.student_id=u.id AND sh.status='pending'
                       ), 0) as pending_hw
            FROM users u
            WHERE u.user_type='student'
            GROUP BY u.id, u.name
            ORDER BY week_minutes DESC
            """,
            (),
        )
        if not stats:
            return (
                "【数据不足】暂无学生数据（请检查 study_records/error_questions/student_homework 是否有写入）。"
            )

        # 注意：此处避免“再调用 LLM 编造补全”，而是把 SQL 结果做成确定性汇总，
        # 让 agent 后续只做解释/润色，不会凭空编造学生信息。
        n = len(stats)
        avg_week = round(sum(r["week_minutes"] for r in stats) / n, 1)
        total_errors = sum(r["error_count"] for r in stats)
        total_done_hw = sum(r["done_hw"] for r in stats)

        # 需关注条件：学习时长偏少 或 错题偏多
        focus_candidates = [
            r for r in stats
            if r["week_minutes"] < 60 or r["error_count"] > 5 or r["pending_hw"] > 2
        ]
        focus_candidates_sorted = sorted(
            focus_candidates,
            key=lambda x: (-x["error_count"], x["week_minutes"], -x["pending_hw"])
        )

        top3 = sorted(stats, key=lambda x: (-x["week_minutes"], -x["error_count"]))[:3]
        focus5 = focus_candidates_sorted[:5]

        lines = [
            "【全体学生学习概览（教师默认权限）】",
            f"本周统计共 {n} 名学生，平均学习时长 {avg_week} 分钟，错题总次数 {total_errors}。",
        ]
        lines.append(f"作业完成（已完成）累计 {total_done_hw} 次（按 student_homework.status='completed' 统计）。")

        lines.append("")
        lines.append("## 学习最积极的3名学生")
        if not top3:
            lines.append("暂无学习数据。")
        else:
            for r in top3:
                lines.append(f"- {r['name']}（ID: {r['student_id']}）：学习 {r['week_minutes']} 分钟，错题 {r['error_count']}，已完成作业 {r['done_hw']}，待完成作业 {r['pending_hw']}。")

        lines.append("")
        lines.append("## 需要关注的学生（学习时长偏少/错题偏多/待完成作业）")
        if not focus5:
            lines.append("暂无明显需要重点关注的学生（满足条件的学生数为 0）。")
        else:
            for r in focus5:
                reason = []
                if r["week_minutes"] < 60:
                    reason.append("学习时长<60")
                if r["error_count"] > 5:
                    reason.append("错题数>5")
                if r["pending_hw"] > 2:
                    reason.append("待完成作业>2")
                lines.append(f"- {r['name']}（ID: {r['student_id']}）：学习 {r['week_minutes']} 分钟，错题 {r['error_count']}，待完成作业 {r['pending_hw']}；触发条件：{'/'.join(reason)}。")

        lines.append("")
        lines.append("## 对教师的教学建议（基于上述指标）")
        if any(r["week_minutes"] < 60 for r in stats):
            lines.append("- 对学习时长偏少的学生：建议采用“短周期+强反馈”的练习节奏（例如每次10-15分钟，完成即刻订正/讲评）。")
        if any(r["error_count"] > 5 for r in stats):
            lines.append("- 对错题偏多的学生：建议从错题本中提炼薄弱知识点后进行针对性复盘（先讲错因，再给同类变式练习）。")
        if any(r["pending_hw"] > 2 for r in stats):
            lines.append("- 对待完成作业偏多的学生：建议将作业拆分为阶段性任务，并在截止前提供一次中间检查。")
        if len(lines) < 8:
            lines.append("- 数据显示班级整体较稳定，可继续保持现有课堂练习与巩固节奏。")

        # 保留可被 agent 继续润色的结构化文本
        return "\n".join(lines)

    def _at_risk_analysis(self, teacher_id: int) -> str:
        # 老师权限：默认管理所有学生，不依赖 classes/student_class。
        at_risk = query(
            """
            SELECT
              u.id as student_id,
              u.name,
              COALESCE((SELECT SUM(sr.duration_minutes) FROM study_records sr
                        WHERE sr.user_id=u.id AND sr.record_date >= DATE_SUB(CURDATE(), INTERVAL 7 DAY)
                       ), 0) as week_minutes,
              COALESCE((SELECT COUNT(*) FROM error_questions eq WHERE eq.student_id=u.id), 0) as error_count,
              COALESCE((SELECT COUNT(*) FROM student_homework sh
                        WHERE sh.student_id=u.id AND sh.status='pending'
                       ), 0) as pending_hw
            FROM users u
            WHERE u.user_type='student'
            GROUP BY u.id, u.name
            HAVING week_minutes < 60 OR error_count > 5 OR pending_hw > 2
            ORDER BY week_minutes ASC
            """,
            (),
        )
        if not at_risk:
            return "目前没有明显需要重点关注的学生（按统计阈值筛选），班级整体情况良好！"

        lines = ["【需重点关注的学生（全体学生，教师默认权限）】"]
        # 展示最多 5 个，避免信息过载；如需要可在 agent 中继续追问
        for r in at_risk[:5]:
            reasons = []
            if r["week_minutes"] < 60:
                reasons.append("学习时长偏少")
            if r["error_count"] > 5:
                reasons.append("错题偏多")
            if r["pending_hw"] > 2:
                reasons.append("待完成作业偏多")

            # 干预建议只做“基于数据的行动建议”，不编造原因为具体的推断事实
            actions = []
            if r["week_minutes"] < 60:
                actions.append("安排短时高频练习并设置完成提醒")
            if r["error_count"] > 5:
                actions.append("组织错题复盘：先讲错因，再做同类变式训练")
            if r["pending_hw"] > 2:
                actions.append("把作业拆成阶段任务，截止前做一次中间检查")

            lines.append(
                f"- {r['name']}（ID: {r['student_id']}）：学习 {r['week_minutes']} 分钟；错题 {r['error_count']}；待完成作业 {r['pending_hw']}。"
                f"主要问题：{'/'.join(reasons)}；建议行动：{'；'.join(actions)}。"
            )

        return "\n".join(lines)

    def _homework_analysis(self, teacher_id: int) -> str:
        hw_stats = query(
            """
            SELECT
                h.title,
                h.due_date,
                COUNT(sh.id) as assigned_count,
                SUM(CASE WHEN sh.status='completed' THEN 1 ELSE 0 END) as completed_count,
                AVG(CASE WHEN sh.status='completed' THEN sh.completion_time END) as avg_time
            FROM homework h
            LEFT JOIN student_homework sh ON h.id = sh.homework_id
            WHERE h.teacher_id = %s
            GROUP BY h.id ORDER BY h.created_at DESC LIMIT 10
            """,
            (teacher_id,),
        )
        if not hw_stats:
            return "暂无作业数据。"

        data = []
        for r in hw_stats:
            assigned = r["assigned_count"] or 0
            completed = r["completed_count"] or 0
            rate = round(completed / assigned * 100, 1) if assigned > 0 else 0
            data.append({"title": r["title"], "due": str(r["due_date"]),
                         "rate": f"{rate}%", "avg_time_min": round(float(r["avg_time"] or 0), 1)})
        # 亦避免再调 LLM 编造“原因”，只基于完成率与平均用时做确定性结论与建议。
        low_rate = [d for d in data if float(d["rate"].replace('%', '')) < 60]
        # 取平均用时最大（忽略 rate==0 的“无数据”情况）
        top_time = sorted(data, key=lambda x: x["avg_time_min"], reverse=True)[:3]

        lines = ["【作业完成情况分析（教师维度）】"]
        lines.append(f"共分析 {len(data)} 项作业。")
        lines.append("")
        lines.append("## 完成率概览（最近10项作业）")
        for d in data:
            lines.append(f"- {d['title']}（到期 {d['due']}）：完成率 {d['rate']}，平均用时 {d['avg_time_min']} 分钟。")

        lines.append("")
        lines.append("## 需要重点关注的作业（完成率 < 60%）")
        if not low_rate:
            lines.append("暂无明显低完成率作业。")
        else:
            for d in low_rate[:5]:
                lines.append(f"- {d['title']}：完成率 {d['rate']}（平均用时 {d['avg_time_min']} 分钟）")

        lines.append("")
        lines.append("## 教学/布置建议（基于完成率与用时）")
        lines.append("- 对低完成率作业：建议拆分为阶段任务，并提供“示例题/检查清单”，降低启动难度。")
        lines.append("- 对平均用时偏长的作业：建议优先讲评该作业中最关键的知识点，并给出同类变式练习巩固。")
        lines.append("- 如需更精确的原因归因（题目难度/流程问题），建议同时查看各学生的错题与作业提交过程数据。")

        return "\n".join(lines)

    async def _arun(self, teacher_id: int, analysis_type: str = "overview") -> str:
        return self._run(teacher_id, analysis_type)
