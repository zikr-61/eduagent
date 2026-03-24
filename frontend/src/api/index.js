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

export const register = (username, password, userType, name, grade, subject, school) => {
  return api.post('/user/register', { username, password, userType, name, grade, subject, school });
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
  return api.post('/study/end', { userId, activityType, activityId, startTime });
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
  removeErrorQuestion
};
