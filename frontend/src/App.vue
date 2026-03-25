<template>
  <div id="app">
    <router-view v-if="!isLoggedIn"></router-view>
    <el-container v-else style="height: 100vh;">
      <el-aside width="200px" style="background-color: #545c64;">
        <el-menu
          :default-active="activeMenu"
          class="el-menu-vertical"
          background-color="#545c64"
          text-color="#fff"
          active-text-color="#ffd04b"
          router
        >
          <el-menu-item index="/study-stats">
            <el-icon><Clock /></el-icon>
            <span>学习成果统计</span>
          </el-menu-item>
          <el-menu-item index="/knowledge-summary">
            <el-icon><Document /></el-icon>
            <span>知识点总结</span>
          </el-menu-item>
          <el-menu-item index="/homework">
            <el-icon><List /></el-icon>
            <span>作业管理</span>
          </el-menu-item>
          <el-menu-item v-if="userType === 'student'" index="/wrong-questions">
            <el-icon><CircleClose /></el-icon>
            <span>错题管理</span>
          </el-menu-item>
          <el-menu-item index="/chat">
            <el-icon><ChatDotRound /></el-icon>
            <span>AI 辅导</span>
          </el-menu-item>
        </el-menu>
      </el-aside>
      <el-container style="height: 100%;">
        <el-header style="background-color: #fff; box-shadow: 0 2px 4px rgba(0,0,0,.1); display: flex; justify-content: space-between; align-items: center;">
          <h3>教育助手AI Agent</h3>
          <div style="display:flex;align-items:center;gap:12px;">
            <span style="font-size:12px;color:#909399;background:#f5f7fa;padding:3px 10px;border-radius:12px;">
              已在线 {{ sessionMinutes }} 分钟
            </span>
            <span>欢迎, {{ username }}</span>
            <el-button type="danger" size="small" @click="logout">退出登录</el-button>
          </div>
        </el-header>
        <el-main style="height: 100%;">
          <router-view></router-view>
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, onUnmounted, computed } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { Clock, Document, List, CircleClose, ChatDotRound } from '@element-plus/icons-vue';

const router = useRouter();
const route = useRoute();
const isLoggedIn = ref(false);
const username = ref('User');
const userType = ref('');
const activeMenu = ref('/study-stats');

// ── 在线计时器（纯展示，不写数据库）──
const sessionMinutes = ref(0);
let timerInterval = null;

const updateSessionTime = () => {
  const loginTime = localStorage.getItem('loginTime');
  if (loginTime) {
    sessionMinutes.value = Math.floor((Date.now() - parseInt(loginTime)) / 60000);
  }
};

const startTimer = () => {
  updateSessionTime();
  timerInterval = setInterval(updateSessionTime, 60000);
};

const stopTimer = () => {
  if (timerInterval) {
    clearInterval(timerInterval);
    timerInterval = null;
  }
  sessionMinutes.value = 0;
};

onMounted(() => {
  if (localStorage.getItem('loginTime')) startTimer();
});

onUnmounted(() => stopTimer());

const checkLogin = () => {
  const userInfo = localStorage.getItem('userInfo');
  if (userInfo) {
    isLoggedIn.value = true;
    const user = JSON.parse(userInfo);
    username.value = user.username || 'User';
    userType.value = user.userType || localStorage.getItem('userType') || '';
    if (localStorage.getItem('loginTime') && !timerInterval) startTimer();
  } else {
    isLoggedIn.value = false;
    userType.value = '';
    stopTimer();
  }
};

const logout = () => {
  localStorage.removeItem('userInfo');
  localStorage.removeItem('userType');
  localStorage.removeItem('authToken');
  localStorage.removeItem('loginTime');
  isLoggedIn.value = false;
  stopTimer();
  router.push('/login');
};

watch(() => route.path, (newPath) => {
  if (newPath !== '/login' && newPath !== '/register') {
    checkLogin();
  }
  activeMenu.value = newPath;
}, { immediate: true });

checkLogin();
</script>

<style>
#app {
  font-family: Avenir, Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

.el-aside {
  color: #fff;
}

.el-menu-vertical {
  border-right: none;
}

.el-main {
  background-color: #f0f2f5;
  height: 100%;
}
</style>
