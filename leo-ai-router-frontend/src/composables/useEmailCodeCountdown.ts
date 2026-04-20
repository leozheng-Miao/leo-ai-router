import { computed, onUnmounted, reactive, ref } from 'vue'

const COUNTDOWN_SEC = 60

export function useEmailCodeCountdown() {
  const countdown = ref(0)
  let timer: ReturnType<typeof setInterval> | null = null

  const isCounting = computed(() => countdown.value > 0)
  const buttonText = computed(() =>
    countdown.value > 0 ? `${countdown.value}s 后重发` : '发送验证码',
  )

  const start = () => {
    if (timer) {
      clearInterval(timer)
      timer = null
    }
    countdown.value = COUNTDOWN_SEC
    timer = setInterval(() => {
      countdown.value -= 1
      if (countdown.value <= 0 && timer) {
        clearInterval(timer)
        timer = null
      }
    }, 1000)
  }

  onUnmounted(() => {
    if (timer) {
      clearInterval(timer)
      timer = null
    }
  })

  // 包一层 reactive，模板里用 countdown.xxx 访问时才会自动解包 ref/computed，
  // 避免 :disabled="obj.isCounting" 拿到的是 Ref 对象而永远为 true
  return reactive({
    countdown,
    isCounting,
    buttonText,
    start,
  })
}
