<template>
  <div class="user-center-layout">
    <aside class="user-center-sidebar">
      <RouterLink to="/" class="user-center-brand">LeoAI Router</RouterLink>
      <SidebarNav :items="items" />
    </aside>
    <main class="user-center-content">
      <slot>
        <RouterView />
      </slot>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { ApiOutlined, BarChartOutlined, CreditCardOutlined, UserOutlined } from '@ant-design/icons-vue'
import SidebarNav from '@/components/ui/SidebarNav.vue'
import { useLoginUserStore } from '@/stores/loginUser'

const loginUserStore = useLoginUserStore()

const allItems = [
  { label: '个人中心', path: '/profile', icon: UserOutlined, auth: true },
  { label: 'API Keys', path: '/keys', icon: ApiOutlined, auth: true },
  { label: '会员充值', path: '/membership', icon: CreditCardOutlined },
  { label: '使用统计', path: '/history', icon: BarChartOutlined, auth: true },
]

const items = computed(() => allItems.filter((item) => !item.auth || loginUserStore.loginUser.id))
</script>

<style scoped>
.user-center-layout {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr);
  gap: 18px;
  width: min(100%, var(--leo-page-max));
  margin: 0 auto;
  padding: 18px 24px 32px;
}

.user-center-sidebar {
  position: sticky;
  top: calc(var(--leo-header-height) + 16px);
  align-self: start;
  display: flex;
  flex-direction: column;
  gap: 14px;
  min-height: 240px;
  padding: 16px;
  background: var(--leo-bg-panel);
  border: 1px solid var(--leo-border);
  border-radius: var(--leo-radius-md);
}

.user-center-brand {
  display: inline-flex;
  align-items: center;
  min-height: 32px;
  color: var(--leo-text-primary);
  font-size: 15px;
  font-weight: 800;
  line-height: 1;
  text-decoration: none;
}

.user-center-content {
  min-width: 0;
}

@media (max-width: 900px) {
  .user-center-layout {
    grid-template-columns: 1fr;
    padding: 14px 16px 28px;
  }

  .user-center-sidebar {
    position: static;
    min-height: 0;
  }
}
</style>
