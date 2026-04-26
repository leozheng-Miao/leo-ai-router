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
      path: '/chat',
      name: 'chat',
      component: () => import('@/pages/chat/ChatPage.vue'),
    },
  ],
})

export default router
