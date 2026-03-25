<template>
  <div class="chat-assistant">
    <div class="chat-layout">
      <!-- 左侧：历史会话 -->
      <div class="sidebar">
        <div class="sidebar-header">
          <span class="sidebar-title">历史对话</span>
          <el-button type="primary" size="small" :icon="Plus" @click="newChat">新对话</el-button>
        </div>
        <el-scrollbar class="sidebar-scroll" style="height: 100%;">
          <div
            v-for="s in sessions"
            :key="s.session_id"
            class="session-item"
            :class="{ active: currentSessionId === s.session_id }"
            @click="loadSession(s.session_id)"
          >
            <el-icon><ChatDotRound /></el-icon>
            <span class="session-text">{{ s.first_message || '新对话' }}</span>
            <span class="session-time">{{ formatDate(s.created_at) }}</span>
          </div>
          <div v-if="sessions.length === 0" class="empty-sessions">暂无历史记录</div>
        </el-scrollbar>
      </div>

      <!-- 右侧：主对话区 -->
      <div class="main-area">
        <!-- 顶部标题 -->
        <div class="chat-header">
          <div class="header-left">
            <el-icon size="20"><Cpu /></el-icon>
            <span class="chat-title">
              {{ userType === 'teacher' ? 'AI 教学助手' : 'AI 学习助手' }}
            </span>
            <el-tag size="small" type="success">ReAct Agent · LangGraph</el-tag>
          </div>
          <div class="header-right">
            <el-tag v-if="agentOffline" size="small" type="danger">
              服务未启动
            </el-tag>
            <el-tag v-else-if="agentRunning" size="small" type="warning">
              Agent 运行中...
            </el-tag>
            <el-tag v-else size="small" type="success">就绪</el-tag>
          </div>
        </div>

        <!-- 服务离线提示 -->
        <el-alert
          v-if="agentOffline"
          type="error"
          show-icon
          :closable="false"
          style="margin: 12px 16px 0; border-radius: 6px;"
        >
          <template #title>
            Python Agent 未运行（后端需能访问本机 8000 端口）
          </template>
          <template #default>
            <p style="margin: 0 0 8px">
              前端已改为只连 <strong>Spring Boot 8080</strong>，由后端转发到 Python。
              请在本机启动 Agent：在 IDEA 中右键 <code>agent_service/main.py</code> → Run，
              或终端进入 <code>agent_service</code> 执行 <code>pip install -r requirements.txt</code> 后 <code>python main.py</code>（或双击 <code>start.bat</code>）。
            </p>
            <el-button type="primary" size="small" @click="retryAgent">启动后点此重新检测</el-button>
          </template>
        </el-alert>

        <!-- 欢迎卡片（空对话时显示） -->
        <div v-if="messages.length === 0 && !agentRunning && !agentOffline" class="welcome-area">
          <div class="welcome-icon">🤖</div>
          <h3 class="welcome-title">
            {{ userType === 'teacher' ? '你好，老师！我是你的AI教学助手' : '你好！我是你的AI学习助手' }}
          </h3>
          <p class="welcome-sub">我可以调用多种工具帮助你，你可以这样问我：</p>
          <div class="suggestion-grid">
            <div
              v-for="s in suggestions"
              :key="s"
              class="suggestion-chip"
              @click="sendSuggestion(s)"
            >{{ s }}</div>
          </div>
        </div>

        <!-- 消息列表 -->
        <el-scrollbar ref="msgScrollbar" class="msg-area">
          <div ref="msgContainer" class="msg-container">
            <template v-for="(msg, idx) in messages" :key="idx">
              <!-- 用户消息 -->
              <div v-if="msg.role === 'human'" class="msg-row user-row">
                <div class="msg-bubble user-bubble">{{ msg.content }}</div>
                <el-avatar size="small" class="avatar" :icon="User" style="background:#409eff" />
              </div>

              <!-- AI 消息 -->
              <div v-else class="msg-row ai-row">
                <el-avatar size="small" class="avatar ai-avatar" :icon="Cpu" />
                <div class="ai-content">
                  <!-- Agent 执行步骤可视化（复用 ExecutionVisualizer） -->
                  <ExecutionVisualizer
                    v-if="msg.steps && msg.steps.length > 0"
                    :steps="msg.steps"
                    :total-duration="msg.duration || ''"
                  />
                  <!-- 最终回答 -->
                  <div v-if="msg.content" class="msg-bubble ai-bubble">
                    <div class="ai-text" v-html="renderMarkdown(msg.content)"></div>
                  </div>
                  <!-- 正在打字动画 -->
                  <div v-if="msg.typing" class="msg-bubble ai-bubble typing-bubble">
                    <span class="dot"></span><span class="dot"></span><span class="dot"></span>
                  </div>
                </div>
              </div>
            </template>
          </div>
        </el-scrollbar>

        <!-- 输入区 -->
        <div class="input-area">
          <el-input
            v-model="inputText"
            type="textarea"
            :rows="2"
            :placeholder="inputPlaceholder"
            :disabled="agentRunning"
            resize="none"
            @keydown.enter.exact.prevent="sendMessage"
          />
          <div class="input-actions">
            <span class="input-hint">Enter 发送 · Shift+Enter 换行</span>
            <el-button
              type="primary"
              :loading="agentRunning"
              :disabled="!inputText.trim()"
              :icon="agentRunning ? '' : Promotion"
              @click="sendMessage"
            >
              {{ agentRunning ? 'Agent运行中...' : '发送' }}
            </el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue';
import { ElMessage } from 'element-plus';
import { Plus, ChatDotRound, Cpu, User, Promotion } from '@element-plus/icons-vue';
import ExecutionVisualizer from '@/components/ExecutionVisualizer.vue';
import { getAgentSessions, getAgentSessionMessages, API_BASE } from '@/api';

// ── 用户信息 ──────────────────────────────────────────────────────────────────
const userInfo = ref(JSON.parse(localStorage.getItem('userInfo') || '{}'));
const userId = computed(() => userInfo.value?.id || 1);
const userType = ref(localStorage.getItem('userType') || 'student');

/** 经 Spring Boot 代理到 Python，与主站同源 8080，无需浏览器直连 8000 */
const STREAM_URL = `${API_BASE}/agent/proxy/chat/stream`;

// ── 状态 ──────────────────────────────────────────────────────────────────────
const messages = ref([]);        // [{role, content, steps, typing, duration}]
const inputText = ref('');
const agentRunning = ref(false);
const agentOffline = ref(false);   // Python 服务未启动时为 true
const currentSessionId = ref(null);
const sessions = ref([]);
const msgScrollbar = ref(null);
const msgContainer = ref(null);

// ── 建议问题（角色不同显示不同） ──────────────────────────────────────────────
const suggestions = computed(() => {
  if (userType.value === 'teacher') {
    return [
      '班级本周学习情况怎么样？',
      '哪些学生需要重点关注？',
      '帮我出5道关于三角函数的练习题',
      '查看最近作业的完成情况',
    ];
  }
  return [
    '帮我分析一下我的薄弱点',
    '给我出几道针对性练习题',
    '帮我制定本周学习计划',
    '我想复习上次学的内容',
  ];
});

const inputPlaceholder = computed(() =>
  userType.value === 'teacher'
    ? '问我班级学情、出题、或查看作业完成情况...'
    : '问我任何学习问题，我会自动调用工具帮你...'
);

// ── 发送消息 ──────────────────────────────────────────────────────────────────
async function sendMessage() {
  const text = inputText.value.trim();
  if (!text || agentRunning.value) return;
  if (agentOffline.value) {
    ElMessage.warning('请先在本机启动 Python Agent（端口 8000），再点击页面上方「重新检测」');
    return;
  }

  inputText.value = '';
  messages.value.push({ role: 'human', content: text });

  // 添加 AI 消息占位（等待流式填充）
  messages.value.push({ role: 'ai', content: '', steps: [], typing: true, duration: '' });
  const aiMsgIdx = messages.value.length - 1; // 通过索引访问保证 Vue 响应式代理生效
  scrollToBottom();

  agentRunning.value = true;
  const startTime = Date.now();

  try {
    const token = localStorage.getItem('authToken');
    const response = await fetch(STREAM_URL, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(token ? { 'Authorization': `Bearer ${token}` } : {})
      },
      body: JSON.stringify({
        message: text,
        user_id: userId.value,
        user_type: userType.value,
        session_id: currentSessionId.value,
      }),
    });

    if (!response.ok) throw new Error(`HTTP ${response.status}`);

    const reader = response.body.getReader();
    const decoder = new TextDecoder();
    let buffer = '';
    let done = false;

    while (!done) {
      const result = await reader.read();
      done = result.done;
      if (done) break;
      const value = result.value;

      buffer += decoder.decode(value, { stream: true });
      const lines = buffer.split('\n');
      buffer = lines.pop();

      for (const line of lines) {
        if (!line.startsWith('data:')) continue;
        // 兼容 "data: " 和 "data:" 两种格式
        const raw = line.startsWith('data: ') ? line.slice(6).trim() : line.slice(5).trim();
        if (!raw) continue;

        try {
          const event = JSON.parse(raw);
          handleSSEEvent(event, aiMsgIdx, startTime);
        } catch (e) {
          console.warn('SSE parse error:', e);
        }
      }
      scrollToBottom();
    }
  } catch (err) {
    const isOffline = err.message === 'Network Error' || err.message?.includes('ERR_CONNECTION_REFUSED');
    agentOffline.value = isOffline;
    messages.value[aiMsgIdx].content = isOffline
      ? '⚠️ 无法连接 Spring Boot（8080）。请确认后端已启动。'
      : `请求失败：${err.message}`;
    messages.value[aiMsgIdx].typing = false;
    if (!isOffline) ElMessage.error('对话请求失败');
  } finally {
    agentRunning.value = false;
    messages.value[aiMsgIdx].typing = false;
    scrollToBottom();
    await loadSessions();
  }
}

function handleSSEEvent(event, aiMsgIdx, startTime) {
  const msg = messages.value[aiMsgIdx]; // 通过响应式数组访问，确保 Vue 能检测到变更
  const type = event.type;

  if (type === 'session_id') {
    currentSessionId.value = event.session_id;
  } else if (type === 'status') {
    msg.typing = true;
  } else if (type === 'step_start' || type === 'tool_call') {
    const step = event.step || {};
    const existing = msg.steps.find(s => s.stepId === step.stepId);
    if (!existing) msg.steps.push(step);
    msg.typing = false;
  } else if (type === 'tool_result' || type === 'step_done') {
    const step = event.step || {};
    const idx = msg.steps.findIndex(s => s.stepId === step.stepId);
    if (idx >= 0) msg.steps[idx] = step;
    else msg.steps.push(step);
  } else if (type === 'text_chunk') {
    msg.content += event.content;
    msg.typing = false;
  } else if (type === 'done') {
    msg.typing = false;
    msg.duration = ((Date.now() - startTime) / 1000).toFixed(1) + 's';
  } else if (type === 'error') {
    msg.content = `Agent 错误：${event.message}`;
    msg.typing = false;
  }
}

function sendSuggestion(text) {
  inputText.value = text;
  sendMessage();
}

// ── 会话管理 ──────────────────────────────────────────────────────────────────
async function retryAgent() {
  await loadSessions();
  if (!agentOffline.value) {
    ElMessage.success('Agent 已连通');
  } else {
    ElMessage.warning('仍未检测到 Agent，请确认 8000 端口已监听');
  }
}

async function loadSessions() {
  try {
    const res = await getAgentSessions(userId.value);
    if (res.data?.agentOnline === false) {
      agentOffline.value = true;
      sessions.value = [];
      return;
    }
    agentOffline.value = false;
    sessions.value = res.data?.sessions || [];
  } catch (e) {
    agentOffline.value = true;
    sessions.value = [];
    console.warn('加载会话失败:', e.message);
  }
}

async function loadSession(sessionId) {
  if (sessionId === currentSessionId.value) return;
  try {
    const res = await getAgentSessionMessages(sessionId, userId.value);
    if (res.data?.agentOnline === false) {
      ElMessage.warning('Agent 服务未启动，无法加载历史消息');
      return;
    }
    const msgs = res.data?.messages || [];
    messages.value = msgs.map(m => ({ role: m.role, content: m.content, steps: [] }));
    currentSessionId.value = sessionId;
    await nextTick();
    scrollToBottom();
  } catch (e) {
    ElMessage.error('加载对话历史失败');
  }
}

function newChat() {
  messages.value = [];
  currentSessionId.value = null;
}

// ── 工具函数 ──────────────────────────────────────────────────────────────────
function scrollToBottom() {
  nextTick(() => {
    if (msgContainer.value) {
      msgContainer.value.scrollIntoView({ behavior: 'smooth', block: 'end' });
    }
  });
}

function formatDate(dt) {
  if (!dt) return '';
  const d = new Date(dt);
  const now = new Date();
  if (d.toDateString() === now.toDateString()) {
    return d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
  }
  return d.toLocaleDateString('zh-CN', { month: 'numeric', day: 'numeric' });
}

// 简单 markdown → html（加粗、换行）
function renderMarkdown(text) {
  if (!text) return '';
  return text
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/\*(.*?)\*/g, '<em>$1</em>')
    .replace(/`(.*?)`/g, '<code>$1</code>')
    .replace(/\n/g, '<br>');
}

onMounted(() => {
  loadSessions();
});
</script>

<style scoped>
.chat-assistant {
  height: 100%;
  display: flex;
  min-height: 0;
}

.chat-layout {
  display: flex;
  flex: 1;
  gap: 16px;
  min-height: 0;
  height: 100%;
}

/* ── 左侧会话栏 ── */
.sidebar {
  flex: 0 0 20%;
  background: #fff;
  border-radius: 8px;
  padding: 16px 0;
  box-shadow: 0 1px 4px rgba(0,0,0,.08);
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
}
.sidebar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 12px 12px;
  border-bottom: 1px solid #f0f0f0;
  flex-shrink: 0;
}
.sidebar-scroll {
  flex: 1;
  min-height: 0;
}
.sidebar-title { font-weight: 600; font-size: 14px; color: #303133; }
.session-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  cursor: pointer;
  border-radius: 6px;
  margin: 2px 8px;
  transition: background .2s;
  color: #606266;
  font-size: 13px;
}
.session-item:hover { background: #f5f7fa; }
.session-item.active { background: #ecf5ff; color: #409eff; }
.session-text { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.session-time { font-size: 11px; color: #c0c4cc; flex-shrink: 0; }
.empty-sessions { text-align: center; padding: 24px; color: #c0c4cc; font-size: 13px; }

/* ── 主对话区 ── */
.main-area {
  flex: 1;
  min-width: 0;
  background: #fff;
  border-radius: 8px;
  padding: 0;
  box-shadow: 0 1px 4px rgba(0,0,0,.08);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 0;
  height: 100%;
}
.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 20px;
  border-bottom: 1px solid #f0f0f0;
  flex-shrink: 0;
}
.header-left { display: flex; align-items: center; gap: 10px; }
.chat-title { font-size: 16px; font-weight: 600; color: #303133; }

/* ── 欢迎区 ── */
.welcome-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
}
.welcome-icon { font-size: 56px; margin-bottom: 12px; }
.welcome-title { font-size: 20px; font-weight: 600; color: #303133; margin: 0 0 8px; }
.welcome-sub { color: #909399; font-size: 14px; margin: 0 0 24px; }
.suggestion-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  max-width: 560px;
  width: 100%;
}
.suggestion-chip {
  padding: 12px 16px;
  background: #f5f7fa;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  cursor: pointer;
  font-size: 13px;
  color: #606266;
  transition: all .2s;
  text-align: left;
}
.suggestion-chip:hover { background: #ecf5ff; border-color: #b3d8ff; color: #409eff; }

/* ── 消息列表 ── */
.msg-area { flex: 1; min-height: 0; }
.msg-container { padding: 16px 20px; display: flex; flex-direction: column; gap: 16px; min-height: 0; }
.msg-row { display: flex; align-items: flex-start; gap: 10px; }
.user-row { flex-direction: row-reverse; }
.ai-row { flex-direction: row; }
.avatar { flex-shrink: 0; }
.ai-avatar { background: linear-gradient(135deg, #667eea, #764ba2) !important; }
.ai-content { display: flex; flex-direction: column; gap: 8px; max-width: 82%; }
.msg-bubble {
  padding: 12px 16px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
}
.user-bubble {
  background: #409eff;
  color: #fff;
  border-radius: 12px 2px 12px 12px;
  max-width: 400px;
}
.ai-bubble {
  background: #f5f7fa;
  color: #303133;
  border-radius: 2px 12px 12px 12px;
}
.ai-text :deep(strong) { color: #303133; }
.ai-text :deep(code) {
  background: #e8f4fd;
  padding: 1px 5px;
  border-radius: 3px;
  font-size: 13px;
  color: #c0392b;
}

/* ── 打字动画 ── */
.typing-bubble { display: flex; align-items: center; gap: 5px; padding: 14px 18px; }
.dot {
  width: 8px; height: 8px; border-radius: 50%; background: #c0c4cc;
  animation: bounce 1.2s infinite ease-in-out;
}
.dot:nth-child(2) { animation-delay: .2s; }
.dot:nth-child(3) { animation-delay: .4s; }
@keyframes bounce { 0%, 80%, 100% { transform: scale(.8); opacity:.5; } 40% { transform: scale(1.1); opacity:1; } }

/* ── 输入区 ── */
.input-area {
  padding: 12px 20px 16px;
  border-top: 1px solid #f0f0f0;
  flex-shrink: 0;
}
.input-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;
}
.input-hint { font-size: 12px; color: #c0c4cc; }
</style>
