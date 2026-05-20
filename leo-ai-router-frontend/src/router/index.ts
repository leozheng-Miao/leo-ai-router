import { createRouter, createWebHistory } from 'vue-router'
import HomePage from '@/pages/HomePage.vue'
import UserCenterLayout from '@/layouts/UserCenterLayout.vue'
import { useLoginUserStore } from '@/stores/loginUser'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomePage,
    },
    {
      path: '/dashboard',
      name: 'dashboard',
      component: () => import('@/pages/DashboardPage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/user/login',
      name: 'userLogin',
      component: () => import('@/pages/user/UserLoginPage.vue'),
    },
    {
      path: '/user/register',
      name: 'userRegister',
      component: () => import('@/pages/user/UserRegisterPage.vue'),
    },
    {
      path: '/oauth/wechat/callback',
      name: 'wechatOAuthCallback',
      component: () => import('@/pages/user/WechatOAuthCallbackPage.vue'),
    },
    {
      path: '/keys',
      component: UserCenterLayout,
      children: [
        {
          path: '',
          name: 'apiKeys',
          component: () => import('@/pages/user/ApiKeyPage.vue'),
          meta: { requiresAuth: true },
        },
      ],
    },
    {
      path: '/profile',
      component: UserCenterLayout,
      children: [
        {
          path: '',
          name: 'profile',
          component: () => import('@/pages/user/ProfilePage.vue'),
          meta: { requiresAuth: true },
        },
      ],
    },
    {
      path: '/membership',
      component: UserCenterLayout,
      children: [
        {
          path: '',
          name: 'membership',
          component: () => import('@/pages/user/MembershipPage.vue'),
        },
      ],
    },
    {
      path: '/recharge/success',
      name: 'rechargeSuccess',
      component: () => import('@/pages/user/RechargeSuccessPage.vue'),
    },
    {
      path: '/recharge/cancel',
      name: 'rechargeCancel',
      component: () => import('@/pages/user/RechargeCancelPage.vue'),
    },
    {
      path: '/history',
      component: UserCenterLayout,
      children: [
        {
          path: '',
          name: 'history',
          component: () => import('@/pages/user/HistoryPage.vue'),
          meta: { requiresAuth: true },
        },
      ],
    },
    {
      path: '/providers',
      name: 'providers',
      component: () => import('@/pages/admin/ProviderManagementPage.vue'),
    },
    {
      path: '/models',
      name: 'models',
      component: () => import('@/pages/admin/ModelManagementPage.vue'),
    },
    {
      path: '/plugins',
      name: 'plugins',
      component: () => import('@/pages/admin/PluginManagementPage.vue'),
    },
    {
      path: '/chat',
      name: 'chat',
      component: () => import('@/pages/chat/ChatPage.vue'),
    },
    {
      path: '/images',
      name: 'images',
      component: () => import('@/pages/image/ImageGenerationPage.vue'),
    },
    {
      path: '/users',
      name: 'users',
      component: () => import('@/pages/admin/UserManagementPage.vue'),
    },
    {
      path: '/roles',
      name: 'roles',
      component: () => import('@/pages/admin/RoleManagementPage.vue'),
    },
  ],
})

router.beforeEach(async (to) => {
  if (!to.matched.some((route) => route.meta.requiresAuth)) {
    return true
  }

  const loginUserStore = useLoginUserStore()
  if (!loginUserStore.loginUser.id) {
    await loginUserStore.fetchLoginUser()
  }

  if (loginUserStore.loginUser.id) {
    return true
  }

  return {
    path: '/user/login',
    query: { redirect: to.fullPath },
  }
})

export default router
