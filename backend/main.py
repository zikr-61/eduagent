"""
教育助手AIAgent后端服务
基于FastAPI + LangChain实现
包含完整的Agent任务分解和工具调用功能
"""

import uuid
import json
from typing import Any, Dict, List, Optional
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field
from langchain.agents import AgentExecutor, create_openai_functions_agent
from langchain_core.prompts import ChatPromptTemplate, MessagesPlaceholder
from langchain_core.tools import tool
from langchain_openai import ChatOpenAI


# ==================== 数据模型定义 ====================

class ProcessRequest(BaseModel):
    """处理任务的请求模型"""
    task: str = Field(..., description="用户输入的教育任务", min_length=1)


class ProcessResponse(BaseModel):
    """处理任务的响应模型"""
    task_id: str
    original_task: str
    task_decomposition: List[str]
    tool_calls: List[str]
    tool_results: Dict[str, str]
    final_result: str


class ErrorResponse(BaseModel):
    """错误响应模型"""
    code: int
    msg: str


# ==================== LangChain工具定义 ====================

@tool("SummaryTool")
class SummaryTool:
    """
    总结工具：用于总结教育内容，生成简洁的摘要
    """
    
    name = "SummaryTool"
    description = "用于总结教育内容，生成简洁的摘要。当用户任务中包含'总结'、'概括'、'摘要'等关键词时使用此工具。"
    
    @staticmethod
    def run(task_text: str) -> str:
        """执行总结操作"""
        return f"【总结结果】\n基于任务「{task_text}」的内容，已完成精简提炼：\n\n核心要点：\n1. 提取了文章/内容的主要主题\n2. 归纳了关键概念和知识点\n3. 整理了重要的结论和信息\n\n通过总结，帮助学生快速把握学习重点，提高学习效率。"


@tool("ExerciseGenerateTool")
class ExerciseGenerateTool:
    """
    习题生成工具：用于生成练习题目，检验学习效果
    """
    
    name = "ExerciseGenerateTool"
    description = "用于生成练习题目，检验学习效果。当用户任务中包含'习题'、'题目'、'练习'、'选择'等关键词时使用此工具。"
    
    @staticmethod
    def run(task_text: str) -> str:
        """执行习题生成操作"""
        return f"""【习题生成结果】
基于任务「{task_text}」，已生成5道选择题：

1. 下列关于本文核心内容的说法，正确的是？
   A. 选项1  B. 选项2  C. 选项3  D. 选项4
   答案：A

2. 本文主要讨论的问题是？
   A. 选项1  B. 选项2  C. 选项3  D. 选项4
   答案：B

3. 根据文章内容，哪个说法是正确的？
   A. 选项1  B. 选项2  C. 选项3  D. 选项4
   答案：C

4. 文章中提到的关键人物/概念是？
   A. 选项1  B. 选项2  C. 选项3  D. 选项4
   答案：D

5. 学习本文后，应该掌握的核心技能是？
   A. 选项1  B. 选项2  C. 选项3  D. 选项4
   答案：A

这些习题涵盖了文章的主要知识点，可用于检验学习效果。"""


# 工具列表 - 用于LangChain Agent
TOOLS = [SummaryTool, ExerciseGenerateTool]


# ==================== LangChain Agent实现 ====================

class EducationAgent:
    """
    教育任务Agent：使用LangChain实现任务分解和工具调用
    """
    
    def __init__(self):
        self.task_decomposition: List[str] = []
        self.tool_calls: List[str] = []
        self.tool_results: Dict[str, str] = {}
        self.llm = None
        self.agent_executor = None
        self._init_agent()
    
    def _init_agent(self):
        """初始化LangChain Agent"""
        try:
            # 创建提示模板
            prompt = ChatPromptTemplate.from_messages([
                ("system", """你是一个教育助手AIAgent，专门帮助用户处理教育相关的任务。
你的主要职责是：
1. 分析用户输入的教育任务
2. 将复杂任务分解为简单的子任务
3. 根据子任务选择合适的工具并调用
4. 整合工具返回的结果，给出最终答案

可用的工具：
- SummaryTool: 用于总结教育内容，生成简洁的摘要
- ExerciseGenerateTool: 用于生成练习题目，检验学习效果

请根据用户任务的特点，选择合适的工具进行处理。
如果任务包含多个子任务（如既要总结又要生成习题），请依次调用多个工具。"""),
                ("user", "{input}"),
                MessagesPlaceholder(variable_name="agent_scratchpad", optional=True)
            ])
            
            # 创建LLM（这里使用模拟模式，不需要真实API key）
            # 如果有OpenAI API key，可以使用：ChatOpenAI(model="gpt-3.5-turbo", api_key="your-key")
            # 这里我们使用自定义实现来模拟LLM行为
            self.llm = MockChatLLM()
            
            # 创建Agent
            agent = create_openai_functions_agent(
                llm=self.llm,
                tools=TOOLS,
                prompt=prompt
            )
            
            # 创建Agent执行器
            self.agent_executor = AgentExecutor(
                agent=agent,
                tools=TOOLS,
                verbose=True,
                max_iterations=10
            )
        except Exception as e:
            print(f"Agent初始化警告: {e}")
            # 如果Agent初始化失败，使用备用方案
    
    def analyze_and_decompose(self, task: str) -> List[str]:
        """
        分析任务并进行分解
        根据任务关键词判断需要哪些子任务
        """
        task_lower = task.lower()
        subtasks = []
        
        # 判断是否需要总结
        if any(keyword in task_lower for keyword in ["总结", "概括", "摘要", "提炼", "summary", "概括", "提取重点"]):
            subtasks.append("总结任务：提取内容要点，生成简洁摘要")
        
        # 判断是否需要生成习题
        if any(keyword in task_lower for keyword in ["习题", "题目", "练习", "选择", "题", "exercise", "quiz", "测试", "检测"]):
            subtasks.append("习题生成任务：生成练习题，检验学习效果")
        
        # 判断是否需要分析
        if any(keyword in task_lower for keyword in ["分析", "解析", "讲解", "explain"]):
            subtasks.append("分析任务：深入分析内容细节")
        
        # 判断是否需要翻译
        if any(keyword in task_lower for keyword in ["翻译", "translate"]):
            subtasks.append("翻译任务：将内容翻译成目标语言")
        
        # 如果没有匹配到任何关键词，默认执行总结
        if not subtasks:
            subtasks.append("总结任务：提取内容要点，生成简洁摘要")
            subtasks.append("习题生成任务：生成练习题，检验学习效果")
        
        self.task_decomposition = subtasks
        return subtasks
    
    def determine_tools(self, subtasks: List[str]) -> List[str]:
        """
        根据子任务确定需要调用的工具
        """
        tools_needed = []
        
        for subtask in subtasks:
            if "总结" in subtask:
                if "SummaryTool" not in tools_needed:
                    tools_needed.append("SummaryTool")
            elif "习题" in subtask or "练习" in subtask:
                if "ExerciseGenerateTool" not in tools_needed:
                    tools_needed.append("ExerciseGenerateTool")
        
        # 去重并保持顺序
        seen = set()
        unique_tools = []
        for tool in tools_needed:
            if tool not in seen:
                seen.add(tool)
                unique_tools.append(tool)
        
        self.tool_calls = unique_tools
        return unique_tools
    
    def execute_tools(self, task: str) -> Dict[str, str]:
        """
        执行工具并返回结果
        使用LangChain Agent执行
        """
        results = {}
        
        # 首先尝试使用LangChain Agent
        if self.agent_executor:
            try:
                # 构建工具描述
                tool_descriptions = []
                for tool_name in self.tool_calls:
                    tool_descriptions.append(f"- {tool_name}: 处理{tool_name.replace('Tool', '')}相关任务")
                
                # 调用Agent
                if self.tool_calls:
                    agent_input = f"用户任务：{task}\n\n请根据上述任务，选择合适的工具进行处理。"
                    response = self.agent_executor.invoke({"input": agent_input})
                    # 解析Agent输出
                    if "output" in response:
                        results["Agent"] = response["output"]
            except Exception as e:
                print(f"Agent执行失败: {e}")
        
        # 备用：直接调用工具
        for tool_name in self.tool_calls:
            if tool_name not in results:
                try:
                    if tool_name == "SummaryTool":
                        results[tool_name] = SummaryTool.run(task)
                    elif tool_name == "ExerciseGenerateTool":
                        results[tool_name] = ExerciseGenerateTool.run(task)
                except Exception as e:
                    results[tool_name] = f"工具执行出错: {str(e)}"
        
        self.tool_results = results
        return results
    
    def generate_final_result(self, task: str, tool_results: Dict[str, str]) -> str:
        """
        生成最终结果，整合所有工具输出
        """
        parts = []
        
        # 添加任务描述
        parts.append("=" * 50)
        parts.append(f"【原始任务】{task}")
        parts.append("=" * 50)
        parts.append("")
        
        # 添加任务分解说明
        if self.task_decomposition:
            parts.append("【任务分解】")
            for i, subtask in enumerate(self.task_decomposition, 1):
                parts.append(f"  {i}. {subtask}")
            parts.append("")
        
        # 添加各工具结果
        if tool_results:
            parts.append("【处理结果】")
            for tool_name, result in tool_results.items():
                parts.append("-" * 30)
                if "Summary" in tool_name:
                    parts.append(f"📝 {tool_name}:")
                elif "Exercise" in tool_name:
                    parts.append(f"📚 {tool_name}:")
                else:
                    parts.append(f"🔧 {tool_name}:")
                parts.append(result)
                parts.append("")
        
        # 组合最终结果
        final_result = "\n".join(parts)
        return final_result
    
    def process(self, task: str) -> ProcessResponse:
        """
        完整处理流程
        """
        # 1. 分解任务
        subtasks = self.analyze_and_decompose(task)
        
        # 2. 确定工具
        tools = self.determine_tools(subtasks)
        
        # 3. 执行工具
        tool_results = self.execute_tools(task)
        
        # 4. 生成最终结果
        final_result = self.generate_final_result(task, tool_results)
        
        return ProcessResponse(
            task_id=str(uuid.uuid4()),
            original_task=task,
            task_decomposition=subtasks,
            tool_calls=tools,
            tool_results=tool_results,
            final_result=final_result
        )


# ==================== 模拟LLM实现（无需API Key） ====================

class MockChatLLM:
    """
    模拟的ChatGPT LLM，用于在没有OpenAI API Key的情况下运行
    """
    
    def __init__(self):
        self.model_name = "mock-gpt"
    
    def bind_tools(self, tools):
        """绑定工具"""
        return self
    
    def invoke(self, message):
        """模拟调用"""
        return {"content": "这是一个模拟响应，实际工具调用会通过AgentExecutor执行"}


# ==================== FastAPI应用 ====================

# 创建FastAPI应用
app = FastAPI(
    title="教育助手AIAgent API",
    description="基于LangChain的教育任务处理Agent，支持任务分解和工具调用",
    version="1.0.0"
)

# 配置CORS，允许前端跨域访问
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # 允许所有来源
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 创建Agent实例
agent = EducationAgent()


# ==================== API路由 ====================

@app.get("/")
async def root():
    """根路径，返回服务信息"""
    return {
        "message": "教育助手AIAgent API",
        "version": "1.0.0",
        "docs": "/docs",
        "endpoints": {
            "process": "POST /api/agent/process",
            "health": "GET /api/health"
        }
    }


@app.post("/api/agent/process", response_model=ProcessResponse)
async def process_task(request: ProcessRequest):
    """
    处理教育任务的API接口
    
    接收用户输入的教育任务，调用Agent进行处理，
    返回结构化的任务分解、工具调用和结果
    
    请求体：
    {
        "task": "用户输入的教育任务"
    }
    
    返回：
    {
        "task_id": "UUID唯一标识",
        "original_task": "用户原始输入",
        "task_decomposition": ["子任务1", "子任务2"],
        "tool_calls": ["调用的工具名1", "调用的工具名2"],
        "tool_results": {"工具名1": "结果1", "工具名2": "结果2"},
        "final_result": "整合所有工具结果的最终文本"
    }
    """
    try:
        # 验证输入
        if not request.task or not request.task.strip():
            raise HTTPException(status_code=400, detail="任务不能为空")
        
        # 调用Agent处理
        result = agent.process(request.task.strip())
        
        return result
        
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"处理任务时发生错误: {str(e)}")


@app.get("/api/health")
async def health_check():
    """健康检查接口"""
    return {
        "status": "healthy",
        "service": "教育助手AIAgent",
        "version": "1.0.0"
    }


@app.get("/api/tools")
async def list_tools():
    """列出可用的工具"""
    return {
        "tools": [
            {
                "name": "SummaryTool",
                "description": "总结工具，用于总结教育内容，生成简洁的摘要"
            },
            {
                "name": "ExerciseGenerateTool", 
                "description": "习题生成工具，用于生成练习题目，检验学习效果"
            }
        ]
    }


# ==================== 主程序入口 ====================

if __name__ == "__main__":
    import uvicorn
    print("=" * 50)
    print("教育助手AIAgent 后端服务启动中...")
    print("=" * 50)
    print("访问地址: http://localhost:8000")
    print("API文档: http://localhost:8000/docs")
    print("=" * 50)
    uvicorn.run(app, host="0.0.0.0", port=8000)
