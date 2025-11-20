<script setup lang="ts">
import { User, Lock } from '@element-plus/icons-vue'
import { ref } from 'vue'
import { login, register } from '@/api/userInfo'
import { useUserStore } from '@/stores/modules/userStore'
import router from '@/router'
import type { FormInstance } from 'element-plus'
const userStore = useUserStore()

// 定义是否已登录（true=登录，false=注册）
const isLoggedIn = ref(true)
const loginFormRef = ref<FormInstance>()
const registerFormRef = ref<FormInstance>()

// 登录表单数据
const loginForm = ref({
  name: '',
  password: '',
})
// 注册表单数据
const registerForm = ref({
  name: '',
  password: '',
  confirmPassword: '',
})
// rules 规则校验
const rules = {
  name: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    {
      min: 5,
      max: 10,
      message: '用户名必须是5-10位的字符',
      trigger: 'blur',
    },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    {
      pattern: /^\S{6,15}$/,
      message: '密码必须是6-15位的非空字符',
      trigger: 'blur',
    },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      pattern: /^\S{6,15}$/,
      message: '密码必须是6-15的非空字符',
      trigger: 'blur',
    },
    {
      validator: (
        _rule: unknown,
        value: string,
        callback: (error?: Error) => void,
      ) => {
        if (value !== registerForm.value.password) {
          callback(new Error('两次输入密码不一致!'))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
}
/**
 * 切换表单
 * @param none
 * @returns {void}
 */
const toggleForm = () => {
  isLoggedIn.value = !isLoggedIn.value
}
/**
 * 登录
 * @param none
 * @returns {void}
 */
const handleLogin = async () => {
  // 验证规则
  if (!loginFormRef.value) return
  await loginFormRef.value.validate()

  try {
    console.log('loginForm', loginForm.value)
    // 调用登录接口
    const response = await login(loginForm.value)
    console.log('response', response)

    if (response.data.code === 200) {
      // 存储用户信息和token
      userStore.setUserInfo(response.data.data)
      // 无需存储token到userStore，Cookie已由浏览器自动保存
      // userStore.setToken(response.data.token)

      ElMessage.success(response.data.message)
      // 登录成功跳转到地图页面
      router.push('/app/dashboard')
    }
  } catch (error) {
    console.error('登录失败', error)
  }
}

/**
 * 注册
 * @param none
 * @returns {void}
 */
const handleRegister = async () => {
  // 验证规则
  if (!registerFormRef.value) return
  await registerFormRef.value.validate()

  try {
    console.log('registerForm', registerForm.value)
    // 调用注册接口
    const response = await register({
      name: registerForm.value.name,
      password: registerForm.value.password,
    })
    if (response.data.code === 200) {
      ElMessage.success(response.data.message)
      // 注册成功切换到登录页面
      toggleForm()
      // 清空注册表单
      registerForm.value = {
        name: '',
        password: '',
        confirmPassword: '',
      }
    }
  } catch (error) {
    console.error('注册失败', error)
  }
}
</script>

<template>
  <div class="auth-container">
    <!-- 过渡动画：添加mode="out-in"确保「先出后入」，避免元素重叠 -->
    <button @click="toggleForm">切换测试</button>
    <transition name="auth-transition" mode="out-in">
      <!-- 登录表单 -->
      <div class="login-page" v-if="isLoggedIn" key="login">
        <el-form
          ref="loginFormRef"
          :model="loginForm"
          :rules="rules"
          size="large"
          autocomplete="off"
          class="form"
        >
          <el-form-item>
            <h1 class="form-title">登录</h1>
          </el-form-item>
          <el-form-item prop="name">
            <el-input
              v-model="loginForm.name"
              :prefix-icon="User"
              placeholder="请输入用户名"
            ></el-input>
          </el-form-item>
          <el-form-item prop="password"
            ><el-input
              v-model="loginForm.password"
              :prefix-icon="Lock"
              type="password"
              placeholder="请输入密码"
            ></el-input
          ></el-form-item>
          <el-form-item
            ><el-button
              type="primary"
              class="form-btn"
              @click="handleLogin"
              >登录</el-button
            >
          </el-form-item>
          <el-form-item class="form-row">
            还没账号？
            <el-link
              type="info"
              :underline="false"
              @click="toggleForm()"
            >
              点击注册 →
            </el-link>
          </el-form-item>
        </el-form>
      </div>
      <!-- 注册表单 -->
      <div class="register-page" v-else key="register">
        <div class="register-info">
          <h1 class="info-title">欢迎注册</h1>
          <p class="info-desc">立即注册，探索更多精彩功能</p>
          <div class="info-divider"></div>
          <p class="info-tip">校园消防栓管理系统：</p>
          <ul class="info-list">
            <li>· (*^▽^*)</li>
            <li>·(*^▽^*)</li>
            <li>· (*^▽^*)</li>
          </ul>
        </div>
        <el-form
          ref="registerFormRef"
          :model="registerForm"
          :rules="rules"
          size="large"
          autocomplete="off"
          class="form"
        >
          <el-form-item>
            <h1 class="form-title">创建账号</h1>
          </el-form-item>
          <el-form-item prop="name"
            ><el-input
              v-model="registerForm.name"
              :prefix-icon="User"
              placeholder="请输入用户名"
            ></el-input>
          </el-form-item>

          <el-form-item prop="password">
            <el-input
              v-model="registerForm.password"
              :prefix-icon="Lock"
              type="password"
              placeholder="请输入密码"
            ></el-input>
          </el-form-item>
          <el-form-item prop="confirmPassword">
            <el-input
              v-model="registerForm.confirmPassword"
              :prefix-icon="Lock"
              type="password"
              placeholder="请再次输入密码"
            ></el-input>
          </el-form-item>
          <el-form-item
            ><el-button
              type="primary"
              class="form-btn"
              @click="handleRegister"
              >注册</el-button
            >
          </el-form-item>
          <el-form-item class="form-row">
            <el-link
              type="info"
              :underline="false"
              @click="toggleForm()"
            >
              ← 返回登录
            </el-link>
          </el-form-item>
        </el-form>
      </div>
    </transition>
  </div>
</template>

<style scoped>
/* 外层容器：统一控制全屏背景 */
.auth-container {
  min-height: 100vh;
  background: url('https://cn.bing.com/th?id=OHR.MaldivesWhaleShark_EN-US3819740955_UHD.jpg&pid=hp&w=1920')
    no-repeat center center/cover;
  padding: 20px 0;
  /* 上下左右居中 */
  display: flex;
  justify-content: center;
  align-items: center;
  position: relative; /* 添加相对定位 */
}

/* 登录/注册页面：统一flex居中（子元素通过col控制位置） */
.login-page,
.register-page {
  display: flex; /* 使用Flexbox布局 */
  align-items: center; /* 上下居中 */
  justify-content: center; /* 左右居中 */
  position: absolute; /* 改为绝对定位，确保过渡时位置一致 */
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
}
.login-page {
  width: 500px;
}
.register-page {
  width: 1000px;
}
/* 表单主体样式：自适应高度 + 半透明背景 */
.form {
  width: 500px;
  height: 450px;
  padding: 40px 30px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.651);
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.2);
  backdrop-filter: blur(2px); /* 毛玻璃效果 */
}

.register-info {
  width: 400px;
  height: 450px;
  padding: 40px 30px;
  border-radius: 16px 0 0 16px;
  background-color: rgba(255, 255, 255, 0.8); /* 半透明背景 */
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.1); /* 柔和阴影 */
  backdrop-filter: blur(2px); /* 毛玻璃效果 */
  display: flex;
  flex-direction: column;
}
.register-page .form {
  border-radius: 0 16px 16px 0;
  background-color: rgba(255, 255, 255, 0.8); /* 半透明背景 */
}

/* 注册信息区样式优化 */
.info-title {
  font-size: 28px;
  font-weight: 700;
  margin-bottom: 15px;
  color: #2c3e50;
}
.info-desc {
  font-size: 18px;
  margin-bottom: 20px;
  color: #666;
}
.info-divider {
  height: 2px;
  background: linear-gradient(90deg, #409eff, transparent);
  margin: 20px 0;
}
.info-tip {
  font-size: 16px;
  font-weight: 500;
  margin-bottom: 10px;
  color: #409eff;
}
.info-list {
  list-style: none;
  padding: 0;
  font-size: 14px;
  color: #666;
  line-height: 1.8;
}

/* 表单标题样式 */
.form-title {
  font-size: 26px;
  font-weight: 600;
  color: #2c3e50;
  text-align: left;
}

/* 表单按钮样式 */
.form-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
  border-radius: 8px;
  transition: all 0.3s ease;
}
.form-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 15px rgba(64, 158, 255, 0.4);
}

/* 表单行布局（复选框/链接） */
.form-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
  font-size: 14px;
}
.el-link {
  cursor: pointer;
  margin-left: 10px;
  color: #409eff;
}
/* 输入框样式优化 */
:deep(.el-input__inner) {
  height: 48px;
  margin-left: 5px;
  background-color: #fff;
  border-radius: 8px;
  border-color: #e5e7eb;
  transition: all 0.3s ease;
}

/* 过渡动画 */
.auth-transition-enter-active,
.auth-transition-leave-active {
  transition: all 0.5s ease;
}
.auth-transition-enter-from {
  opacity: 0;
  transform: translate(-50%, -50%) translateX(30px);
}
.auth-transition-leave-to {
  opacity: 0;
  transform: translate(-50%, -50%) translateX(-30px);
}
</style>
