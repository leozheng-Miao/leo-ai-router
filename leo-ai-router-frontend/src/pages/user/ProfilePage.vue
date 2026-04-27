<template>
  <div class="page-shell">
    <div class="page-header">
      <div class="page-title">个人中心</div>
      <div class="page-desc">查看配额信息、Token 消耗、费用统计和每日趋势</div>
    </div>

    <div class="summary-grid">
      <a-card v-for="card in summaryCards" :key="card.title" :bordered="false" class="summary-card">
        <div class="summary-title">{{ card.title }}</div>
        <div class="summary-value">{{ card.value }}</div>
        <div class="summary-extra">{{ card.extra }}</div>
      </a-card>
    </div>

    <a-card :bordered="false" class="chart-card">
      <div class="chart-head">
        <div>
          <div class="card-title">每日消耗趋势</div>
          <div class="card-desc">左轴显示 Token 与请求数，右轴显示费用</div>
        </div>
        <div class="date-range">
          <label class="date-item">
            <span>开始日期</span>
            <input v-model="dateRange.startDate" type="date" class="date-input" />
          </label>
          <label class="date-item">
            <span>结束日期</span>
            <input v-model="dateRange.endDate" type="date" class="date-input" />
          </label>
          <a-space>
            <a-button type="primary" @click="loadDailyStats">更新趋势</a-button>
            <a-button @click="resetDateRange">最近 7 天</a-button>
          </a-space>
        </div>
      </div>
      <DailyStatsTrendChart :data="dailyStats" />
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import DailyStatsTrendChart from '@/components/DailyStatsTrendChart.vue'
import { getMyDailyStats, getMySummaryStats } from '@/api/statsController'
import { getMyQuota } from '@/api/userController'

interface DailyStatPoint {
  date: string
  totalTokens: number
  requestCount: number
  successCount: number
  totalCost: number
}

const summary = ref<API.UserSummaryStatsVO>({})
const quota = ref<API.QuotaVO>({})
const dailyStats = ref<DailyStatPoint[]>([])

const createDefaultRange = () => {
  const end = new Date()
  const start = new Date()
  start.setDate(end.getDate() - 6)
  return {
    startDate: start.toISOString().slice(0, 10),
    endDate: end.toISOString().slice(0, 10),
  }
}

const dateRange = reactive(createDefaultRange())

const formatNumber = (value?: number) => (value ?? 0).toLocaleString('zh-CN')
const formatCurrency = (value?: number) => `¥${Number(value ?? 0).toFixed(2)}`
const formatQuota = (value?: number) => (value === -1 ? '无限制' : formatNumber(value))

const summaryCards = computed(() => [
  {
    title: 'Token 配额',
    value: formatQuota(quota.value.tokenQuota),
    extra: `已用 ${formatNumber(quota.value.usedTokens)} / 剩余 ${formatQuota(quota.value.remainingQuota)}`,
  },
  {
    title: '累计 Token',
    value: formatNumber(summary.value.totalTokens),
    extra: `成功请求 ${formatNumber(summary.value.successRequests)} / ${formatNumber(summary.value.totalRequests)}`,
  },
  {
    title: '累计费用',
    value: formatCurrency(summary.value.totalCost),
    extra: `今日消费 ${formatCurrency(summary.value.todayCost)}`,
  },
  {
    title: '请求总数',
    value: formatNumber(summary.value.totalRequests),
    extra: `成功率 ${summary.value.totalRequests ? (((summary.value.successRequests ?? 0) / (summary.value.totalRequests ?? 1)) * 100).toFixed(1) : '0.0'}%`,
  },
])

const loadSummary = async () => {
  const [summaryRes, quotaRes] = await Promise.all([getMySummaryStats(), getMyQuota()])
  if (summaryRes.data.code === 0) {
    summary.value = summaryRes.data.data ?? {}
  }
  if (quotaRes.data.code === 0) {
    quota.value = quotaRes.data.data ?? {}
  }
}

const loadDailyStats = async () => {
  if (!dateRange.startDate || !dateRange.endDate) {
    message.warning('请选择完整的日期范围')
    return
  }
  const res = await getMyDailyStats({
    startDate: dateRange.startDate,
    endDate: dateRange.endDate,
  })
  if (res.data.code === 0) {
    dailyStats.value = (res.data.data ?? []).map((item) => ({
      date: String(item.date ?? ''),
      totalTokens: Number(item.totalTokens ?? 0),
      requestCount: Number(item.requestCount ?? 0),
      successCount: Number(item.successCount ?? 0),
      totalCost: Number(item.totalCost ?? 0),
    }))
  } else {
    message.error(res.data.message ?? '加载趋势失败')
  }
}

const resetDateRange = () => {
  Object.assign(dateRange, createDefaultRange())
  void loadDailyStats()
}

onMounted(() => {
  void Promise.all([loadSummary(), loadDailyStats()])
})
</script>

<style scoped>
.page-shell { max-width: 1280px; margin: 0 auto; padding: 28px 24px 40px; }
.page-header { margin-bottom: 20px; }
.page-title { font-size: 24px; font-weight: 700; color: #111827; }
.page-desc { margin-top: 6px; color: #6b7280; }
.summary-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 16px; margin-bottom: 18px; }
.summary-card { border-radius: 18px; background: linear-gradient(180deg, #ffffff, #f8fbff); }
.summary-title { color: #64748b; font-size: 13px; }
.summary-value { margin-top: 10px; font-size: 28px; font-weight: 700; color: #0f172a; }
.summary-extra { margin-top: 10px; font-size: 12px; color: #94a3b8; }
.chart-card { border-radius: 18px; }
.chart-head { display: flex; justify-content: space-between; gap: 16px; margin-bottom: 20px; flex-wrap: wrap; }
.card-title { font-size: 18px; font-weight: 700; color: #0f172a; }
.card-desc { margin-top: 4px; color: #6b7280; }
.date-range { display: flex; align-items: end; gap: 12px; flex-wrap: wrap; }
.date-item { display: flex; flex-direction: column; gap: 6px; font-size: 12px; color: #64748b; }
.date-input { min-width: 160px; height: 38px; padding: 0 12px; border-radius: 10px; border: 1px solid #dbe4f0; background: #fff; }
@media (max-width: 960px) { .summary-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
@media (max-width: 640px) { .summary-grid { grid-template-columns: 1fr; } }
</style>
