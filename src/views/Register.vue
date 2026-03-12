<template>
  <div class="register-container">
    <el-card class="register-card">
      <h2 class="title">Register for Education AI Assistant</h2>
      <el-form @submit.prevent="register" class="register-form">
        <el-form-item label="Username">
          <el-input v-model="username" placeholder="Choose a username" />
        </el-form-item>

        <el-form-item label="Password">
          <el-input
            v-model="password"
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
export default {
  name: 'RegisterPage',  // 修改为多单词组件名
  data() {
    return {
      username: '',
      password: ''
    };
  },
  methods: {
    register() {
      alert(`Username: ${this.username}, Password: ${this.password}`);
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