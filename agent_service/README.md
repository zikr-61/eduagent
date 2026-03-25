# EduAgent Python 服务（LangGraph + FastAPI）

监听 **8000** 端口。前端只访问 Spring Boot **8080**，由 Java 反向代理到本服务。

## 环境要求

- Python 3.10+
- 已安装 MySQL，且库名/账号与 `application.properties`、`config.py` 一致
- 与 `eduagent.sql` 中 `chat_sessions`、`chat_messages` 表已创建

## 方式一：IntelliJ IDEA

1. 安装插件 **Python**（Settings → Plugins → 搜索 Python）。
2. File → Project Structure → SDK → 添加本机 Python 解释器。
3. 在 `agent_service` 目录打开终端，执行：
   ```bash
   python -m venv venv
   venv\Scripts\activate
   pip install -r requirements.txt
   ```
4. 右键 **`main.py`** → **Run 'main'**（或 Debug）。
5. 控制台出现 `Uvicorn running on http://0.0.0.0:8000` 即成功。

可选：Run → Edit Configurations → **+** → Python → Script path 选 `main.py`，Working directory 选 `agent_service` 文件夹。

## 方式二：命令行 / start.bat

```bash
cd agent_service
pip install -r requirements.txt
python main.py
```

Windows 可双击 **`start.bat`**。

## 配置

复制 `.env.example` 为 `.env`，填写 `QWEN_API_KEY` 等（与后端 Qwen 配置一致即可）。

## 验证

- 浏览器打开：<http://localhost:8000/health> 应返回 `{"status":"ok",...}`
- Spring Boot 启动后，前端「AI 辅导」页应不再报直连 8000 的 `ERR_CONNECTION_REFUSED`（会话列表走 `8080/api/agent/proxy/...`）。

---

## 费用说明（自定义 Agent 是否还要付 API 钱？）

**要。**「自定义 Agent」指的是：**编排方式**（LangGraph、工具、ReAct）由你实现；**底层能力**仍然依赖云端大模型与向量接口，只要发起调用，就会按服务商（如阿里云 DashScope）的 **Token / 次数** 计费。

典型计费点：

| 环节 | 说明 |
|------|------|
| **对话主模型** | `qwen-plus` 等，每轮 ReAct 可能多轮「思考 + 选工具 + 再回答」，调用次数多于「单次问答」。 |
| **工具内再调 LLM** | 如错题分析、学习计划、出题等工具内部会再次调用模型，会额外计费。 |
| **向量检索（RAG）** | `text-embedding-v3` 建索引/查询时按 embedding 计费（用量一般小于对话）。 |

**不花钱的部分**：本地 FAISS 检索计算、MySQL 查询、Spring 转发，不走大模型 API。

节省费用的方向：换更小模型、减少 `recursion_limit`、精简工具内 Prompt、缓存 embedding 索引（已实现进程内缓存）。

---

## pip 报错：`ProxyError` / `Could not find a version`

说明：**pip 走了系统里失效的 HTTP 代理**，连清华源也握手超时，并不是真的没有 `fastapi` 这个包。

### Windows CMD（当前窗口临时关掉代理）

```bat
set HTTP_PROXY=
set HTTPS_PROXY=
set ALL_PROXY=
pip install -r requirements.txt -i https://pypi.org/simple
```

若你**必须用国内镜像**且代理已关，可再试：

```bat
pip install -r requirements.txt -i https://pypi.tuna.tsinghua.edu.cn/simple
```

### 查看 pip 是否配置了代理

```bat
pip config list
```

若有 `global.proxy`，可执行：

```bat
pip config unset global.proxy
```

或在用户目录下编辑 `pip\pip.ini`，删掉 `[global]` 里的 `proxy = ...`。

### 在「未激活 venv」时你用的到底是哪个 Python？

在 `agent_service` 里执行：

```bat
where python
python -c "import sys; print(sys.executable)"
```

- 若**没**先执行 `venv\Scripts\activate`，用的通常是 **系统 PATH 里第一个** `python.exe`（可能是商店版、Anaconda、或别的安装）。
- 激活虚拟环境后应显示路径类似：`...\agent_service\venv\Scripts\python.exe`。

---

## IntelliJ IDEA 能用 Conda 环境吗？**可以**

1. **Settings**（或 **File → Project Structure**）→ **Project → Python Interpreter**。  
2. 点齿轮 → **Add Interpreter → Add Local Interpreter**。  
3. 选 **Conda Environment**：  
   - 选已有环境，或 **Create new** 新建（指定 Python 3.10+）。  
4. 应用后，IDEA 底部 **Terminal** 里若配置了「Activate conda」，会自动进该环境；否则手动：  
   `conda activate 你的环境名`  
5. 在该环境中安装依赖：  
   `pip install -r requirements.txt`（同样可先关代理，见上文）。  
6. **Run/Debug Configuration**：Python 解释器选上述 **Conda 环境**，Script path 选 `main.py`，Working directory 选 **`agent_service` 文件夹**。

**不必**同时用 `venv` 和 `conda`：二选一即可（Conda 里装全依赖就不用再建 `venv`）。

---

## 小结

| 问题 | 处理 |
|------|------|
| `ProxyError` / SSL 握手超时 | 临时 `set HTTP_PROXY=` / `HTTPS_PROXY=`，或 `pip config unset global.proxy` |
| `No matching distribution` 紧跟上面错误 | 多半是网络没连上索引，不是版本号写错 |
| IDEA 用 Conda | Project Interpreter 里添加 Conda，Run 配置指向该解释器 |
| 当前是哪个 Python | `where python` 与 `python -c "import sys; print(sys.executable)"` |
