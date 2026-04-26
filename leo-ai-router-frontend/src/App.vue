<script setup lang="ts">
import BasicLayout from '@/layouts/BasicLayout.vue'
import { onMounted } from 'vue'
import { useLoginUserStore } from '@/stores/loginUser'

const loginUserStore = useLoginUserStore()

// 每次应用挂载时尝试拉取登录状态
onMounted(async () => {
  await loginUserStore.fetchLoginUser()
})
</script>

<template>
  <BasicLayout />
</template>

<style>
:root {
  --color-bg-page: #f0f2f5;
  --color-bg-card: #ffffff;
  --color-bg-muted: #f5f5f5;
  --color-bg-header: #ffffff;
  --color-text-primary: #1a1a1a;
  --color-text-secondary: #666666;
  --color-text-tertiary: #999999;
  --color-border: #e8e8e8;
  --color-border-weak: rgba(0, 0, 0, 0.06);
  --color-primary: #1890ff;
  --color-primary-hover: #40a9ff;
  --color-on-primary: #ffffff;
  --color-shadow: rgba(0, 0, 0, 0.08);
  --gradient-brand: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  --gradient-surface: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);

  --color-glass-bg: rgba(255, 255, 255, 0.8);
  --color-glass-border: rgba(102, 126, 234, 0.1);

  --color-hero-secondary-bg: rgba(255, 255, 255, 0.2);
  --color-hero-secondary-bg-hover: rgba(255, 255, 255, 0.3);
  --color-hero-secondary-border: rgba(255, 255, 255, 1);
  --color-hero-secondary-text: #ffffff;
}

[data-theme='dark'] {
  --color-bg-page: #0f172a;
  --color-bg-card: #111827;
  --color-bg-muted: #1f2937;
  --color-bg-header: #0b1220;
  --color-text-primary: #e5e7eb;
  --color-text-secondary: #cbd5e1;
  --color-text-tertiary: #94a3b8;
  --color-border: #2b3645;
  --color-border-weak: rgba(255, 255, 255, 0.08);
  --color-primary: #4f9cff;
  --color-primary-hover: #7ab6ff;
  --color-on-primary: #0b1220;
  --color-shadow: rgba(0, 0, 0, 0.45);
  --gradient-brand: linear-gradient(135deg, #1d4ed8 0%, #7c3aed 100%);
  --gradient-surface: linear-gradient(135deg, #131a2b 0%, #0f1422 100%);

  --color-glass-bg: rgba(15, 23, 42, 0.55);
  --color-glass-border: rgba(79, 156, 255, 0.2);

  --color-hero-secondary-bg: rgba(255, 255, 255, 0.08);
  --color-hero-secondary-bg-hover: rgba(255, 255, 255, 0.12);
  --color-hero-secondary-border: rgba(148, 163, 184, 0.7);
  --color-hero-secondary-text: #e5e7eb;
}

/* 全局样式优化 */
* {
  box-sizing: border-box;
}

body {
  margin: 0;
  padding: 0;
  background: var(--color-bg-page);
  color: var(--color-text-primary);
  font-family:
    -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', 'Oxygen', 'Ubuntu', 'Cantarell',
    'Fira Sans', 'Droid Sans', 'Helvetica Neue', sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  overflow-x: hidden;
}

html {
  overflow-x: hidden;
}
</style>
