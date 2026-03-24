<template>
  <div class="register-container">
    <el-card class="register-card">
      <h2 class="title">注册教育AI助手</h2>
      <el-form @submit.prevent="register" class="register-form" :model="form" :rules="rules" ref="registerForm" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
          />
        </el-form-item>

        <el-form-item label="用户类型" prop="userType">
          <el-radio-group v-model="form.userType">
            <el-radio label="student">学生</el-radio>
            <el-radio label="teacher">教师</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="姓名" prop="name">
          <el-input v-model="form.name" placeholder="请输入姓名" />
        </el-form-item>

        <!-- 学生特有字段 -->
        <el-form-item v-if="form.userType === 'student'" label="年级" prop="grade">
          <el-select v-model="form.grade" placeholder="请选择年级">
            <el-option label="初一" value="初一" />
            <el-option label="初二" value="初二" />
            <el-option label="初三" value="初三" />
            <el-option label="高一" value="高一" />
            <el-option label="高二" value="高二" />
            <el-option label="高三" value="高三" />
          </el-select>
        </el-form-item>

        <!-- 教师特有字段 -->
        <el-form-item v-if="form.userType === 'teacher'" label="学科" prop="subject">
          <el-select v-model="form.subject" placeholder="请选择学科">
            <el-option label="语文" value="语文" />
            <el-option label="数学" value="数学" />
            <el-option label="英语" value="英语" />
            <el-option label="物理" value="物理" />
            <el-option label="化学" value="化学" />
            <el-option label="生物" value="生物" />
            <el-option label="历史" value="历史" />
            <el-option label="地理" value="地理" />
            <el-option label="政治" value="政治" />
          </el-select>
        </el-form-item>

        <el-form-item v-if="form.userType === 'teacher'" label="学校" prop="school">
          <el-input v-model="form.school" placeholder="请输入学校名称" />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" native-type="submit" class="register-button">注册</el-button>
        </el-form-item>
      </el-form>
      <p class="footer">
        已有账号？<router-link to="/login">去登录</router-link>
      </p>
    </el-card>
  </div>
</template>

<script>
import { register } from '@/api';

export default {
  name: 'RegisterPage',
  data() {
    return {
      form: {
        username: '',
        password: '',
        userType: 'student',
        name: '',
        grade: '',
        subject: '',
        school: ''
      },
      rules: {
        username: [
          { required: true, message: '请输入用户名', trigger: 'blur' },
          { min: 3, max: 20, message: '用户名长度在3-20个字符之间', trigger: 'blur' }
        ],
        password: [
          { required: true, message: '请输入密码', trigger: 'blur' },
          { min: 6, message: '密码长度至少6个字符', trigger: 'blur' }
        ],
        userType: [
          { required: true, message: '请选择用户类型', trigger: 'change' }
        ],
        name: [
          { required: true, message: '请输入姓名', trigger: 'blur' }
        ],
        grade: [
          { required: true, message: '请选择年级', trigger: 'change' }
        ],
        subject: [
          { required: true, message: '请选择学科', trigger: 'change' }
        ],
        school: [
          { required: true, message: '请输入学校名称', trigger: 'blur' }
        ]
      }
    };
  },
  methods: {
    async register() {
      this.$refs.registerForm.validate(async (valid) => {
        if (valid) {
          try {
            await register(
              this.form.username, 
              this.form.password, 
              this.form.userType, 
              this.form.name, 
              this.form.grade, 
              this.form.subject, 
              this.form.school
            );
            this.$alert('注册成功！');
            this.$router.push('/login');
          } catch (error) {
            this.$alert('注册失败：' + (error.response?.data?.error || '未知错误'));
          }
        }
      });
    }
  }
};
</script>

<style scoped>
.register-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  padding: 20px;
}

.register-card {
  width: 100%;
  max-width: 500px;
  padding: 40px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.2);
  border-radius: 10px;
  background-color: rgba(255, 255, 255, 0.95);
}

.title {
  text-align: center;
  font-size: 2.2em;
  font-weight: bold;
  margin-bottom: 30px;
  color: #333;
}

.register-form {
  margin-bottom: 30px;
}

.register-button {
  width: 100%;
  height: 45px;
  font-size: 1.1em;
  margin-top: 20px;
}

.footer {
  text-align: center;
  margin-top: 30px;
  color: #666;
}

.footer a {
  color: #409eff;
  text-decoration: none;
  transition: color 0.3s;
}

.footer a:hover {
  color: #66b1ff;
}

.el-form-item {
  margin-bottom: 20px;
}

.el-input,
.el-select {
  height: 45px;
}

.el-radio-group {
  display: flex;
  gap: 30px;
}
</style>
