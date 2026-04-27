<template>
  <div class="result-shell">
    <a-card :bordered="false" class="result-card">
      <div class="result-icon cancel">!</div>
      <div class="result-title">充值已取消</div>
      <div class="result-desc">本次支付流程未完成，账户余额不会发生变化。你可以返回个人中心，或重新发起充值。</div>
      <a-space wrap size="middle" class="result-actions">
        <a-button size="large" @click="router.push('/profile')">返回个人中心</a-button>
        <a-button type="primary" size="large" @click="router.push('/profile?openRecharge=1')">重新充值</a-button>
      </a-space>
      <div class="tips-box">
        <div class="tips-title">温馨提示</div>
        <div class="tips-text">如遇支付问题，请检查沙箱账号状态、浏览器弹窗权限，或确认公网回调地址当前可访问。</div>
      </div>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { useRouter } from 'vue-router'

const router = useRouter()

onMounted(() => {
  message.warning('您已取消本次充值')
})
</script>

<style scoped>
.result-shell { min-height: calc(100vh - 180px); display: grid; place-items: center; padding: 40px 24px; }
.result-card { width: min(720px, 100%); border-radius: 28px; padding: 24px 10px; text-align: center; background: linear-gradient(180deg, #ffffff, #fff7ed); box-shadow: 0 24px 80px rgba(15, 23, 42, 0.08); }
.result-icon { width: 84px; height: 84px; margin: 0 auto 20px; border-radius: 50%; display: grid; place-items: center; font-size: 38px; font-weight: 700; }
.result-icon.cancel { background: #ffedd5; color: #ea580c; }
.result-title { font-size: 30px; font-weight: 700; color: #0f172a; }
.result-desc { margin: 12px auto 0; max-width: 520px; color: #64748b; line-height: 1.8; }
.result-actions { margin-top: 28px; justify-content: center; }
.tips-box { margin: 30px auto 0; max-width: 540px; padding: 18px 20px; border-radius: 18px; text-align: left; background: rgba(255,255,255,0.9); border: 1px solid #fed7aa; }
.tips-title { font-size: 15px; font-weight: 700; color: #9a3412; }
.tips-text { margin-top: 8px; color: #7c2d12; line-height: 1.8; }
</style>
