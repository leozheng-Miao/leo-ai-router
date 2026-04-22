<template>
  <a-layout-header class="header">
    <div class="header-inner">
      <!-- Logo -->
      <RouterLink to="/" class="brand">
        <div class="logo-icon">
          <svg width="28" height="28" viewBox="0 0 28 28" fill="none">
            <rect width="28" height="28" rx="7" fill="url(#logoGrad)" />
            <path
              d="M8 9h12M8 14h8M8 19h12"
              stroke="#fff"
              stroke-width="2"
              stroke-linecap="round"
            />
            <defs>
              <linearGradient
                id="logoGrad"
                x1="0"
                y1="0"
                x2="28"
                y2="28"
                gradientUnits="userSpaceOnUse"
              >
                <stop stop-color="#2563EB" />
                <stop offset="1" stop-color="#7C3AED" />
              </linearGradient>
            </defs>
          </svg>
        </div>
        <span class="brand-name">LeoAI Router</span>
        <span class="brand-tag">Beta</span>
      </RouterLink>

      <!-- Nav -->
      <nav class="nav-links">
        <RouterLink
          v-for="item in navItems"
          :key="item.path"
          :to="item.path"
          class="nav-link"
          :class="{ active: isActive(item.path) }"
        >
          {{ item.label }}
        </RouterLink>
      </nav>

      <!-- Right -->
      <div class="header-right">
        <a href="#" class="doc-link" target="_blank">
          <FileTextOutlined />
          <span>文档</span>
        </a>

        <template v-if="loginUserStore.loginUser.id">
          <a-dropdown placement="bottomRight" :trigger="['click']">
            <div class="user-trigger">
              <a-avatar :src="loginUserStore.loginUser.userAvatar" :size="28" class="user-avatar">
                {{ userInitial }}
              </a-avatar>
              <span class="user-name">{{ loginUserStore.loginUser.userName ?? '用户' }}</span>
              <DownOutlined class="chevron" />
            </div>
            <template #overlay>
              <a-menu class="user-menu">
                <a-menu-item key="info" disabled>
                  <div class="menu-user-info">
                    <div class="menu-user-name">{{ loginUserStore.loginUser.userName }}</div>
                    <div class="menu-user-role">
                      {{ loginUserStore.loginUser.userRole === 'admin' ? '管理员' : '普通用户' }}
                    </div>
                  </div>
                </a-menu-item>
                <a-menu-divider />
                <a-menu-item key="keys">
                  <RouterLink to="/keys" style="color: #374151; text-decoration: none">
                    <KeyOutlined /> API Keys
                  </RouterLink>
                </a-menu-item>
                <a-menu-divider />

                <a-menu-item key="logout" @click="handleLogout">
                  <LogoutOutlined /> 退出登录
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </template>

        <template v-else>
          <RouterLink to="/user/login" class="btn-login">登录</RouterLink>
          <RouterLink to="/user/register" class="btn-register">免费注册</RouterLink>
        </template>
      </div>
    </div>
  </a-layout-header>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import { DownOutlined, LogoutOutlined, FileTextOutlined, KeyOutlined } from '@ant-design/icons-vue'
import { useLoginUserStore } from '@/stores/loginUser'
import { userLogout } from '@/api/userController'

const router = useRouter()
const route = useRoute()
const loginUserStore = useLoginUserStore()

const navItems = [
  { label: '首页', path: '/' },
  { label: '模型广场', path: '/models' },
  { label: '在线对话', path: '/chat' },
  { label: '价格', path: '/pricing' },
]

const isActive = (path: string) => (path === '/' ? route.path === '/' : route.path.startsWith(path))

const userInitial = computed(() =>
  (loginUserStore.loginUser.userName ?? 'U').charAt(0).toUpperCase(),
)

const handleLogout = async () => {
  const res = await userLogout()
  if (res.data.code === 0) {
    loginUserStore.setLoginUser({ userName: '未登录' })
    message.success('已退出登录')
    router.push('/user/login')
  } else {
    message.error('退出失败：' + res.data.message)
  }
}
</script>

<style scoped>
.header {
  position: sticky;
  top: 0;
  z-index: 100;
  height: 56px;
  line-height: 56px;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.97);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid #e5e7eb;
  padding: 0;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.header-inner {
  max-width: 1280px;
  margin: 0 auto;
  padding: 0 24px;
  gap: 32px;
  height: 100%;
  display: flex;
  align-items: center;
}

.brand {
  display: flex;
  align-items: center;
  gap: 8px;
  text-decoration: none;
  flex-shrink: 0;
}

.logo-icon {
  display: flex;
  align-items: center;
}

.brand-name {
  font-size: 15px;
  font-weight: 700;
  color: #111827;
  letter-spacing: -0.3px;
}

.brand-tag {
  font-size: 10px;
  font-weight: 600;
  color: #7c3aed;
  background: #f5f3ff;
  border: 1px solid #e9d5ff;
  border-radius: 4px;
  padding: 1px 5px;
  line-height: 16px;
}

.nav-links {
  display: flex;
  align-items: center;
  gap: 2px;
  flex: 1;
}

.nav-link {
  padding: 0 12px;
  font-size: 14px;
  font-weight: 500;
  color: #6b7280;
  text-decoration: none;
  border-radius: 6px;
  transition: all 0.15s;
}

.nav-link:hover {
  color: #111827;
  background: #f3f4f6;
}
.nav-link.active {
  color: #2563eb;
  background: #eff6ff;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.doc-link {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 13px;
  color: #6b7280;
  text-decoration: none;
  padding: 5px 10px;
  border-radius: 6px;
  transition: all 0.15s;
}

.doc-link:hover {
  color: #374151;
  background: #f3f4f6;
}

.user-trigger {
  display: flex;
  align-items: center;
  gap: 7px;
  cursor: pointer;
  padding: 0 10px;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  transition: all 0.15s;
}

.user-trigger:hover {
  background: #f9fafb;
  border-color: #d1d5db;
}

.user-avatar {
  background: linear-gradient(135deg, #2563eb, #7c3aed);
  color: #fff;
  font-size: 11px;
  font-weight: 700;
}

.user-name {
  font-size: 13px;
  font-weight: 500;
  color: #374151;
  max-width: 80px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chevron {
  font-size: 10px;
  color: #9ca3af;
}

.menu-user-info {
  padding: 2px 0;
}
.menu-user-name {
  font-size: 13px;
  font-weight: 600;
  color: #111827;
}
.menu-user-role {
  font-size: 12px;
  color: #9ca3af;
  margin-top: 1px;
}

.btn-login {
  font-size: 13px;
  font-weight: 500;
  color: #374151;
  text-decoration: none;
  padding: 6px 14px;
  border-radius: 7px;
  border: 1px solid #e5e7eb;
  transition: all 0.15s;
}

.btn-login:hover {
  background: #f9fafb;
  border-color: #d1d5db;
  color: #111827;
}

.btn-register {
  font-size: 13px;
  font-weight: 600;
  color: #fff;
  text-decoration: none;
  padding: 6px 14px;
  border-radius: 7px;
  background: linear-gradient(135deg, #2563eb, #7c3aed);
  transition: opacity 0.15s;
}

.btn-register:hover {
  opacity: 0.88;
}
</style>
