<template>
  <div class="execution-visualizer">
    <el-card class="flow-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <span class="title">
            <el-icon><Operation /></el-icon>
            任务执行流程
          </span>
          <div class="header-actions">
            <el-tag v-if="totalDuration" type="info" size="small">
              总耗时: {{ totalDuration }}
            </el-tag>
            <el-button 
              :icon="isCollapsed ? 'ArrowDown' : 'ArrowUp'" 
              size="small" 
              text
              @click="toggleCollapse"
            >
              {{ isCollapsed ? '展开' : '折叠' }}
            </el-button>
          </div>
        </div>
      </template>

      <transition name="slide">
        <div v-show="!isCollapsed" class="flow-container">
          <div class="flow-steps">
            <div 
              v-for="(step, index) in steps" 
              :key="step.stepId"
              class="step-wrapper"
            >
              <div class="step-node" :class="getStepClass(step)">
                <div class="step-icon">
                  <el-icon v-if="step.status === 'completed'" class="success-icon">
                    <CircleCheckFilled />
                  </el-icon>
                  <el-icon v-else-if="step.status === 'running'" class="running-icon">
                    <Loading />
                  </el-icon>
                  <el-icon v-else-if="step.status === 'failed'" class="failed-icon">
                    <CircleCloseFilled />
                  </el-icon>
                  <el-icon v-else class="pending-icon">
                    <Clock />
                  </el-icon>
                </div>
                
                <div class="step-content">
                  <div class="step-header">
                    <span class="step-name">{{ step.stepName }}</span>
                    <el-tag 
                      :type="getStatusTagType(step.status)" 
                      size="small"
                    >
                      {{ getStatusText(step.status) }}
                    </el-tag>
                  </div>
                  <p class="step-description">{{ step.description }}</p>
                  <div v-if="step.duration" class="step-duration">
                    <el-icon><Timer /></el-icon>
                    耗时: {{ step.duration }}
                  </div>
                </div>

                <el-button 
                  v-if="step.details" 
                  size="small" 
                  text
                  @click="toggleStepDetail(step.stepId)"
                >
                  {{ expandedSteps.includes(step.stepId) ? '收起详情' : '查看详情' }}
                </el-button>
              </div>

              <transition name="fade">
                <div 
                  v-if="step.details && expandedSteps.includes(step.stepId)" 
                  class="step-details"
                >
                  <el-descriptions :column="1" border size="small">
                    <el-descriptions-item 
                      v-for="(value, key) in step.details" 
                      :key="key"
                      :label="formatDetailKey(key)"
                    >
                      <template v-if="isObject(value)">
                        <pre class="json-content">{{ JSON.stringify(value, null, 2) }}</pre>
                      </template>
                      <template v-else>
                        {{ value }}
                      </template>
                    </el-descriptions-item>
                  </el-descriptions>
                </div>
              </transition>

              <div v-if="index < steps.length - 1" class="step-connector">
                <div class="connector-line" :class="{ active: step.status === 'completed' }"></div>
                <el-icon class="connector-arrow" :class="{ active: step.status === 'completed' }">
                  <ArrowDown />
                </el-icon>
              </div>
            </div>
          </div>
        </div>
      </transition>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { 
  Operation, 
  CircleCheckFilled, 
  CircleCloseFilled, 
  Loading, 
  Clock,
  Timer,
  ArrowDown
} from '@element-plus/icons-vue';

defineProps({
  steps: {
    type: Array,
    default: () => []
  },
  totalDuration: {
    type: String,
    default: ''
  }
});

const isCollapsed = ref(false);
const expandedSteps = ref([]);

const toggleCollapse = () => {
  isCollapsed.value = !isCollapsed.value;
};

const toggleStepDetail = (stepId) => {
  const index = expandedSteps.value.indexOf(stepId);
  if (index > -1) {
    expandedSteps.value.splice(index, 1);
  } else {
    expandedSteps.value.push(stepId);
  }
};

const getStepClass = (step) => {
  return {
    'step-completed': step.status === 'completed',
    'step-running': step.status === 'running',
    'step-failed': step.status === 'failed',
    'step-pending': step.status === 'pending'
  };
};

const getStatusTagType = (status) => {
  const types = {
    completed: 'success',
    running: 'warning',
    failed: 'danger',
    pending: 'info'
  };
  return types[status] || 'info';
};

const getStatusText = (status) => {
  const texts = {
    completed: '已完成',
    running: '执行中',
    failed: '失败',
    pending: '等待中'
  };
  return texts[status] || status;
};

const formatDetailKey = (key) => {
  const keyMap = {
    input: '输入参数',
    subTasks: '子任务',
    tool: '调用工具',
    model: 'AI模型',
    outputLength: '输出长度',
    result: '执行结果',
    questionCount: '题目数量',
    questions: '题目列表',
    knowledgePointId: '知识点ID',
    savedQuestions: '保存题目数',
    summaryLength: '摘要长度'
  };
  return keyMap[key] || key;
};

const isObject = (value) => {
  return typeof value === 'object' && value !== null;
};
</script>

<style scoped>
.execution-visualizer {
  margin-top: 20px;
}

.flow-card {
  border-radius: 8px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header .title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: bold;
  font-size: 16px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.flow-container {
  padding: 10px 0;
}

.flow-steps {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.step-wrapper {
  display: flex;
  flex-direction: column;
}

.step-node {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 8px;
  transition: all 0.3s ease;
}

.step-node:hover {
  background: #f0f2f5;
}

.step-completed {
  border-left: 3px solid #67c23a;
}

.step-running {
  border-left: 3px solid #e6a23c;
  animation: pulse 1.5s infinite;
}

.step-failed {
  border-left: 3px solid #f56c6c;
}

.step-pending {
  border-left: 3px solid #909399;
}

@keyframes pulse {
  0% { opacity: 1; }
  50% { opacity: 0.7; }
  100% { opacity: 1; }
}

.step-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: white;
  flex-shrink: 0;
}

.success-icon {
  color: #67c23a;
  font-size: 24px;
}

.running-icon {
  color: #e6a23c;
  font-size: 24px;
  animation: spin 1s linear infinite;
}

.failed-icon {
  color: #f56c6c;
  font-size: 24px;
}

.pending-icon {
  color: #909399;
  font-size: 24px;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.step-content {
  flex: 1;
}

.step-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.step-name {
  font-weight: 600;
  font-size: 14px;
  color: #303133;
}

.step-description {
  margin: 0;
  font-size: 13px;
  color: #606266;
}

.step-duration {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
}

.step-details {
  margin: 12px 0 12px 44px;
  padding: 12px;
  background: #fff;
  border-radius: 6px;
  border: 1px solid #ebeef5;
}

.json-content {
  margin: 0;
  padding: 8px;
  background: #f5f7fa;
  border-radius: 4px;
  font-size: 12px;
  max-height: 200px;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-all;
}

.step-connector {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 4px 0;
}

.connector-line {
  width: 2px;
  height: 20px;
  background: #dcdfe6;
  transition: background 0.3s ease;
}

.connector-line.active {
  background: #67c23a;
}

.connector-arrow {
  color: #dcdfe6;
  font-size: 16px;
  transition: color 0.3s ease;
}

.connector-arrow.active {
  color: #67c23a;
}

.slide-enter-active,
.slide-leave-active {
  transition: all 0.3s ease;
}

.slide-enter-from,
.slide-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

.fade-enter-active,
.fade-leave-active {
  transition: all 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
