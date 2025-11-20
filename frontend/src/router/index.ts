import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/modules/userStore'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    // 认证（登陆注册）页面 - 使用 AuthLayout
    {
      path: '/',
      component: () => import('@/layouts/AuthLayout.vue'),
      redirect: '/login', // 默认进入登陆页面
      children: [
        {
          path: 'login',
          name: 'login',
          component: () => import('@/views/auth/Auth.vue'),
        },
      ],
    },
    // 主应用页面 - 使用 MainLayout
    {
      path: '/app',
      component: () => import('@/layouts/MainLayout.vue'),
      redirect: '/app/dashboard', // 默认进入仪表盘页面
      children: [
        // 仪表盘页面
        {
          path: 'dashboard',
          name: 'Dashboard',
          component: () => import('@/views/dashboard/Dashboard.vue'),
        },
        // 控制台页面
        {
          path: 'console',
          name: 'Console',
          meta: { requiresAdmin: true }, // 需要管理员权限
          component: () => import('@/views/console/Console.vue'),
          redirect: 'console/hydrantlist', // 默认进入消防栓列表页面
          children: [
            {
              path: 'hydrantlist',
              name: 'HydrantList',
              component: () =>
                import('@/views/console/list/HydrantList.vue'),
            },
            {
              path: 'userlist',
              name: 'userlist',
              component: () =>
                import('@/views/console/list/UserList.vue'),
            },
          ],
        },
        // 用户信息页面
        {
          path: 'profile',
          name: 'Profile',
          component: () => import('@/views/profile/Profile.vue'),
        },
      ],
    },
  ],
})
// 路由守卫
router.beforeEach((to, from, next) => {
  // 获取用户信息
  const userStore = useUserStore()
  // 判断目标路由是否需要登录
  // 即除了登录页外的所有页面都需要登录
  const needLogin = to.path !== '/login'

  // 如果需要登录且用户未登录
  if (needLogin && !userStore.isLoggedIn) {
    // 跳转登录页
    next('/')
    // 如果是访问登录页且用户已登录
  } else if (to.path === '/login' && userStore.isLoggedIn) {
    next('/app/dashboard') // 跳转到默认登录后页面
  } else if (to.meta.requiresAdmin) {
    // 访问需要管理员权限的页面
    if (userStore.role === 'ADMIN') {
      // 是管理员，正常访问
      next()
    } else {
      // 不是管理员，提示无权限并跳转到地图页
      ElMessage.error('无权限访问该页面')
      next('/app/dashboard')
    }
  } else {
    // 其他情况，正常访问
    next()
  }
})

export default router
