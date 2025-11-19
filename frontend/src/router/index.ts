import { createRouter, createWebHistory } from 'vue-router'

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
          component: () => import('@/views/console/Console.vue'),
          redirect: 'console/hydrantlist',
          children: [
            {
              path: 'hydrantlist',
              name: 'HydrantList',
              component: () =>
                import('@/views/console/list/HydrantList.vue'),
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

export default router
