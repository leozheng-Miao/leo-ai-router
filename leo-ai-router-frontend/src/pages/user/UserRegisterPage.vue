<template>
  <div class="auth-shell">
    <div class="auth-card">
      <div class="auth-logo">
        <img src="@/assets/logo.png" alt="logo" class="logo-img" />
        <span class="logo-text">AI Router</span>
      </div>

      <h2 class="auth-title">创建账号</h2>
      <p class="auth-sub">注册即可获得免费配额</p>

      <a-tabs v-model:activeKey="registerTab" class="auth-tabs" size="large" centered>
        <!-- 账号密码注册 -->
        <a-tab-pane key="password" tab="账号密码">
          <a-form :model="formState" layout="vertical" autocomplete="off" @finish="handleSubmit">
            <a-form-item
              name="userAccount"
              :rules="[
                { required: true, message: '请输入账号' },
                { min: 4, message: '至少 4 个字符' },
              ]"
            >
              <a-input
                v-model:value="formState.userAccount"
                placeholder="请输入账号（至少 4 位）"
                size="large"
                class="auth-input"
              >
                <template #prefix><UserOutlined class="input-icon" /></template>
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
                v-model:value="formState.userPassword"
                placeholder="请输入密码（至少 8 位）"
                size="large"
                class="auth-input"
              >
                <template #prefix><LockOutlined class="input-icon" /></template>
              </a-input-password>
            </a-form-item>
            <a-form-item
              name="checkPassword"
              :rules="[
                { required: true, message: '请确认密码' },
                { validator: validateCheckPassword },
              ]"
            >
              <a-input-password
                v-model:value="formState.checkPassword"
                placeholder="再次输入密码"
                size="large"
                class="auth-input"
              >
                <template #prefix><LockOutlined class="input-icon" /></template>
              </a-input-password>
            </a-form-item>

            <div class="form-helper">
              <RouterLink to="/user/login" class="link-text">已有账号？去登录</RouterLink>
            </div>

            <a-button type="primary" html-type="submit" block size="large" class="auth-btn">
              注册
            </a-button>
          </a-form>
        </a-tab-pane>

        <!-- 邮箱验证码注册 -->
        <a-tab-pane key="email" tab="邮箱验证码">
          <a-form
            :model="emailRegisterForm"
            layout="vertical"
            autocomplete="off"
            @finish="handleEmailRegisterSubmit"
          >
            <a-form-item
              name="email"
              :rules="[
                { required: true, message: '请输入邮箱' },
                { type: 'email', message: '邮箱格式不正确' },
              ]"
            >
              <a-input
                v-model:value="emailRegisterForm.email"
                placeholder="请输入邮箱"
                size="large"
                class="auth-input"
              >
                <template #prefix><MailOutlined class="input-icon" /></template>
              </a-input>
            </a-form-item>
            <a-form-item name="code" :rules="[{ required: true, message: '请输入验证码' }]">
              <div class="code-row">
                <a-input
                  v-model:value="emailRegisterForm.code"
                  placeholder="6 位验证码"
                  size="large"
                  class="auth-input"
                />
                <a-button
                  size="large"
                  class="code-btn"
                  :loading="sendRegisterCodeLoading"
                  :disabled="emailRegisterCountdown.isCounting"
                  @click.prevent="handleSendRegisterCode"
                >
                  {{
                    emailRegisterCountdown.isCounting
                      ? `${emailRegisterCountdown.countdown}s`
                      : '发送验证码'
                  }}
                </a-button>
              </div>
            </a-form-item>
            <a-form-item
              name="userPassword"
              :rules="[
                { required: true, message: '请输入密码' },
                { min: 8, message: '至少 8 位' },
              ]"
            >
              <a-input-password
                v-model:value="emailRegisterForm.userPassword"
                placeholder="请输入密码（至少 8 位）"
                size="large"
                class="auth-input"
              >
                <template #prefix><LockOutlined class="input-icon" /></template>
              </a-input-password>
            </a-form-item>
            <a-form-item
              name="checkPassword"
              :rules="[
                { required: true, message: '请确认密码' },
                { validator: validateEmailRegisterCheckPassword },
              ]"
            >
              <a-input-password
                v-model:value="emailRegisterForm.checkPassword"
                placeholder="再次输入密码"
                size="large"
                class="auth-input"
              >
                <template #prefix><LockOutlined class="input-icon" /></template>
              </a-input-password>
            </a-form-item>

            <div class="form-helper">
              <RouterLink to="/user/login" class="link-text">已有账号？去登录</RouterLink>
            </div>

            <a-button type="primary" html-type="submit" block size="large" class="auth-btn">
              注册
            </a-button>
          </a-form>
        </a-tab-pane>
      </a-tabs>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { UserOutlined, LockOutlined, MailOutlined } from '@ant-design/icons-vue'
import { userRegister, userRegisterByEmail } from '@/api/userController.ts'
import { useEmailCodeCountdown } from '@/composables/useEmailCodeCountdown.ts'
import { requestSendEmailCode } from '@/utils/sendEmailVerificationCode.ts'

const router = useRouter()
const registerTab = ref('password')

const formState = reactive<API.UserRegisterRequest>({
  userAccount: '',
  userPassword: '',
  checkPassword: '',
})
const emailRegisterForm = reactive({ email: '', code: '', userPassword: '', checkPassword: '' })

const emailRegisterCountdown = useEmailCodeCountdown()
const sendRegisterCodeLoading = ref(false)

const validateCheckPassword = (_: unknown, value: string, callback: (e?: Error) => void) => {
  if (value && value !== formState.userPassword) callback(new Error('两次密码不一致'))
  else callback()
}

const validateEmailRegisterCheckPassword = (
  _: unknown,
  value: string,
  callback: (e?: Error) => void,
) => {
  if (value && value !== emailRegisterForm.userPassword) callback(new Error('两次密码不一致'))
  else callback()
}

const handleSendRegisterCode = () => {
  void requestSendEmailCode(
    emailRegisterForm.email,
    'register',
    emailRegisterCountdown.start,
    sendRegisterCodeLoading,
  )
}

const handleSubmit = async (values: API.UserRegisterRequest) => {
  const res = await userRegister(values)
  if (res.data.code === 0) {
    message.success('注册成功')
    router.push({ path: '/user/login', replace: true })
  } else {
    message.error('注册失败：' + res.data.message)
  }
}

const handleEmailRegisterSubmit = async () => {
  try {
    const res = await userRegisterByEmail({
      email: emailRegisterForm.email.trim(),
      code: emailRegisterForm.code,
      userPassword: emailRegisterForm.userPassword,
      checkPassword: emailRegisterForm.checkPassword,
    })
    if (res.data.code === 0) {
      message.success('注册成功')
      router.push({ path: '/user/login', replace: true })
    } else {
      message.error('注册失败：' + (res.data.message ?? ''))
    }
  } catch {
    message.error('网络错误，请稍后重试')
  }
}
</script>

<style scoped>
.auth-shell {
  min-height: calc(100vh - 64px - 61px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  background: linear-gradient(160deg, #f0f9ff 0%, #faf5ff 50%, #f0fdf4 100%);
}

.auth-card {
  width: 100%;
  max-width: 420px;
  background: #fff;
  border-radius: 20px;
  padding: 40px 36px 36px;
  box-shadow: 0 8px 40px rgba(0, 0, 0, 0.08);
  border: 1px solid rgba(0, 0, 0, 0.06);
}

.auth-logo {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  margin-bottom: 24px;
}

.logo-img {
  width: 36px;
  height: 36px;
  border-radius: 8px;
}

.logo-text {
  font-size: 20px;
  font-weight: 700;
  background: linear-gradient(135deg, #3b82f6, #8b5cf6);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.auth-title {
  text-align: center;
  font-size: 22px;
  font-weight: 700;
  color: #111827;
  margin: 0 0 6px;
}

.auth-sub {
  text-align: center;
  font-size: 14px;
  color: #9ca3af;
  margin: 0 0 24px;
}

.auth-tabs :deep(.ant-tabs-nav) {
  margin-bottom: 24px;
}

.auth-tabs :deep(.ant-tabs-nav::before) {
  border-color: #f3f4f6;
}

.input-icon {
  color: #9ca3af;
}

.auth-input {
  border-radius: 10px !important;
}

.form-helper {
  display: flex;
  justify-content: flex-end;
  margin: -4px 0 20px;
}

.link-text {
  font-size: 13px;
  color: #6b7280;
  cursor: pointer;
  text-decoration: none;
  transition: color 0.2s;
}

.link-text:hover {
  color: #3b82f6;
}

.auth-btn {
  height: 46px;
  border-radius: 12px;
  font-size: 15px;
  font-weight: 600;
  background: linear-gradient(135deg, #3b82f6, #8b5cf6);
  border: none;
}

.auth-btn:hover {
  opacity: 0.88;
}

.code-row {
  display: flex;
  gap: 10px;
}

.code-row :deep(.ant-input) {
  flex: 1;
  min-width: 0;
}

.code-btn {
  flex-shrink: 0;
  width: 118px;
  border-radius: 10px;
  font-size: 13px;
  font-weight: 500;
}
</style>
