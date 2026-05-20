<script setup lang="ts">
withDefaults(
  defineProps<{
    label: string
    value: string | number
    trend?: string
    trendTone?: 'success' | 'danger' | 'muted'
  }>(),
  {
    trend: '',
    trendTone: 'muted',
  },
)
</script>

<template>
  <div class="metric-card">
    <div class="metric-card__label">{{ label }}</div>
    <div class="metric-card__value">{{ value }}</div>
    <div v-if="trend" class="metric-card__trend" :class="`metric-card__trend--${trendTone}`">
      {{ trend }}
    </div>
  </div>
</template>

<style scoped>
.metric-card {
  min-width: 0;
  padding: 14px;
  background: var(--leo-bg-panel);
  border: 1px solid var(--leo-border);
  border-radius: var(--leo-radius-md);
}

.metric-card__label {
  color: var(--leo-text-secondary);
  font-size: 13px;
  line-height: 18px;
}

.metric-card__value {
  margin-top: 8px;
  color: var(--leo-text-primary);
  font-size: 24px;
  font-weight: 700;
  line-height: 30px;
}

.metric-card__trend {
  margin-top: 8px;
  font-size: 12px;
  font-weight: 600;
  line-height: 16px;
}

.metric-card__trend--success {
  color: var(--leo-success);
}

.metric-card__trend--danger {
  color: var(--leo-danger);
}

.metric-card__trend--muted {
  color: var(--leo-text-tertiary);
}
</style>
