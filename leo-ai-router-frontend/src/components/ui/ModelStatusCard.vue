<script setup lang="ts">
withDefaults(
  defineProps<{
    name: string
    successRate: string
    latency: string
    status?: string
  }>(),
  {
    status: '正常',
  },
)
</script>

<template>
  <div class="model-status-card">
    <div class="model-status-card__header">
      <div class="model-status-card__name">{{ name }}</div>
      <span class="model-status-card__status">{{ status }}</span>
    </div>
    <div class="model-status-card__metrics">
      <div>
        <span class="model-status-card__label">成功率</span>
        <strong>{{ successRate }}</strong>
      </div>
      <div>
        <span class="model-status-card__label">延迟</span>
        <strong>{{ latency }}</strong>
      </div>
    </div>
  </div>
</template>

<style scoped>
.model-status-card {
  min-width: 0;
  padding: 14px;
  background: var(--leo-bg-panel);
  border: 1px solid var(--leo-border);
  border-radius: var(--leo-radius-md);
}

.model-status-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.model-status-card__name {
  min-width: 0;
  overflow: hidden;
  color: var(--leo-text-primary);
  font-size: 14px;
  font-weight: 700;
  line-height: 20px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.model-status-card__status {
  flex: 0 0 auto;
  padding: 3px 8px;
  color: var(--leo-success);
  font-size: 12px;
  font-weight: 600;
  line-height: 16px;
  background: rgba(18, 183, 106, 0.1);
  border-radius: 999px;
}

.model-status-card__metrics {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin-top: 14px;
}

.model-status-card__label {
  display: block;
  margin-bottom: 4px;
  color: var(--leo-text-tertiary);
  font-size: 12px;
  line-height: 16px;
}

.model-status-card__metrics strong {
  color: var(--leo-text-primary);
  font-size: 15px;
  line-height: 20px;
}
</style>
