<template>
  <div class="callback-page">
    <a-spin />
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { wechatOAuthCallback } from '@/api/userController'
import { saveAuthTokens } from '@/utils/authToken'
import { useLoginUserStore } from '@/stores/loginUser'

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()

onMounted(async () => {
  const code = String(route.query.code || '')
  const state = String(route.query.state || '')
  if (!code || !state) {
    message.error('微信登录参数不完整')
    router.replace('/user/login')
    return
  }
  const res = await wechatOAuthCallback({ code, state })
  if (res.data.code === 0 && res.data.data) {
    saveAuthTokens(res.data.data.accessToken, res.data.data.refreshToken)
    if (res.data.data.loginUser) {
      loginUserStore.setLoginUser(res.data.data.loginUser)
    }
    message.success('登录成功')
    router.replace('/')
  } else {
    message.error(res.data.message ?? '微信登录失败')
    router.replace('/user/login')
  }
})
</script>

<style scoped>
.callback-page {
  min-height: calc(100vh - 56px);
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
