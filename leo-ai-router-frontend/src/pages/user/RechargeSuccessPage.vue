<template>
  <div class="result-shell">
    <a-card :bordered="false" class="result-card">
      <div class="result-icon success">✓</div>
      <div class="result-title">充值成功</div>
      <div class="result-desc">款项已成功到账，可前往个人中心查看最新余额并立即开始使用。</div>
      <div class="result-meta">
        <span>支付方式：{{ methodText }}</span>
        <span v-if="outTradeNo">订单号：{{ outTradeNo }}</span>
      </div>
      <a-space wrap size="middle" class="result-actions">
        <a-button type="primary" size="large" @click="router.push('/profile')">查看余额</a-button>
        <a-button size="large" @click="router.push('/chat')">立即开始使用</a-button>
      </a-space>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

const outTradeNo = computed(() => String(route.query.outTradeNo ?? route.query.session_id ?? ''))
const methodText = computed(() => {
  const method = String(route.query.method ?? '').toLowerCase()
  return method.includes('stripe') ? 'Stripe' : '支付宝'
})
</script>

<style scoped>
.result-shell { min-height: calc(100vh - 180px); display: grid; place-items: center; padding: 40px 24px; }
.result-card { width: min(680px, 100%); border-radius: 28px; padding: 24px 10px; text-align: center; background: linear-gradient(180deg, #ffffff, #f0fdf4); box-shadow: 0 24px 80px rgba(15, 23, 42, 0.08); }
.result-icon { width: 84px; height: 84px; margin: 0 auto 20px; border-radius: 50%; display: grid; place-items: center; font-size: 38px; font-weight: 700; }
.result-icon.success { background: #dcfce7; color: #16a34a; }
.result-title { font-size: 30px; font-weight: 700; color: #0f172a; }
.result-desc { margin: 12px auto 0; max-width: 460px; color: #64748b; line-height: 1.8; }
.result-meta { margin-top: 18px; display: flex; gap: 12px; justify-content: center; flex-wrap: wrap; color: #475569; }
.result-actions { margin-top: 28px; justify-content: center; }
</style>
