import { createRouter, createWebHistory } from 'vue-router'
import HomePage from '@/pages/HomePage.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomePage,
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
      path: '/keys',
      name: 'apiKeys',
      component: () => import('@/pages/user/ApiKeyPage.vue'),
    },
    {
      path: '/profile',
      name: 'profile',
      component: () => import('@/pages/user/ProfilePage.vue'),
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
      name: 'history',
      component: () => import('@/pages/user/HistoryPage.vue'),
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
  ],
})

export default router
