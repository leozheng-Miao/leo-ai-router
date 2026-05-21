<script setup lang="ts">
const props = withDefaults(
  defineProps<{
    variant?: 'primary' | 'secondary' | 'ghost' | 'danger'
    size?: 'sm' | 'md' | 'lg'
    disabled?: boolean
    type?: 'button' | 'submit' | 'reset'
  }>(),
  {
    variant: 'primary',
    size: 'md',
    disabled: false,
    type: 'button',
  },
)

const emit = defineEmits<{
  click: [event: MouseEvent]
}>()

const handleClick = (event: MouseEvent) => {
  if (props.disabled) {
    event.preventDefault()
    return
  }

  emit('click', event)
}
</script>

<template>
  <button
    class="app-button"
    :class="[`app-button--${variant}`, `app-button--${size}`]"
    :disabled="disabled"
    :type="type"
    @click="handleClick"
  >
    <slot />
  </button>
</template>

<style scoped>
.app-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  border: 1px solid transparent;
  border-radius: var(--leo-radius-sm);
  font-weight: 600;
  line-height: 1;
  cursor: pointer;
  transition:
    background-color 0.16s ease,
    border-color 0.16s ease,
    color 0.16s ease,
    box-shadow 0.16s ease;
}

.app-button:focus-visible {
  outline: 2px solid var(--leo-primary-soft);
  outline-offset: 2px;
}

.app-button:disabled {
  cursor: not-allowed;
  opacity: 0.56;
}

.app-button--sm {
  min-height: 30px;
  padding: 0 12px;
  font-size: 13px;
}

.app-button--md {
  min-height: 36px;
  padding: 0 16px;
  font-size: 14px;
}

.app-button--lg {
  min-height: 42px;
  padding: 0 20px;
  font-size: 15px;
}

.app-button--primary {
  color: #ffffff;
  background: var(--leo-primary);
  border-color: var(--leo-primary);
}

.app-button--primary:not(:disabled):hover {
  background: var(--leo-primary-hover);
  border-color: var(--leo-primary-hover);
}

.app-button--secondary {
  color: var(--leo-text-primary);
  background: var(--leo-bg-panel);
  border-color: var(--leo-border-strong);
}

.app-button--secondary:not(:disabled):hover {
  color: var(--leo-primary);
  border-color: var(--leo-primary);
}

.app-button--ghost {
  color: var(--leo-text-secondary);
  background: transparent;
  border-color: transparent;
}

.app-button--ghost:not(:disabled):hover {
  color: var(--leo-primary);
  background: var(--leo-primary-soft);
}

.app-button--danger {
  color: #ffffff;
  background: var(--leo-danger);
  border-color: var(--leo-danger);
}

.app-button--danger:not(:disabled):hover {
  box-shadow: 0 6px 16px rgba(240, 68, 56, 0.22);
}
</style>
