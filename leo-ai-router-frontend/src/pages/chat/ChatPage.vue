<template>
  <div class="chat-page">
    <!-- 左侧配置栏 -->
    <div class="chat-sidebar">
      <div class="sidebar-section">
        <div class="sidebar-label">选择模型</div>
        <a-select
          v-model:value="selectedModel"
          placeholder="请选择模型"
          class="sidebar-select"
          :loading="modelsLoading"
        >
          <a-select-option v-for="m in models" :key="m.value" :value="m.value">
            <div class="model-option">
              <span class="model-opt-name">{{ m.label }}</span>
              <a-tag :color="m.tagColor" class="model-opt-tag">{{ m.provider }}</a-tag>
            </div>
          </a-select-option>
        </a-select>
      </div>

      <div class="sidebar-section">
        <div class="sidebar-label">选择 API Key</div>
        <a-select
          v-model:value="selectedApiKey"
          placeholder="网页端聊天无需选择，可选"
          class="sidebar-select"
          :loading="keysLoading"
          allow-clear
        >
          <a-select-option v-for="k in apiKeys" :key="k.id" :value="k.id">
            <div class="key-option">
              <KeyOutlined class="key-opt-icon" />
              <span class="key-opt-name">{{ k.keyName || '未命名' }}</span>
            </div>
          </a-select-option>
        </a-select>
      </div>

      <a-divider style="margin: 8px 0" />

      <!-- Token 统计 -->
      <div class="sidebar-section">
        <div class="sidebar-label">本次会话统计</div>
        <div class="stats-grid">
          <div class="stat-cell">
            <div class="stat-val">{{ sessionStats.totalMessages }}</div>
            <div class="stat-key">消息数</div>
          </div>
          <div class="stat-cell">
            <div class="stat-val">{{ formatNum(sessionStats.totalTokens) }}</div>
            <div class="stat-key">Token</div>
          </div>
        </div>
      </div>

      <a-divider style="margin: 8px 0" />

      <!-- 清空按钮 -->
      <a-button
        block
        danger
        ghost
        :disabled="messages.length === 0"
        class="clear-btn"
        @click="clearMessages"
      >
        <DeleteOutlined /> 清空对话
      </a-button>
    </div>

    <!-- 右侧对话区 -->
    <div class="chat-main">
      <!-- 消息列表 -->
      <div ref="messageListRef" class="message-list">
        <!-- 空状态 -->
        <div v-if="messages.length === 0" class="empty-state">
          <div class="empty-icon">
            <MessageOutlined />
          </div>
          <div class="empty-title">开始一段对话</div>
          <div class="empty-desc">选择模型后即可开始流式对话，API Key 仅供查看</div>
          <div class="quick-tips">
            <div v-for="tip in quickTips" :key="tip" class="quick-tip" @click="fillTip(tip)">
              {{ tip }}
            </div>
          </div>
        </div>

        <!-- 消息列表 -->
        <template v-else>
          <div
            v-for="(msg, idx) in messages"
            :key="idx"
            class="message-row"
            :class="msg.role === 'user' ? 'message-row--user' : 'message-row--assistant'"
          >
            <!-- Avatar -->
            <div class="msg-avatar" :class="msg.role === 'user' ? 'avatar-user' : 'avatar-ai'">
              <UserOutlined v-if="msg.role === 'user'" />
              <RobotOutlined v-else />
            </div>

            <!-- 气泡 -->
            <div class="msg-bubble" :class="msg.role === 'user' ? 'bubble-user' : 'bubble-ai'">
              <!-- AI 消息的推理内容 -->
              <div v-if="msg.reasoning && msg.role === 'assistant'" class="reasoning-block">
                <div class="reasoning-title"><BulbOutlined /> 思考过程</div>
                <div class="reasoning-content">{{ msg.reasoning }}</div>
              </div>

              <!-- 消息内容 -->
              <div class="msg-content" v-html="renderContent(msg.content)"></div>

              <!-- 流式光标 -->
              <span v-if="msg.streaming" class="streaming-cursor"></span>

              <!-- 底部信息 -->
              <div class="msg-meta">
                <span class="msg-time">{{ msg.time }}</span>
                <span v-if="msg.tokens && msg.tokens > 0" class="msg-tokens">
                  {{ msg.tokens }} tokens
                </span>
                <a-tooltip v-if="msg.role === 'assistant' && !msg.streaming" title="复制内容">
                  <span class="msg-copy" @click="copyText(msg.content)">
                    <CopyOutlined />
                  </span>
                </a-tooltip>
              </div>
            </div>
          </div>

          <!-- 流式加载中占位 -->
          <div v-if="isStreaming && !streamingContent" class="message-row message-row--assistant">
            <div class="msg-avatar avatar-ai"><RobotOutlined /></div>
            <div class="msg-bubble bubble-ai">
              <div class="thinking-dots"><span></span><span></span><span></span></div>
            </div>
          </div>
        </template>
      </div>

      <!-- 输入区 -->
      <div class="input-area">
        <div class="input-wrap">
          <a-textarea
            v-model:value="inputText"
            placeholder="输入你的问题… (Ctrl+Enter 发送)"
            :auto-size="{ minRows: 2, maxRows: 6 }"
            :disabled="isStreaming"
            class="msg-input"
            @keydown="handleKeydown"
          />
          <div class="input-actions">
            <span class="char-count">{{ inputText.length }} 字</span>
            <a-button
              type="primary"
              class="send-btn"
              :loading="isStreaming"
              :disabled="!inputText.trim() || !selectedModel"
              @click="sendMessage"
            >
              <SendOutlined v-if="!isStreaming" />
              {{ isStreaming ? '生成中…' : '发送' }}
            </a-button>
          </div>
        </div>
        <div class="input-hint">
          <span v-if="!selectedModel" class="hint-warn"
            ><ExclamationCircleOutlined /> 请先选择模型</span
          >
          <span v-else-if="!selectedApiKey" class="hint-ok"
            ><CheckCircleOutlined /> 当前使用网页端登录态聊天，API Key 可不选</span
          >
          <span v-else class="hint-ok"><CheckCircleOutlined /> 准备就绪，可以发送</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, nextTick, onMounted, onBeforeUnmount, watch } from 'vue'
import { message as antMessage } from 'ant-design-vue'
import { marked } from 'marked'
import {
  KeyOutlined,
  DeleteOutlined,
  MessageOutlined,
  UserOutlined,
  RobotOutlined,
  BulbOutlined,
  CopyOutlined,
  SendOutlined,
  ExclamationCircleOutlined,
  CheckCircleOutlined,
} from '@ant-design/icons-vue'
import { listMyApiKeys } from '@/api/apiKeyController'
import { listActiveModels } from '@/api/modelController'

const API_BASE_URL = 'http://localhost:8123/api'
const CHAT_STORAGE_KEY = 'leo-ai-router-chat-session'
const SEND_DEBOUNCE_MS = 350

// ───── 类型 ─────
interface ChatMessage {
  role: 'user' | 'assistant'
  content: string
  reasoning?: string
  time: string
  tokens?: number
  streaming?: boolean
}

interface ModelOption {
  label: string
  value: string
  provider: string
  tagColor: string
}

interface StreamChunkChoice {
  delta?: {
    role?: string
    content?: string
    reasoningContent?: string
  }
  finishReason?: string | null
}

interface StreamChunkPayload {
  choices?: StreamChunkChoice[]
}

interface BusinessResponse<T = unknown> {
  code?: number
  data?: T
  message?: string
}

interface StoredChatSession {
  selectedModel?: string
  selectedApiKey?: number
  inputText: string
  messages: ChatMessage[]
}

const providerColorMap: Record<string, string> = {
  qwen: 'orange',
  dashscope: 'orange',
  tongyi: 'orange',
  aliyun: 'orange',
  zhipu: 'green',
  zhipuai: 'green',
  glm: 'green',
  deepseek: 'blue',
  openai: 'purple',
  gpt: 'purple',
}

// ───── 状态 ─────
const selectedModel = ref<string | undefined>(undefined)
const selectedApiKey = ref<number | undefined>(undefined)
const inputText = ref('')
const messages = ref<ChatMessage[]>([])
const isStreaming = ref(false)
const streamingContent = ref('')
const streamingReasoning = ref('')
const messageListRef = ref<HTMLDivElement | null>(null)
const sendDebounceTimer = ref<number | null>(null)

const modelsLoading = ref(false)
const keysLoading = ref(false)

const models = ref<ModelOption[]>([])
const apiKeys = ref<API.ApiKeyVO[]>([])

const sessionStats = reactive({
  totalMessages: 0,
  totalTokens: 0,
})

// ───── 快捷提示 ─────
const quickTips = [
  '用 Python 写一个冒泡排序',
  '解释一下什么是 RAG',
  '帮我写一份技术方案大纲',
  '分析一下 DeepSeek 与 GPT 的区别',
]

const fillTip = (tip: string) => {
  inputText.value = tip
}

// ───── 工具 ─────
const formatNum = (n: number) => (n >= 1000 ? (n / 1000).toFixed(1) + 'k' : String(n))

const now = () => new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })

marked.setOptions({
  gfm: true,
  breaks: true,
})

const sanitizeHtml = (html: string) => {
  return html
    .replace(/<script[\s\S]*?>[\s\S]*?<\/script>/gi, '')
    .replace(/\son\w+="[^"]*"/gi, '')
    .replace(/\son\w+='[^']*'/gi, '')
    .replace(/javascript:/gi, '')
}

const renderContent = (text: string) => {
  return sanitizeHtml(marked.parse(text) as string)
}

const copyText = (text: string) => {
  navigator.clipboard.writeText(text).then(() => {
    antMessage.success('已复制')
  })
}

const isBusinessResponse = (value: unknown): value is BusinessResponse => {
  return typeof value === 'object' && value !== null && 'code' in value
}

const redirectToLogin = () => {
  const redirect = encodeURIComponent(window.location.href)
  window.location.href = `/user/login?redirect=${redirect}`
}

const resolveErrorMessage = (payload: unknown, fallback = '请求失败') => {
  if (isBusinessResponse(payload)) {
    if (payload.code === 40100) {
      antMessage.warning(payload.message || '请先登录')
      redirectToLogin()
      return payload.message || '请先登录'
    }
    return payload.message || fallback
  }
  if (payload instanceof Error) {
    return payload.message
  }
  return fallback
}

const parseChunkPayload = (rawLine: string): StreamChunkPayload | BusinessResponse | null => {
  const line = rawLine.trim()
  if (!line) {
    return null
  }

  const jsonText = line.startsWith('data:') ? line.slice(5).trim() : line
  if (!jsonText || jsonText === '[DONE]') {
    return null
  }

  try {
    return JSON.parse(jsonText) as StreamChunkPayload | BusinessResponse
  } catch {
    return null
  }
}

const estimateTokenCount = (text: string) => {
  const normalized = text.trim()
  if (!normalized) {
    return 0
  }

  const asciiChars = (normalized.match(/[\x00-\x7F]/g) || []).length
  const nonAsciiChars = normalized.length - asciiChars
  const wordCount = normalized.split(/\s+/).filter(Boolean).length
  return Math.max(1, Math.ceil(asciiChars / 4) + nonAsciiChars + Math.ceil(wordCount * 0.25))
}

const recalculateSessionStats = () => {
  sessionStats.totalMessages = messages.value.length
  sessionStats.totalTokens = messages.value.reduce((sum, message) => {
    return sum + (message.tokens ?? estimateTokenCount(`${message.reasoning ?? ''}\n${message.content}`))
  }, 0)
}

const saveChatSession = () => {
  if (typeof window === 'undefined') {
    return
  }

  const session: StoredChatSession = {
    selectedModel: selectedModel.value,
    selectedApiKey: selectedApiKey.value,
    inputText: inputText.value,
    messages: messages.value,
  }
  localStorage.setItem(CHAT_STORAGE_KEY, JSON.stringify(session))
}

const restoreChatSession = () => {
  if (typeof window === 'undefined') {
    return
  }

  const raw = localStorage.getItem(CHAT_STORAGE_KEY)
  if (!raw) {
    recalculateSessionStats()
    return
  }

  try {
    const session = JSON.parse(raw) as StoredChatSession
    selectedModel.value = session.selectedModel
    selectedApiKey.value = session.selectedApiKey
    inputText.value = session.inputText ?? ''
    messages.value = Array.isArray(session.messages)
      ? session.messages.map((msg) => ({
          ...msg,
          streaming: false,
          tokens: msg.tokens ?? estimateTokenCount(`${msg.reasoning ?? ''}\n${msg.content}`),
        }))
      : []
  } catch {
    localStorage.removeItem(CHAT_STORAGE_KEY)
    messages.value = []
  }

  recalculateSessionStats()
}

const updateAssistantMessage = (index: number, patch: Partial<ChatMessage> = {}) => {
  const currentMessage = messages.value[index]
  if (!currentMessage) {
    return
  }

  messages.value[index] = {
    role: currentMessage.role,
    content: patch.content ?? currentMessage.content,
    reasoning: patch.reasoning ?? currentMessage.reasoning,
    time: currentMessage.time,
    tokens:
      patch.tokens ??
      estimateTokenCount(
        `${patch.reasoning ?? currentMessage.reasoning ?? ''}\n${patch.content ?? currentMessage.content}`,
      ),
    streaming: patch.streaming ?? currentMessage.streaming,
  }
  recalculateSessionStats()
}

const scrollToBottom = async () => {
  await nextTick()
  if (messageListRef.value) {
    messageListRef.value.scrollTop = messageListRef.value.scrollHeight
  }
}

const clearMessages = () => {
  messages.value = []
  recalculateSessionStats()
}

// ───── 加载数据 ─────
// const loadModels = async () => {
//   // 使用固定模型列表（与后端 db.sql 一致）
//   models.value = [
//     { label: 'Qwen Plus', value: 'qwen-plus', provider: '通义千问', tagColor: 'orange' },
//     { label: 'Qwen Max', value: 'qwen-max', provider: '通义千问', tagColor: 'orange' },
//     { label: 'Qwen Turbo', value: 'qwen-turbo', provider: '通义千问', tagColor: 'orange' },
//     { label: 'GLM-4.7', value: 'glm-4.7', provider: '智谱AI', tagColor: 'green' },
//     { label: 'GLM-4.7 Flash', value: 'glm-4.7-flash', provider: '智谱AI', tagColor: 'green' },
//     { label: 'DeepSeek Chat', value: 'deepseek-chat', provider: 'DeepSeek', tagColor: 'blue' },
//     {
//       label: 'DeepSeek Reasoner',
//       value: 'deepseek-reasoner',
//       provider: 'DeepSeek',
//       tagColor: 'blue',
//     },
//   ]
// }

const loadModels = async () => {
  modelsLoading.value = true
  try {
    const res = await listActiveModels()
    if (res.data.code === 0 && res.data.data) {
      models.value = res.data.data
        .filter((m) => m.modelType === 'chat')
        .map((m) => ({
          label: m.modelName ?? m.modelKey ?? '',
          value: m.modelKey ?? '',
          provider: m.providerDisplayName ?? m.providerName ?? '',
          tagColor: providerColorMap[m.providerName?.toLowerCase() ?? ''] ?? 'default',
        }))
    }
  } catch {
    antMessage.error('加载模型列表失败')
  } finally {
    modelsLoading.value = false
  }
}

const loadApiKeys = async () => {
  keysLoading.value = true
  try {
    const res = await listMyApiKeys({ pageNum: 1, pageSize: 100 })
    if (res.data.code === 0 && res.data.data) {
      apiKeys.value = (res.data.data.records ?? []).filter((k) => k.status === 'active')
    }
  } catch {
    antMessage.error('加载 API Key 失败')
  } finally {
    keysLoading.value = false
  }
}

// ───── 发送消息（流式） ─────
const sendMessageInternal = async () => {
  const text = inputText.value.trim()
  if (!text || !selectedModel.value) return
  if (isStreaming.value) return

  // 构建历史消息（发送给后端的 messages 数组）
  const historyMessages = messages.value.map((m) => ({
    role: m.role,
    content: m.content,
  }))

  // 添加用户消息到界面
  messages.value.push({
    role: 'user',
    content: text,
    time: now(),
    tokens: estimateTokenCount(text),
  })
  inputText.value = ''
  recalculateSessionStats()
  await scrollToBottom()

  // 开始流式请求
  isStreaming.value = true
  streamingContent.value = ''
  streamingReasoning.value = ''

  // 添加 AI 消息占位（流式填充）
  const aiMsgIndex = messages.value.length
  messages.value.push({
    role: 'assistant',
    content: '',
    reasoning: '',
    time: now(),
    tokens: 0,
    streaming: true,
  })

  try {
    const requestBody = {
      model: selectedModel.value,
      messages: [...historyMessages, { role: 'user', content: text }],
      stream: true,
      routing_strategy: 'fixed',
    }

    const response = await fetch(`${API_BASE_URL}/internal/chat/completions`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      credentials: 'include',
      body: JSON.stringify(requestBody),
    })

    const contentType = response.headers.get('content-type') || ''
    if (contentType.includes('application/json')) {
      const payload = (await response.json()) as BusinessResponse
      if (payload.code !== 0) {
        throw new Error(resolveErrorMessage(payload))
      }
      throw new Error('当前接口未返回流式数据')
    }

    if (!response.ok) {
      const rawText = await response.text()
      const parsedPayload = parseChunkPayload(rawText)
      throw new Error(resolveErrorMessage(parsedPayload ?? rawText, `HTTP ${response.status}`))
    }

    const reader = response.body?.getReader()
    const decoder = new TextDecoder()

    if (!reader) throw new Error('无法读取响应流')

    let buffer = ''
    let totalTokens = 0

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop() ?? ''

      for (const line of lines) {
        const payload = parseChunkPayload(line)
        if (!payload) {
          continue
        }
        if (isBusinessResponse(payload)) {
          throw new Error(resolveErrorMessage(payload))
        }

        const choices = payload.choices ?? []
        if (choices.length === 0) {
          continue
        }

        const delta = choices[0]?.delta ?? {}
        const finishReason = choices[0]?.finishReason

        if (delta.content) {
          streamingContent.value += delta.content
        }

        if (delta.reasoningContent) {
          streamingReasoning.value += delta.reasoningContent
        }

        updateAssistantMessage(aiMsgIndex, {
          content: streamingContent.value,
          reasoning: streamingReasoning.value,
        })

        await scrollToBottom()

        if (finishReason === 'stop') {
          break
        }
      }
    }

    const lastPayload = parseChunkPayload(buffer)
    if (lastPayload) {
      if (isBusinessResponse(lastPayload)) {
        throw new Error(resolveErrorMessage(lastPayload))
      }

      const lastChoice = lastPayload.choices?.[0]
      if (lastChoice?.delta?.content) {
        streamingContent.value += lastChoice.delta.content
      }
      if (lastChoice?.delta?.reasoningContent) {
        streamingReasoning.value += lastChoice.delta.reasoningContent
      }

      updateAssistantMessage(aiMsgIndex, {
        content: streamingContent.value,
        reasoning: streamingReasoning.value,
      })
    }

    totalTokens = estimateTokenCount(`${streamingReasoning.value}\n${streamingContent.value}`)

    // 完成：关闭 streaming 状态
    updateAssistantMessage(aiMsgIndex, {
      streaming: false,
      tokens: totalTokens,
    })

    recalculateSessionStats()
  } catch (err: unknown) {
    const errMsg = err instanceof Error ? err.message : '未知错误'
    antMessage.error('请求失败：' + errMsg)
    // 移除失败的 AI 消息
    messages.value.splice(aiMsgIndex, 1)
    recalculateSessionStats()
  } finally {
    isStreaming.value = false
    streamingContent.value = ''
    streamingReasoning.value = ''
    await scrollToBottom()
  }
}

const sendMessage = () => {
  if (sendDebounceTimer.value) {
    window.clearTimeout(sendDebounceTimer.value)
  }

  sendDebounceTimer.value = window.setTimeout(() => {
    sendDebounceTimer.value = null
    void sendMessageInternal()
  }, SEND_DEBOUNCE_MS)
}

// ───── 键盘快捷键 ─────
const handleKeydown = (e: KeyboardEvent) => {
  if (e.key === 'Enter' && e.ctrlKey) {
    e.preventDefault()
    sendMessage()
  }
}

// ───── 初始化 ─────
onMounted(() => {
  restoreChatSession()
  loadModels()
  loadApiKeys()
})

onBeforeUnmount(() => {
  if (sendDebounceTimer.value) {
    window.clearTimeout(sendDebounceTimer.value)
  }
})

watch(
  [selectedModel, selectedApiKey, inputText, messages],
  () => {
    saveChatSession()
    recalculateSessionStats()
  },
  { deep: true },
)
</script>

<style scoped>
.chat-page {
  display: flex;
  height: calc(100vh - 56px);
  background: #f3f4f6;
  overflow: hidden;
}

/* ── 左侧配置栏 ── */
.chat-sidebar {
  width: 240px;
  flex-shrink: 0;
  background: #fff;
  border-right: 1px solid #e5e7eb;
  padding: 20px 16px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  overflow-y: auto;
}

.sidebar-section {
  margin-bottom: 12px;
}

.sidebar-label {
  font-size: 11px;
  font-weight: 600;
  color: #9ca3af;
  text-transform: uppercase;
  letter-spacing: 0.6px;
  margin-bottom: 8px;
}

.sidebar-select {
  width: 100%;
}

:deep(.sidebar-select .ant-select-selector) {
  border-radius: 7px !important;
  font-size: 13px !important;
}

.model-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
}

.model-opt-name {
  font-size: 13px;
  color: #374151;
}
.model-opt-tag {
  font-size: 10px;
  margin: 0;
}

.key-option {
  display: flex;
  align-items: center;
  gap: 6px;
}

.key-opt-icon {
  color: #9ca3af;
  font-size: 12px;
}
.key-opt-name {
  font-size: 13px;
  color: #374151;
}

/* 统计 */
.stats-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.stat-cell {
  background: #f9fafb;
  border-radius: 8px;
  padding: 10px 8px;
  text-align: center;
  border: 1px solid #e5e7eb;
}

.stat-val {
  font-size: 18px;
  font-weight: 700;
  color: #2563eb;
  line-height: 1.2;
}

.stat-key {
  font-size: 11px;
  color: #9ca3af;
  margin-top: 2px;
}

.clear-btn {
  border-radius: 7px;
  font-size: 13px;
  margin-top: auto;
}

/* ── 右侧对话区 ── */
.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  overflow: hidden;
}

/* 消息列表 */
.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 24px 16px 16px;
  scroll-behavior: smooth;
}

.message-list::-webkit-scrollbar {
  width: 4px;
}
.message-list::-webkit-scrollbar-thumb {
  background: #d1d5db;
  border-radius: 2px;
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  min-height: 300px;
  text-align: center;
}

.empty-icon {
  font-size: 40px;
  color: #d1d5db;
  margin-bottom: 16px;
}

.empty-title {
  font-size: 18px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 8px;
}

.empty-desc {
  font-size: 14px;
  color: #9ca3af;
  margin-bottom: 24px;
}

.quick-tips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: center;
  max-width: 560px;
}

.quick-tip {
  padding: 8px 14px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 20px;
  font-size: 13px;
  color: #374151;
  cursor: pointer;
  transition: all 0.15s;
}

.quick-tip:hover {
  border-color: #2563eb;
  color: #2563eb;
  background: #eff6ff;
}

/* 消息行 */
.message-row {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 20px;
  max-width: 820px;
}

.message-row--user {
  flex-direction: row-reverse;
  margin-left: auto;
}

.message-row--assistant {
  margin-right: auto;
}

/* 头像 */
.msg-avatar {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  flex-shrink: 0;
}

.avatar-user {
  background: linear-gradient(135deg, #2563eb, #7c3aed);
  color: #fff;
}

.avatar-ai {
  background: linear-gradient(135deg, #059669, #0d9488);
  color: #fff;
}

/* 气泡 */
.msg-bubble {
  max-width: calc(100% - 44px);
  padding: 12px 14px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.7;
  position: relative;
}

.bubble-user {
  background: linear-gradient(135deg, #2563eb, #7c3aed);
  color: #fff;
  border-bottom-right-radius: 4px;
}

.bubble-ai {
  background: #fff;
  border: 1px solid #e5e7eb;
  color: #374151;
  border-bottom-left-radius: 4px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
}

/* 消息内容 */
.msg-content {
  word-break: break-word;
}

:deep(.msg-content > :first-child) {
  margin-top: 0;
}

:deep(.msg-content > :last-child) {
  margin-bottom: 0;
}

:deep(.msg-content p) {
  margin: 0 0 10px;
}

:deep(.msg-content ul),
:deep(.msg-content ol) {
  margin: 8px 0 12px 20px;
  padding: 0;
}

:deep(.msg-content li) {
  margin-bottom: 4px;
}

:deep(.msg-content pre) {
  margin: 10px 0;
}

:deep(.code-block) {
  background: #1e1e2e;
  color: #cdd6f4;
  border-radius: 8px;
  padding: 12px 14px;
  margin: 8px 0;
  font-family: 'JetBrains Mono', 'Fira Code', monospace;
  font-size: 12px;
  overflow-x: auto;
  white-space: pre;
}

:deep(.inline-code) {
  background: rgba(0, 0, 0, 0.08);
  border-radius: 4px;
  padding: 1px 5px;
  font-family: 'JetBrains Mono', monospace;
  font-size: 12px;
}

:deep(.msg-content blockquote) {
  margin: 10px 0;
  padding: 8px 12px;
  border-left: 3px solid #93c5fd;
  background: #f8fafc;
  color: #475569;
}

:deep(.msg-content table) {
  width: 100%;
  border-collapse: collapse;
  margin: 10px 0;
  font-size: 13px;
}

:deep(.msg-content th),
:deep(.msg-content td) {
  border: 1px solid #e5e7eb;
  padding: 6px 8px;
  text-align: left;
}

:deep(.msg-content th) {
  background: #f8fafc;
}

.bubble-user :deep(.inline-code) {
  background: rgba(255, 255, 255, 0.2);
}

/* 推理内容 */
.reasoning-block {
  background: #f8faff;
  border: 1px solid #bfdbfe;
  border-radius: 8px;
  padding: 10px 12px;
  margin-bottom: 10px;
  font-size: 12px;
}

.reasoning-title {
  font-size: 11px;
  font-weight: 600;
  color: #2563eb;
  margin-bottom: 6px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.reasoning-content {
  color: #6b7280;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

/* 流式光标 */
.streaming-cursor {
  display: inline-block;
  width: 2px;
  height: 14px;
  background: #2563eb;
  margin-left: 2px;
  vertical-align: middle;
  animation: blink 0.8s step-end infinite;
}

@keyframes blink {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0;
  }
}

/* 思考中动画 */
.thinking-dots {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 4px 0;
}

.thinking-dots span {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #9ca3af;
  animation: bounce 1.2s ease-in-out infinite;
}

.thinking-dots span:nth-child(2) {
  animation-delay: 0.2s;
}
.thinking-dots span:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes bounce {
  0%,
  80%,
  100% {
    transform: scale(0.7);
    opacity: 0.5;
  }
  40% {
    transform: scale(1);
    opacity: 1;
  }
}

/* 消息底部信息 */
.msg-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 6px;
  font-size: 11px;
}

.msg-time {
  color: #9ca3af;
}

.msg-tokens {
  color: #9ca3af;
  background: rgba(0, 0, 0, 0.05);
  border-radius: 4px;
  padding: 1px 5px;
}

.bubble-user .msg-time {
  color: rgba(255, 255, 255, 0.6);
}
.bubble-user .msg-tokens {
  background: rgba(255, 255, 255, 0.15);
  color: rgba(255, 255, 255, 0.7);
}

.msg-copy {
  color: #9ca3af;
  cursor: pointer;
  font-size: 12px;
  transition: color 0.15s;
  margin-left: auto;
}

.msg-copy:hover {
  color: #2563eb;
}

/* ── 输入区 ── */
.input-area {
  padding: 12px 16px 16px;
  background: #fff;
  border-top: 1px solid #e5e7eb;
}

.input-wrap {
  border: 1.5px solid #e5e7eb;
  border-radius: 12px;
  overflow: hidden;
  transition: border-color 0.15s;
  background: #fff;
}

.input-wrap:focus-within {
  border-color: #2563eb;
}

.msg-input {
  width: 100%;
  border: none !important;
  box-shadow: none !important;
  resize: none;
  padding: 12px 14px 8px;
  font-size: 14px;
  line-height: 1.6;
  background: transparent;
}

:deep(.msg-input .ant-input) {
  border: none !important;
  box-shadow: none !important;
  padding: 0 !important;
  font-size: 14px;
  background: transparent;
}

.input-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 14px 10px;
  border-top: 1px solid #f3f4f6;
}

.char-count {
  font-size: 12px;
  color: #d1d5db;
}

.send-btn {
  height: 34px;
  padding: 0 16px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  background: linear-gradient(135deg, #2563eb, #7c3aed);
  border: none;
  display: flex;
  align-items: center;
  gap: 5px;
}

.send-btn:hover:not(:disabled) {
  opacity: 0.9;
}

.input-hint {
  margin-top: 6px;
  font-size: 12px;
  padding: 0 2px;
}

.hint-warn {
  color: #f59e0b;
  display: flex;
  align-items: center;
  gap: 4px;
}
.hint-ok {
  color: #10b981;
  display: flex;
  align-items: center;
  gap: 4px;
}
</style>
