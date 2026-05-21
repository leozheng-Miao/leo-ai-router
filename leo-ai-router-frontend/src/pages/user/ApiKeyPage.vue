<template>
  <div class="apikey-page">
    <!-- 页头 -->
    <div class="page-header">
      <div class="page-header-left">
        <h2 class="page-title">API Keys</h2>
        <p class="page-desc">管理你的 API Key，用于调用 LeoAI Router 接口</p>
      </div>
      <a-button type="primary" class="create-btn" @click="showCreateModal">
        <PlusOutlined /> 创建 API Key
      </a-button>
    </div>

    <!-- 提示栏 -->
    <a-alert
      type="info"
      show-icon
      class="tip-alert"
      message="安全提示：API Key 创建后仅显示一次完整值，请妥善保存。列表中仅显示部分掩码值。"
    />

    <!-- 列表 -->
    <a-card :bordered="false" class="table-card">
      <a-table
        :columns="columns"
        :data-source="apiKeys"
        :loading="loading"
        :pagination="pagination"
        :scroll="{ x: 980 }"
        row-key="id"
        @change="handleTableChange"
      >
        <!-- Key 值 -->
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'keyValue'">
            <a-space>
              <code class="key-code">{{ record.keyValue }}</code>
              <a-tooltip title="复制 Key">
                <a-button type="text" size="small" @click="copyKey(record.keyValue)">
                  <CopyOutlined />
                </a-button>
              </a-tooltip>
            </a-space>
          </template>

          <!-- 状态 -->
          <template v-else-if="column.key === 'status'">
            <a-tag :color="statusColor(record.status)">{{ statusLabel(record.status) }}</a-tag>
          </template>

          <!-- Token 用量 -->
          <template v-else-if="column.key === 'totalTokens'">
            <span class="token-count">{{ formatNumber(record.totalTokens ?? 0) }}</span>
          </template>

          <!-- 最后使用 -->
          <template v-else-if="column.key === 'lastUsedTime'">
            <span class="time-text">{{ formatTime(record.lastUsedTime) }}</span>
          </template>

          <!-- 创建时间 -->
          <template v-else-if="column.key === 'createTime'">
            <span class="time-text">{{ formatTime(record.createTime) }}</span>
          </template>

          <!-- 操作 -->
          <template v-else-if="column.key === 'action'">
            <a-popconfirm
              title="确认撤销该 API Key？"
              description="撤销后该 Key 将立即失效，无法恢复。"
              ok-text="确认撤销"
              cancel-text="取消"
              ok-type="danger"
              :disabled="record.status === 'revoked'"
              @confirm="handleRevoke(record)"
            >
              <a-button
                type="link"
                danger
                size="small"
                :disabled="record.status === 'revoked'"
                :loading="revoking === record.id"
              >
                撤销
              </a-button>
            </a-popconfirm>
          </template>
        </template>

        <!-- 空状态 -->
        <template #emptyText>
          <a-empty description="暂无 API Key，点击右上角创建">
            <a-button type="primary" @click="showCreateModal">
              <PlusOutlined /> 创建第一个 API Key
            </a-button>
          </a-empty>
        </template>
      </a-table>
    </a-card>

    <!-- 创建弹窗 -->
    <a-modal
      v-model:open="createModalVisible"
      :title="newApiKey ? 'API Key 创建成功' : '创建 API Key'"
      :footer="null"
      :closable="!creating"
      :mask-closable="!newApiKey"
      :width="480"
      destroy-on-close
      @cancel="handleCreateModalClose"
    >
      <!-- 创建成功展示 Key -->
      <template v-if="newApiKey">
        <a-alert
          type="warning"
          show-icon
          class="modal-alert"
          message="请立即保存，此 Key 关闭后将无法再次查看完整值！"
        />
        <div class="key-display-wrap">
          <div class="key-label">你的 API Key</div>
          <div class="key-display">
            <code class="key-full">{{ newApiKey }}</code>
            <a-button
              type="primary"
              ghost
              size="small"
              class="copy-btn"
              @click="copyKey(newApiKey)"
            >
              <CopyOutlined /> 复制
            </a-button>
          </div>
        </div>
        <div class="key-name-display">
          <span class="kn-label">备注名称：</span>
          <span class="kn-value">{{ createForm.keyName || '（未命名）' }}</span>
        </div>
        <a-divider />
        <div class="modal-footer-row">
          <a-button type="primary" block @click="handleCreateModalClose">我已保存，关闭</a-button>
        </div>
      </template>

      <!-- 创建表单 -->
      <template v-else>
        <a-form :model="createForm" layout="vertical" @finish="handleCreate">
          <a-form-item
            name="keyName"
            label="Key 名称 / 备注"
            :rules="[{ required: true, message: '请输入 Key 名称' }]"
          >
            <a-input
              v-model:value="createForm.keyName"
              placeholder="例如：生产环境、测试用途等"
              size="large"
              :maxlength="64"
              show-count
            />
          </a-form-item>
          <a-form-item style="margin-bottom: 0">
            <a-space style="width: 100%; justify-content: flex-end">
              <a-button @click="handleCreateModalClose">取消</a-button>
              <a-button type="primary" html-type="submit" :loading="creating">创建</a-button>
            </a-space>
          </a-form-item>
        </a-form>
      </template>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, CopyOutlined } from '@ant-design/icons-vue'
import { listMyApiKeys, createApiKey, revokeApiKey } from '@/api/apiKeyController'

// ───── 状态 ─────
const apiKeys = ref<API.ApiKeyVO[]>([])
const loading = ref(false)
const createModalVisible = ref(false)
const creating = ref(false)
const newApiKey = ref('')
const revoking = ref<number | null>(null)

const createForm = reactive({ keyName: '' })

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`,
})

// ───── 表格列 ─────
const columns = [
  {
    title: '名称 / 备注',
    dataIndex: 'keyName',
    key: 'keyName',
    width: 140,
    ellipsis: true,
  },
  {
    title: 'Key 值',
    dataIndex: 'keyValue',
    key: 'keyValue',
    width: 240,
  },
  {
    title: '状态',
    dataIndex: 'status',
    key: 'status',
    width: 90,
  },
  {
    title: 'Token 用量',
    dataIndex: 'totalTokens',
    key: 'totalTokens',
    width: 120,
    align: 'right' as const,
  },
  {
    title: '最后使用',
    dataIndex: 'lastUsedTime',
    key: 'lastUsedTime',
    width: 160,
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    key: 'createTime',
    width: 160,
  },
  {
    title: '操作',
    key: 'action',
    width: 80,
    fixed: 'right' as const,
  },
]

// ───── 工具函数 ─────
const statusColor = (status?: string) => {
  if (status === 'active') return 'green'
  if (status === 'revoked') return 'red'
  return 'default'
}

const statusLabel = (status?: string) => {
  if (status === 'active') return '正常'
  if (status === 'revoked') return '已撤销'
  if (status === 'inactive') return '未激活'
  return status ?? '-'
}

const formatNumber = (n: number) => n.toLocaleString()

const formatTime = (t?: string) => {
  if (!t) return '-'
  return new Date(t).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

const copyKey = (val?: string) => {
  if (!val) return
  navigator.clipboard
    .writeText(val)
    .then(() => {
      message.success('已复制到剪贴板')
    })
    .catch(() => {
      message.error('复制失败，请手动复制')
    })
}

// ───── 加载列表 ─────
const loadApiKeys = async (page = 1, pageSize = 10) => {
  loading.value = true
  try {
    const res = await listMyApiKeys({ pageNum: page, pageSize })
    if (res.data.code === 0 && res.data.data) {
      apiKeys.value = res.data.data.records ?? []
      pagination.total = res.data.data.totalRow ?? 0
      pagination.current = page
      pagination.pageSize = pageSize
    } else {
      message.error(res.data.message ?? '加载失败')
    }
  } catch {
    message.error('网络错误，请稍后重试')
  } finally {
    loading.value = false
  }
}

const handleTableChange = (pag: { current: number; pageSize: number }) => {
  loadApiKeys(pag.current, pag.pageSize)
}

// ───── 创建 ─────
const showCreateModal = () => {
  createForm.keyName = ''
  newApiKey.value = ''
  createModalVisible.value = true
}

const handleCreate = async () => {
  creating.value = true
  try {
    const res = await createApiKey({ keyName: createForm.keyName.trim() })
    if (res.data.code === 0 && res.data.data) {
      newApiKey.value = res.data.data.keyValue ?? ''
      await loadApiKeys(1, pagination.pageSize)
    } else {
      message.error(res.data.message ?? '创建失败')
    }
  } catch {
    message.error('网络错误，请稍后重试')
  } finally {
    creating.value = false
  }
}

const handleCreateModalClose = () => {
  createModalVisible.value = false
  newApiKey.value = ''
  createForm.keyName = ''
}

// ───── 撤销 ─────
const handleRevoke = async (record: API.ApiKeyVO) => {
  if (!record.id) return
  revoking.value = record.id
  try {
    const res = await revokeApiKey({ id: record.id })
    if (res.data.code === 0) {
      message.success('已成功撤销')
      await loadApiKeys(pagination.current, pagination.pageSize)
    } else {
      message.error(res.data.message ?? '撤销失败')
    }
  } catch {
    message.error('网络错误，请稍后重试')
  } finally {
    revoking.value = null
  }
}

// ───── 初始化 ─────
onMounted(() => loadApiKeys())
</script>

<style scoped>
.apikey-page {
  width: 100%;
  padding: 0;
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 16px;
  gap: 14px;
  flex-wrap: wrap;
}

.page-title {
  font-size: 22px;
  font-weight: 800;
  color: var(--leo-text-primary);
  margin: 0 0 4px;
  line-height: 1.25;
}

.page-desc {
  font-size: 14px;
  color: var(--leo-text-secondary);
  margin: 0;
}

.create-btn {
  flex-shrink: 0;
  height: 38px;
  padding: 0 18px;
  border-radius: var(--leo-radius-md);
  font-weight: 700;
  background: var(--leo-primary);
  border: none;
}

.create-btn:hover {
  background: var(--leo-primary-hover);
}

.tip-alert {
  margin-bottom: 14px;
  border: 1px solid var(--leo-border);
  border-radius: var(--leo-radius-md);
}

.table-card {
  border: 1px solid var(--leo-border);
  border-radius: var(--leo-radius-md);
  background: var(--leo-bg-panel);
}

:deep(.ant-card-body) {
  padding: 0;
}

:deep(.ant-table-thead > tr > th) {
  background: var(--leo-bg-muted);
  font-size: 13px;
  font-weight: 700;
  color: var(--leo-text-secondary);
  border-bottom: 1px solid var(--leo-border);
}

:deep(.ant-table-tbody > tr > td) {
  font-size: 13px;
  color: var(--leo-text-primary);
}

:deep(.ant-table-tbody > tr:hover > td) {
  background: var(--leo-bg-muted) !important;
}

.key-code {
  font-family: 'JetBrains Mono', 'Fira Code', monospace;
  font-size: 12px;
  background: var(--leo-bg-muted);
  color: var(--leo-text-primary);
  padding: 2px 8px;
  border-radius: 4px;
  border: 1px solid var(--leo-border);
  letter-spacing: 0;
}

.token-count {
  font-family: 'JetBrains Mono', monospace;
  font-size: 13px;
  color: var(--leo-primary);
  font-weight: 500;
}

.time-text {
  color: var(--leo-text-tertiary);
  font-size: 12px;
}

.modal-alert {
  margin-bottom: 16px;
  border-radius: var(--leo-radius-md);
}

.key-display-wrap {
  background: var(--leo-bg-muted);
  border: 1px solid var(--leo-border-strong);
  border-radius: var(--leo-radius-md);
  padding: 16px;
  margin-bottom: 12px;
}

.key-label {
  font-size: 12px;
  font-weight: 700;
  color: var(--leo-text-secondary);
  margin-bottom: 10px;
  text-transform: uppercase;
  letter-spacing: 0;
}

.key-display {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.key-full {
  font-family: 'JetBrains Mono', 'Fira Code', monospace;
  font-size: 13px;
  color: var(--leo-primary);
  word-break: break-all;
  flex: 1;
  background: transparent;
  padding: 0;
  border: none;
}

.copy-btn {
  flex-shrink: 0;
  border-radius: var(--leo-radius-sm);
}

.key-name-display {
  font-size: 13px;
  color: var(--leo-text-secondary);
  margin-bottom: 4px;
}

.kn-label {
  color: var(--leo-text-tertiary);
}

.kn-value {
  color: var(--leo-text-primary);
  font-weight: 500;
}

.modal-footer-row {
  margin-top: 4px;
}

@media (max-width: 640px) {
  .page-header-left,
  .create-btn {
    width: 100%;
  }
}
</style>
