import { createRouter, createWebHistory } from 'vue-router';
import LoginPage from '@/views/Login.vue';   // 导入 Login 页面
import RegisterPage from '@/views/Register.vue'; // 导入 Register 页面

const routes = [
  { path: '/', redirect: '/login' },  // 设置默认路由为 Login 页面
  { path: '/login', component: LoginPage },
  { path: '/register', component: RegisterPage }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

export default router;