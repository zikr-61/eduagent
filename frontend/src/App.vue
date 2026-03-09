<template>
  <div class="app-container">
    <!-- 头部区域 -->
    <header class="header">
      <div class="header-content">
        <h1 class="title">
          <span class="title-icon">🎓</span>
          教育助手AIAgent
        </h1>
        <p class="subtitle">基于LangChain的智能教育任务处理平台</p>
      </div>
    </header>

    <!-- 主体内容区域 -->
    <main class="main-content">
      <div class="container">
        <!-- 输入卡片 -->
        <el-card class="input-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <div class="card-header-left">
                <el-icon><Edit /></el-icon>
                <span>输入教育任务</span>
              </div>
              <div class="word-count">
                <span class="count-label">字数：</span>
                <span class="count-number">{{ taskText.length }}</span>
              </div>
            </div>
          </template>
          
          <!-- 多行输入框 -->
          <el-input
            v-model="taskText"
            type="textarea"
            :rows="6"
            placeholder="请输入教育类任务，例如：
• 总结这篇历史课文的主要内容
• 帮我生成5道关于唐朝的选择题
• 总结这篇英语课文并生成对应的练习题"
            :disabled="loading"
            @input="handleInput"
            class="task-textarea"
            show-word-limit
            :maxlength="2000"
          />
          
          <!-- 按钮组 -->
          <div class="button-group">
            <el-button 
              type="primary" 
              size="large"
              :loading="loading"
              :disabled="!taskText.trim() || loading"
              @click="handleSubmit"
              class="submit-btn"
            >
              <el-icon v-if="!loading"><CaretRight /></el-icon>
              {{ loading ? '处理中...' : '提交任务' }}
            </el-button>
            
            <el-button 
              size="large"
              :disabled="loading"
              @click="handleReset"
              class="reset-btn"
            >
              <el-icon><RefreshRight /></el-icon>
              重置
            </el-button>
          </div>
        </el-card>

        <!-- 加载状态显示 -->
        <div v-if="loading" class="loading-container">
          <el-skeleton :rows="5" animated />
          <div class="loading-text">
            <el-icon class="loading-icon is-loading"><Loading /></el-icon>
            <span>AI正在分析任务并进行任务分解...</span>
          </div>
          <div class="loading-steps">
            <el-steps :active="loadingStep" finish-status="success" align-center>
              <el-step title="任务分析" />
              <el-step title="任务分解" />
              <el-step title="工具选择" />
              <el-step title="执行工具" />
              <el-step title="生成结果" />
            </el-steps>
          </div>
        </div>

        <!-- 结果展示区域 -->
        <el-card v-if="result && !loading" class="result-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <div class="card-header-left">
                <el-icon><DataAnalysis /></el-icon>
                <span>处理结果</span>
              </div>
              <el-tag type="success" effect="dark" round>
                <el-icon><CircleCheck /></el-icon>
                已完成
              </el-tag>
            </div>
          </template>

          <!-- 格式化结果展示 -->
          <div class="result-content">
            <!-- 任务ID -->
            <div class="result-section">
              <div class="section-header">
                <el-icon><Tickets /></el-icon>
                <h3 class="section-title">任务ID</h3>
              </div>
              <div class="section-content">
                <el-tag type="info" effect="plain">{{ result.task_id }}</el-tag>
              </div>
            </div>

            <!-- 原始任务 -->
            <div class="result-section">
              <div class="section-header">
                <el-icon><Document /></el-icon>
                <h3 class="section-title">原始任务</h3>
              </div>
              <div class="section-content original-task">
                {{ result.original_task }}
              </div>
            </div>

            <!-- 任务分解 -->
            <div class="result-section">
              <div class="section-header">
                <el-icon><Operation /></el-icon>
                <h3 class="section-title">任务分解</h3>
              </div>
              <div class="section-content">
                <div class="task-decomposition">
                  <el-tag 
                    v-for="(task, index) in result.task_decomposition" 
                    :key="index"
                    type="warning"
                    effect="plain"
                    class="decomposition-tag"
                  >
                    <el-icon><Star /></el-icon>
                    {{ index + 1 }}. {{ task }}
                  </el-tag>
                </div>
              </div>
            </div>

            <!-- 调用工具 -->
            <div class="result-section">
              <div class="section-header">
                <el-icon><Tools /></el-icon>
                <h3 class="section-title">调用工具</h3>
              </div>
              <div class="section-content">
                <div class="tool-calls">
                  <el-tag 
                    v-for="(tool, index) in result.tool_calls" 
                    :key="index"
                    type="success"
                    effect="dark"
                    class="tool-tag"
                  >
                    <el-icon><Cpu /></el-icon>
                    {{ tool }}
                  </el-tag>
                </div>
              </div>
            </div>

            <!-- 工具结果 -->
            <div class="result-section">
              <div class="section-header">
                <el-icon><List /></el-icon>
                <h3 class="section-title">工具结果</h3>
              </div>
              <div class="section-content">
                <el-collapse v-model="activeCollapse" class="result-collapse">
                  <el-collapse-item 
                    v-for="(toolResult, toolName) in result.tool_results" 
                    :key="toolName"
                    :name="toolName"
                  >
                    <template #title>
                      <div class="collapse-title">
                        <el-icon><Box /></el-icon>
                        <span>{{ toolName }}</span>
                      </div>
                    </template>
                    <div class="tool-result-content">
                      {{ toolResult }}
                    </div>
                  </el-collapse-item>
                </el-collapse>
              </div>
            </div>

            <!-- 最终结果 -->
            <div class="result-section">
              <div class="section-header">
                <el-icon><Finished /></el-icon>
                <h3 class="section-title">最终结果</h3>
              </div>
              <div class="section-content">
                <div class="final-result">
                  <pre>{{ result.final_result }}</pre>
                </div>
              </div>
            </div>
          </div>
        </el-card>

        <!-- 错误提示 -->
        <el-alert
          v-if="errorMessage"
          :title="errorMessage"
          type="error"
          show-icon
          closable
          @close="errorMessage = ''"
          class="error-alert"
        />
      </div>
    </main>

    <!-- 底部区域 -->
    <footer class="footer">
      <div class="footer-content">
        <p>© 2024 教育助手AIAgent - 智能教育任务处理平台</p>
        <p class="footer-tech">Powered by Vue3 + Element Plus + FastAPI + LangChain</p>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'
import {
  Edit,
  CaretRight,
  RefreshRight,
  Loading,
  DataAnalysis,
  CircleCheck,
  Tickets,
  Document,
  Operation,
  Star,
  Tools,
  Cpu,
  List,
  Box,
  Finished
} from '@element-plus/icons-vue'

const taskText = ref('')
const loading = ref(false)
const result = ref(null)
const errorMessage = ref('')
const activeCollapse = ref([])
const loadingStep = ref(0)

const API_BASE_URL = '/api'

let loadingInterval = null

const startLoadingAnimation = () => {
  loadingStep.value = 0
  loadingInterval = setInterval(() => {
    if (loadingStep.value < 4) {
      loadingStep.value++
    }
  }, 800)
}

const stopLoadingAnimation = () => {
  if (loadingInterval) {
    clearInterval(loadingInterval)
    loadingInterval = null
  }
  loadingStep.value = 4
}

const handleInput = () => {
  errorMessage.value = ''
}

const handleReset = () => {
  taskText.value = ''
  result.value = null
  errorMessage.value = ''
  activeCollapse.value = []
  loadingStep.value = 0
}

const handleSubmit = async () => {
  if (!taskText.value.trim()) {
    errorMessage.value = '请输入教育任务'
    ElMessage.warning('请输入教育任务')
    return
  }

  loading.value = true
  errorMessage.value = ''
  result.value = null
  
  startLoadingAnimation()

  try {
    const response = await axios.post(`${API_BASE_URL}/agent/process`, {
      task: taskText.value.trim()
    }, {
      headers: {
        'Content-Type': 'application/json'
      },
      timeout: 30000
    })

    if (response.data) {
      result.value = response.data
      ElMessage.success('任务处理完成！')
      
      if (result.value.tool_results) {
        activeCollapse.value = Object.keys(result.value.tool_results)
      }
    }
  } catch (error) {
    let errorMsg = '网络错误，请稍后重试'
    
    if (error.response) {
      const status = error.response.status
      const data = error.response.data
      
      if (status === 400) {
        errorMsg = data.detail || '任务不能为空'
      } else if (status === 500) {
        errorMsg = data.detail || '服务器内部错误'
      } else if (status === 404) {
        errorMsg = '接口不存在，请检查后端服务'
      } else {
        errorMsg = `错误 (${status}): ${data.detail || '未知错误'}`
      }
    } else if (error.request) {
      errorMsg = '无法连接到后端服务，请确保后端服务已启动在 http://localhost:8000'
    } else {
      errorMsg = error.message || '请求失败'
    }
    
    errorMessage.value = errorMsg
    ElMessage.error(errorMsg)
  } finally {
    stopLoadingAnimation()
    loading.value = false
  }
}

onMounted(async () => {
  try {
    const response = await axios.get(`${API_BASE_URL}/health`, {
      timeout: 5000
    })
    console.log('后端服务状态:', response.data)
  } catch (error) {
    console.warn('后端服务未启动或不可用')
  }
})
</script>

<style scoped>
.app-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.header {
  background: rgba(255, 255, 255, 0.95);
  padding: 24px 0;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.header-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
  text-align: center;
}

.title {
  margin: 0;
  font-size: 36px;
  font-weight: 700;
  color: #303133;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
}

.title-icon {
  font-size: 40px;
}

.subtitle {
  margin: 10px 0 0;
  font-size: 16px;
  color: #909399;
}

.main-content {
  flex: 1;
  padding: 40px 20px;
}

.container {
  max-width: 900px;
  margin: 0 auto;
}

.input-card {
  margin-bottom: 24px;
  border-radius: 12px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header-left {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
}

.word-count {
  font-size: 14px;
  color: #909399;
}

.count-number {
  font-weight: 600;
  color: #409eff;
}

.task-textarea {
  font-size: 15px;
}

.task-textarea :deep(.el-textarea__inner) {
  line-height: 1.8;
  border-radius: 8px;
}

.button-group {
  margin-top: 24px;
  display: flex;
  gap: 16px;
  justify-content: center;
}

.submit-btn {
  min-width: 160px;
  font-size: 16px;
  font-weight: 600;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
}

.submit-btn:hover:not(:disabled) {
  opacity: 0.9;
  transform: translateY(-1px);
}

.reset-btn {
  min-width: 100px;
  font-size: 16px;
}

.loading-container {
  background: white;
  border-radius: 12px;
  padding: 32px;
  margin-bottom: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.loading-text {
  margin-top: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: #667eea;
  font-size: 16px;
}

.loading-icon {
  font-size: 24px;
}

.loading-steps {
  margin-top: 32px;
  padding: 0 40px;
}

.result-card {
  margin-bottom: 24px;
  border-radius: 12px;
}

.result-content {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.result-section {
  padding-bottom: 20px;
  border-bottom: 1px solid #ebeef5;
}

.result-section:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  color: #303133;
}

.section-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}

.section-content {
  padding: 12px;
  background: #f5f7fa;
  border-radius: 8px;
  line-height: 1.8;
}

.original-task {
  color: #606266;
  font-size: 15px;
}

.task-decomposition {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.decomposition-tag {
  font-size: 14px;
  padding: 10px 16px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.tool-calls {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.tool-tag {
  font-size: 14px;
  padding: 10px 16px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.result-collapse {
  border: none;
}

.result-collapse :deep(.el-collapse-item__header) {
  font-weight: 600;
  font-size: 15px;
}

.result-collapse :deep(.el-collapse-item__wrap) {
  border: none;
}

.collapse-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.tool-result-content {
  padding: 12px;
  background: white;
  border-radius: 8px;
  line-height: 1.8;
  white-space: pre-wrap;
  word-break: break-word;
  border: 1px solid #ebeef5;
}

.final-result {
  padding: 16px;
  background: linear-gradient(135deg, #667eea10 0%, #764ba210 100%);
  border: 2px solid #667eea30;
  border-radius: 8px;
}

.final-result pre {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: 'Consolas', 'Monaco', monospace;
  line-height: 1.8;
  font-size: 13px;
  color: #303133;
}

.error-alert {
  margin-top: 20px;
}

.footer {
  background: rgba(255, 255, 255, 0.95);
  padding: 24px 0;
  box-shadow: 0 -2px 12px rgba(0, 0, 0, 0.1);
}

.footer-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
  text-align: center;
}

.footer p {
  margin: 0;
  color: #909399;
  font-size: 14px;
}

.footer-tech {
  margin-top: 8px !important;
  font-size: 12px !important;
  color: #c0c4cc;
}

@media (max-width: 768px) {
  .title {
    font-size: 28px;
    flex-direction: column;
    gap: 8px;
  }
  
  .title-icon {
    font-size: 32px;
  }
  
  .container {
    padding: 0 12px;
  }
  
  .button-group {
    flex-direction: column;
  }
  
  .submit-btn,
  .reset-btn {
    width: 100%;
  }
  
  .loading-steps {
    padding: 0;
  }
}
</style>
