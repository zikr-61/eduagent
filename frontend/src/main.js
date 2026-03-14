import { createApp } from 'vue';
import App from './App.vue';
import router from './router'; // 导入路由配置
import ElementPlus from 'element-plus'; // 引入 Element Plus
import 'element-plus/dist/index.css'; // 引入 Element Plus 样式

// 全局处理 ResizeObserver 错误
if (typeof window !== 'undefined') {
  window.addEventListener('error', (e) => {
    if (e.message.includes('ResizeObserver loop completed with undelivered notifications')) {
      e.preventDefault();
    }
  });
}

createApp(App)
  .use(router)  // 使用路由
  .use(ElementPlus)  // 使用 Element Plus
  .mount('#app');