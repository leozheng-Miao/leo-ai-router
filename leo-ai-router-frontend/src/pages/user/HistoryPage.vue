<template>
  <div class="page-shell">
    <div class="page-header">
      <div class="page-title">调度历史</div>
      <div class="page-desc">查看请求日志、路由策略和详细调用信息</div>
    </div>

    <a-card :bordered="false" class="filter-card">
      <a-form layout="vertical">
        <div class="filter-grid">
          <a-form-item label="模型标识">
            <a-input v-model:value="query.requestModel" allow-clear />
          </a-form-item>
          <a-form-item label="请求类型">
            <a-select v-model:value="query.requestType" allow-clear>
              <a-select-option value="chat">chat</a-select-option>
              <a-select-option value="embedding">embedding</a-select-option>
              <a-select-option value="image">image</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="来源">
            <a-select v-model:value="query.source" allow-clear>
              <a-select-option value="web">web</a-select-option>
              <a-select-option value="api">api</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="状态">
            <a-select v-model:value="query.status" allow-clear>
              <a-select-option value="success">success</a-select-option>
              <a-select-option value="failed">failed</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="开始日期">
            <input v-model="query.startDate" type="date" class="date-input" />
          </a-form-item>
          <a-form-item label="结束日期">
            <input v-model="query.endDate" type="date" class="date-input" />
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
      <a-table row-key="id" :columns="columns" :data-source="logs" :loading="loading" :pagination="pagination" @change="handleTableChange">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'requestModel'">
            <div class="model-cell">
              <span class="model-main">{{ record.requestModel || '-' }}</span>
              <span class="model-sub">{{ record.traceId }}</span>
            </div>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="record.status === 'success' ? 'green' : 'red'">{{ record.status }}</a-tag>
          </template>
          <template v-else-if="column.key === 'tokens'">
            {{ (record.totalTokens ?? 0).toLocaleString('zh-CN') }}
          </template>
          <template v-else-if="column.key === 'cost'">
            ¥{{ Number(record.cost ?? 0).toFixed(4) }}
          </template>
          <template v-else-if="column.key === 'createTime'">
            {{ formatTime(record.createTime) }}
          </template>
          <template v-else-if="column.key === 'action'">
            <a-button type="link" @click="openDetail(record)">详情</a-button>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:open="detailOpen" title="请求详情" width="860px" :footer="null">
      <a-descriptions bordered :column="2" size="small" v-if="detailRecord">
        <a-descriptions-item label="Trace ID">{{ detailRecord.traceId || '-' }}</a-descriptions-item>
        <a-descriptions-item label="模型">{{ detailRecord.requestModel || '-' }}</a-descriptions-item>
        <a-descriptions-item label="请求类型">{{ detailRecord.requestType || '-' }}</a-descriptions-item>
        <a-descriptions-item label="来源">{{ detailRecord.source || '-' }}</a-descriptions-item>
        <a-descriptions-item label="状态">{{ detailRecord.status || '-' }}</a-descriptions-item>
        <a-descriptions-item label="路由策略">{{ detailRecord.routingStrategy || '-' }}</a-descriptions-item>
        <a-descriptions-item label="耗时">{{ detailRecord.duration ?? 0 }} ms</a-descriptions-item>
        <a-descriptions-item label="总 Token">{{ detailRecord.totalTokens ?? 0 }}</a-descriptions-item>
        <a-descriptions-item label="输入 Token">{{ detailRecord.promptTokens ?? 0 }}</a-descriptions-item>
        <a-descriptions-item label="输出 Token">{{ detailRecord.completionTokens ?? 0 }}</a-descriptions-item>
        <a-descriptions-item label="费用">¥{{ Number(detailRecord.cost ?? 0).toFixed(4) }}</a-descriptions-item>
        <a-descriptions-item label="请求时间">{{ formatTime(detailRecord.createTime) }}</a-descriptions-item>
        <a-descriptions-item label="客户端 IP" :span="2">{{ detailRecord.clientIp || '-' }}</a-descriptions-item>
        <a-descriptions-item label="User-Agent" :span="2">{{ detailRecord.userAgent || '-' }}</a-descriptions-item>
        <a-descriptions-item label="错误码" :span="2">{{ detailRecord.errorCode || '-' }}</a-descriptions-item>
        <a-descriptions-item label="错误信息" :span="2">{{ detailRecord.errorMessage || '-' }}</a-descriptions-item>
      </a-descriptions>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { getHistoryDetail, pageHistory, pageMyHistory } from '@/api/statsController'
import { useLoginUserStore } from '@/stores/loginUser'

const loginUserStore = useLoginUserStore()
const loading = ref(false)
const detailOpen = ref(false)
const detailRecord = ref<API.RequestLog>()
const logs = ref<API.RequestLog[]>([])

const query = reactive<API.RequestLogQueryRequest>({
  pageNum: 1,
  pageSize: 10,
  requestModel: '',
  requestType: undefined,
  source: undefined,
  status: undefined,
  startDate: '',
  endDate: '',
})

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`,
})

const columns = [
  { title: '模型 / Trace', key: 'requestModel', width: 240 },
  { title: '类型', dataIndex: 'requestType', key: 'requestType', width: 100 },
  { title: '来源', dataIndex: 'source', key: 'source', width: 90 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 100 },
  { title: '耗时', dataIndex: 'duration', key: 'duration', width: 100 },
  { title: 'Token', key: 'tokens', width: 110 },
  { title: '费用', key: 'cost', width: 120 },
  { title: '时间', key: 'createTime', width: 180 },
  { title: '操作', key: 'action', width: 90 },
]

const formatTime = (value?: string) => {
  if (!value) return '-'
  return new Date(value).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

const loadHistory = async () => {
  loading.value = true
  try {
    const payload = {
      ...query,
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
      startDate: query.startDate || undefined,
      endDate: query.endDate || undefined,
    }
    const res = loginUserStore.loginUser.userRole === 'admin' ? await pageHistory(payload) : await pageMyHistory(payload)
    if (res.data.code === 0 && res.data.data) {
      logs.value = res.data.data.records ?? []
      pagination.total = res.data.data.totalRow ?? 0
    } else {
      message.error(res.data.message ?? '加载调度历史失败')
    }
  } finally {
    loading.value = false
  }
}

const openDetail = async (record: API.RequestLog) => {
  if (!record.id) return
  const res = await getHistoryDetail({ id: record.id })
  if (res.data.code === 0) {
    detailRecord.value = res.data.data
    detailOpen.value = true
  } else {
    message.error(res.data.message ?? '加载详情失败')
  }
}

const handleSearch = async () => {
  pagination.current = 1
  await loadHistory()
}

const handleReset = async () => {
  query.requestModel = ''
  query.requestType = undefined
  query.source = undefined
  query.status = undefined
  query.startDate = ''
  query.endDate = ''
  pagination.current = 1
  await loadHistory()
}

const handleTableChange = (pag: { current: number; pageSize: number }) => {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  void loadHistory()
}

onMounted(() => {
  void loadHistory()
})
</script>

<style scoped>
.page-shell { max-width: 1280px; margin: 0 auto; padding: 28px 24px 40px; }
.page-header { margin-bottom: 20px; }
.page-title { font-size: 24px; font-weight: 700; color: #111827; }
.page-desc { margin-top: 6px; color: #6b7280; }
.filter-card { margin-bottom: 16px; }
.filter-grid { display: grid; grid-template-columns: repeat(6, minmax(0, 1fr)); gap: 14px 16px; }
.filter-actions { margin-top: 16px; display: flex; justify-content: flex-end; }
.date-input { width: 100%; height: 32px; padding: 0 10px; border-radius: 8px; border: 1px solid #dbe4f0; }
.model-cell { display: flex; flex-direction: column; }
.model-main { color: #111827; font-weight: 600; }
.model-sub { font-size: 12px; color: #94a3b8; }
@media (max-width: 1200px) { .filter-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); } }
@media (max-width: 768px) { .filter-grid { grid-template-columns: 1fr; } .filter-actions { justify-content: flex-start; } }
</style>
