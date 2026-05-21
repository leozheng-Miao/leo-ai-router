<script setup lang="ts">
withDefaults(
  defineProps<{
    flush?: boolean
  }>(),
  {
    flush: false,
  },
)
</script>

<template>
  <section class="app-panel" :class="{ 'app-panel--flush': flush }">
    <slot />
  </section>
</template>

<style scoped>
.app-panel {
  width: 100%;
  padding: 18px;
  background: var(--leo-bg-panel);
  border: 1px solid var(--leo-border);
  border-radius: var(--leo-radius-md);
}

.app-panel--flush {
  padding: 0;
}
</style>
