<script setup lang="ts">
import { User, Lock } from '@element-plus/icons-vue'
import { ref } from 'vue'
// 定义是否已登录（true=登录，false=注册）
const isLoggedIn = ref(true)
// 登录表单
const loginFormRef = ref(null)
// 注册表单
const registerFormRef = ref(null)
/**
 * 切换表单
 * @param none
 * @returns {void}
 */
const toggleForm = () => {
  isLoggedIn.value = !isLoggedIn.value
}
</script>

<template>
  <div class="auth-container">
    <!-- 过渡动画：添加mode="out-in"确保「先出后入」，避免元素重叠 -->
    <button @click="toggleForm">切换测试</button>
    <transition name="auth-transition" mode="out-in">
      <!-- 登录表单 -->
      <div class="login-page" v-if="isLoggedIn" key="login">
        <p>这里是登录表单</p>
        <el-form
          ref="loginFormRef"
          size="large"
          autocomplete="off"
          class="form"
        >
        </el-form>
      </div>
      <!-- 注册表单 -->
      <div class="register-page" v-else key="register">
        <p>这里是注册表单</p>
        <el-form
          ref="registerFormRef"
          size="large"
          autocomplete="off"
          class="form"
        >
        </el-form>
      </div>
    </transition>
  </div>
</template>

<style scoped></style>
