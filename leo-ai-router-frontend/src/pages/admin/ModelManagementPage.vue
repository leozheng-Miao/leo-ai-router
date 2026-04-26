<template>
  <div class="page-shell">
    <div class="page-header">
      <div>
        <div class="page-title">模型管理</div>
        <div class="page-desc">管理模型规格、价格、能力和健康度</div>
      </div>
      <a-button type="primary" size="large" @click="openCreateModal">新增模型</a-button>
    </div>

    <a-card :bordered="false" class="filter-card">
      <a-form layout="vertical" class="filter-form">
        <div class="filter-grid">
          <a-form-item label="模型 Key">
            <a-input v-model:value="query.modelKey" allow-clear placeholder="如 deepseek-chat" />
          </a-form-item>
          <a-form-item label="模型名称">
            <a-input v-model:value="query.modelName" allow-clear placeholder="显示名称" />
          </a-form-item>
          <a-form-item label="提供者">
            <a-select v-model:value="query.providerId" allow-clear placeholder="全部提供者">
              <a-select-option v-for="item in providers" :key="item.id" :value="item.id">
                {{ item.displayName }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="类型">
            <a-select v-model:value="query.modelType" allow-clear placeholder="全部类型">
              <a-select-option v-for="item in modelTypes" :key="item" :value="item">
                {{ item }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="状态">
            <a-select v-model:value="query.status" allow-clear placeholder="全部状态">
              <a-select-option v-for="item in modelStatuses" :key="item.value" :value="item.value">
                {{ item.label }}
              </a-select-option>
            </a-select>
          </a-form-item>
        </div>
        <div class="filter-actions">
          <a-space>
            <a-button type="primary" @click="handleSearch">搜索</a-button>
            <a-button @click="handleReset">重置</a-button>
          </a-space>
        </div>
      </a-form>
    </a-card>

    <a-card :bordered="false">
      <a-table
        row-key="id"
        :columns="columns"
        :data-source="models"
        :loading="loading"
        :pagination="pagination"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'modelKey'">
            <div class="model-name">
              <span class="model-main">{{ record.modelName || record.modelKey }}</span>
              <span class="model-sub">{{ record.modelKey }}</span>
            </div>
          </template>

          <template v-else-if="column.key === 'providerDisplayName'">
            <a-tag color="blue">{{ record.providerDisplayName || record.providerName }}</a-tag>
          </template>

          <template v-else-if="column.key === 'price'">
            <div class="price-block">
              <div class="price-item">
                <span class="price-label">输入</span>
                <span class="price-value">{{ formatPrice(record.inputPrice) }}</span>
              </div>
              <div class="price-item">
                <span class="price-label">输出</span>
                <span class="price-value">{{ formatPrice(record.outputPrice) }}</span>
              </div>
            </div>
          </template>

          <template v-else-if="column.key === 'healthStatus'">
            <a-tag :color="healthMeta(record.healthStatus).color">
              {{ healthMeta(record.healthStatus).label }}
            </a-tag>
          </template>

          <template v-else-if="column.key === 'supportReasoning'">
            <a-tag :color="record.supportReasoning ? 'purple' : 'default'">
              {{ record.supportReasoning ? '支持' : '关闭' }}
            </a-tag>
          </template>

          <template v-else-if="column.key === 'status'">
            <a-tag :color="statusMeta(record.status).color">{{ statusMeta(record.status).label }}</a-tag>
          </template>

          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button type="link" @click="openEditModal(record)">编辑</a-button>
              <a-popconfirm title="确认删除该模型？" @confirm="handleDelete(record)">
                <a-button type="link" danger>删除</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal
      v-model:open="modalOpen"
      :title="editingId ? '编辑模型' : '新增模型'"
      :confirm-loading="saving"
      width="860px"
      @ok="handleSubmit"
    >
      <a-form layout="vertical">
        <a-row :gutter="16">
          <a-col :span="8">
            <a-form-item label="提供者" required>
              <a-select v-model:value="form.providerId" placeholder="请选择">
                <a-select-option v-for="item in providers" :key="item.id" :value="item.id">
                  {{ item.displayName }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="模型 Key" required>
              <a-input v-model:value="form.modelKey" :disabled="Boolean(editingId)" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="模型名称" required>
              <a-input v-model:value="form.modelName" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="模型类型" required>
              <a-select v-model:value="form.modelType">
                <a-select-option v-for="item in modelTypes" :key="item" :value="item">
                  {{ item }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="状态">
              <a-select v-model:value="form.status">
                <a-select-option v-for="item in modelStatuses" :key="item.value" :value="item.value">
                  {{ item.label }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="深度思考">
              <a-switch v-model:checked="supportReasoningBool" checked-children="开" un-checked-children="关" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="输入价格">
              <a-input-number v-model:value="form.inputPrice" style="width: 100%" :min="0" :step="0.01">
                <template #addonBefore>¥</template>
                <template #addonAfter>/1K Token</template>
              </a-input-number>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="输出价格">
              <a-input-number v-model:value="form.outputPrice" style="width: 100%" :min="0" :step="0.01">
                <template #addonBefore>¥</template>
                <template #addonAfter>/1K Token</template>
              </a-input-number>
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="上下文长度">
              <a-input-number v-model:value="form.contextLength" style="width: 100%" :min="0" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="优先级">
              <a-input-number v-model:value="form.priority" style="width: 100%" :min="1" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="默认超时(ms)">
              <a-input-number v-model:value="form.defaultTimeout" style="width: 100%" :min="1000" :step="1000" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="能力标签(JSON数组)">
              <a-input v-model:value="form.capabilities" placeholder='如 ["fast","reasoning"]' />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="描述">
              <a-textarea v-model:value="form.description" :auto-size="{ minRows: 3, maxRows: 6 }" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { addModel, deleteModel, listModelVoByPage, updateModel } from '@/api/modelController'
import { listProviderVo } from '@/api/providerController'

const loading = ref(false)
const saving = ref(false)
const modalOpen = ref(false)
const editingId = ref<number>()
const models = ref<API.ModelVO[]>([])
const providers = ref<API.ProviderVO[]>([])

const modelTypes = ['chat', 'embedding', 'image', 'audio']
const modelStatuses = [
  { label: '启用', value: 'active' },
  { label: '禁用', value: 'inactive' },
  { label: '已废弃', value: 'deprecated' },
]

const query = reactive<API.ModelQueryRequest>({
  pageNum: 1,
  pageSize: 10,
  providerId: undefined,
  modelKey: '',
  modelName: '',
  modelType: undefined,
  status: undefined,
})

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`,
})

const form = reactive<API.ModelAddRequest & API.ModelUpdateRequest>({
  providerId: undefined,
  modelKey: '',
  modelName: '',
  modelType: 'chat',
  description: '',
  contextLength: 8192,
  inputPrice: 0,
  outputPrice: 0,
  status: 'active',
  priority: 100,
  defaultTimeout: 60000,
  capabilities: '[]',
  supportReasoning: 0,
})

const supportReasoningBool = computed({
  get: () => form.supportReasoning === 1,
  set: (value: boolean) => {
    form.supportReasoning = value ? 1 : 0
  },
})

const columns = [
  { title: '模型', key: 'modelKey', width: 220 },
  { title: '提供者', dataIndex: 'providerDisplayName', key: 'providerDisplayName', width: 140 },
  { title: '类型', dataIndex: 'modelType', key: 'modelType', width: 100 },
  { title: '价格', key: 'price', width: 180 },
  { title: '健康', dataIndex: 'healthStatus', key: 'healthStatus', width: 100 },
  { title: '深度思考', dataIndex: 'supportReasoning', key: 'supportReasoning', width: 110 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '操作', key: 'action', width: 140, fixed: 'right' as const },
]

const statusMeta = (status?: string) => {
  const map: Record<string, { label: string; color: string }> = {
    active: { label: '启用', color: 'green' },
    inactive: { label: '禁用', color: 'default' },
    deprecated: { label: '已废弃', color: 'red' },
  }
  return map[status ?? ''] ?? { label: status ?? '未知', color: 'default' }
}

const healthMeta = (status?: string) => {
  const map: Record<string, { label: string; color: string }> = {
    healthy: { label: '健康', color: 'success' },
    degraded: { label: '降级', color: 'warning' },
    unhealthy: { label: '异常', color: 'error' },
    unknown: { label: '未知', color: 'default' },
  }
  return map[status ?? ''] ?? { label: status ?? '未知', color: 'default' }
}

const formatPrice = (price?: number) => {
  if (price === undefined || price === null) {
    return '未配置'
  }
  return `¥${Number(price).toFixed(4)}/1K Token`
}

const resetForm = () => {
  editingId.value = undefined
  form.providerId = undefined
  form.modelKey = ''
  form.modelName = ''
  form.modelType = 'chat'
  form.description = ''
  form.contextLength = 8192
  form.inputPrice = 0
  form.outputPrice = 0
  form.status = 'active'
  form.priority = 100
  form.defaultTimeout = 60000
  form.capabilities = '[]'
  form.supportReasoning = 0
}

const loadProviders = async () => {
  const res = await listProviderVo()
  if (res.data.code === 0) {
    providers.value = res.data.data ?? []
  }
}

const loadModels = async () => {
  loading.value = true
  try {
    const res = await listModelVoByPage({
      ...query,
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
    })
    if (res.data.code === 0 && res.data.data) {
      models.value = res.data.data.records ?? []
      pagination.total = res.data.data.totalRow ?? 0
    } else {
      message.error(res.data.message ?? '加载模型失败')
    }
  } finally {
    loading.value = false
  }
}

const openCreateModal = () => {
  resetForm()
  modalOpen.value = true
}

const openEditModal = (record: API.ModelVO) => {
  editingId.value = record.id
  form.providerId = record.providerId
  form.modelKey = record.modelKey ?? ''
  form.modelName = record.modelName ?? ''
  form.modelType = record.modelType ?? 'chat'
  form.description = record.description ?? ''
  form.contextLength = record.contextLength ?? 8192
  form.inputPrice = record.inputPrice ?? 0
  form.outputPrice = record.outputPrice ?? 0
  form.status = record.status ?? 'active'
  form.priority = record.priority ?? 100
  form.defaultTimeout = record.defaultTimeout ?? 60000
  form.capabilities = record.capabilities ?? '[]'
  form.supportReasoning = record.supportReasoning ?? 0
  modalOpen.value = true
}

const handleSubmit = async () => {
  if (!form.providerId || !form.modelKey?.trim() || !form.modelName?.trim()) {
    message.warning('请完整填写模型信息')
    return
  }

  saving.value = true
  try {
    const payload = {
      providerId: form.providerId,
      modelKey: form.modelKey?.trim(),
      modelName: form.modelName?.trim(),
      modelType: form.modelType,
      description: form.description?.trim(),
      contextLength: form.contextLength,
      inputPrice: form.inputPrice,
      outputPrice: form.outputPrice,
      status: form.status,
      priority: form.priority,
      defaultTimeout: form.defaultTimeout,
      capabilities: form.capabilities?.trim(),
      supportReasoning: form.supportReasoning,
    }

    const res = editingId.value
      ? await updateModel({ id: editingId.value, ...payload })
      : await addModel(payload)

    if (res.data.code === 0) {
      message.success(editingId.value ? '模型更新成功' : '模型创建成功')
      modalOpen.value = false
      await loadModels()
    } else {
      message.error(res.data.message ?? '保存失败')
    }
  } finally {
    saving.value = false
  }
}

const handleDelete = async (record: API.ModelVO) => {
  if (!record.id) {
    return
  }
  const res = await deleteModel({ id: record.id })
  if (res.data.code === 0) {
    message.success('删除成功')
    await loadModels()
  } else {
    message.error(res.data.message ?? '删除失败')
  }
}

const handleSearch = async () => {
  pagination.current = 1
  await loadModels()
}

const handleReset = async () => {
  query.providerId = undefined
  query.modelKey = ''
  query.modelName = ''
  query.modelType = undefined
  query.status = undefined
  pagination.current = 1
  await loadModels()
}

const handleTableChange = (pag: { current: number; pageSize: number }) => {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  void loadModels()
}

onMounted(() => {
  void Promise.all([loadProviders(), loadModels()])
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

.filter-form :deep(.ant-form-item) {
  margin-bottom: 0;
}

.filter-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 14px 16px;
}

.filter-actions {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.model-name {
  display: flex;
  flex-direction: column;
}

.model-main {
  font-weight: 600;
  color: #111827;
}

.model-sub {
  font-size: 12px;
  color: #6b7280;
}

.price-block {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.price-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.price-label {
  display: inline-flex;
  min-width: 34px;
  justify-content: center;
  padding: 2px 6px;
  border-radius: 999px;
  background: #eef2ff;
  color: #4f46e5;
  font-size: 11px;
  font-weight: 600;
}

.price-value {
  font-family: 'JetBrains Mono', monospace;
  font-size: 12px;
  color: #334155;
}

@media (max-width: 1200px) {
  .filter-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .filter-grid {
    grid-template-columns: 1fr;
  }

  .filter-actions {
    justify-content: flex-start;
  }
}
</style>
