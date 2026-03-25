import { createApp } from 'vue';
import App from './App.vue';
import router from './router';
import ElementPlus from 'element-plus';
import 'element-plus/dist/index.css';

const debounce = (fn, delay) => {
  let timer = null;
  return function() {
    const context = this;
    const args = arguments;
    clearTimeout(timer);
    timer = setTimeout(function() {
      fn.apply(context, args);
    }, delay);
  };
};


// 移除或修改 ResizeObserver 的处理
// 原代码：
// const _ResizeObserver = window.ResizeObserver;
// window.ResizeObserver = class ResizeObserver extends _ResizeObserver {
//   constructor(callback) {
//     callback = debounce(callback, 16);
//     super(callback);
//   }
// };

// 替换为：
if (window.ResizeObserver) {
  const _ResizeObserver = window.ResizeObserver;
  window.ResizeObserver = class ResizeObserver extends _ResizeObserver {
    constructor(callback) {
      callback = debounce(callback, 16);
      super(callback);
    }
  };
}

const errorHandler = (event) => {
  if (event.message && event.message.includes('ResizeObserver')) {
    event.preventDefault();
    event.stopPropagation();
    return false;
  }
};

window.addEventListener('error', errorHandler, true);
window.addEventListener('unhandledrejection', (event) => {
  if (event.reason && event.reason.message && event.reason.message.includes('ResizeObserver')) {
    event.preventDefault();
  }
}, true);

createApp(App)
  .use(router)
  .use(ElementPlus)
  .mount('#app');