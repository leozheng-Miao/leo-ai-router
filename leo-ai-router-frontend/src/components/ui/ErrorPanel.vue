<script setup lang="ts">
import AppButton from './AppButton.vue'

defineProps<{
  message: string
}>()

const emit = defineEmits<{
  retry: []
}>()
</script>

<template>
  <div class="error-panel">
    <div class="error-panel__message">{{ message }}</div>
    <AppButton variant="secondary" size="sm" @click="emit('retry')">重试</AppButton>
  </div>
</template>

<style scoped>
.error-panel {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 14px;
  color: var(--leo-danger);
  background: rgba(240, 68, 56, 0.08);
  border: 1px solid rgba(240, 68, 56, 0.2);
  border-radius: var(--leo-radius-md);
}

.error-panel__message {
  min-width: 0;
  font-size: 13px;
  line-height: 20px;
}
</style>
