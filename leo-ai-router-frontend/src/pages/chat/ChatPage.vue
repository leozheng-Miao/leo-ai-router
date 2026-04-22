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
          placeholder="请选择 API Key"
          class="sidebar-select"
          :loading="keysLoading"
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
          <div class="empty-desc">选择模型和 API Key，然后输入你的问题</div>
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
              :disabled="!inputText.trim() || !selectedModel || !selectedApiKey"
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
          <span v-else-if="!selectedApiKey" class="hint-warn"
            ><ExclamationCircleOutlined /> 请先选择 API Key</span
          >
          <span v-else class="hint-ok"><CheckCircleOutlined /> 准备就绪，可以发送</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, nextTick, onMounted } from 'vue'
import { message as antMessage } from 'ant-design-vue'
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

// ───── 状态 ─────
const selectedModel = ref<string | undefined>(undefined)
const selectedApiKey = ref<number | undefined>(undefined)
const inputText = ref('')
const messages = ref<ChatMessage[]>([])
const isStreaming = ref(false)
const streamingContent = ref('')
const streamingReasoning = ref('')
const messageListRef = ref<HTMLDivElement | null>(null)

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

const renderContent = (text: string) => {
  // 简单处理代码块和换行，不引入额外依赖
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/```([\s\S]*?)```/g, '<pre class="code-block"><code>$1</code></pre>')
    .replace(/`([^`]+)`/g, '<code class="inline-code">$1</code>')
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/\n/g, '<br/>')
}

const copyText = (text: string) => {
  navigator.clipboard.writeText(text).then(() => {
    antMessage.success('已复制')
  })
}

const scrollToBottom = async () => {
  await nextTick()
  if (messageListRef.value) {
    messageListRef.value.scrollTop = messageListRef.value.scrollHeight
  }
}

const clearMessages = () => {
  messages.value = []
  sessionStats.totalMessages = 0
  sessionStats.totalTokens = 0
}

// ───── 加载数据 ─────
const loadModels = async () => {
  // 使用固定模型列表（与后端 db.sql 一致）
  models.value = [
    { label: 'Qwen Plus', value: 'qwen-plus', provider: '通义千问', tagColor: 'orange' },
    { label: 'Qwen Max', value: 'qwen-max', provider: '通义千问', tagColor: 'orange' },
    { label: 'Qwen Turbo', value: 'qwen-turbo', provider: '通义千问', tagColor: 'orange' },
    { label: 'GLM-4.7', value: 'glm-4.7', provider: '智谱AI', tagColor: 'green' },
    { label: 'GLM-4.7 Flash', value: 'glm-4.7-flash', provider: '智谱AI', tagColor: 'green' },
    { label: 'DeepSeek Chat', value: 'deepseek-chat', provider: 'DeepSeek', tagColor: 'blue' },
    {
      label: 'DeepSeek Reasoner',
      value: 'deepseek-reasoner',
      provider: 'DeepSeek',
      tagColor: 'blue',
    },
  ]
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
const sendMessage = async () => {
  const text = inputText.value.trim()
  if (!text || !selectedModel.value || !selectedApiKey.value) return
  if (isStreaming.value) return

  // 找到选中 key 的值
  const keyObj = apiKeys.value.find((k) => k.id === selectedApiKey.value)
  if (!keyObj?.keyValue) {
    antMessage.error('无法获取 API Key 值')
    return
  }

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
  })
  inputText.value = ''
  sessionStats.totalMessages++
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

    // 获取当前 API Key 原始值（未掩码），通过 keyValue 字段
    const rawKey = keyObj.keyValue

    const response = await fetch('http://localhost:8123/api/v1/chat/completions', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${rawKey}`,
      },
      body: JSON.stringify(requestBody),
    })

    if (!response.ok) {
      const errText = await response.text()
      throw new Error(`HTTP ${response.status}: ${errText}`)
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
        const trimmed = line.trim()
        if (!trimmed || !trimmed.startsWith('{')) continue

        try {
          const chunk = JSON.parse(trimmed)

          // 提取 delta
          const choices = chunk.choices ?? []
          if (choices.length === 0) continue

          const delta = choices[0]?.delta ?? {}
          const finishReason = choices[0]?.finishReason

          // 普通文本
          if (delta.content) {
            streamingContent.value += delta.content
          }

          // 深度思考内容
          if (delta.reasoningContent) {
            streamingReasoning.value += delta.reasoningContent
          }

          // 更新界面
          messages.value[aiMsgIndex] = {
            ...messages.value[aiMsgIndex],
            content: streamingContent.value,
            reasoning: streamingReasoning.value,
          }

          await scrollToBottom()

          // 结束
          if (finishReason === 'stop') {
            break
          }
        } catch {
          // 跳过无法解析的行
        }
      }
    }

    // 完成：关闭 streaming 状态
    messages.value[aiMsgIndex] = {
      ...messages.value[aiMsgIndex],
      streaming: false,
      tokens: totalTokens,
    }

    sessionStats.totalMessages++
    sessionStats.totalTokens += totalTokens
  } catch (err: unknown) {
    const errMsg = err instanceof Error ? err.message : '未知错误'
    antMessage.error('请求失败：' + errMsg)
    // 移除失败的 AI 消息
    messages.value.splice(aiMsgIndex, 1)
  } finally {
    isStreaming.value = false
    streamingContent.value = ''
    streamingReasoning.value = ''
    await scrollToBottom()
  }
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
  loadModels()
  loadApiKeys()
})
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
