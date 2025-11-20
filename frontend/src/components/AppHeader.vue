<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/stores/modules/userStore'
import { logout } from '@/api/user'
import router from '@/router'
const userStore = useUserStore()
const route = useRoute() // 获取当前路由信息
// const router = useRouter() // 用于编程式导航
// 计算属性：应该激活的顶级菜单项
const activeMenu = computed(() => {
  const path = route.path
  if (path.includes('/console')) {
    return '/app/console' // 高亮“控制台”菜单
  } else if (path.includes('/profile')) {
    return '/app/profile' // 高亮“用户管理”菜单
  } else if (path.includes('/dashboard')) {
    return '/app/dashboard' // 高亮“用户管理”菜单
  } else {
    return path
  }
})
// 退出登录方法
const HandleLogout = async () => {
  try {
    await logout()
    userStore.clearUserInfo()
    router.push('/login')
    ElMessage.success('退出成功')
  } catch (error) {
    console.error('退出登录失败', error)
  }
}
</script>
<template>
  <header>
    <!-- logo -->
    <div class="header-logo">
      <img
        class="logo-img"
        src="/logo.png"
        alt="校园消防栓管理系统"
      />
      <span class="logo-text">校园消防栓管理系统</span>
      <span class="logo-version">0.0.1</span>
    </div>
    <!-- 导航栏 -->
    <div class="header-nav">
      <el-menu mode="horizontal" :default-active="activeMenu" router>
        <el-menu-item index="/app/dashboard">主页</el-menu-item>
        <el-menu-item index="/app/console">控制台</el-menu-item>
        <!-- <el-menu-item index="/app/profile">个人中心</el-menu-item>
          -->
        <!-- 主题开关（示例：控制深色模式） -->
        <!-- <div class="header-actions">
          <theme-toggle></theme-toggle>
        </div> -->
        <el-menu-item index="/app/profile"
          ><el-dropdown>
            <div class="user-info">
              <p>欢迎: {{ userStore.userInfo?.name }}</p>
              <!-- <div class="user-icon">
                <el-icon color="#fff" :size="24"><User /></el-icon>
              </div> -->
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="toInfo()"
                  >基本资料</el-dropdown-item
                >
                <!-- <el-dropdown-item @click="toRepwd()"
                  >重置密码</el-dropdown-item
                > -->
                <el-dropdown-item @click="HandleLogout()"
                  >退出登录</el-dropdown-item
                >
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </el-menu-item>
      </el-menu>
    </div>
  </header>
</template>

<style scoped lang="scss">
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
  margin-left: 8px;
  .logo-img {
    width: 40px;
    margin-right: 10px;
  }
  .logo-text {
    font-size: 18px;
    font-weight: bold;
    color: #409eff;
  }
  .logo-version {
    margin-left: 8px;
    font-size: 12px;
    color: #999;
  }
}

/* 导航栏 */
.header-nav {
  margin-right: 10px;
  width: 350px;
}
// 用户信息下拉菜单
.el-dropdown {
  display: flex;
  // 往下移动
  align-items: center;
  cursor: pointer;
  .user-info {
    display: flex;
    align-items: center;
  }
  .user-name {
    font-size: 14px;
    color: #333;
    font-weight: bold;
  }
}
.el-dropdown:focus-visible {
  outline: none;
}
.user-info:focus-visible {
  outline: none;
}
p :focus-visible {
  outline: none;
}
</style>
