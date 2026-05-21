<template>
  <div class="chat-workspace">
    <aside class="conversation-rail">
      <div class="rail-header">
        <div>
          <div class="panel-label">会话列表</div>
          <div class="conversation-count">{{ conversations.length }} 个会话</div>
        </div>
        <a-button type="primary" size="small" class="new-conversation-btn" @click="handleCreateConversation">
          <PlusOutlined /> 新建
        </a-button>
      </div>

      <div class="conversation-panel">
        <div v-if="conversationLoading" class="conversation-state">加载会话中...</div>
        <div v-else-if="conversationError" class="conversation-state conversation-state--error">
          <span>{{ conversationError }}</span>
          <a-button size="small" type="link" @click="() => loadConversations()">
            <ReloadOutlined /> 重试
          </a-button>
        </div>
        <div v-else-if="conversations.length === 0" class="conversation-state">暂无会话</div>
        <div v-else class="conversation-list">
          <button
            v-for="item in conversations"
            :key="item.id"
            type="button"
            class="conversation-item"
            :class="{ 'conversation-item--active': item.id === activeConversationId }"
            @click="selectConversation(item.id)"
          >
            <span class="conversation-title">{{ item.title || '新对话' }}</span>
            <span class="conversation-preview">{{ item.lastMessagePreview || '暂无回复' }}</span>
            <span class="conversation-time">{{ formatConversationTime(item.lastMessageAt) }}</span>
            <a-tooltip title="删除会话">
              <span class="conversation-delete" @click.stop="handleDeleteConversation(item.id)">
                <DeleteOutlined />
              </span>
            </a-tooltip>
          </button>
        </div>
      </div>

      <div class="rail-footer">
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
        <a-button block ghost :disabled="!activeConversationId" class="sync-btn" @click="reloadCurrentConversation">
          <ReloadOutlined /> 同步历史
        </a-button>
      </div>
    </aside>

    <main class="chat-stage">
      <header class="stage-header">
        <div>
          <div class="panel-label">当前对话</div>
          <h1 class="stage-title">
            {{ conversations.find((item) => item.id === activeConversationId)?.title || '新对话' }}
          </h1>
        </div>
        <div class="stage-context">
          <a-tag color="blue">{{ currentStrategyLabel }}</a-tag>
          <a-tag :color="enableReasoning ? 'purple' : 'default'">
            {{ enableReasoning ? '深度思考' : '标准模式' }}
          </a-tag>
        </div>
      </header>

      <div ref="messageListRef" class="message-list">
        <div v-if="messageLoading" class="empty-state">
          <div class="thinking-dots"><span></span><span></span><span></span></div>
        </div>
        <div v-else-if="messages.length === 0" class="empty-state">
          <div class="empty-icon">
            <MessageOutlined />
          </div>
          <div class="empty-title">开始一段对话</div>
          <div class="empty-desc">消息会自动保存到当前会话，刷新后仍可继续</div>
          <div class="quick-tips">
            <div v-for="tip in quickTips" :key="tip" class="quick-tip" @click="fillTip(tip)">
              {{ tip }}
            </div>
          </div>
        </div>

        <template v-else>
          <div
            v-for="(msg, idx) in messages"
            :key="idx"
            class="message-row"
            :class="msg.role === 'user' ? 'message-row--user' : 'message-row--assistant'"
          >
            <div class="msg-avatar" :class="msg.role === 'user' ? 'avatar-user' : 'avatar-ai'">
              <UserOutlined v-if="msg.role === 'user'" />
              <RobotOutlined v-else />
            </div>

            <div class="msg-bubble" :class="msg.role === 'user' ? 'bubble-user' : 'bubble-ai'">
              <div v-if="msg.reasoning && msg.role === 'assistant'" class="reasoning-block">
                <button class="reasoning-toggle" type="button" @click="toggleReasoning(idx)">
                  <span class="reasoning-title"><BulbOutlined /> 思考过程</span>
                  <span class="reasoning-action">{{ msg.reasoningExpanded ? '收起' : '展开' }}</span>
                </button>
                <div v-if="msg.reasoningExpanded" class="reasoning-content">{{ msg.reasoning }}</div>
              </div>

              <div class="msg-content" v-html="renderContent(msg.content)"></div>
              <span v-if="msg.streaming" class="streaming-cursor"></span>

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

          <div v-if="isStreaming && !streamingContent" class="message-row message-row--assistant">
            <div class="msg-avatar avatar-ai"><RobotOutlined /></div>
            <div class="msg-bubble bubble-ai">
              <div class="thinking-dots"><span></span><span></span><span></span></div>
            </div>
          </div>
        </template>
      </div>

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
              :disabled="!canSendMessage"
              @click="sendMessage"
            >
              <SendOutlined v-if="!isStreaming" />
              {{ isStreaming ? '生成中…' : '发送' }}
            </a-button>
          </div>
        </div>
        <div class="input-hint">
          <span v-if="selectedRoutingStrategy === 'fixed' && !selectedModel" class="hint-warn"
            ><ExclamationCircleOutlined /> 固定模型策略下请先选择模型</span
          >
          <span v-else-if="reasoningModelWarning" class="hint-warn"
            ><ExclamationCircleOutlined /> {{ reasoningModelWarning }}</span
          >
          <span v-else-if="quotaWarning" class="hint-warn"
            ><ExclamationCircleOutlined /> {{ quotaWarning }}</span
          >
          <span v-else class="hint-ok"
            ><CheckCircleOutlined /> 当前使用网页端登录态聊天，{{ currentStrategyLabel }}已就绪</span
          >
        </div>
      </div>
    </main>

    <aside class="route-panel">
      <section class="route-card">
        <div class="panel-label">路由策略</div>
        <a-segmented v-model:value="selectedRoutingStrategy" :options="routingStrategyOptions" block />
        <div class="route-note">策略会直接透传给后端路由</div>
      </section>

      <section class="route-card">
        <div class="panel-label">模型</div>
        <button type="button" class="model-summary" @click="modelModalOpen = true">
          <span>{{ selectedRoutingStrategy === 'fixed' ? '固定模型' : '候选模型' }}</span>
          <strong>{{ selectedModelLabel }}</strong>
        </button>
      </section>

      <section class="route-card">
        <div class="route-line">
          <div>
            <div class="panel-label">深度思考</div>
            <div class="route-note">需要模型支持 reasoning</div>
          </div>
          <a-switch v-model:checked="enableReasoning" />
        </div>
        <div v-if="reasoningModelWarning" class="reasoning-warning-chip">
          {{ reasoningModelWarning }}
        </div>
      </section>

      <section class="route-card">
        <div class="panel-label">会员额度</div>
        <div class="quota-strip">
          <div class="quota-pill">
            <span>套餐</span>
            <strong>{{ membership.planName || '免费版' }}</strong>
          </div>
          <div class="quota-pill">
            <span>普通剩余</span>
            <strong>{{ formatRemaining(membership.dailyProRemaining) }}</strong>
          </div>
          <div class="quota-pill">
            <span>高级剩余</span>
            <strong>{{ formatRemaining(membership.dailyAdvancedRemaining) }}</strong>
          </div>
          <div class="quota-pill">
            <span>积分</span>
            <strong>{{ formatNum(membership.pointBalance ?? 0) }}</strong>
          </div>
        </div>
      </section>
    </aside>

    <a-modal v-model:open="modelModalOpen" title="选择模型" width="880px" :footer="null">
      <a-tabs v-model:activeKey="activeModelTab">
        <a-tab-pane key="all" tab="全部模型" />
        <a-tab-pane key="fast" tab="快速模型" />
        <a-tab-pane key="reasoning" tab="深度思考" />
      </a-tabs>
      <div class="model-grid">
        <button
          v-for="model in displayedModels"
          :key="model.value"
          type="button"
          class="model-card"
          :class="{ 'model-card--active': selectedModel === model.value }"
          @click="selectModel(model.value)"
        >
          <div class="model-card__head">
            <span class="model-card__title">{{ model.label }}</span>
            <a-space size="small">
              <a-tag :color="tierTagColor(model.accessTier)">{{ tierLabel(model.accessTier) }}</a-tag>
              <a-tag :color="model.tagColor">{{ model.provider }}</a-tag>
            </a-space>
          </div>
          <div class="model-card__meta">
            <span>{{ model.fastLabel }}</span>
            <span>{{ model.reasoningLabel }}</span>
          </div>
          <div class="model-card__desc">
            {{ model.description || `类型：${model.modelType || 'chat'}` }}
          </div>
        </button>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, reactive, nextTick, onMounted, onBeforeUnmount, watch } from 'vue'
import { message as antMessage, Modal } from 'ant-design-vue'
import { marked } from 'marked'
import {
  DeleteOutlined,
  MessageOutlined,
  UserOutlined,
  RobotOutlined,
  BulbOutlined,
  CopyOutlined,
  SendOutlined,
  ExclamationCircleOutlined,
  CheckCircleOutlined,
  PlusOutlined,
  ReloadOutlined,
} from '@ant-design/icons-vue'
import { listAvailableModels } from '@/api/modelController'
import { getMyMembership, type MembershipVO } from '@/api/membershipController'
import {
  createConversation,
  deleteConversation,
  listConversationMessages,
  listConversations,
  streamConversationMessage,
  type ConversationVO,
  type MessageVO,
} from '@/api/conversationController'
import { useLoginUserStore } from '@/stores/loginUser'

const CHAT_CONFIG_STORAGE_KEY = 'leo-ai-router-chat-config'
const SEND_DEBOUNCE_MS = 350
const CONVERSATION_PAGE_SIZE = 20
const MESSAGE_PAGE_SIZE = 30
const FAILED_ASSISTANT_CONTENT = '消息发送失败，请重试'

// ───── 类型 ─────
interface ChatMessage {
  id?: number
  seq?: number
  role: 'user' | 'assistant'
  content: string
  reasoning?: string
  reasoningExpanded?: boolean
  time: string
  tokens?: number
  streaming?: boolean
}

interface ModelOption {
  label: string
  value: string
  provider: string
  tagColor: string
  description?: string
  supportReasoning?: number
  avgLatency?: number
  modelType?: string
  accessTier?: string
  pointCost?: number
  capabilities?: string
  fastLabel: string
  reasoningLabel: string
}

interface BusinessResponse<T = unknown> {
  code?: number
  data?: T
  message?: string
}

interface StoredChatSession {
  selectedModel?: string
  selectedRoutingStrategy?: string
  enableReasoning?: boolean
  inputText: string
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
  gemini: 'gold',
  google: 'gold',
}

// ───── 状态 ─────
const selectedModel = ref<string | undefined>(undefined)
const selectedRoutingStrategy = ref<'auto' | 'cost_first' | 'latency_first' | 'fixed'>('auto')
const enableReasoning = ref(false)
const inputText = ref('')
const messages = ref<ChatMessage[]>([])
const conversations = ref<ConversationVO[]>([])
const activeConversationId = ref<number | undefined>(undefined)
const isStreaming = ref(false)
const streamingContent = ref('')
const streamingReasoning = ref('')
const messageListRef = ref<HTMLDivElement | null>(null)
const sendDebounceTimer = ref<number | null>(null)
const modelModalOpen = ref(false)
const activeModelTab = ref<'all' | 'fast' | 'reasoning'>('all')
const conversationLoading = ref(false)
const messageLoading = ref(false)
const conversationError = ref('')

const modelsLoading = ref(false)
const models = ref<ModelOption[]>([])
const membership = ref<MembershipVO>({})
const loginUserStore = useLoginUserStore()

const sessionStats = reactive({
  totalMessages: 0,
  totalTokens: 0,
})

const routingStrategyOptions = [
  { label: '自动路由', value: 'auto' },
  { label: '成本优先', value: 'cost_first' },
  { label: '速度优先', value: 'latency_first' },
  { label: '固定模型', value: 'fixed' },
]

const currentStrategyLabel = computed(() => {
  return routingStrategyOptions.find((item) => item.value === selectedRoutingStrategy.value)?.label ?? '自动路由'
})

const selectedModelLabel = computed(() => {
  if (!selectedModel.value) {
    return selectedRoutingStrategy.value === 'fixed' ? '未选择' : '由路由自动挑选'
  }
  return models.value.find((item) => item.value === selectedModel.value)?.label ?? selectedModel.value
})

const displayedModels = computed(() => {
  if (activeModelTab.value === 'reasoning') {
    return models.value.filter((item) => item.supportReasoning === 1)
  }
  if (activeModelTab.value === 'fast') {
    return models.value.filter((item) => item.supportReasoning !== 1 || (item.avgLatency ?? 99999) <= 1500)
  }
  return models.value
})

const normalizeTier = (tier?: string) => {
  const normalized = (tier || 'free').toLowerCase()
  if (['advanced', 'pro', 'free', 'image', 'video'].includes(normalized)) {
    return normalized
  }
  return 'free'
}

const hasRemaining = (value?: number) => value === undefined || value === -1 || value > 0

const currentUserId = computed(() => {
  const id = Number(loginUserStore.loginUser.id)
  return Number.isFinite(id) && id > 0 ? id : undefined
})

const currentSelectedModel = computed(() => {
  if (!selectedModel.value) {
    return undefined
  }
  return models.value.find((item) => item.value === selectedModel.value)
})

const hasAnyChatRemaining = computed(() => {
  return hasRemaining(membership.value.dailyProRemaining) || hasRemaining(membership.value.dailyAdvancedRemaining)
})

const selectedModelCanSend = computed(() => {
  if (selectedRoutingStrategy.value !== 'fixed') {
    return hasAnyChatRemaining.value
  }
  const model = currentSelectedModel.value
  if (!model) {
    return true
  }
  const tier = normalizeTier(model.accessTier)
  if (tier === 'advanced') {
    return hasRemaining(membership.value.dailyAdvancedRemaining)
  }
  if (tier === 'image' || tier === 'video') {
    return false
  }
  return hasRemaining(membership.value.dailyProRemaining)
})

const quotaWarning = computed(() => {
  if (selectedRoutingStrategy.value !== 'fixed') {
    return hasAnyChatRemaining.value ? '' : '今日聊天次数已用尽，请升级套餐或明日再试'
  }
  const model = currentSelectedModel.value
  if (!model) {
    return ''
  }
  const tier = normalizeTier(model.accessTier)
  if (tier === 'advanced' && !hasRemaining(membership.value.dailyAdvancedRemaining)) {
    return '今日高级模型次数已用完，请切换普通模型或升级套餐'
  }
  if ((tier === 'free' || tier === 'pro') && !hasRemaining(membership.value.dailyProRemaining)) {
    return '今日普通模型次数已用完，请升级套餐或明日再试'
  }
  if (tier === 'image' || tier === 'video') {
    return '该模型不能用于聊天'
  }
  return ''
})

const canSendMessage = computed(() => {
  if (!inputText.value.trim()) {
    return false
  }
  if (selectedRoutingStrategy.value === 'fixed' && !selectedModel.value) {
    return false
  }
  if (!selectedModelCanSend.value) {
    return false
  }
  return true
})

const reasoningModelWarning = computed(() => {
  if (!enableReasoning.value) {
    return ''
  }
  if (selectedRoutingStrategy.value !== 'fixed') {
    return '当前策略无法保证一定命中支持深度思考的模型'
  }
  if (!currentSelectedModel.value) {
    return '请先选择一个支持深度思考的模型'
  }
  if (currentSelectedModel.value.supportReasoning !== 1) {
    return `${currentSelectedModel.value.label} 不支持深度思考`
  }
  return ''
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
const formatRemaining = (value?: number) => (value === -1 ? '无限' : formatNum(Number(value ?? 0)))

const tierLabel = (tier?: string) => {
  const normalized = normalizeTier(tier)
  if (normalized === 'advanced') return '高级'
  if (normalized === 'pro') return '普通'
  if (normalized === 'image') return '图片'
  if (normalized === 'video') return '视频'
  return '免费'
}

const tierTagColor = (tier?: string) => {
  const normalized = normalizeTier(tier)
  if (normalized === 'advanced') return 'purple'
  if (normalized === 'pro') return 'blue'
  if (normalized === 'image') return 'green'
  if (normalized === 'video') return 'volcano'
  return 'default'
}

const now = () => new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })

const escapeHtml = (text: string) => {
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

const markdownRenderer = new marked.Renderer()
markdownRenderer.code = (token: any) => {
  const content = typeof token?.text === 'string' ? token.text : ''
  const language = typeof token?.lang === 'string' && token.lang ? ` language-${token.lang}` : ''
  return `<pre class="code-block"><code class="${language.trim()}">${escapeHtml(content)}</code></pre>`
}
markdownRenderer.codespan = (token: any) => {
  const content = typeof token?.text === 'string' ? token.text : ''
  return `<code class="inline-code">${escapeHtml(content)}</code>`
}

marked.setOptions({
  gfm: true,
  breaks: true,
  renderer: markdownRenderer,
})

const sanitizeHtml = (html: string) => {
  return html
    .replace(/<script[\s\S]*?>[\s\S]*?<\/script>/gi, '')
    .replace(/\son\w+="[^"]*"/gi, '')
    .replace(/\son\w+='[^']*'/gi, '')
    .replace(/javascript:/gi, '')
}

const renderContent = (text: string) => {
  const html = marked.parse(text) as string
  return sanitizeHtml(
    html.replace(/<table>/g, '<div class="table-wrapper"><table>').replace(/<\/table>/g, '</table></div>'),
  )
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

const requireUserId = async () => {
  if (!currentUserId.value) {
    await loginUserStore.fetchLoginUser()
  }
  if (!currentUserId.value) {
    antMessage.warning('请先登录')
    redirectToLogin()
    throw new Error('请先登录')
  }
  return currentUserId.value
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

const normalizeStreamChunk = (text: string) => {
  return text.replace(/\r\n/g, '\n').replace(/\r/g, '\n')
}

const stripNestedDataPrefix = (text: string) => {
  let normalized = text
  while (normalized.startsWith('data:')) {
    normalized = normalized.slice(5).replace(/^\s/, '')
  }
  return normalized.replace(/(^|\n)data:\s?/g, '$1')
}

const extractSseEvents = (rawBuffer: string) => {
  const normalized = normalizeStreamChunk(rawBuffer)
  const events: string[] = []
  let cursor = 0

  while (true) {
    const boundary = normalized.indexOf('\n\n', cursor)
    if (boundary < 0) {
      break
    }
    events.push(normalized.slice(cursor, boundary))
    cursor = boundary + 2
  }

  return {
    events,
    rest: normalized.slice(cursor),
  }
}

const parseSseEvent = (rawEvent: string) => {
  const dataLines = rawEvent
    .split('\n')
    .filter((line) => line.startsWith('data:'))
    .map((line) => line.slice(5).replace(/^\s/, ''))

  if (dataLines.length === 0) {
    return { done: false, text: '' }
  }

  const text = stripNestedDataPrefix(dataLines.join('\n'))
  if (text.trim() === '[DONE]') {
    return { done: true, text: '' }
  }
  return { done: false, text }
}

const parseBusinessPayload = (rawText: string): BusinessResponse | string => {
  const text = rawText.trim()
  if (!text) {
    return rawText
  }
  const jsonText = text.startsWith('data:') ? text.slice(5).trim() : text
  try {
    return JSON.parse(jsonText) as BusinessResponse
  } catch {
    return rawText
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
    selectedRoutingStrategy: selectedRoutingStrategy.value,
    enableReasoning: enableReasoning.value,
    inputText: inputText.value,
  }
  localStorage.setItem(CHAT_CONFIG_STORAGE_KEY, JSON.stringify(session))
}

const restoreChatSession = () => {
  if (typeof window === 'undefined') {
    return
  }

  const raw = localStorage.getItem(CHAT_CONFIG_STORAGE_KEY)
  if (!raw) {
    recalculateSessionStats()
    return
  }

  try {
    const session = JSON.parse(raw) as StoredChatSession
    selectedModel.value = session.selectedModel
    selectedRoutingStrategy.value = (session.selectedRoutingStrategy as typeof selectedRoutingStrategy.value) ?? 'auto'
    enableReasoning.value = session.enableReasoning ?? false
    inputText.value = session.inputText ?? ''
  } catch {
    localStorage.removeItem(CHAT_CONFIG_STORAGE_KEY)
  }

  recalculateSessionStats()
}

const updateAssistantMessage = (index: number, patch: Partial<ChatMessage> = {}) => {
  const currentMessage = messages.value[index]
  if (!currentMessage) {
    return
  }

  messages.value[index] = {
    id: currentMessage.id,
    seq: currentMessage.seq,
    role: currentMessage.role,
    content: patch.content ?? currentMessage.content,
    reasoning: patch.reasoning ?? currentMessage.reasoning,
    reasoningExpanded: patch.reasoningExpanded ?? currentMessage.reasoningExpanded,
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

const toggleReasoning = (index: number) => {
  const currentMessage = messages.value[index]
  if (!currentMessage?.reasoning) {
    return
  }
  updateAssistantMessage(index, {
    reasoningExpanded: !currentMessage.reasoningExpanded,
  })
}

const selectModel = (value: string) => {
  selectedModel.value = value
  selectedRoutingStrategy.value = 'fixed'
  modelModalOpen.value = false
}

const formatConversationTime = (value?: string) => {
  if (!value) {
    return ''
  }
  return new Date(value).toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

const formatMessageTime = (value?: string) => {
  if (!value) {
    return now()
  }
  return new Date(value).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

const toChatMessage = (message: MessageVO): ChatMessage | null => {
  if (message.role !== 'user' && message.role !== 'assistant') {
    return null
  }
  const content = message.content ?? ''
  return {
    id: message.id,
    seq: message.seq,
    role: message.role,
    content,
    reasoning: '',
    reasoningExpanded: false,
    time: formatMessageTime(message.createdAt),
    tokens: estimateTokenCount(content),
    streaming: false,
  }
}

const loadMessages = async (conversationId: number) => {
  messageLoading.value = true
  try {
    const userId = await requireUserId()
    const res = await listConversationMessages(userId, conversationId, {
      page: 0,
      size: MESSAGE_PAGE_SIZE,
    })
    if (res.data.code !== 0) {
      throw new Error(res.data.message || '加载消息历史失败')
    }
    messages.value = (res.data.data?.records ?? [])
      .map(toChatMessage)
      .filter((item): item is ChatMessage => Boolean(item))
    recalculateSessionStats()
    await scrollToBottom()
  } catch (err) {
    antMessage.error(resolveErrorMessage(err, '加载消息历史失败'))
  } finally {
    messageLoading.value = false
  }
}

const selectConversation = async (conversationId?: number) => {
  if (!conversationId || conversationId === activeConversationId.value || isStreaming.value) {
    return
  }
  activeConversationId.value = conversationId
  await loadMessages(conversationId)
}

const loadConversations = async (preferredConversationId?: number) => {
  conversationLoading.value = true
  conversationError.value = ''
  try {
    const userId = await requireUserId()
    const res = await listConversations(userId, { page: 0, size: CONVERSATION_PAGE_SIZE })
    if (res.data.code !== 0) {
      throw new Error(res.data.message || '加载会话列表失败')
    }
    conversations.value = res.data.data?.records ?? []
    const preferred = preferredConversationId
      ? conversations.value.find((item) => item.id === preferredConversationId)
      : undefined
    const activeStillExists = conversations.value.find((item) => item.id === activeConversationId.value)
    const nextConversation = preferred ?? activeStillExists ?? conversations.value[0]

    if (nextConversation?.id) {
      activeConversationId.value = nextConversation.id
      await loadMessages(nextConversation.id)
    } else {
      activeConversationId.value = undefined
      messages.value = []
      recalculateSessionStats()
    }
  } catch (err) {
    conversationError.value = resolveErrorMessage(err, '加载会话列表失败')
  } finally {
    conversationLoading.value = false
  }
}

const handleCreateConversation = async () => {
  if (isStreaming.value) {
    return
  }
  try {
    const userId = await requireUserId()
    const res = await createConversation(userId, { convType: 1 })
    if (res.data.code !== 0 || !res.data.data?.conversationId) {
      throw new Error(res.data.message || '创建会话失败')
    }
    activeConversationId.value = res.data.data.conversationId
    messages.value = []
    recalculateSessionStats()
    await loadConversations(res.data.data.conversationId)
  } catch (err) {
    antMessage.error(resolveErrorMessage(err, '创建会话失败'))
  }
}

const ensureConversation = async () => {
  if (activeConversationId.value) {
    return activeConversationId.value
  }
  const userId = await requireUserId()
  const res = await createConversation(userId, { convType: 1 })
  if (res.data.code !== 0 || !res.data.data?.conversationId) {
    throw new Error(res.data.message || '创建会话失败')
  }
  activeConversationId.value = res.data.data.conversationId
  await loadConversations(res.data.data.conversationId)
  return res.data.data.conversationId
}

const reloadCurrentConversation = async () => {
  if (!activeConversationId.value) {
    return
  }
  await loadMessages(activeConversationId.value)
  await loadConversations(activeConversationId.value)
}

const handleDeleteConversation = async (conversationId?: number) => {
  if (!conversationId || isStreaming.value) {
    return
  }
  Modal.confirm({
    title: '删除会话',
    content: '删除后该会话不会再出现在列表中。',
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      try {
        const userId = await requireUserId()
        const res = await deleteConversation(userId, conversationId)
        if (res.data.code !== 0) {
          throw new Error(res.data.message || '删除会话失败')
        }
        if (conversationId === activeConversationId.value) {
          activeConversationId.value = undefined
          messages.value = []
        }
        await loadConversations()
      } catch (err) {
        antMessage.error(resolveErrorMessage(err, '删除会话失败'))
      }
    },
  })
}


const loadModels = async () => {
  modelsLoading.value = true
  try {
    const res = await listAvailableModels()
    if (res.data.code === 0 && res.data.data) {
      models.value = res.data.data
        .filter((m) => m.modelType === 'chat')
        .map((m) => ({
          label: m.modelName ?? m.modelKey ?? '',
          value: m.modelKey ?? '',
          provider: m.providerDisplayName ?? m.providerName ?? '',
          tagColor: providerColorMap[m.providerName?.toLowerCase() ?? ''] ?? 'default',
          description: m.description ?? '',
          supportReasoning: m.supportReasoning ?? 0,
          avgLatency: m.avgLatency ?? undefined,
          modelType: m.modelType ?? '',
          accessTier: m.accessTier ?? 'free',
          pointCost: m.pointCost ?? 0,
          capabilities: m.capabilities ?? '',
          fastLabel: m.avgLatency ? `${m.avgLatency} ms` : '延迟未知',
          reasoningLabel: m.supportReasoning === 1 ? '支持深度思考' : '标准模式',
        }))

      if (!selectedModel.value || !models.value.some((item) => item.value === selectedModel.value)) {
        selectedModel.value = models.value[0]?.value
      }
    }
  } catch {
    antMessage.error('加载模型列表失败')
  } finally {
    modelsLoading.value = false
  }
}

const loadMembership = async () => {
  try {
    const res = await getMyMembership()
    if (res.data.code === 0) {
      membership.value = res.data.data ?? {}
    }
  } catch {
    membership.value = {}
  }
}

// ───── 发送消息（流式） ─────
const sendMessageInternal = async () => {
  const text = inputText.value.trim()
  if (!text) return
  if (selectedRoutingStrategy.value === 'fixed' && !selectedModel.value) {
    antMessage.warning('固定模型策略下请先选择模型')
    return
  }
  if (isStreaming.value) return

  let conversationId: number
  let userId: number
  try {
    conversationId = await ensureConversation()
    userId = await requireUserId()
  } catch (err) {
    antMessage.error(resolveErrorMessage(err, '创建会话失败'))
    return
  }
  const mode = enableReasoning.value ? 2 : 1

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
    reasoningExpanded: false,
    time: now(),
    tokens: 0,
    streaming: true,
  })

  try {
    const response = await streamConversationMessage(userId, conversationId, {
      content: text,
      mode,
      model: selectedRoutingStrategy.value === 'fixed' ? selectedModel.value : undefined,
      routing_strategy: selectedRoutingStrategy.value,
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
      const parsedPayload = parseBusinessPayload(rawText)
      throw new Error(resolveErrorMessage(parsedPayload ?? rawText, `HTTP ${response.status}`))
    }

    const reader = response.body?.getReader()
    const decoder = new TextDecoder()

    if (!reader) throw new Error('无法读取响应流')

    let buffer = ''
    let totalTokens = 0
    let streamDone = false

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })
      const parsed = extractSseEvents(buffer)
      buffer = parsed.rest

      for (const eventText of parsed.events) {
        const payload = parseSseEvent(eventText)
        if (!payload.text && !payload.done) {
          continue
        }
        if (payload.done) {
          streamDone = true
          break
        }
        streamingContent.value += payload.text

        updateAssistantMessage(aiMsgIndex, {
          content: streamingContent.value,
        })

        await scrollToBottom()
      }
      if (streamDone) break
    }

    if (buffer.trim()) {
      const parsed = parseSseEvent(buffer)
      if (parsed.done) {
        streamDone = true
      } else if (parsed.text) {
        streamingContent.value += parsed.text
        updateAssistantMessage(aiMsgIndex, {
          content: streamingContent.value,
        })
      }
    }

    totalTokens = estimateTokenCount(`${streamingReasoning.value}\n${streamingContent.value}`)

    // 完成：关闭 streaming 状态
    updateAssistantMessage(aiMsgIndex, {
      streaming: false,
      tokens: totalTokens,
    })

    recalculateSessionStats()
    await loadMessages(conversationId)
    await loadConversations(conversationId)
    await loadMembership()
  } catch (err: unknown) {
    const errMsg = err instanceof Error ? err.message : '未知错误'
    antMessage.error('请求失败：' + errMsg)
    updateAssistantMessage(aiMsgIndex, {
      content: FAILED_ASSISTANT_CONTENT,
      streaming: false,
      tokens: estimateTokenCount(FAILED_ASSISTANT_CONTENT),
    })
    recalculateSessionStats()
    await loadMessages(conversationId)
    if (!messages.value.some((item) => item.role === 'assistant' && item.content === FAILED_ASSISTANT_CONTENT)) {
      messages.value.push({
        role: 'assistant',
        content: FAILED_ASSISTANT_CONTENT,
        reasoning: '',
        reasoningExpanded: false,
        time: now(),
        tokens: estimateTokenCount(FAILED_ASSISTANT_CONTENT),
        streaming: false,
      })
    }
    await loadConversations(conversationId)
    await loadMembership()
  } finally {
    isStreaming.value = false
    streamingContent.value = ''
    streamingReasoning.value = ''
    await scrollToBottom()
  }
}

const sendMessage = () => {
  if (!canSendMessage.value) {
    return
  }
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
  void Promise.all([loadModels(), loadConversations(), loadMembership()])
})

onBeforeUnmount(() => {
  if (sendDebounceTimer.value) {
    window.clearTimeout(sendDebounceTimer.value)
  }
})

watch(
  [selectedModel, selectedRoutingStrategy, enableReasoning, inputText],
  () => {
    saveChatSession()
    recalculateSessionStats()
  },
  { deep: true },
)

watch(selectedRoutingStrategy, (value) => {
  if (value !== 'fixed') {
    return
  }
  if (!selectedModel.value && models.value.length > 0) {
    selectedModel.value = models.value[0]?.value
  }
})
</script>

<style scoped>
.chat-workspace {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr) 300px;
  gap: 12px;
  height: calc(100vh - var(--leo-header-height));
  min-height: 640px;
  padding: 12px;
  background: var(--leo-bg-page);
  overflow: hidden;
}

.conversation-rail,
.chat-stage,
.route-panel {
  min-height: 0;
  background: var(--leo-bg-panel);
  border: 1px solid var(--leo-border);
  border-radius: var(--leo-radius-md);
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.05);
}

.conversation-rail {
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  overflow: hidden;
}

.chat-stage {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.route-panel {
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  overflow-y: auto;
}

.route-panel::-webkit-scrollbar,
.conversation-panel::-webkit-scrollbar,
.message-list::-webkit-scrollbar {
  width: 4px;
}

.route-panel::-webkit-scrollbar-thumb,
.conversation-panel::-webkit-scrollbar-thumb,
.message-list::-webkit-scrollbar-thumb {
  background: var(--leo-border-strong);
  border-radius: 4px;
}

.panel-label {
  font-size: 11px;
  font-weight: 700;
  color: var(--leo-text-tertiary);
  text-transform: uppercase;
  letter-spacing: 0;
  margin-bottom: 8px;
}

.rail-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.conversation-count {
  color: var(--leo-text-secondary);
  font-size: 12px;
  line-height: 1.2;
}

.new-conversation-btn {
  border-radius: var(--leo-radius-sm);
  display: inline-flex;
  align-items: center;
  gap: 4px;
  background: var(--leo-primary);
}

.conversation-panel {
  flex: 1;
  min-height: 160px;
  overflow-y: auto;
  padding-right: 2px;
}

.conversation-state {
  min-height: 92px;
  border: 1px dashed var(--leo-border-strong);
  border-radius: var(--leo-radius-md);
  color: var(--leo-text-tertiary);
  font-size: 13px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: 6px;
  text-align: center;
  padding: 12px;
  background: var(--leo-bg-muted);
}

.conversation-state--error {
  color: var(--leo-danger);
  background: #fff7f7;
  border-color: #fecaca;
}

.conversation-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.conversation-item {
  position: relative;
  width: 100%;
  border: 1px solid var(--leo-border);
  background: var(--leo-bg-panel);
  border-radius: var(--leo-radius-md);
  padding: 10px 34px 10px 10px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  text-align: left;
  cursor: pointer;
  transition: all 0.16s ease;
}

.conversation-item:hover {
  border-color: var(--leo-border-strong);
  background: var(--leo-bg-muted);
}

.conversation-item--active {
  border-color: var(--leo-primary);
  background: var(--leo-bg-active);
}

.conversation-title {
  color: var(--leo-text-primary);
  font-size: 13px;
  font-weight: 700;
  line-height: 1.3;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.conversation-preview {
  color: var(--leo-text-secondary);
  font-size: 12px;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.conversation-time {
  color: var(--leo-text-tertiary);
  font-size: 11px;
  line-height: 1.2;
}

.conversation-delete {
  position: absolute;
  right: 8px;
  top: 9px;
  color: var(--leo-text-tertiary);
  width: 22px;
  height: 22px;
  border-radius: 6px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: all 0.16s ease;
}

.conversation-delete:hover {
  color: var(--leo-danger);
  background: #fee2e2;
}

.rail-footer {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.stage-header {
  min-height: 72px;
  padding: 16px 18px 14px;
  border-bottom: 1px solid var(--leo-border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.stage-title {
  margin: 0;
  max-width: 560px;
  color: var(--leo-text-primary);
  font-size: 18px;
  line-height: 1.35;
  font-weight: 700;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.stage-context {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.route-card {
  border: 1px solid var(--leo-border);
  border-radius: var(--leo-radius-md);
  background: var(--leo-bg-panel);
  padding: 12px;
}

.route-note {
  color: var(--leo-text-tertiary);
  font-size: 12px;
  line-height: 1.5;
  margin-top: 8px;
}

.route-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.model-summary {
  width: 100%;
  border: 1px solid var(--leo-border);
  border-radius: var(--leo-radius-md);
  background: var(--leo-bg-muted);
  padding: 10px 12px;
  text-align: left;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  gap: 4px;
  transition: all 0.16s ease;
}

.model-summary:hover {
  border-color: var(--leo-primary);
  background: var(--leo-primary-soft);
}

.model-summary span {
  color: var(--leo-text-tertiary);
  font-size: 12px;
}

.model-summary strong {
  color: var(--leo-text-primary);
  font-size: 14px;
  line-height: 1.4;
  word-break: break-word;
}

.session-card {
  border: 1px solid var(--leo-border);
  border-radius: var(--leo-radius-md);
  background: var(--leo-bg-muted);
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.session-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  font-size: 13px;
  color: var(--leo-text-secondary);
}

.session-value {
  max-width: 120px;
  text-align: right;
  color: var(--leo-text-primary);
  font-weight: 600;
}

/* 统计 */
.stats-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.stat-cell {
  background: var(--leo-bg-muted);
  border-radius: var(--leo-radius-md);
  padding: 10px 8px;
  text-align: center;
  border: 1px solid var(--leo-border);
}

.stat-val {
  font-size: 18px;
  font-weight: 700;
  color: var(--leo-primary);
  line-height: 1.2;
}

.stat-key {
  font-size: 11px;
  color: var(--leo-text-tertiary);
  margin-top: 2px;
}

.sync-btn {
  border-radius: var(--leo-radius-sm);
  font-size: 13px;
  margin-top: auto;
}

/* 消息列表 */
.message-list {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 22px 18px 18px;
  scroll-behavior: smooth;
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
  color: var(--leo-border-strong);
  margin-bottom: 16px;
}

.empty-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--leo-text-primary);
  margin-bottom: 8px;
}

.empty-desc {
  font-size: 14px;
  color: var(--leo-text-tertiary);
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
  background: var(--leo-bg-panel);
  border: 1px solid var(--leo-border);
  border-radius: var(--leo-radius-md);
  font-size: 13px;
  color: var(--leo-text-primary);
  cursor: pointer;
  transition: all 0.15s;
}

.quick-tip:hover {
  border-color: var(--leo-primary);
  color: var(--leo-primary);
  background: var(--leo-primary-soft);
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
  background: var(--leo-primary);
  color: #fff;
}

.avatar-ai {
  background: var(--leo-success);
  color: #fff;
}

/* 气泡 */
.msg-bubble {
  max-width: calc(100% - 44px);
  padding: 12px 14px;
  border-radius: var(--leo-radius-md);
  font-size: 14px;
  line-height: 1.7;
  position: relative;
}

.bubble-user {
  background: var(--leo-primary);
  color: #fff;
  border-bottom-right-radius: 4px;
}

.bubble-ai {
  background: var(--leo-bg-panel);
  border: 1px solid var(--leo-border);
  color: var(--leo-text-primary);
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

:deep(.msg-content h1),
:deep(.msg-content h2),
:deep(.msg-content h3),
:deep(.msg-content h4) {
  margin: 16px 0 10px;
  line-height: 1.4;
  color: var(--leo-text-primary);
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

:deep(.code-block code) {
  display: block;
  color: inherit;
  background: transparent;
  font-family: inherit;
  line-height: 1.65;
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
  background: var(--leo-bg-muted);
  color: var(--leo-text-secondary);
}

:deep(.table-wrapper) {
  width: 100%;
  overflow-x: auto;
  margin: 10px 0;
}

:deep(.msg-content table) {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
  min-width: 420px;
}

:deep(.msg-content th),
:deep(.msg-content td) {
  border: 1px solid var(--leo-border);
  padding: 6px 8px;
  text-align: left;
}

:deep(.msg-content th) {
  background: var(--leo-bg-muted);
}

:deep(.msg-content hr) {
  border: none;
  border-top: 1px solid var(--leo-border);
  margin: 14px 0;
}

.bubble-user :deep(.inline-code) {
  background: rgba(255, 255, 255, 0.2);
}

/* 推理内容 */
.reasoning-block {
  background: var(--leo-bg-muted);
  border: 1px solid #bfdbfe;
  border-radius: var(--leo-radius-md);
  padding: 10px 12px;
  margin-bottom: 10px;
  font-size: 12px;
}

.reasoning-toggle {
  width: 100%;
  border: none;
  background: transparent;
  padding: 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  cursor: pointer;
}

.reasoning-title {
  font-size: 11px;
  font-weight: 600;
  color: var(--leo-primary);
  display: flex;
  align-items: center;
  gap: 4px;
}

.reasoning-action {
  color: var(--leo-text-secondary);
}

.reasoning-content {
  color: var(--leo-text-secondary);
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
  margin-top: 10px;
}

/* 流式光标 */
.streaming-cursor {
  display: inline-block;
  width: 2px;
  height: 14px;
  background: var(--leo-primary);
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
  background: var(--leo-text-tertiary);
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
  color: var(--leo-text-tertiary);
}

.msg-tokens {
  color: var(--leo-text-tertiary);
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
  color: var(--leo-text-tertiary);
  cursor: pointer;
  font-size: 12px;
  transition: color 0.15s;
  margin-left: auto;
}

.msg-copy:hover {
  color: var(--leo-primary);
}

/* ── 输入区 ── */
.input-area {
  flex-shrink: 0;
  border-top: 1px solid var(--leo-border);
  background: var(--leo-bg-panel);
  padding: 14px 16px 16px;
}

.composer-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.toolbar-right {
  font-size: 12px;
  color: var(--leo-text-tertiary);
}

.quota-strip {
  display: grid;
  grid-template-columns: 1fr;
  gap: 8px;
}

.quota-pill {
  min-width: 0;
  border: 1px solid var(--leo-border);
  border-radius: var(--leo-radius-md);
  background: var(--leo-bg-muted);
  padding: 8px 10px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}

.quota-pill span {
  color: var(--leo-text-secondary);
  font-size: 12px;
}

.quota-pill strong {
  color: var(--leo-text-primary);
  font-size: 13px;
  white-space: nowrap;
}

.model-trigger {
  border-radius: 999px;
  border-color: #cbd5e1;
}

.reasoning-switch {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 7px 10px;
  border-radius: 999px;
  background: var(--leo-bg-muted);
  border: 1px solid var(--leo-border);
}

.reasoning-switch__label {
  font-size: 13px;
  color: var(--leo-text-secondary);
}

.reasoning-warning-chip {
  display: inline-flex;
  align-items: center;
  padding: 7px 10px;
  border-radius: var(--leo-radius-md);
  background: #fff7ed;
  border: 1px solid #fdba74;
  color: #c2410c;
  font-size: 12px;
  font-weight: 500;
}

.input-wrap {
  border: 1px solid var(--leo-border);
  border-radius: var(--leo-radius-md);
  overflow: hidden;
  transition: border-color 0.15s;
  background: #fff;
}

.input-wrap:focus-within {
  border-color: var(--leo-primary);
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
  border-top: 1px solid var(--leo-border);
}

.char-count {
  font-size: 12px;
  color: var(--leo-text-tertiary);
}

.send-btn {
  height: 34px;
  padding: 0 16px;
  border-radius: var(--leo-radius-md);
  font-size: 13px;
  font-weight: 600;
  background: var(--leo-primary);
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
  color: var(--leo-warning);
  display: flex;
  align-items: center;
  gap: 4px;
}
.hint-ok {
  color: var(--leo-success);
  display: flex;
  align-items: center;
  gap: 4px;
}

.model-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 12px;
  margin-top: 8px;
}

.model-card {
  text-align: left;
  border: 1px solid var(--leo-border);
  background: var(--leo-bg-panel);
  border-radius: var(--leo-radius-md);
  padding: 14px;
  cursor: pointer;
  transition: all 0.18s ease;
}

.model-card:hover {
  border-color: var(--leo-primary);
  box-shadow: 0 10px 30px rgba(37, 99, 235, 0.08);
  transform: translateY(-1px);
}

.model-card--active {
  border-color: var(--leo-primary);
  background: var(--leo-primary-soft);
}

.model-card__head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 8px;
  margin-bottom: 8px;
}

.model-card__title {
  font-weight: 700;
  color: var(--leo-text-primary);
}

.model-card__meta {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  font-size: 12px;
  color: var(--leo-text-secondary);
  margin-bottom: 8px;
}

.model-card__desc {
  font-size: 12px;
  color: var(--leo-text-secondary);
  line-height: 1.6;
}

@media (max-width: 1180px) {
  .chat-workspace {
    grid-template-columns: 240px minmax(0, 1fr);
    grid-template-rows: minmax(0, 1fr) auto;
    grid-template-areas:
      "rail stage"
      "route route";
  }

  .conversation-rail {
    grid-area: rail;
    max-height: calc(100vh - 24px - var(--leo-header-height));
  }

  .chat-stage {
    grid-area: stage;
    min-height: 0;
  }

  .route-panel {
    grid-area: route;
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    overflow: visible;
  }
}

@media (max-width: 860px) {
  .chat-workspace {
    grid-template-columns: 1fr;
    grid-template-rows: auto auto auto;
    grid-template-areas:
      "rail"
      "stage"
      "route";
    height: auto;
    min-height: calc(100vh - var(--leo-header-height));
    overflow: visible;
    padding: 8px;
  }

  .conversation-rail {
    max-height: none;
  }

  .conversation-panel {
    max-height: 180px;
  }

  .chat-stage {
    min-height: 620px;
  }

  .stage-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .stage-title {
    max-width: 100%;
    white-space: normal;
  }

  .route-panel {
    display: flex;
  }
}

@media (max-width: 560px) {
  .chat-workspace {
    gap: 8px;
  }

  .message-list {
    padding: 16px 10px;
  }

  .message-row {
    max-width: 100%;
  }

  .msg-avatar {
    width: 28px;
    height: 28px;
  }

  .msg-bubble {
    max-width: calc(100% - 36px);
  }

  .stats-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
