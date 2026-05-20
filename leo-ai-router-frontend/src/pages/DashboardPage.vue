<template>
  <main class="leo-page dashboard-page">
    <header class="dashboard-header">
      <div>
        <p class="dashboard-header__eyebrow">Control Console</p>
        <h1 class="dashboard-header__title">控制台总览</h1>
        <p class="dashboard-header__subtitle">查看模型接入状态、调用趋势和当前会员权益使用情况</p>
      </div>
      <div class="dashboard-header__status" :class="{ 'dashboard-header__status--loading': loading }">
        {{ loading ? '同步中' : '数据已就绪' }}
      </div>
    </header>

    <ErrorPanel v-if="error" :message="error" class="dashboard-error" @retry="loadDashboard" />

    <section class="metric-grid" aria-label="控制台指标">
      <MetricCard
        v-for="metric in metrics"
        :key="metric.label"
        :label="metric.label"
        :value="metric.value"
        :trend="metric.trend"
        :trend-tone="metric.trendTone"
      />
    </section>

    <section class="dashboard-layout">
      <div class="dashboard-layout__main">
        <AppPanel>
          <div class="panel-head">
            <div>
              <h2 class="panel-title">模型健康状态</h2>
              <p class="panel-desc">当前账号可见模型的可用状态与服务质量</p>
            </div>
            <span class="panel-count">{{ formatNumber(models.length) }} 个模型</span>
          </div>
          <div v-if="models.length" class="model-grid">
            <ModelStatusCard
              v-for="model in models"
              :key="model.id ?? model.modelKey ?? model.modelName"
              :name="formatModelName(model)"
              :success-rate="formatPercent(model.successRate)"
              :latency="formatLatency(model.avgLatency)"
              :status="formatModelStatus(model)"
            />
          </div>
          <EmptyState v-else title="暂无可用模型" description="当前账号暂未获取到可接入的模型状态。" />
        </AppPanel>

        <AppPanel>
          <div class="panel-head">
            <div>
              <h2 class="panel-title">请求趋势</h2>
              <p class="panel-desc">最近 7 天 Token、请求量与费用变化</p>
            </div>
            <span class="panel-count">{{ startDate }} - {{ endDate }}</span>
          </div>
          <DailyStatsTrendChart v-if="hasDailyStats" :data="dailyStats" />
          <EmptyState v-else title="暂无趋势数据" description="最近 7 天还没有可展示的调用统计。" />
        </AppPanel>
      </div>

      <aside class="dashboard-layout__side">
        <AppPanel>
          <div class="panel-head panel-head--compact">
            <div>
              <h2 class="panel-title">会员与用量</h2>
              <p class="panel-desc">套餐权益、剩余额度和积分余额</p>
            </div>
          </div>
          <dl class="usage-list">
            <div class="usage-item">
              <dt>当前套餐</dt>
              <dd>{{ membership.planName || '免费版' }}</dd>
            </div>
            <div class="usage-item">
              <dt>Pro 剩余额度</dt>
              <dd>{{ formatQuota(membership.dailyProRemaining) }}</dd>
            </div>
            <div class="usage-item">
              <dt>Advanced 剩余额度</dt>
              <dd>{{ formatQuota(membership.dailyAdvancedRemaining) }}</dd>
            </div>
            <div class="usage-item">
              <dt>积分余额</dt>
              <dd>{{ formatNumber(membership.pointBalance) }}</dd>
            </div>
          </dl>
        </AppPanel>

        <AppPanel>
          <div class="panel-head panel-head--compact">
            <div>
              <h2 class="panel-title">调用摘要</h2>
              <p class="panel-desc">基于当前可用统计字段汇总</p>
            </div>
          </div>
          <dl v-if="hasSummary" class="summary-list">
            <div class="summary-item">
              <dt>累计 Token</dt>
              <dd>{{ formatNumber(summary.totalTokens) }}</dd>
            </div>
            <div class="summary-item">
              <dt>累计费用</dt>
              <dd>{{ formatCurrency(summary.totalCost) }}</dd>
            </div>
            <div class="summary-item">
              <dt>今日费用</dt>
              <dd>{{ formatCurrency(summary.todayCost) }}</dd>
            </div>
            <div class="summary-item">
              <dt>成功请求</dt>
              <dd>{{ formatNumber(summary.successRequests) }} / {{ formatNumber(summary.totalRequests) }}</dd>
            </div>
          </dl>
          <EmptyState v-else title="暂无调用摘要" description="当前账号还没有可展示的调用汇总。" />
        </AppPanel>
      </aside>
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import DailyStatsTrendChart from '@/components/DailyStatsTrendChart.vue'
import AppPanel from '@/components/ui/AppPanel.vue'
import EmptyState from '@/components/ui/EmptyState.vue'
import ErrorPanel from '@/components/ui/ErrorPanel.vue'
import MetricCard from '@/components/ui/MetricCard.vue'
import ModelStatusCard from '@/components/ui/ModelStatusCard.vue'
import { getMyMembership, type MembershipVO } from '@/api/membershipController'
import { listAvailableModels } from '@/api/modelController'
import { getMyDailyStats, getMySummaryStats } from '@/api/statsController'

interface DailyPoint {
  date: string
  totalTokens: number
  requestCount: number
  successCount: number
  totalCost: number
}

type MetricTone = 'success' | 'danger' | 'muted'

const loading = ref(false)
const error = ref('')
const summary = ref<API.UserSummaryStatsVO>({})
const dailyStats = ref<DailyPoint[]>([])
const membership = ref<MembershipVO>({})
const models = ref<API.ModelVO[]>([])

const endDate = formatDate(new Date())
const startDate = formatDate(addDays(new Date(), -6))

const todayPoint = computed(() => dailyStats.value.find((item) => item.date === endDate))
const hasDailyStats = computed(() => dailyStats.value.length > 0)
const hasSummary = computed(() =>
  [
    summary.value.totalTokens,
    summary.value.totalCost,
    summary.value.todayCost,
    summary.value.totalRequests,
    summary.value.successRequests,
  ].some((value) => Number(value ?? 0) > 0),
)

const averageLatency = computed(() => {
  const latencyValues = models.value.map((item) => toFiniteNumber(item.avgLatency)).filter((value) => value > 0)
  if (!latencyValues.length) return 0
  return latencyValues.reduce((sum, value) => sum + value, 0) / latencyValues.length
})

const overallSuccessRate = computed(() => {
  const totalRequests = toFiniteNumber(summary.value.totalRequests)
  if (totalRequests <= 0) return 0
  return toFiniteNumber(summary.value.successRequests) / totalRequests
})

const metrics = computed<Array<{ label: string; value: string; trend: string; trendTone: MetricTone }>>(() => [
  {
    label: '接入模型',
    value: formatNumber(models.value.length),
    trend: '当前账号可见',
    trendTone: 'muted',
  },
  {
    label: '今日请求',
    value: formatNumber(todayPoint.value?.requestCount),
    trend: `${formatNumber(todayPoint.value?.successCount)} 次成功`,
    trendTone: 'success',
  },
  {
    label: '成功率',
    value: formatPercent(overallSuccessRate.value),
    trend: `${formatNumber(summary.value.successRequests)} / ${formatNumber(summary.value.totalRequests)}`,
    trendTone: overallSuccessRate.value >= 0.95 ? 'success' : 'muted',
  },
  {
    label: '平均延迟',
    value: formatLatency(averageLatency.value),
    trend: '来自模型状态',
    trendTone: 'muted',
  },
  {
    label: '积分余额',
    value: formatNumber(membership.value.pointBalance),
    trend: membership.value.planName || '免费版',
    trendTone: 'muted',
  },
])

function addDays(date: Date, days: number) {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate() + days)
}

function formatDate(date: Date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function toFiniteNumber(value?: number | string | null) {
  const numberValue = Number(value ?? 0)
  return Number.isFinite(numberValue) ? numberValue : 0
}

function normalizeDailyStats(items?: Record<string, any>[]) {
  return (items ?? []).map((item) => ({
    date: String(item.date ?? ''),
    totalTokens: toFiniteNumber(item.totalTokens),
    requestCount: toFiniteNumber(item.requestCount),
    successCount: toFiniteNumber(item.successCount),
    totalCost: toFiniteNumber(item.totalCost),
  }))
}

function formatNumber(value?: number) {
  return toFiniteNumber(value).toLocaleString('zh-CN')
}

function formatCurrency(value?: number) {
  return `¥${toFiniteNumber(value).toFixed(2)}`
}

function formatPercent(value?: number) {
  const numericValue = toFiniteNumber(value)
  const percentValue = numericValue <= 1 ? numericValue * 100 : numericValue
  return `${percentValue.toFixed(1)}%`
}

function formatLatency(value?: number) {
  const numericValue = Math.round(toFiniteNumber(value))
  return numericValue > 0 ? `${numericValue}ms` : '0ms'
}

function formatQuota(value?: number) {
  return value === -1 ? '无限制' : formatNumber(value)
}

function formatModelName(model: API.ModelVO) {
  return model.modelName || model.modelKey || model.providerDisplayName || '未命名模型'
}

function formatModelStatus(model: API.ModelVO) {
  return model.healthStatus || model.status || '正常'
}

async function loadDashboard() {
  loading.value = true
  error.value = ''
  try {
    const [summaryRes, dailyRes, membershipRes, modelsRes] = await Promise.all([
      getMySummaryStats(),
      getMyDailyStats({ startDate, endDate }),
      getMyMembership(),
      listAvailableModels(),
    ])

    if (summaryRes.data.code === 0) {
      summary.value = summaryRes.data.data ?? {}
    }
    if (dailyRes.data.code === 0) {
      dailyStats.value = normalizeDailyStats(dailyRes.data.data)
    }
    if (membershipRes.data.code === 0) {
      membership.value = membershipRes.data.data ?? {}
    }
    if (modelsRes.data.code === 0) {
      models.value = modelsRes.data.data ?? []
    }
  } catch {
    error.value = '控制台数据加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void loadDashboard()
})
</script>

<style scoped>
.dashboard-page {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 16px;
}

.dashboard-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.dashboard-header__eyebrow {
  margin: 0 0 6px;
  color: var(--leo-primary);
  font-size: 12px;
  font-weight: 700;
  line-height: 16px;
  text-transform: uppercase;
}

.dashboard-header__title {
  margin: 0;
  color: var(--leo-text-primary);
  font-size: 28px;
  font-weight: 800;
  line-height: 36px;
}

.dashboard-header__subtitle {
  margin: 6px 0 0;
  color: var(--leo-text-secondary);
  font-size: 14px;
  line-height: 22px;
}

.dashboard-header__status,
.panel-count {
  flex: 0 0 auto;
  padding: 5px 10px;
  color: var(--leo-success);
  font-size: 12px;
  font-weight: 700;
  line-height: 18px;
  background: rgba(18, 183, 106, 0.1);
  border-radius: 999px;
}

.dashboard-header__status--loading {
  color: var(--leo-primary);
  background: var(--leo-primary-soft);
}

.dashboard-error {
  width: 100%;
}

.metric-grid,
.model-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 12px;
}

.dashboard-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 340px;
  gap: 16px;
  align-items: start;
}

.dashboard-layout__main,
.dashboard-layout__side {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 16px;
}

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 16px;
}

.panel-head--compact {
  margin-bottom: 12px;
}

.panel-title {
  margin: 0;
  color: var(--leo-text-primary);
  font-size: 16px;
  font-weight: 800;
  line-height: 24px;
}

.panel-desc {
  margin: 4px 0 0;
  color: var(--leo-text-secondary);
  font-size: 13px;
  line-height: 20px;
}

.panel-count {
  color: var(--leo-text-secondary);
  background: var(--leo-bg-muted);
  border: 1px solid var(--leo-border);
}

.usage-list,
.summary-list {
  display: grid;
  gap: 10px;
  margin: 0;
}

.usage-item,
.summary-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid var(--leo-border);
}

.usage-item:last-child,
.summary-item:last-child {
  border-bottom: 0;
}

.usage-item dt,
.summary-item dt {
  color: var(--leo-text-secondary);
  font-size: 13px;
  line-height: 20px;
}

.usage-item dd,
.summary-item dd {
  margin: 0;
  color: var(--leo-text-primary);
  font-size: 14px;
  font-weight: 700;
  line-height: 20px;
  text-align: right;
}

@media (max-width: 1180px) {
  .metric-grid,
  .model-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .dashboard-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .dashboard-page {
    padding: 16px;
  }

  .dashboard-header {
    flex-direction: column;
  }

  .metric-grid,
  .model-grid {
    grid-template-columns: 1fr;
  }
}
</style>
