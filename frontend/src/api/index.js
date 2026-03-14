import axios from 'axios';

const api = axios.create({
  baseURL: 'http://localhost:8080/api',
  timeout: 60000,
  headers: {
    'Content-Type': 'application/x-www-form-urlencoded'
  }
});

api.interceptors.request.use(
  config => {
    return config;
  },
  error => {
    console.error('请求错误:', error);
    return Promise.reject(error);
  }
);

api.interceptors.response.use(
  response => {
    return response;
  },
  error => {
    console.error('响应错误:', error);
    if (error.response) {
      switch (error.response.status) {
        case 400:
          console.error('请求错误:', error.response.data.error);
          break;
        case 401:
          console.error('未授权，请重新登录');
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
      console.error('网络错误，请检查网络连接');
    } else {
      console.error('请求配置错误:', error.message);
    }
    return Promise.reject(error);
  }
);

export const login = (username, password) => {
  return api.post('/user/login', { username, password });
};

export const register = (username, password) => {
  return api.post('/user/register', { username, password });
};

export const getStudyRecords = (userId) => {
  return api.get('/study/list', { params: { userId } });
};

export const getTotalDuration = (userId) => {
  return api.get('/study/total', { params: { userId } });
};

export const addStudyRecord = (userId, durationMinutes) => {
  return api.post('/study/add', { userId, durationMinutes });
};

export const getWeeklyReport = (userId) => {
  return api.get('/study/weekly-report', { params: { userId } });
};

export const getMonthlyReport = (userId) => {
  return api.get('/study/monthly-report', { params: { userId } });
};

export const getKnowledgePoints = (userId) => {
  return api.get('/knowledge/list', { params: { userId } });
};

export const getKnowledgePoint = (id) => {
  return api.get(`/knowledge/${id}`);
};

export const createKnowledgePoint = (userId, title, content, summary, fileName) => {
  return api.post('/knowledge/create', { userId, title, content, summary, fileName });
};

export const generateSummaryAndQuestions = (userId, title, content, fileName) => {
  return api.post('/knowledge/generate', { userId, title, content, fileName });
};

export const getQuestions = (knowledgePointId) => {
  return api.get(`/knowledge/${knowledgePointId}/questions`);
};

export const getHomeworkList = (userId) => {
  return api.get('/homework/list', { params: { userId } });
};

export const getPendingHomework = (userId) => {
  return api.get('/homework/pending', { params: { userId } });
};

export const createHomework = (userId, title, description, priority, dueDate) => {
  return api.post('/homework/create', { userId, title, description, priority, dueDate });
};

export const updateHomework = (id, title, description, priority, dueDate) => {
  return api.put(`/homework/update/${id}`, { title, description, priority, dueDate });
};

export const deleteHomework = (id) => {
  return api.delete(`/homework/delete/${id}`);
};

export const completeHomework = (id) => {
  return api.put(`/homework/complete/${id}`);
};

export const incompleteHomework = (id) => {
  return api.put(`/homework/incomplete/${id}`);
};

export const setReminder = (id, reminderTime) => {
  return api.put(`/homework/reminder/${id}`, { reminderTime });
};

export default {
  login,
  register,
  getStudyRecords,
  getTotalDuration,
  addStudyRecord,
  getWeeklyReport,
  getMonthlyReport,
  getKnowledgePoints,
  getKnowledgePoint,
  createKnowledgePoint,
  generateSummaryAndQuestions,
  getQuestions,
  getHomeworkList,
  getPendingHomework,
  createHomework,
  updateHomework,
  deleteHomework,
  completeHomework,
  incompleteHomework,
  setReminder
};
