<template>
  <div id="app">
    <router-view></router-view>
    <nav>
      <router-link to="/login">Login</router-link> |
      <router-link to="/register">Register</router-link>
    </nav>
  </div>
</template>

<script>
export default {
  name: 'App'
};
</script>