import axios from 'axios';

// 创建axios实例
const api = axios.create({
  baseURL: 'http://localhost:8080/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/x-www-form-urlencoded'
  }
});

// 请求拦截器
api.interceptors.request.use(
  config => {
    // 可以在这里添加token等认证信息
    return config;
  },
  error => {
    console.error('请求错误:', error);
    return Promise.reject(error);
  }
);

// 响应拦截器
api.interceptors.response.use(
  response => {
    return response;
  },
  error => {
    console.error('响应错误:', error);
    // 统一处理错误
    if (error.response) {
      // 服务器返回错误状态码
      switch (error.response.status) {
        case 400:
          console.error('请求错误:', error.response.data.error);
          break;
        case 401:
          console.error('未授权，请重新登录');
          // 可以在这里跳转到登录页
          break;
        case 403:
          console.error('拒绝访问');
          break;
        case 404:
          console.error('请求路径不存在');
          break;
        case 500:
          console.error('服务器内部错误');
          break;
        default:
          console.error('未知错误');
      }
    } else if (error.request) {
      // 请求已发送但没有收到响应
      console.error('网络错误，请检查网络连接');
    } else {
      // 请求配置错误
      console.error('请求配置错误:', error.message);
    }
    return Promise.reject(error);
  }
);

// 登录
export const login = (username, password) => {
  return api.post('/user/login', {
    username,
    password
  });
};

// 注册
export const register = (username, password) => {
  return api.post('/user/register', {
    username,
    password
  });
};

export default {
  login,
  register
};
