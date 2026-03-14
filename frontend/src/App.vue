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
        </el-menu>
      </el-aside>
      <el-container>
        <el-header style="background-color: #fff; box-shadow: 0 2px 4px rgba(0,0,0,.1); display: flex; justify-content: space-between; align-items: center;">
          <h3>教育助手AI Agent</h3>
          <div>
            <span style="margin-right: 20px;">欢迎, {{ username }}</span>
            <el-button type="danger" size="small" @click="logout">退出登录</el-button>
          </div>
        </el-header>
        <el-main>
          <router-view></router-view>
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { Clock, Document, List } from '@element-plus/icons-vue';

const router = useRouter();
const route = useRoute();
const isLoggedIn = ref(false);
const username = ref('User');
const activeMenu = ref('/study-stats');

const checkLogin = () => {
  const userInfo = localStorage.getItem('userInfo');
  if (userInfo) {
    isLoggedIn.value = true;
    const user = JSON.parse(userInfo);
    username.value = user.username || 'User';
  } else {
    isLoggedIn.value = false;
  }
};

const logout = () => {
  localStorage.removeItem('userInfo');
  isLoggedIn.value = false;
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
}
</style>
