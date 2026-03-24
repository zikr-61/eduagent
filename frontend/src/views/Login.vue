<template>
  <div class="login-container">
    <el-card class="login-card">
      <h2 class="title">AI 教育助手</h2>
      <p class="sub-title">没有账号？<router-link to="/register">去注册</router-link></p>
      <el-form @submit.prevent="login" class="login-form" :model="form" :rules="rules" ref="loginForm" label-width="80px">
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

        <el-form-item>
          <el-button type="primary" native-type="submit" class="login-button">登录</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script>
import { login } from '@/api';

export default {
  name: 'LoginPage',
  data() {
    return {
      form: {
        username: '',
        password: ''
      },
      rules: {
        username: [
          { required: true, message: '请输入用户名', trigger: 'blur' },
          { min: 3, max: 20, message: '用户名长度在3-20个字符之间', trigger: 'blur' }
        ],
        password: [
          { required: true, message: '请输入密码', trigger: 'blur' },
          { min: 6, message: '密码长度至少6个字符', trigger: 'blur' }
        ]
      }
    };
  },
  methods: {
    async login() {
      this.$refs.loginForm.validate(async (valid) => {
        if (valid) {
          try {
            const response = await login(this.form.username, this.form.password);
            if (response.data) {
              localStorage.setItem('userInfo', JSON.stringify(response.data.user));
              localStorage.setItem('userType', response.data.userType);
              
              // 所有用户都跳转到学习成果统计页面
              this.$router.push('/study-stats');
            } else {
              this.$alert('登录成功！');
              console.log('登录成功，用户信息：', response.data);
            }
          } catch (error) {
            this.$alert('登录失败：' + (error.response?.data?.error || '未知错误'));
          }
        }
      });
    }
  }
};
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-card {
  width: 100%;
  max-width: 450px;
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

.sub-title {
  text-align: center;
  font-size: 1.1em;
  margin-bottom: 30px;
  color: #666;
}

.login-form {
  margin-bottom: 30px;
}

.login-button {
  width: 100%;
  height: 45px;
  font-size: 1.1em;
  margin-top: 20px;
}

.sub-title a {
  color: #409eff;
  text-decoration: none;
  transition: color 0.3s;
}

.sub-title a:hover {
  color: #66b1ff;
}

.el-form-item {
  margin-bottom: 20px;
}

.el-input {
  height: 45px;
}
</style>
