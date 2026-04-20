<template>
  <div class="auth-page">
    <div class="auth-container">
      <!-- Left panel -->
      <div class="auth-left">
        <RouterLink to="/" class="auth-logo">
          <svg width="32" height="32" viewBox="0 0 28 28" fill="none">
            <rect width="28" height="28" rx="7" fill="url(#loginLogoGrad)" />
            <path
              d="M8 9h12M8 14h8M8 19h12"
              stroke="#fff"
              stroke-width="2"
              stroke-linecap="round"
            />
            <defs>
              <linearGradient
                id="loginLogoGrad"
                x1="0"
                y1="0"
                x2="28"
                y2="28"
                gradientUnits="userSpaceOnUse"
              >
                <stop stop-color="#2563EB" />
                <stop offset="1" stop-color="#7C3AED" />
              </linearGradient>
            </defs>
          </svg>
          <span>LeoAI Router</span>
        </RouterLink>

        <div class="auth-left-content">
          <h2 class="auth-left-title">欢迎回来</h2>
          <p class="auth-left-sub">登录后即可使用智能路由、管理 API Key、查看用量统计</p>
          <div class="auth-features">
            <div v-for="f in leftFeatures" :key="f" class="auth-feature-item">
              <CheckCircleFilled class="feature-check" />
              <span>{{ f }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Right panel -->
      <div class="auth-right">
        <div class="auth-card">
          <h1 class="auth-title">登录账号</h1>
          <p class="auth-subtitle">
            还没有账号？
            <RouterLink to="/user/register" class="auth-link">免费注册</RouterLink>
          </p>

          <a-tabs v-model:activeKey="loginTab" class="auth-tabs">
            <!-- 账号密码 -->
            <a-tab-pane key="password" tab="账号密码">
              <a-form :model="pwdForm" layout="vertical" @finish="handlePwdLogin">
                <a-form-item
                  name="userAccount"
                  :rules="[{ required: true, message: '请输入账号' }]"
                >
                  <a-input
                    v-model:value="pwdForm.userAccount"
                    placeholder="账号"
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
                    { min: 8, message: '密码至少 8 位' },
                  ]"
                >
                  <a-input-password
                    v-model:value="pwdForm.userPassword"
                    placeholder="密码"
                    size="large"
                    class="auth-input"
                  >
                    <template #prefix><LockOutlined class="input-prefix-icon" /></template>
                  </a-input-password>
                </a-form-item>

                <div class="form-helper-row">
                  <a class="forgot-link" @click="resetModalVisible = true">忘记密码？</a>
                </div>

                <a-button
                  type="primary"
                  html-type="submit"
                  block
                  size="large"
                  class="submit-btn"
                  :loading="pwdLoading"
                >
                  登录
                </a-button>
              </a-form>
            </a-tab-pane>

            <!-- 邮箱验证码 -->
            <a-tab-pane key="email" tab="邮箱验证码">
              <a-form :model="emailForm" layout="vertical" @finish="handleEmailLogin">
                <a-form-item
                  name="email"
                  :rules="[
                    { required: true, message: '请输入邮箱' },
                    { type: 'email', message: '邮箱格式不正确' },
                  ]"
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

                <a-button
                  type="primary"
                  html-type="submit"
                  block
                  size="large"
                  class="submit-btn"
                  :loading="emailLoading"
                >
                  登录
                </a-button>
              </a-form>
            </a-tab-pane>
          </a-tabs>
        </div>
      </div>
    </div>

    <!-- 重置密码 Modal -->
    <a-modal
      v-model:open="resetModalVisible"
      title="重置密码"
      :footer="null"
      :width="420"
      destroy-on-close
    >
      <a-form :model="resetForm" layout="vertical" @finish="handleReset">
        <a-form-item
          name="email"
          label="绑定邮箱"
          :rules="[{ required: true }, { type: 'email', message: '邮箱格式不正确' }]"
        >
          <div class="code-row">
            <a-input v-model:value="resetForm.email" placeholder="请输入绑定邮箱" size="large" />
            <a-button
              size="large"
              class="code-btn"
              :loading="resetSendLoading"
              :disabled="resetCountdown.isCounting"
              @click.prevent="handleSendResetCode"
            >
              {{ resetCountdown.isCounting ? `${resetCountdown.countdown}s` : '发送验证码' }}
            </a-button>
          </div>
        </a-form-item>
        <a-form-item name="code" label="验证码" :rules="[{ required: true }]">
          <a-input v-model:value="resetForm.code" placeholder="6 位验证码" size="large" />
        </a-form-item>
        <a-form-item
          name="newPassword"
          label="新密码"
          :rules="[{ required: true }, { min: 8, message: '至少 8 位' }]"
        >
          <a-input-password
            v-model:value="resetForm.newPassword"
            placeholder="请输入新密码"
            size="large"
          />
        </a-form-item>
        <a-form-item
          name="checkPassword"
          label="确认密码"
          :rules="[{ required: true }, { validator: validateResetConfirm }]"
        >
          <a-input-password
            v-model:value="resetForm.checkPassword"
            placeholder="再次输入新密码"
            size="large"
          />
        </a-form-item>
        <a-button
          type="primary"
          html-type="submit"
          block
          size="large"
          class="submit-btn"
          style="margin-top: 8px"
        >
          确认重置
        </a-button>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { UserOutlined, LockOutlined, MailOutlined, CheckCircleFilled } from '@ant-design/icons-vue'
import { useLoginUserStore } from '@/stores/loginUser'
import { userLogin, userLoginByEmail, resetPassword } from '@/api/userController'
import { useEmailCodeCountdown } from '@/composables/useEmailCodeCountdown'
import { requestSendEmailCode } from '@/utils/sendEmailVerificationCode'

const router = useRouter()
const loginUserStore = useLoginUserStore()

const loginTab = ref('password')
const pwdLoading = ref(false)
const emailLoading = ref(false)
const sendCodeLoading = ref(false)
const resetSendLoading = ref(false)
const resetModalVisible = ref(false)

const pwdForm = reactive<API.UserLoginRequest>({ userAccount: '', userPassword: '' })
const emailForm = reactive({ email: '', code: '' })
const resetForm = reactive({ email: '', code: '', newPassword: '', checkPassword: '' })

const emailCountdown = useEmailCodeCountdown()
const resetCountdown = useEmailCodeCountdown()

const leftFeatures = [
  '一个 API Key 接入全部模型',
  '智能路由，自动选最优模型',
  '实时监控 Token 用量与费用',
  '企业级安全与高可用保障',
]

const validateResetConfirm = (_: unknown, value: string, callback: (e?: Error) => void) => {
  if (value && value !== resetForm.newPassword) callback(new Error('两次密码不一致'))
  else callback()
}

const handlePwdLogin = async (values: API.UserLoginRequest) => {
  pwdLoading.value = true
  try {
    const res = await userLogin(values)
    if (res.data.code === 0 && res.data.data) {
      await loginUserStore.fetchLoginUser()
      message.success('登录成功')
      router.push({ path: '/', replace: true })
    } else {
      message.error('登录失败：' + res.data.message)
    }
  } finally {
    pwdLoading.value = false
  }
}

const handleSendCode = () => {
  requestSendEmailCode(emailForm.email, 'login', emailCountdown.start, sendCodeLoading)
}

const handleEmailLogin = async () => {
  emailLoading.value = true
  try {
    const res = await userLoginByEmail({ email: emailForm.email.trim(), code: emailForm.code })
    if (res.data.code === 0 && res.data.data) {
      await loginUserStore.fetchLoginUser()
      message.success('登录成功')
      router.push({ path: '/', replace: true })
    } else {
      message.error('登录失败：' + (res.data.message ?? ''))
    }
  } catch {
    message.error('网络错误，请稍后重试')
  } finally {
    emailLoading.value = false
  }
}

const handleSendResetCode = () => {
  requestSendEmailCode(resetForm.email, 'reset', resetCountdown.start, resetSendLoading)
}

const handleReset = async () => {
  try {
    const res = await resetPassword({ ...resetForm })
    if (res.data.code === 0) {
      message.success('密码重置成功，请重新登录')
      resetModalVisible.value = false
    } else {
      message.error(res.data.message ?? '重置失败')
    }
  } catch {
    message.error('网络错误，请稍后重试')
  }
}
</script>

<style scoped>
.auth-page {
  min-height: calc(100vh - 56px);
  background: #f8faff;
  display: flex;
  align-items: stretch;
}

.auth-container {
  display: flex;
  width: 100%;
  min-height: calc(100vh - 56px);
}

/* Left */
.auth-left {
  flex: 1;
  background: linear-gradient(145deg, #1e3a8a 0%, #4c1d95 100%);
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
  font-size: 36px;
  font-weight: 800;
  color: #fff;
  margin: 0 0 14px;
  letter-spacing: -0.8px;
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

/* Right */
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

.auth-input :deep(.ant-input),
.auth-input :deep(.ant-input-password) {
  border-radius: 8px !important;
}

.input-prefix-icon {
  color: #d1d5db;
}

.form-helper-row {
  display: flex;
  justify-content: flex-end;
  margin: -8px 0 20px;
}

.forgot-link {
  font-size: 13px;
  color: #6b7280;
  cursor: pointer;
  transition: color 0.15s;
}

.forgot-link:hover {
  color: #2563eb;
}

.submit-btn {
  height: 44px;
  border-radius: 9px;
  font-size: 15px;
  font-weight: 600;
  background: linear-gradient(135deg, #2563eb, #7c3aed);
  border: none;
}

.submit-btn:hover {
  opacity: 0.9;
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

@media (max-width: 768px) {
  .auth-left {
    display: none;
  }
  .auth-right {
    width: 100%;
  }
}
</style>
