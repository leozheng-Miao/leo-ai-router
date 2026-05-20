<template>
  <nav class="sidebar-nav" aria-label="侧边导航">
    <RouterLink v-for="item in items" :key="item.path" :to="item.path" class="sidebar-nav__item">
      <component :is="item.icon" v-if="item.icon" class="sidebar-nav__icon" />
      <span class="sidebar-nav__label">{{ item.label }}</span>
    </RouterLink>
  </nav>
</template>

<script setup lang="ts">
defineProps<{
  items: Array<{ label: string; path: string; icon?: unknown }>
}>()
</script>

<style scoped>
.sidebar-nav {
  display: flex;
  flex-direction: column;
  gap: 4px;
  width: 100%;
}

.sidebar-nav__item {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 38px;
  padding: 0 12px;
  color: var(--leo-text-secondary);
  font-size: 14px;
  font-weight: 600;
  line-height: 1;
  text-decoration: none;
  border-radius: var(--leo-radius-sm);
  transition:
    background-color 0.16s ease,
    color 0.16s ease;
}

.sidebar-nav__item:hover {
  color: var(--leo-primary);
  background: var(--leo-primary-soft);
}

.sidebar-nav__item.router-link-active {
  color: var(--leo-primary);
  background: var(--leo-bg-active);
}

.sidebar-nav__icon {
  width: 16px;
  height: 16px;
  flex: 0 0 16px;
}

.sidebar-nav__label {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
