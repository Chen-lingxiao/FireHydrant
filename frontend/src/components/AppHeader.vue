<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
// import { useRouter } from 'vue-router'
const route = useRoute() // 获取当前路由信息
// const router = useRouter() // 用于编程式导航
// 计算属性：应该激活的顶级菜单项
const activeMenu = computed(() => {
  const path = route.path
  if (path.includes('/console')) {
    return '/console' // 高亮“控制台”菜单
  } else if (path.includes('/profile')) {
    return '/profile' // 高亮“用户管理”菜单
  } else {
    return path
  }
})
</script>
<template>
  <header>
    <!-- logo -->
    <div class="header-logo">
      <img src="/logo.png" alt="市政消防栓管理系统" />
      <span class="logo-text">市政消防栓管理系统</span>
      <span class="logo-version">0.0.1</span>
    </div>
    <!-- 菜单栏 -->
    <div class="header-nav">
      <el-menu mode="horizontal" :default-active="activeMenu" router>
        <el-menu-item index="/app/dashboard">主页</el-menu-item>
        <el-menu-item index="/app/console">控制台</el-menu-item>
        <el-menu-item index="/app/profile">个人中心</el-menu-item>
        <!-- 主题开关（示例：控制深色模式） -->
        <!-- <div class="header-actions">
          <theme-toggle></theme-toggle>
        </div> -->
        <!-- <el-dropdown>
          <div class="user-info">
            <div class="user-icon">
              <el-icon color="#fff" :size="24"><User /></el-icon>
            </div>
            <div class="user-name">
              <p>{{ userStore.userInfo?.name }}</p>
            </div>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="toInfo()">基本资料</el-dropdown-item>
              <el-dropdown-item @click="toRepwd()">重置密码</el-dropdown-item>
              <el-dropdown-item @click="logout()">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown> -->
      </el-menu>
    </div>
  </header>
</template>

<style>
header {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #ebeef5;
  position: relative;
  z-index: 10;
}

.header-logo {
  display: flex;
  align-items: center;
}

.header-logo img {
  width: 40px;
  margin-right: 10px;
}

.header-logo .logo-text {
  font-size: 18px;
  font-weight: bold;
  color: #409eff;
}

.header-logo .logo-version {
  margin-left: 8px;
  font-size: 12px;
  color: #999;
}

/* 导航栏 */
.header-nav {
  margin-right: 10px;
  width: 350px;
}
.el-dropdown {
  display: flex;
  align-items: center;
  cursor: pointer;
  margin-left: 10px;
}
.el-dropdown .user-info {
  display: flex;
  align-items: center;
}
.el-dropdown .user-icon {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background-color: #409eff;
  margin-right: 8px;
}

.el-dropdown .user-icon .el-icon {
  /* 居中 */
  height: 100%;
  width: 100%;
}
.el-dropdown .user-name {
  font-size: 14px;
  color: #333;
  font-weight: bold;
}
</style>
