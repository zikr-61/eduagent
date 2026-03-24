import { createRouter, createWebHistory } from 'vue-router';
import LoginPage from '@/views/Login.vue';
import RegisterPage from '@/views/Register.vue';
import StudyStats from '@/views/StudyStats.vue';
import KnowledgeSummary from '@/views/KnowledgeSummary.vue';
import HomeworkManagement from '@/views/HomeworkManagement.vue';
import WrongQuestionManagement from '@/views/WrongQuestionManagement.vue';

const routes = [
  { path: '/', redirect: '/login' },
  { path: '/login', component: LoginPage },
  { path: '/register', component: RegisterPage },
  { path: '/study-stats', component: StudyStats },
  { path: '/knowledge-summary', component: KnowledgeSummary },
  { path: '/homework', component: HomeworkManagement },
  { path: '/wrong-questions', component: WrongQuestionManagement }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

export default router;
