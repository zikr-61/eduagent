import axios from 'axios';

/** 与 Spring Boot 一致，供 fetch 流式接口等使用 */
export const API_BASE = 'http://localhost:8080/api';

const api = axios.create({
  baseURL: API_BASE,
  timeout: 60000,
  headers: {
    'Content-Type': 'application/x-www-form-urlencoded'
  }
});

api.interceptors.request.use(
  config => {
    // 附加 JWT Token
    const token = localStorage.getItem('authToken');
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`;
    }

    // 后端接口普遍使用 @RequestParam，需要 application/x-www-form-urlencoded 的表单字段；
    // axios 传普通对象时默认会按 JSON 序列化，与 Content-Type 不一致会导致 400。
    const ct = config.headers['Content-Type'] || config.headers['content-type'] || '';
    if (
      config.data &&
      typeof config.data === 'object' &&
      !(config.data instanceof FormData) &&
      !(config.data instanceof URLSearchParams) &&
      String(ct).includes('application/x-www-form-urlencoded')
    ) {
      const params = new URLSearchParams();
      Object.keys(config.data).forEach((key) => {
        const v = config.data[key];
        if (v !== undefined && v !== null) {
          params.append(key, String(v));
        }
      });
      config.data = params.toString();
      config.headers['Content-Type'] = 'application/x-www-form-urlencoded';
    }
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
          localStorage.removeItem('authToken');
          localStorage.removeItem('userInfo');
          localStorage.removeItem('userType');
          localStorage.removeItem('loginTime');
          window.location.href = '/login';
          break;
        case 403:
          // 403 通常意味着 token 丢失或已过期（Spring Security 有时返回 403 而非 401）
          console.error('拒绝访问，token 可能已失效，重新登录');
          localStorage.removeItem('authToken');
          localStorage.removeItem('userInfo');
          localStorage.removeItem('userType');
          localStorage.removeItem('loginTime');
          window.location.href = '/login';
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

export const register = (username, password, userType, name, grade, subject) => {
  return api.post('/user/register', { username, password, userType, name, grade, subject });
};

export const getUserInfo = (id) => {
  return api.get(`/user/info/${id}`);
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

export const getAllStudentsStats = () => {
  return api.get('/study/all-students');
};

export const startLearning = (userId, activityType, activityId) => {
  return api.post('/study/start', { userId, activityType, activityId });
};

export const endLearning = (userId, activityType, activityId, startTime) => {
  // Spring LocalDateTime.parse 不支持 ISO 8601 的 'Z' 后缀和毫秒，需要裁剪
  const cleanTime = String(startTime)
    .replace('Z', '')           // 去掉时区标识
    .replace(/\.\d+$/, '');    // 去掉毫秒部分
  return api.post('/study/end', { userId, activityType, activityId, startTime: cleanTime });
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
  return api.post('/knowledge/generate', { userId, title, content, fileName }, { timeout: 120000 });
};

export const generateLessonPlan = (userId, title, gradeLevel, content) => {
  return api.post('/knowledge/generate-plan', { userId, title, gradeLevel, content }, { timeout: 120000 });
};

export const getQuestions = (knowledgePointId) => {
  return api.get(`/knowledge/${knowledgePointId}/questions`);
};

export const createHomework = (teacherId, title, description, difficulty, dueDate) => {
  return api.post('/homework/create', { teacherId, title, description, difficulty, dueDate });
};

export const getTeacherHomeworkList = (teacherId) => {
  return api.get('/homework/teacher/list', { params: { teacherId } });
};

export const getStudentHomeworkList = (studentId) => {
  return api.get('/homework/student/list', { params: { studentId } });
};

export const getPendingStudentHomework = (studentId) => {
  return api.get('/homework/student/pending', { params: { studentId } });
};

export const updateHomework = (id, title, description, difficulty, dueDate) => {
  return api.put(`/homework/update/${id}`, { title, description, difficulty, dueDate });
};

export const deleteHomework = (id) => {
  return api.delete(`/homework/delete/${id}`);
};

export const assignHomework = (homeworkId, studentId) => {
  return api.post('/homework/assign', { homeworkId, studentId });
};

export const submitHomework = (studentId, homeworkId, completionTime) => {
  return api.post('/homework/submit', { studentId, homeworkId, completionTime });
};

export const getHomeworkCompletionRate = (homeworkId) => {
  return api.get(`/homework/completion-rate/${homeworkId}`);
};

export const assignHomeworkToAll = (homeworkId, teacherId) => {
  return api.post('/homework/assign-all', { homeworkId, teacherId });
};

export const getHomeworkCompletionDetail = (homeworkId, teacherId) => {
  return api.get(`/homework/${homeworkId}/completion`, { params: { teacherId } });
};

export const recordErrorQuestion = (studentId, questionId) => {
  return api.post('/error-questions/record', { studentId, questionId });
};

export const getErrorQuestions = (studentId) => {
  return api.get('/error-questions/list', { params: { studentId } });
};

export const removeErrorQuestion = (studentId, questionId) => {
  return api.delete('/error-questions/remove', { params: { studentId, questionId } });
};

// ── Agent：经 Spring Boot 8080 代理到 Python 8000（避免浏览器直连 8000 连接被拒绝）────────
export const getAgentSessions = (userId) => {
  return api.get('/agent/proxy/sessions', { params: { user_id: userId } });
};

export const getAgentSessionMessages = (sessionId, userId) => {
  return api.get(`/agent/proxy/sessions/${sessionId}/messages`, { params: { user_id: userId } });
};

export const invalidateKnowledgeCache = (userId) => {
  return api.post('/agent/knowledge/invalidate', null, { params: { userId } });
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
  generateLessonPlan,
  getQuestions,
  getTeacherHomeworkList,
  getStudentHomeworkList,
  getPendingStudentHomework,
  createHomework,
  updateHomework,
  deleteHomework,
  assignHomework,
  submitHomework,
  getHomeworkCompletionRate,
  assignHomeworkToAll,
  getHomeworkCompletionDetail,
  recordErrorQuestion,
  getErrorQuestions,
  removeErrorQuestion,
  getAgentSessions,
  getAgentSessionMessages,
  invalidateKnowledgeCache,
  API_BASE
};
