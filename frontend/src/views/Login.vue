<template>
  <div class="login-container">
    <el-card class="login-card">
      <h2 class="title">AI 教育助手</h2>
      <p class="sub-title">没有账号？<router-link to="/register">去注册</router-link></p>
      <el-form @submit.prevent="login" class="login-form" :model="form" :rules="rules" ref="loginForm">
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
            if (response.data && response.data.id) {
              localStorage.setItem('userInfo', JSON.stringify(response.data));
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
  background: linear-gradient(to right, #a8edea, #fed6e3);
}

.login-card {
  width: 100%;
  max-width: 400px;
  padding: 30px;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.1);
}

.title {
  text-align: center;
  font-size: 2em;
  margin-bottom: 20px;
}

.sub-title {
  text-align: center;
  font-size: 1em;
  margin-bottom: 20px;
}

.login-form {
  margin-bottom: 20px;
}

.login-button {
  width: 100%;
}

.sub-title a {
  color: #409eff;
}
</style>
