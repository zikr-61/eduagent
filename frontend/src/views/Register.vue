<template>
  <div class="register-container">
    <el-card class="register-card">
      <h2 class="title">Register for Education AI Assistant</h2>
      <el-form @submit.prevent="register" class="register-form" :model="form" :rules="rules" ref="registerForm">
        <el-form-item label="Username" prop="username">
          <el-input v-model="form.username" placeholder="Choose a username" />
        </el-form-item>

        <el-form-item label="Password" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="Choose a password"
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" native-type="submit" class="register-button">Register</el-button>
        </el-form-item>
      </el-form>
      <p class="footer">
        Already have an account? <router-link to="/login">Login here</router-link>
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
        password: ''
      },
      rules: {
        username: [
          { required: true, message: 'Please enter username', trigger: 'blur' },
          { min: 3, max: 20, message: 'Username length between 3-20 characters', trigger: 'blur' }
        ],
        password: [
          { required: true, message: 'Please enter password', trigger: 'blur' },
          { min: 6, message: 'Password length at least 6 characters', trigger: 'blur' }
        ]
      }
    };
  },
  methods: {
    async register() {
      this.$refs.registerForm.validate(async (valid) => {
        if (valid) {
          try {
            await register(this.form.username, this.form.password);
            alert('注册成功！');
            this.$router.push('/login');
          } catch (error) {
            alert('注册失败：' + (error.response?.data?.error || '未知错误'));
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
  height: 100vh;
  background: linear-gradient(to right, #fbc2eb, #a6c1ee);
}

.register-card {
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

.register-form {
  margin-bottom: 20px;
}

.register-button {
  width: 100%;
}

.footer {
  text-align: center;
  margin-top: 20px;
}

.footer a {
  color: #409eff;
}
</style>
