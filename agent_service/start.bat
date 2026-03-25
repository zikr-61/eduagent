@echo off
echo ============================================
echo  EduAgent Python Service Startup
echo ============================================

REM 检查虚拟环境
if not exist "venv\Scripts\activate.bat" (
    echo [1/3] 创建虚拟环境...
    python -m venv venv
)

echo [2/3] 激活虚拟环境并安装依赖...
call venv\Scripts\activate.bat
pip install -r requirements.txt -q

echo [3/3] 启动 FastAPI 服务（端口 8000）...
echo Agent 服务地址: http://localhost:8000
echo 健康检查:      http://localhost:8000/health
echo API 文档:      http://localhost:8000/docs
echo.
python main.py

pause
