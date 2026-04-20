<template>
  <a-layout-header class="header">
    <div class="header-inner">
      <!-- 左侧 Logo -->
      <RouterLink to="/" class="brand">
        <img class="logo" src="@/assets/logo.png" alt="logo" />
        <span class="site-title">AI Router</span>
      </RouterLink>

      <!-- 中间导航 -->
      <nav class="nav-menu">
        <a-menu
          v-model:selectedKeys="selectedKeys"
          mode="horizontal"
          :items="menuItems"
          class="menu"
          @click="handleMenuClick"
        />
      </nav>

      <!-- 右侧用户区 -->
      <div class="user-area">
        <template v-if="loginUserStore.loginUser.id">
          <a-dropdown placement="bottomRight">
            <div class="user-info">
              <a-avatar :src="loginUserStore.loginUser.userAvatar" :size="32" class="avatar">
                {{ loginUserStore.loginUser.userName?.charAt(0) ?? 'U' }}
              </a-avatar>
              <span class="username">{{ loginUserStore.loginUser.userName ?? '用户' }}</span>
              <DownOutlined class="arrow-icon" />
            </div>
            <template #overlay>
              <a-menu class="user-dropdown">
                <a-menu-item key="logout" @click="doLogout">
                  <LogoutOutlined />
                  <span>退出登录</span>
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </template>
        <template v-else>
          <RouterLink to="/user/login">
            <a-button type="primary" class="login-btn">登录</a-button>
          </RouterLink>
        </template>
      </div>
    </div>
  </a-layout-header>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { type MenuProps, message } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/loginUser.ts'
import { userLogout } from '@/api/userController.ts'
import { LogoutOutlined, HomeOutlined, DownOutlined, KeyOutlined } from '@ant-design/icons-vue'
import { h } from 'vue'

const loginUserStore = useLoginUserStore()
const router = useRouter()
const selectedKeys = ref<string[]>(['/'])

router.afterEach((to) => {
  selectedKeys.value = [to.path]
})

const originItems: MenuProps['items'] = [
  { key: '/', icon: () => h(HomeOutlined), label: '主页', title: '主页' },
  { key: '/admin/userManage', label: '用户管理', title: '用户管理' },
]

const menuItems = computed<MenuProps['items']>(() =>
  originItems?.filter((menu) => {
    const key = menu?.key as string
    if (key?.startsWith('/admin')) {
      return loginUserStore.loginUser?.userRole === 'admin'
    }
    return true
  }),
)

const handleMenuClick: MenuProps['onClick'] = (e) => {
  const key = e.key as string
  selectedKeys.value = [key]
  if (key.startsWith('/')) router.push(key)
}

const doLogout = async () => {
  const res = await userLogout()
  if (res.data.code === 0) {
    loginUserStore.setLoginUser({ userName: '未登录' })
    message.success('退出登录成功')
    await router.push('/user/login')
  } else {
    message.error('退出登录失败，' + res.data.message)
  }
}
</script>

<style scoped>
.header {
  position: sticky;
  top: 0;
  z-index: 100;
  height: 64px;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  padding: 0;
  line-height: 64px;
}

.header-inner {
  max-width: 1280px;
  margin: 0 auto;
  padding: 0 24px;
  height: 100%;
  display: flex;
  align-items: center;
  gap: 24px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  text-decoration: none;
  flex-shrink: 0;
}

.logo {
  width: 36px;
  height: 36px;
  border-radius: 8px;
}

.site-title {
  font-size: 18px;
  font-weight: 700;
  background: linear-gradient(135deg, #3b82f6, #8b5cf6);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  white-space: nowrap;
}

.nav-menu {
  flex: 1;
}

.menu {
  border-bottom: none !important;
  background: transparent;
  line-height: 62px;
}

.user-area {
  flex-shrink: 0;
  display: flex;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 12px 4px 4px;
  border-radius: 24px;
  transition: background 0.2s;
}

.user-info:hover {
  background: rgba(0, 0, 0, 0.04);
}

.avatar {
  background: linear-gradient(135deg, #3b82f6, #8b5cf6);
  color: #fff;
  font-weight: 600;
}

.username {
  font-size: 14px;
  color: #374151;
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.arrow-icon {
  font-size: 11px;
  color: #9ca3af;
}

.login-btn {
  border-radius: 20px;
  padding: 0 20px;
  background: linear-gradient(135deg, #3b82f6, #8b5cf6);
  border: none;
  font-weight: 500;
}

.login-btn:hover {
  opacity: 0.88;
}
</style>
