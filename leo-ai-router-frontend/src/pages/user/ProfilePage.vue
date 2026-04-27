<template>
  <div class="page-shell">
    <div class="page-header">
      <div>
        <div class="page-title">个人中心</div>
        <div class="page-desc">查看余额、充值概览、消费统计和每日趋势</div>
      </div>
      <a-space wrap>
        <a-button size="large" @click="openBillDrawer">查看账单</a-button>
        <a-button type="primary" size="large" @click="openRechargeModal">充值</a-button>
      </a-space>
    </div>

    <div class="summary-grid">
      <a-card v-for="card in summaryCards" :key="card.title" :bordered="false" class="summary-card">
        <div class="summary-title">{{ card.title }}</div>
        <div class="summary-value">{{ card.value }}</div>
        <div class="summary-extra">{{ card.extra }}</div>
      </a-card>
    </div>

    <a-card :bordered="false" class="wallet-card">
      <div class="wallet-main">
        <div>
          <div class="wallet-label">账户资产</div>
          <div class="wallet-balance">¥{{ formatCurrencyNumber(balanceInfo.balance) }}</div>
          <div class="wallet-subtitle">
            累计充值 ¥{{ formatCurrencyNumber(balanceInfo.totalRecharge) }}，累计消费 ¥{{ formatCurrencyNumber(balanceInfo.totalSpending) }}
          </div>
        </div>
        <div class="wallet-actions">
          <a-button type="primary" size="large" @click="openRechargeModal">立即充值</a-button>
          <a-button size="large" @click="openBillDrawer">查看账单</a-button>
        </div>
      </div>
    </a-card>

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

    <a-modal
      v-model:open="rechargeModalOpen"
      title="账户充值"
      :confirm-loading="rechargeSubmitting"
      ok-text="立即支付"
      cancel-text="取消"
      @ok="submitRecharge"
    >
      <a-form layout="vertical">
        <a-form-item label="充值金额">
          <a-input-number
            v-model:value="rechargeForm.amount"
            :min="1"
            :max="10000"
            :step="10"
            addon-before="¥"
            style="width: 100%"
          />
        </a-form-item>
        <a-form-item label="快捷金额">
          <div class="quick-amounts">
            <button
              v-for="value in quickAmounts"
              :key="value"
              type="button"
              class="quick-chip"
              :class="{ active: rechargeForm.amount === value }"
              @click="rechargeForm.amount = value"
            >
              ¥{{ value }}
            </button>
          </div>
        </a-form-item>
        <a-form-item label="支付方式">
          <a-radio-group v-model:value="rechargeForm.paymentMethod" class="pay-method-group">
            <a-radio-button value="alipay">支付宝沙箱</a-radio-button>
            <a-radio-button value="stripe">Stripe</a-radio-button>
          </a-radio-group>
        </a-form-item>
      </a-form>
    </a-modal>

    <a-drawer v-model:open="billDrawerOpen" title="我的账单" width="960" @after-open-change="handleBillDrawerChange">
      <a-tabs v-model:activeKey="billTab">
        <a-tab-pane key="recharge" tab="充值记录">
          <a-table
            row-key="id"
            :columns="rechargeColumns"
            :data-source="rechargeRecords"
            :loading="rechargeLoading"
            :pagination="rechargePagination"
            @change="handleRechargeTableChange"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'amount'">¥{{ formatCurrencyNumber(record.amount) }}</template>
              <template v-else-if="column.key === 'paymentMethod'">
                <a-tag :color="record.paymentMethod === 'alipay' ? 'blue' : 'purple'">
                  {{ record.paymentMethod === 'alipay' ? '支付宝' : 'Stripe' }}
                </a-tag>
              </template>
              <template v-else-if="column.key === 'status'">
                <a-tag :color="statusColorMap[record.status ?? 'pending']">{{ statusTextMap[record.status ?? 'pending'] }}</a-tag>
              </template>
              <template v-else-if="column.key === 'createTime'">{{ formatTime(record.createTime) }}</template>
            </template>
          </a-table>
        </a-tab-pane>
        <a-tab-pane key="billing" tab="消费记录">
          <a-table
            row-key="id"
            :columns="billingColumns"
            :data-source="billingRecords"
            :loading="billingLoading"
            :pagination="billingPagination"
            @change="handleBillingTableChange"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'amount'">¥{{ formatCurrencyNumber(record.amount) }}</template>
              <template v-else-if="column.key === 'billingType'">
                <a-tag :color="record.billingType === 'recharge' ? 'green' : 'orange'">
                  {{ record.billingType === 'recharge' ? '充值' : '消费' }}
                </a-tag>
              </template>
              <template v-else-if="column.key === 'createTime'">{{ formatTime(record.createTime) }}</template>
            </template>
          </a-table>
        </a-tab-pane>
      </a-tabs>
    </a-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useRoute, useRouter } from 'vue-router'
import DailyStatsTrendChart from '@/components/DailyStatsTrendChart.vue'
import { getMyBalance, getMyBillingRecords } from '@/api/balanceController'
import { createRecharge, getMyRechargeRecords } from '@/api/rechargeController'
import { getMyDailyStats, getMySummaryStats } from '@/api/statsController'
import { getMyQuota } from '@/api/userController'

interface DailyStatPoint {
  date: string
  totalTokens: number
  requestCount: number
  successCount: number
  totalCost: number
}

const router = useRouter()
const route = useRoute()
const summary = ref<API.UserSummaryStatsVO>({})
const quota = ref<API.QuotaVO>({})
const balanceInfo = ref<API.BalanceVO>({})
const dailyStats = ref<DailyStatPoint[]>([])
const rechargeModalOpen = ref(false)
const rechargeSubmitting = ref(false)
const billDrawerOpen = ref(false)
const billTab = ref('recharge')
const rechargeLoading = ref(false)
const billingLoading = ref(false)
const rechargeRecords = ref<API.RechargeRecord[]>([])
const billingRecords = ref<API.BillingRecord[]>([])
const quickAmounts = [10, 50, 100, 500]

const rechargeForm = reactive({
  amount: 100,
  paymentMethod: 'alipay',
})

const rechargePagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`,
})

const billingPagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`,
})

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

const statusColorMap: Record<string, string> = {
  pending: 'gold',
  success: 'green',
  failed: 'red',
  refunded: 'default',
}

const statusTextMap: Record<string, string> = {
  pending: '待支付',
  success: '成功',
  failed: '失败',
  refunded: '已退款',
}

const rechargeColumns = [
  { title: '金额', key: 'amount', width: 120 },
  { title: '支付方式', key: 'paymentMethod', width: 130 },
  { title: '状态', key: 'status', width: 110 },
  { title: '说明', dataIndex: 'description', key: 'description' },
  { title: '时间', key: 'createTime', width: 180 },
]

const billingColumns = [
  { title: '金额', key: 'amount', width: 120 },
  { title: '类型', key: 'billingType', width: 120 },
  { title: '说明', dataIndex: 'description', key: 'description' },
  { title: '时间', key: 'createTime', width: 180 },
]

const formatNumber = (value?: number) => (value ?? 0).toLocaleString('zh-CN')
const formatCurrency = (value?: number) => `¥${formatCurrencyNumber(value)}`
const formatCurrencyNumber = (value?: number) => Number(value ?? 0).toFixed(2)
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
    title: '请求总数',
    value: formatNumber(summary.value.totalRequests),
    extra: `成功率 ${summary.value.totalRequests ? (((summary.value.successRequests ?? 0) / (summary.value.totalRequests ?? 1)) * 100).toFixed(1) : '0.0'}%`,
  },
])

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

const loadOverview = async () => {
  const [summaryRes, quotaRes, balanceRes] = await Promise.all([getMySummaryStats(), getMyQuota(), getMyBalance()])
  if (summaryRes.data.code === 0) summary.value = summaryRes.data.data ?? {}
  if (quotaRes.data.code === 0) quota.value = quotaRes.data.data ?? {}
  if (balanceRes.data.code === 0) balanceInfo.value = balanceRes.data.data ?? {}
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

const loadRechargeRecords = async () => {
  rechargeLoading.value = true
  try {
    const res = await getMyRechargeRecords({
      pageNum: rechargePagination.current,
      pageSize: rechargePagination.pageSize,
    })
    if (res.data.code === 0 && res.data.data) {
      rechargeRecords.value = res.data.data.records ?? []
      rechargePagination.total = res.data.data.totalRow ?? 0
    } else {
      message.error(res.data.message ?? '加载充值记录失败')
    }
  } finally {
    rechargeLoading.value = false
  }
}

const loadBillingRecords = async () => {
  billingLoading.value = true
  try {
    const res = await getMyBillingRecords({
      pageNum: billingPagination.current,
      pageSize: billingPagination.pageSize,
    })
    if (res.data.code === 0 && res.data.data) {
      billingRecords.value = res.data.data.records ?? []
      billingPagination.total = res.data.data.totalRow ?? 0
    } else {
      message.error(res.data.message ?? '加载消费账单失败')
    }
  } finally {
    billingLoading.value = false
  }
}

const resetDateRange = () => {
  Object.assign(dateRange, createDefaultRange())
  void loadDailyStats()
}

const openRechargeModal = () => {
  rechargeModalOpen.value = true
}

const openBillDrawer = async () => {
  billDrawerOpen.value = true
  await Promise.all([loadRechargeRecords(), loadBillingRecords()])
}

const handleBillDrawerChange = (open: boolean) => {
  if (open) return
  billTab.value = 'recharge'
}

const handleRechargeTableChange = (pag: { current: number; pageSize: number }) => {
  rechargePagination.current = pag.current
  rechargePagination.pageSize = pag.pageSize
  void loadRechargeRecords()
}

const handleBillingTableChange = (pag: { current: number; pageSize: number }) => {
  billingPagination.current = pag.current
  billingPagination.pageSize = pag.pageSize
  void loadBillingRecords()
}

const submitRecharge = async () => {
  if (!rechargeForm.amount || rechargeForm.amount < 1 || rechargeForm.amount > 10000) {
    message.warning('充值金额需在 1 - 10000 元之间')
    return
  }
  rechargeSubmitting.value = true
  try {
    const res = await createRecharge({
      amount: rechargeForm.amount,
      paymentMethod: rechargeForm.paymentMethod,
    })
    if (res.data.code !== 0 || !res.data.data) {
      message.error(res.data.message ?? '创建充值订单失败')
      return
    }
    const { displayType, redirectUrl, formHtml } = res.data.data
    rechargeModalOpen.value = false
    if (displayType === 'redirect_url' && redirectUrl) {
      window.location.href = redirectUrl
      return
    }
    if (displayType === 'form_html' && formHtml) {
      const payWindow = window.open('', '_blank')
      if (!payWindow) {
        message.warning('浏览器拦截了支付窗口，请允许弹窗后重试')
        return
      }
      if (formHtml.includes('<form')) {
        payWindow.document.open()
        payWindow.document.write(formHtml)
        payWindow.document.close()
        return
      }
      if (/^https?:\/\//i.test(formHtml)) {
        payWindow.location.href = formHtml
        return
      }
      payWindow.close()
      message.error('未获取到有效的支付宝支付表单')
      return
    }
    message.error('未获取到有效的支付跳转信息')
  } finally {
    rechargeSubmitting.value = false
  }
}

const handleRechargeRedirect = () => {
  const rechargeState = String(route.query.recharge ?? '')
  if (rechargeState === 'success') {
    void router.replace({
      name: 'rechargeSuccess',
      query: {
        method: route.query.method,
        outTradeNo: route.query.outTradeNo,
        session_id: route.query.session_id,
      },
    })
    return
  }
  if (rechargeState === 'cancelled') {
    void router.replace({
      name: 'rechargeCancel',
      query: {
        method: route.query.method,
        outTradeNo: route.query.outTradeNo,
      },
    })
    return
  }
  if (route.query.openRecharge === '1') {
    rechargeModalOpen.value = true
    void router.replace({ name: 'profile', query: {} })
  }
}

onMounted(() => {
  handleRechargeRedirect()
  void Promise.all([loadOverview(), loadDailyStats()])
})
</script>

<style scoped>
.page-shell { max-width: 1280px; margin: 0 auto; padding: 28px 24px 40px; }
.page-header { margin-bottom: 20px; display: flex; justify-content: space-between; gap: 16px; align-items: center; flex-wrap: wrap; }
.page-title { font-size: 24px; font-weight: 700; color: #111827; }
.page-desc { margin-top: 6px; color: #6b7280; }
.summary-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 16px; margin-bottom: 18px; }
.summary-card { border-radius: 18px; background: linear-gradient(180deg, #ffffff, #f8fbff); }
.summary-title { color: #64748b; font-size: 13px; }
.summary-value { margin-top: 10px; font-size: 28px; font-weight: 700; color: #0f172a; }
.summary-extra { margin-top: 10px; font-size: 12px; color: #94a3b8; line-height: 1.6; }
.wallet-card { margin-bottom: 18px; border-radius: 22px; background: linear-gradient(135deg, #0f172a, #1e3a8a 55%, #38bdf8); color: #fff; }
.wallet-main { display: flex; justify-content: space-between; gap: 20px; align-items: center; flex-wrap: wrap; }
.wallet-label { font-size: 13px; color: rgba(255,255,255,0.72); text-transform: uppercase; letter-spacing: 0.08em; }
.wallet-balance { margin-top: 10px; font-size: 40px; font-weight: 700; }
.wallet-subtitle { margin-top: 8px; color: rgba(255,255,255,0.78); }
.wallet-actions { display: flex; gap: 12px; flex-wrap: wrap; }
.chart-card { border-radius: 18px; }
.chart-head { display: flex; justify-content: space-between; gap: 16px; margin-bottom: 20px; flex-wrap: wrap; }
.card-title { font-size: 18px; font-weight: 700; color: #0f172a; }
.card-desc { margin-top: 4px; color: #6b7280; }
.date-range { display: flex; align-items: end; gap: 12px; flex-wrap: wrap; }
.date-item { display: flex; flex-direction: column; gap: 6px; font-size: 12px; color: #64748b; }
.date-input { min-width: 160px; height: 38px; padding: 0 12px; border-radius: 10px; border: 1px solid #dbe4f0; background: #fff; }
.quick-amounts { display: flex; gap: 10px; flex-wrap: wrap; }
.quick-chip { height: 38px; padding: 0 16px; border-radius: 999px; border: 1px solid #dbe4f0; background: #f8fafc; color: #0f172a; cursor: pointer; transition: all 0.18s; }
.quick-chip.active { border-color: #2563eb; background: #eff6ff; color: #2563eb; box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.08); }
.pay-method-group { width: 100%; }
@media (max-width: 960px) { .summary-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); } .wallet-balance { font-size: 32px; } }
@media (max-width: 640px) { .summary-grid { grid-template-columns: 1fr; } .wallet-actions { width: 100%; } .wallet-actions :deep(.ant-btn) { flex: 1; } }
</style>
