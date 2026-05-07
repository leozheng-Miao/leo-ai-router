<template>
  <div class="page-shell">
    <div class="page-header">
      <div>
        <div class="page-title">模型提供者管理</div>
        <div class="page-desc">管理上游模型服务商、健康状态和连接配置</div>
      </div>
      <a-button type="primary" size="large" @click="openCreateModal">新增提供者</a-button>
    </div>

    <a-card :bordered="false" class="filter-card">
      <a-form layout="inline">
        <a-form-item label="提供者">
          <a-input v-model:value="query.providerName" allow-clear placeholder="providerName" />
        </a-form-item>
        <a-form-item label="显示名称">
          <a-input v-model:value="query.displayName" allow-clear placeholder="displayName" />
        </a-form-item>
        <a-form-item label="状态">
          <a-select v-model:value="query.status" allow-clear style="width: 140px">
            <a-select-option v-for="item in providerStatuses" :key="item.value" :value="item.value">
              {{ item.label }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="健康状态">
          <a-select v-model:value="query.healthStatus" allow-clear style="width: 140px">
            <a-select-option v-for="item in healthStatuses" :key="item.value" :value="item.value">
              {{ item.label }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button type="primary" @click="handleSearch">搜索</a-button>
            <a-button @click="handleReset">重置</a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </a-card>

    <a-card :bordered="false">
      <a-table
        row-key="id"
        :columns="columns"
        :data-source="providers"
        :loading="loading"
        :pagination="pagination"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'providerName'">
            <div class="provider-name">
              <span class="provider-main">{{ record.displayName }}</span>
              <span class="provider-sub">{{ record.providerName }}</span>
            </div>
          </template>

          <template v-else-if="column.key === 'status'">
            <a-tag :color="statusMeta(record.status).color">{{ statusMeta(record.status).label }}</a-tag>
          </template>

          <template v-else-if="column.key === 'healthStatus'">
            <div class="health-block">
              <a-tag :color="healthMeta(record.healthStatus).color">
                {{ healthMeta(record.healthStatus).label }}
              </a-tag>
              <div class="health-metrics">
                <span :class="['health-dot', healthMeta(record.healthStatus).className]"></span>
                <span>{{ record.avgLatency ?? '--' }} ms</span>
                <span>成功率 {{ formatPercent(record.successRate) }}</span>
              </div>
            </div>
          </template>

          <template v-else-if="column.key === 'priority'">
            <span class="mono">{{ record.priority ?? '-' }}</span>
          </template>

          <template v-else-if="column.key === 'createTime'">
            {{ formatTime(record.createTime) }}
          </template>

          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button type="link" @click="openEditModal(record)">编辑</a-button>
              <a-popconfirm title="确认删除该提供者？" @confirm="handleDelete(record)">
                <a-button type="link" danger>删除</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal
      v-model:open="modalOpen"
      :title="editingId ? '编辑提供者' : '新增提供者'"
      :confirm-loading="saving"
      width="720px"
      @ok="handleSubmit"
    >
      <a-form layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="提供者标识" required>
              <a-input
                v-model:value="form.providerName"
                :disabled="Boolean(editingId)"
                placeholder="如 openai / deepseek / qwen / zhipu"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="显示名称" required>
              <a-input v-model:value="form.displayName" placeholder="如 DeepSeek" />
            </a-form-item>
          </a-col>
          <a-col :span="16">
            <a-form-item label="Base URL" required>
              <a-input v-model:value="form.baseUrl" placeholder="https://api.example.com/v1" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="优先级">
              <a-input-number v-model:value="form.priority" style="width: 100%" :min="1" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="状态">
              <a-select v-model:value="form.status">
                <a-select-option v-for="item in providerStatuses" :key="item.value" :value="item.value">
                  {{ item.label }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="API Key">
              <a-input-password v-model:value="form.apiKey" placeholder="编辑时不填则保持原值" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="扩展配置">
              <a-textarea v-model:value="form.config" :auto-size="{ minRows: 4, maxRows: 8 }" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import {
  addProvider,
  deleteProvider,
  listProviderVoByPage,
  updateProvider,
} from '@/api/modelProviderController'

const providerStatuses = [
  { label: '启用', value: 'active' },
  { label: '禁用', value: 'inactive' },
  { label: '维护中', value: 'maintenance' },
]

const healthStatuses = [
  { label: '健康', value: 'healthy' },
  { label: '降级', value: 'degraded' },
  { label: '异常', value: 'unhealthy' },
  { label: '未知', value: 'unknown' },
]

const loading = ref(false)
const saving = ref(false)
const modalOpen = ref(false)
const editingId = ref<number>()
const providers = ref<API.ProviderVO[]>([])

const query = reactive<API.ProviderQueryRequest>({
  pageNum: 1,
  pageSize: 10,
  providerName: '',
  displayName: '',
  status: undefined,
  healthStatus: undefined,
})

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`,
})

const form = reactive<API.ProviderAddRequest & API.ProviderUpdateRequest>({
  providerName: '',
  displayName: '',
  baseUrl: '',
  apiKey: '',
  status: 'active',
  priority: 100,
  config: '',
})

const columns = [
  { title: '提供者', key: 'providerName', width: 220 },
  { title: 'Base URL', dataIndex: 'baseUrl', key: 'baseUrl', ellipsis: true },
  { title: '状态', dataIndex: 'status', key: 'status', width: 110 },
  { title: '健康状态', dataIndex: 'healthStatus', key: 'healthStatus', width: 220 },
  { title: '优先级', dataIndex: 'priority', key: 'priority', width: 90 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 180 },
  { title: '操作', key: 'action', width: 140, fixed: 'right' as const },
]

const statusMeta = (status?: string) => {
  const map: Record<string, { label: string; color: string }> = {
    active: { label: '启用', color: 'green' },
    inactive: { label: '禁用', color: 'default' },
    maintenance: { label: '维护中', color: 'orange' },
  }
  return map[status ?? ''] ?? { label: status ?? '未知', color: 'default' }
}

const healthMeta = (status?: string) => {
  const map: Record<string, { label: string; color: string; className: string }> = {
    healthy: { label: '健康', color: 'success', className: 'health-green' },
    degraded: { label: '降级', color: 'warning', className: 'health-orange' },
    unhealthy: { label: '异常', color: 'error', className: 'health-red' },
    unknown: { label: '未知', color: 'default', className: 'health-gray' },
  }
  return map[status ?? ''] ?? { label: status ?? '未知', color: 'default', className: 'health-gray' }
}

const formatPercent = (value?: number) => {
  if (value === undefined || value === null) {
    return '--'
  }
  return `${Number(value).toFixed(1)}%`
}

const formatTime = (value?: string) => {
  if (!value) {
    return '-'
  }
  return new Date(value).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

const resetForm = () => {
  editingId.value = undefined
  form.providerName = ''
  form.displayName = ''
  form.baseUrl = ''
  form.apiKey = ''
  form.status = 'active'
  form.priority = 100
  form.config = ''
}

const loadProviders = async () => {
  loading.value = true
  try {
    const res = await listProviderVoByPage({
      ...query,
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
    })
    if (res.data.code === 0 && res.data.data) {
      providers.value = res.data.data.records ?? []
      pagination.total = res.data.data.totalRow ?? 0
    } else {
      message.error(res.data.message ?? '加载提供者失败')
    }
  } finally {
    loading.value = false
  }
}

const openCreateModal = () => {
  resetForm()
  modalOpen.value = true
}

const openEditModal = (record: API.ProviderVO) => {
  editingId.value = record.id
  form.providerName = record.providerName ?? ''
  form.displayName = record.displayName ?? ''
  form.baseUrl = record.baseUrl ?? ''
  form.apiKey = ''
  form.status = record.status ?? 'active'
  form.priority = record.priority ?? 100
  form.config = record.config ?? ''
  modalOpen.value = true
}

const handleSubmit = async () => {
  if (!form.providerName?.trim() || !form.displayName?.trim() || !form.baseUrl?.trim()) {
    message.warning('请完整填写提供者信息')
    return
  }

  saving.value = true
  try {
    const payload = {
      providerName: form.providerName?.trim(),
      displayName: form.displayName?.trim(),
      baseUrl: form.baseUrl?.trim(),
      apiKey: form.apiKey?.trim(),
      status: form.status,
      priority: form.priority,
      config: form.config?.trim(),
    }

    const res = editingId.value
      ? await updateProvider({ id: editingId.value, ...payload })
      : await addProvider(payload)

    if (res.data.code === 0) {
      message.success(editingId.value ? '更新成功' : '创建成功')
      modalOpen.value = false
      await loadProviders()
    } else {
      message.error(res.data.message ?? '保存失败')
    }
  } finally {
    saving.value = false
  }
}

const handleDelete = async (record: API.ProviderVO) => {
  if (!record.id) {
    return
  }
  const res = await deleteProvider({ id: record.id })
  if (res.data.code === 0) {
    message.success('删除成功')
    await loadProviders()
  } else {
    message.error(res.data.message ?? '删除失败')
  }
}

const handleSearch = async () => {
  pagination.current = 1
  await loadProviders()
}

const handleReset = async () => {
  query.providerName = ''
  query.displayName = ''
  query.status = undefined
  query.healthStatus = undefined
  pagination.current = 1
  await loadProviders()
}

const handleTableChange = (pag: { current: number; pageSize: number }) => {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  void loadProviders()
}

onMounted(() => {
  void loadProviders()
})
</script>

<style scoped>
.page-shell {
  max-width: 1280px;
  margin: 0 auto;
  padding: 28px 24px 40px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  gap: 16px;
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  color: #111827;
}

.page-desc {
  margin-top: 6px;
  color: #6b7280;
}

.filter-card {
  margin-bottom: 16px;
}

.provider-name {
  display: flex;
  flex-direction: column;
}

.provider-main {
  font-weight: 600;
  color: #111827;
}

.provider-sub {
  font-size: 12px;
  color: #6b7280;
}

.health-block {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.health-metrics {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #6b7280;
}

.health-dot {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  display: inline-block;
}

.health-green {
  background: #22c55e;
  box-shadow: 0 0 0 4px rgba(34, 197, 94, 0.12);
}

.health-orange {
  background: #f59e0b;
  box-shadow: 0 0 0 4px rgba(245, 158, 11, 0.12);
}

.health-red {
  background: #ef4444;
  box-shadow: 0 0 0 4px rgba(239, 68, 68, 0.12);
}

.health-gray {
  background: #94a3b8;
  box-shadow: 0 0 0 4px rgba(148, 163, 184, 0.12);
}

.mono {
  font-family: 'JetBrains Mono', monospace;
}
</style>
