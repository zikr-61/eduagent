/**
 * 全局共享的在线时长 reactive ref
 * App.vue 负责驱动更新，其他组件只读取 sessionMinutes
 */
import { ref } from 'vue';

export const sessionMinutes = ref(0);

export function refreshSessionMinutes() {
  const loginTime = localStorage.getItem('loginTime');
  if (loginTime) {
    sessionMinutes.value = Math.floor((Date.now() - parseInt(loginTime)) / 60000);
  } else {
    sessionMinutes.value = 0;
  }
}
