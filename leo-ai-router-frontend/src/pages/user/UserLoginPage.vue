<template>
  <div class="auth-shell">
    <div class="auth-card">
      <!-- Logo -->
      <div class="auth-logo">
        <img src="@/assets/logo.png" alt="logo" class="logo-img" />
        <span class="logo-text">AI Router</span>
      </div>

      <h2 class="auth-title">欢迎回来</h2>
      <p class="auth-sub">登录以继续使用 AI Router</p>

      <a-tabs v-model:activeKey="loginTab" class="auth-tabs" size="large" centered>
        <!-- 账号密码登录 -->
        <a-tab-pane key="password" tab="账号密码">
          <a-form :model="formState" autocomplete="off" layout="vertical" @finish="handleSubmit">
            <a-form-item name="userAccount" :rules="[{ required: true, message: '请输入账号' }]">
              <a-input
                v-model:value="formState.userAccount"
                placeholder="请输入账号"
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
                { min: 8, message: '密码至少 8 位' },
              ]"
            >
              <a-input-password
                v-model:value="formState.userPassword"
                placeholder="请输入密码"
                size="large"
                class="auth-input"
              >
                <template #prefix><LockOutlined class="input-icon" /></template>
              </a-input-password>
            </a-form-item>

            <div class="form-helper">
              <a class="link-text" @click="resetModalOpen = true">忘记密码？</a>
              <RouterLink to="/user/register" class="link-text">没有账号？去注册</RouterLink>
            </div>

            <a-button type="primary" html-type="submit" block size="large" class="auth-btn">
              登录
            </a-button>
          </a-form>
        </a-tab-pane>

        <!-- 邮箱验证码登录 -->
        <a-tab-pane key="email" tab="邮箱验证码">
          <a-form
            :model="emailLoginForm"
            autocomplete="off"
            layout="vertical"
            @finish="handleEmailLoginSubmit"
          >
            <a-form-item
              name="email"
              :rules="[
                { required: true, message: '请输入邮箱' },
                { type: 'email', message: '邮箱格式不正确' },
              ]"
            >
              <a-input
                v-model:value="emailLoginForm.email"
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
                  v-model:value="emailLoginForm.code"
                  placeholder="6 位验证码"
                  size="large"
                  class="auth-input"
                />
                <a-button
                  size="large"
                  class="code-btn"
                  :loading="sendLoginCodeLoading"
                  :disabled="emailLoginCountdown.isCounting"
                  @click.prevent="handleSendLoginCode"
                >
                  {{
                    emailLoginCountdown.isCounting
                      ? `${emailLoginCountdown.countdown}s`
                      : '发送验证码'
                  }}
                </a-button>
              </div>
            </a-form-item>

            <div class="form-helper">
              <RouterLink to="/user/register" class="link-text">没有账号？去注册</RouterLink>
            </div>

            <a-button type="primary" html-type="submit" block size="large" class="auth-btn">
              登录
            </a-button>
          </a-form>
        </a-tab-pane>
      </a-tabs>
    </div>

    <!-- 重置密码 Modal -->
    <a-modal
      v-model:open="resetModalOpen"
      title="重置密码"
      :footer="null"
      :width="440"
      destroy-on-close
    >
      <a-form
        :model="resetFormState"
        layout="vertical"
        autocomplete="off"
        @finish="handleResetSubmit"
      >
        <a-form-item
          name="email"
          label="邮箱"
          :rules="[
            { required: true, message: '请输入邮箱' },
            { type: 'email', message: '邮箱格式不正确' },
          ]"
        >
          <div class="code-row">
            <a-input
              v-model:value="resetFormState.email"
              placeholder="请输入绑定邮箱"
              size="large"
            />
            <a-button
              size="large"
              class="code-btn"
              :loading="sendResetCodeLoading"
              :disabled="resetCountdown.isCounting"
              @click.prevent="handleSendResetCode"
            >
              {{ resetCountdown.isCounting ? `${resetCountdown.countdown}s` : '发送验证码' }}
            </a-button>
          </div>
        </a-form-item>
        <a-form-item
          name="code"
          label="验证码"
          :rules="[{ required: true, message: '请输入验证码' }]"
        >
          <a-input v-model:value="resetFormState.code" placeholder="6 位验证码" size="large" />
        </a-form-item>
        <a-form-item
          name="newPassword"
          label="新密码"
          :rules="[
            { required: true, message: '请输入新密码' },
            { min: 8, message: '至少 8 位' },
          ]"
        >
          <a-input-password
            v-model:value="resetFormState.newPassword"
            placeholder="请输入新密码"
            size="large"
          />
        </a-form-item>
        <a-form-item
          name="checkPassword"
          label="确认密码"
          :rules="[
            { required: true, message: '请确认密码' },
            { validator: validateResetCheckPassword },
          ]"
        >
          <a-input-password
            v-model:value="resetFormState.checkPassword"
            placeholder="再次输入新密码"
            size="large"
          />
        </a-form-item>
        <a-button
          type="primary"
          html-type="submit"
          block
          size="large"
          class="auth-btn"
          style="margin-top: 8px"
        >
          确认重置
        </a-button>
      </a-form>
    </a-modal>
  </div>
</template>

<script lang="ts" setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { UserOutlined, LockOutlined, MailOutlined } from '@ant-design/icons-vue'
import { resetPassword, userLogin, userLoginByEmail } from '@/api/userController.ts'
import { useLoginUserStore } from '@/stores/loginUser.ts'
import { useEmailCodeCountdown } from '@/composables/useEmailCodeCountdown.ts'
import { requestSendEmailCode } from '@/utils/sendEmailVerificationCode.ts'

const loginTab = ref('password')
const router = useRouter()
const loginUserStore = useLoginUserStore()

const formState = reactive<API.UserLoginRequest>({ userAccount: '', userPassword: '' })
const emailLoginForm = reactive({ email: '', code: '' })
const resetModalOpen = ref(false)
const resetFormState = reactive({ email: '', code: '', newPassword: '', checkPassword: '' })

const emailLoginCountdown = useEmailCodeCountdown()
const resetCountdown = useEmailCodeCountdown()
const sendLoginCodeLoading = ref(false)
const sendResetCodeLoading = ref(false)

const validateResetCheckPassword = (_: unknown, value: string, callback: (e?: Error) => void) => {
  if (value && value !== resetFormState.newPassword) callback(new Error('两次密码不一致'))
  else callback()
}

const handleSendLoginCode = () => {
  void requestSendEmailCode(
    emailLoginForm.email,
    'login',
    emailLoginCountdown.start,
    sendLoginCodeLoading,
  )
}
const handleSendResetCode = () => {
  void requestSendEmailCode(
    resetFormState.email,
    'reset',
    resetCountdown.start,
    sendResetCodeLoading,
  )
}

const handleSubmit = async (values: API.UserLoginRequest) => {
  const res = await userLogin(values)
  if (res.data.code === 0 && res.data.data) {
    await loginUserStore.fetchLoginUser()
    message.success('登录成功')
    router.push({ path: '/', replace: true })
  } else {
    message.error('登录失败：' + res.data.message)
  }
}

const handleEmailLoginSubmit = async () => {
  try {
    const res = await userLoginByEmail({
      email: emailLoginForm.email.trim(),
      code: emailLoginForm.code,
    })
    if (res.data.code === 0 && res.data.data) {
      await loginUserStore.fetchLoginUser()
      message.success('登录成功')
      router.push({ path: '/', replace: true })
    } else {
      message.error('登录失败：' + (res.data.message ?? ''))
    }
  } catch {
    message.error('网络错误，请稍后重试')
  }
}

const handleResetSubmit = async () => {
  try {
    const res = await resetPassword({ ...resetFormState })
    if (res.data.code === 0) {
      message.success('密码重置成功，请重新登录')
      resetModalOpen.value = false
    } else {
      message.error(res.data.message ?? '重置失败')
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
  justify-content: space-between;
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
