import type { Ref } from 'vue'
import { message } from 'ant-design-vue'
import { sendEmailCode } from '@/api/userController.ts'

const EMAIL_REG = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

export function validateEmailFormat(email: string | undefined): boolean {
  const t = (email ?? '').trim()
  if (!EMAIL_REG.test(t)) {
    message.error('请输入正确的邮箱地址')
    return false
  }
  return true
}

export async function requestSendEmailCode(
  email: string | undefined,
  scene: 'login' | 'register' | 'reset',
  onSuccess: () => void,
  loadingRef?: Ref<boolean>,
): Promise<void> {
  if (!validateEmailFormat(email)) {
    return
  }
  if (loadingRef) {
    loadingRef.value = true
  }
  try {
    const res = await sendEmailCode({ email: (email ?? '').trim(), scene })
    if (res.data.code === 0) {
      message.success('验证码已发送')
      onSuccess()
    } else {
      message.error(res.data.message ?? '发送失败')
    }
  } catch {
    message.error('网络错误，请稍后重试')
  } finally {
    if (loadingRef) {
      loadingRef.value = false
    }
  }
}
