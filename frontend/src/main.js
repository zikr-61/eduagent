import { createApp } from 'vue';
import App from './App.vue';
import router from './router'; // 导入路由配置
import ElementPlus from 'element-plus'; // 引入 Element Plus
import 'element-plus/dist/index.css'; // 引入 Element Plus 样式

createApp(App)
  .use(router)  // 使用路由
  .use(ElementPlus)  // 使用 Element Plus
  .mount('#app');