<template>
  <div class="login-container">
    <el-card class="login-card">
      <h2 class="title">AI 教育助手</h2> <!-- 添加标题 -->
      <p class="sub-title">没有账号？<router-link to="/register">去注册</router-link></p> <!-- 添加提示并跳转到注册页面 -->
      <el-form @submit.prevent="login" class="login-form">
        <el-form-item label="用户名">
          <el-input v-model="username" placeholder="请输入用户名" />
        </el-form-item>

        <el-form-item label="密码">
          <el-input
            v-model="password"
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
export default {
  name: 'LoginPage',  // 组件名称
  data() {
    return {
      username: '',
      password: ''
    };
  },
  methods: {
    login() {
      alert(`用户名: ${this.username}, 密码: ${this.password}`);
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