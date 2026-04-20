<template>
  <div class="auth-page">
    <div class="auth-container">
      <!-- Left -->
      <div class="auth-left">
        <RouterLink to="/" class="auth-logo">
          <svg width="32" height="32" viewBox="0 0 28 28" fill="none">
            <rect width="28" height="28" rx="7" fill="url(#regLogoGrad)" />
            <path
              d="M8 9h12M8 14h8M8 19h12"
              stroke="#fff"
              stroke-width="2"
              stroke-linecap="round"
            />
            <defs>
              <linearGradient
                id="regLogoGrad"
                x1="0"
                y1="0"
                x2="28"
                y2="28"
                gradientUnits="userSpaceOnUse"
              >
                <stop stop-color="#059669" />
                <stop offset="1" stop-color="#2563EB" />
              </linearGradient>
            </defs>
          </svg>
          <span>LeoAI Router</span>
        </RouterLink>

        <div class="auth-left-content">
          <h2 class="auth-left-title">创建账号<br />立即使用 AI 路由</h2>
          <p class="auth-left-sub">注册即可获得免费调用配额，无需信用卡，立即开始接入</p>
          <div class="auth-features">
            <div v-for="f in leftFeatures" :key="f" class="auth-feature-item">
              <CheckCircleFilled class="feature-check" />
              <span>{{ f }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Right -->
      <div class="auth-right">
        <div class="auth-card">
          <h1 class="auth-title">免费注册</h1>
          <p class="auth-subtitle">
            已有账号？
            <RouterLink to="/user/login" class="auth-link">立即登录</RouterLink>
          </p>

          <a-tabs v-model:activeKey="registerTab" class="auth-tabs">
            <!-- 账号密码注册 -->
            <a-tab-pane key="password" tab="账号密码">
              <a-form :model="pwdForm" layout="vertical" @finish="handlePwdRegister">
                <a-form-item
                  name="userAccount"
                  :rules="[
                    { required: true, message: '请输入账号' },
                    { min: 4, message: '至少 4 个字符' },
                  ]"
                >
                  <a-input
                    v-model:value="pwdForm.userAccount"
                    placeholder="账号（至少 4 位）"
                    size="large"
                    class="auth-input"
                  >
                    <template #prefix><UserOutlined class="input-prefix-icon" /></template>
                  </a-input>
                </a-form-item>
                <a-form-item
                  name="userPassword"
                  :rules="[
                    { required: true, message: '请输入密码' },
                    { min: 8, message: '至少 8 位' },
                  ]"
                >
                  <a-input-password
                    v-model:value="pwdForm.userPassword"
                    placeholder="密码（至少 8 位）"
                    size="large"
                    class="auth-input"
                  >
                    <template #prefix><LockOutlined class="input-prefix-icon" /></template>
                  </a-input-password>
                </a-form-item>
                <a-form-item
                  name="checkPassword"
                  :rules="[
                    { required: true, message: '请确认密码' },
                    { validator: validatePwdConfirm },
                  ]"
                >
                  <a-input-password
                    v-model:value="pwdForm.checkPassword"
                    placeholder="确认密码"
                    size="large"
                    class="auth-input"
                  >
                    <template #prefix><LockOutlined class="input-prefix-icon" /></template>
                  </a-input-password>
                </a-form-item>

                <a-button
                  type="primary"
                  html-type="submit"
                  block
                  size="large"
                  class="submit-btn"
                  :loading="pwdLoading"
                >
                  注册账号
                </a-button>
              </a-form>
            </a-tab-pane>

            <!-- 邮箱验证码注册 -->
            <a-tab-pane key="email" tab="邮箱验证码">
              <a-form :model="emailForm" layout="vertical" @finish="handleEmailRegister">
                <a-form-item
                  name="email"
                  :rules="[{ required: true }, { type: 'email', message: '邮箱格式不正确' }]"
                >
                  <a-input
                    v-model:value="emailForm.email"
                    placeholder="邮箱地址"
                    size="large"
                    class="auth-input"
                  >
                    <template #prefix><MailOutlined class="input-prefix-icon" /></template>
                  </a-input>
                </a-form-item>
                <a-form-item name="code" :rules="[{ required: true, message: '请输入验证码' }]">
                  <div class="code-row">
                    <a-input
                      v-model:value="emailForm.code"
                      placeholder="6 位验证码"
                      size="large"
                      class="auth-input"
                    />
                    <a-button
                      size="large"
                      class="code-btn"
                      :loading="sendCodeLoading"
                      :disabled="emailCountdown.isCounting"
                      @click.prevent="handleSendCode"
                    >
                      {{
                        emailCountdown.isCounting ? `${emailCountdown.countdown}s` : '发送验证码'
                      }}
                    </a-button>
                  </div>
                </a-form-item>
                <a-form-item
                  name="userPassword"
                  :rules="[{ required: true }, { min: 8, message: '至少 8 位' }]"
                >
                  <a-input-password
                    v-model:value="emailForm.userPassword"
                    placeholder="密码（至少 8 位）"
                    size="large"
                    class="auth-input"
                  >
                    <template #prefix><LockOutlined class="input-prefix-icon" /></template>
                  </a-input-password>
                </a-form-item>
                <a-form-item
                  name="checkPassword"
                  :rules="[{ required: true }, { validator: validateEmailConfirm }]"
                >
                  <a-input-password
                    v-model:value="emailForm.checkPassword"
                    placeholder="确认密码"
                    size="large"
                    class="auth-input"
                  >
                    <template #prefix><LockOutlined class="input-prefix-icon" /></template>
                  </a-input-password>
                </a-form-item>

                <a-button
                  type="primary"
                  html-type="submit"
                  block
                  size="large"
                  class="submit-btn"
                  :loading="emailLoading"
                >
                  注册账号
                </a-button>
              </a-form>
            </a-tab-pane>
          </a-tabs>

          <p class="agreement-text">
            注册即表示同意
            <a href="#" class="auth-link">服务条款</a>
            与
            <a href="#" class="auth-link">隐私政策</a>
          </p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { UserOutlined, LockOutlined, MailOutlined, CheckCircleFilled } from '@ant-design/icons-vue'
import { userRegister, userRegisterByEmail } from '@/api/userController'
import { useEmailCodeCountdown } from '@/composables/useEmailCodeCountdown'
import { requestSendEmailCode } from '@/utils/sendEmailVerificationCode'

const router = useRouter()

const registerTab = ref('password')
const pwdLoading = ref(false)
const emailLoading = ref(false)
const sendCodeLoading = ref(false)

const pwdForm = reactive<API.UserRegisterRequest>({
  userAccount: '',
  userPassword: '',
  checkPassword: '',
})
const emailForm = reactive({ email: '', code: '', userPassword: '', checkPassword: '' })
const emailCountdown = useEmailCodeCountdown()

const leftFeatures = [
  '免费获得调用配额，立即体验',
  '兼容 OpenAI SDK，零成本接入',
  '实时查看调用日志与 Token 统计',
  '多模型智能路由，成本最优',
]

const validatePwdConfirm = (_: unknown, value: string, callback: (e?: Error) => void) => {
  if (value && value !== pwdForm.userPassword) callback(new Error('两次密码不一致'))
  else callback()
}

const validateEmailConfirm = (_: unknown, value: string, callback: (e?: Error) => void) => {
  if (value && value !== emailForm.userPassword) callback(new Error('两次密码不一致'))
  else callback()
}

const handlePwdRegister = async (values: API.UserRegisterRequest) => {
  pwdLoading.value = true
  try {
    const res = await userRegister(values)
    if (res.data.code === 0) {
      message.success('注册成功，请登录')
      router.push({ path: '/user/login', replace: true })
    } else {
      message.error('注册失败：' + res.data.message)
    }
  } finally {
    pwdLoading.value = false
  }
}

const handleSendCode = () => {
  requestSendEmailCode(emailForm.email, 'register', emailCountdown.start, sendCodeLoading)
}

const handleEmailRegister = async () => {
  emailLoading.value = true
  try {
    const res = await userRegisterByEmail({
      email: emailForm.email.trim(),
      code: emailForm.code,
      userPassword: emailForm.userPassword,
      checkPassword: emailForm.checkPassword,
    })
    if (res.data.code === 0) {
      message.success('注册成功，请登录')
      router.push({ path: '/user/login', replace: true })
    } else {
      message.error('注册失败：' + (res.data.message ?? ''))
    }
  } catch {
    message.error('网络错误，请稍后重试')
  } finally {
    emailLoading.value = false
  }
}
</script>

<style scoped>
.auth-page {
  min-height: calc(100vh - 56px);
  display: flex;
  align-items: stretch;
}

.auth-container {
  display: flex;
  width: 100%;
  min-height: calc(100vh - 56px);
}

.auth-left {
  flex: 1;
  background: linear-gradient(145deg, #064e3b 0%, #1e3a8a 100%);
  padding: 48px;
  display: flex;
  flex-direction: column;
}

.auth-logo {
  display: flex;
  align-items: center;
  gap: 10px;
  text-decoration: none;
  margin-bottom: auto;
}

.auth-logo span {
  font-size: 16px;
  font-weight: 700;
  color: #fff;
}

.auth-left-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 48px 0;
}

.auth-left-title {
  font-size: 34px;
  font-weight: 800;
  color: #fff;
  margin: 0 0 14px;
  letter-spacing: -0.8px;
  line-height: 1.2;
}

.auth-left-sub {
  font-size: 15px;
  color: rgba(255, 255, 255, 0.72);
  line-height: 1.7;
  margin: 0 0 36px;
  max-width: 340px;
}

.auth-features {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.auth-feature-item {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.88);
}

.feature-check {
  color: #34d399;
  font-size: 15px;
}

.auth-right {
  width: 480px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px 32px;
  background: #fff;
}

.auth-card {
  width: 100%;
  max-width: 380px;
}

.auth-title {
  font-size: 26px;
  font-weight: 800;
  color: #111827;
  margin: 0 0 8px;
  letter-spacing: -0.5px;
}

.auth-subtitle {
  font-size: 14px;
  color: #9ca3af;
  margin: 0 0 28px;
}

.auth-link {
  color: #2563eb;
  text-decoration: none;
  font-weight: 600;
}
.auth-link:hover {
  color: #1d4ed8;
}

.auth-tabs :deep(.ant-tabs-nav) {
  margin-bottom: 24px;
}
.auth-tabs :deep(.ant-tabs-nav::before) {
  border-color: #e5e7eb;
}

.input-prefix-icon {
  color: #d1d5db;
}

.submit-btn {
  height: 44px;
  border-radius: 9px;
  font-size: 15px;
  font-weight: 600;
  background: linear-gradient(135deg, #059669, #2563eb);
  border: none;
  margin-top: 4px;
}

.code-row {
  display: flex;
  gap: 10px;
}
.code-row :deep(.ant-input) {
  flex: 1;
}

.code-btn {
  flex-shrink: 0;
  width: 110px;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 500;
}

.agreement-text {
  font-size: 12px;
  color: #9ca3af;
  text-align: center;
  margin: 20px 0 0;
}

@media (max-width: 768px) {
  .auth-left {
    display: none;
  }
  .auth-right {
    width: 100%;
  }
}
</style>
