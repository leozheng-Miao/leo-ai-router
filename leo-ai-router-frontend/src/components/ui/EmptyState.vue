<script setup lang="ts">
defineProps<{
  title: string
  description?: string
}>()
</script>

<template>
  <div class="empty-state">
    <div class="empty-state__mark">--</div>
    <div class="empty-state__title">{{ title }}</div>
    <p v-if="description" class="empty-state__description">{{ description }}</p>
    <div v-if="$slots.default" class="empty-state__actions">
      <slot />
    </div>
  </div>
</template>

<style scoped>
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 180px;
  padding: 28px 18px;
  color: var(--leo-text-secondary);
  text-align: center;
}

.empty-state__mark {
  display: grid;
  width: 44px;
  height: 44px;
  margin-bottom: 12px;
  place-items: center;
  color: var(--leo-text-tertiary);
  font-size: 16px;
  font-weight: 700;
  background: var(--leo-bg-muted);
  border: 1px solid var(--leo-border);
  border-radius: 50%;
}

.empty-state__title {
  color: var(--leo-text-primary);
  font-size: 15px;
  font-weight: 700;
  line-height: 22px;
}

.empty-state__description {
  max-width: 360px;
  margin: 6px 0 0;
  color: var(--leo-text-secondary);
  font-size: 13px;
  line-height: 20px;
}

.empty-state__actions {
  display: flex;
  justify-content: center;
  margin-top: 16px;
}
</style>
