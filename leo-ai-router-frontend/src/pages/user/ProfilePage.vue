<template>
  <div class="page-shell">
    <div class="page-header">
      <div>
        <div class="page-title">个人中心</div>
        <div class="page-desc">管理账号信息、会员权益、积分流水和每日使用趋势</div>
      </div>
      <a-space wrap>
        <a-button size="large" @click="openBillDrawer">查看账单</a-button>
        <a-button type="primary" size="large" @click="goMembership">会员充值</a-button>
      </a-space>
    </div>

    <a-card :bordered="false" class="account-card">
      <div class="account-main">
        <a-avatar :size="72" :src="profileForm.userAvatar">
          {{ (profileForm.userName || loginUser.userAccount || 'U').slice(0, 1) }}
        </a-avatar>
        <div class="account-info">
          <div class="account-name">{{ profileForm.userName || loginUser.userAccount || '未命名用户' }}</div>
          <div class="account-meta">
            <a-tag>{{ loginUser.userRole || 'user' }}</a-tag>
            <a-tag color="orange">{{ membership.planName || '免费版' }}</a-tag>
            <a-tag color="green">积分 {{ formatNumber(membership.pointBalance) }}</a-tag>
            <a-tag :color="loginUser.hasPassword ? 'green' : 'orange'">
              {{ loginUser.hasPassword ? '已设置密码' : '未设置密码' }}
            </a-tag>
            <span>{{ loginUser.userPhone || '未绑定手机号' }}</span>
            <span>{{ loginUser.userEmail || '未绑定邮箱' }}</span>
          </div>
        </div>
        <a-space wrap>
          <a-button @click="profileModalOpen = true">编辑资料</a-button>
          <a-button @click="openEmailModal">
            {{ loginUser.userEmail ? '更换邮箱' : '绑定邮箱' }}
          </a-button>
          <a-button @click="openPhoneModal">
            {{ loginUser.userPhone ? '更换手机号' : '绑定手机号' }}
          </a-button>
          <a-button type="primary" @click="passwordModalOpen = true">
            {{ loginUser.hasPassword ? '修改密码' : '设置密码' }}
          </a-button>
        </a-space>
      </div>
    </a-card>

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

    <a-drawer v-model:open="billDrawerOpen" title="我的账单" width="960" @after-open-change="handleBillDrawerChange">
      <a-tabs v-model:activeKey="billTab">
        <a-tab-pane key="orders" tab="支付订单">
          <a-table
            row-key="id"
            :columns="orderColumns"
            :data-source="paymentOrders"
            :loading="orderLoading"
            :pagination="orderPagination"
            @change="handleOrderTableChange"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'orderType'">
                <a-tag :color="record.orderType === 'subscription' ? 'blue' : 'green'">
                  {{ orderTypeTextMap[record.orderType ?? ''] || record.orderType || '-' }}
                </a-tag>
              </template>
              <template v-else-if="column.key === 'amount'">¥{{ formatCurrencyNumber(record.amount) }}</template>
              <template v-else-if="column.key === 'paymentMethod'">
                <a-tag :color="paymentMethodColor(record.paymentMethod)">
                  {{ paymentMethodTextMap[record.paymentMethod ?? ''] || record.paymentMethod || '-' }}
                </a-tag>
              </template>
              <template v-else-if="column.key === 'status'">
                <a-tag :color="statusColorMap[record.status ?? 'pending'] || 'default'">
                  {{ statusTextMap[record.status ?? 'pending'] || record.status || '-' }}
                </a-tag>
              </template>
              <template v-else-if="column.key === 'createTime'">{{ formatTime(record.createTime) }}</template>
            </template>
          </a-table>
        </a-tab-pane>
        <a-tab-pane key="points" tab="积分流水">
          <a-table
            row-key="id"
            :columns="pointTransactionColumns"
            :data-source="pointTransactions"
            :loading="pointTransactionLoading"
            :pagination="pointTransactionPagination"
            @change="handlePointTransactionTableChange"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'changeAmount'">
                <span :class="Number(record.changeAmount ?? 0) >= 0 ? 'point-plus' : 'point-minus'">
                  {{ formatSignedNumber(record.changeAmount) }}
                </span>
              </template>
              <template v-else-if="column.key === 'transactionType'">
                <a-tag :color="pointTransactionColorMap[record.transactionType ?? ''] || 'default'">
                  {{ pointTransactionTypeTextMap[record.transactionType ?? ''] || record.transactionType || '-' }}
                </a-tag>
              </template>
              <template v-else-if="column.key === 'balanceAfter'">{{ formatNumber(record.balanceAfter) }}</template>
              <template v-else-if="column.key === 'createTime'">{{ formatTime(record.createTime) }}</template>
            </template>
          </a-table>
        </a-tab-pane>
        <a-tab-pane key="recharge" tab="旧充值记录">
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
        <a-tab-pane key="billing" tab="旧消费记录">
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

    <a-modal
      v-model:open="profileModalOpen"
      title="编辑个人资料"
      ok-text="保存"
      cancel-text="取消"
      :confirm-loading="profileSaving"
      @ok="submitProfile"
    >
      <a-form layout="vertical">
        <a-form-item label="姓名">
          <a-input v-model:value="profileForm.userName" placeholder="请输入姓名" />
        </a-form-item>
        <a-form-item label="头像 URL">
          <a-input v-model:value="profileForm.userAvatar" placeholder="请输入头像 URL" />
        </a-form-item>
        <a-form-item label="个人简介">
          <a-textarea v-model:value="profileForm.userProfile" :rows="4" placeholder="请输入个人简介" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="passwordModalOpen"
      :title="loginUser.hasPassword ? '修改密码' : '设置登录密码'"
      ok-text="确认密码"
      cancel-text="取消"
      :confirm-loading="passwordSaving"
      @ok="submitPassword"
    >
      <a-form layout="vertical">
        <a-form-item label="登录密码">
          <a-input-password v-model:value="passwordForm.userPassword" placeholder="至少 8 位" />
        </a-form-item>
        <a-form-item label="确认密码">
          <a-input-password v-model:value="passwordForm.checkPassword" placeholder="再次输入密码" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="emailModalOpen"
      :title="loginUser.userEmail ? '更换邮箱' : '绑定邮箱'"
      ok-text="确认绑定"
      cancel-text="取消"
      :confirm-loading="emailBinding"
      @ok="submitEmailBind"
    >
      <a-form layout="vertical">
        <a-form-item label="邮箱">
          <div class="code-row">
            <a-input v-model:value="emailBindForm.email" placeholder="请输入邮箱地址" />
            <a-button :loading="emailCodeSending" :disabled="emailCountdown.isCounting" @click="sendBindEmailCode">
              {{ emailCountdown.isCounting ? `${emailCountdown.countdown}s` : '发送验证码' }}
            </a-button>
          </div>
        </a-form-item>
        <a-form-item label="验证码">
          <a-input v-model:value="emailBindForm.code" placeholder="请输入验证码" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="phoneModalOpen"
      :title="loginUser.userPhone ? '更换手机号' : '绑定手机号'"
      ok-text="确认绑定"
      cancel-text="取消"
      :confirm-loading="phoneBinding"
      @ok="submitPhoneBind"
    >
      <a-form layout="vertical">
        <a-form-item label="手机号">
          <div class="code-row">
            <a-input v-model:value="phoneBindForm.phone" placeholder="请输入手机号" />
            <a-button :loading="phoneCodeSending" :disabled="phoneCountdown.isCounting" @click="sendBindPhoneCode">
              {{ phoneCountdown.isCounting ? `${phoneCountdown.countdown}s` : '发送验证码' }}
            </a-button>
          </div>
        </a-form-item>
        <a-form-item label="验证码">
          <a-input v-model:value="phoneBindForm.code" placeholder="请输入验证码" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { useRoute, useRouter } from 'vue-router'
import DailyStatsTrendChart from '@/components/DailyStatsTrendChart.vue'
import { getMyBillingRecords } from '@/api/balanceController'
import { getMyRechargeRecords } from '@/api/rechargeController'
import { getMyMembership, type MembershipVO } from '@/api/membershipController'
import { listMyPaymentOrders, type PaymentOrderVO } from '@/api/paymentOrderController'
import { listMyPointTransactions } from '@/api/pointController'
import { getMyDailyStats, getMySummaryStats } from '@/api/statsController'
import { bindEmail, bindPhone, getLoginUser, sendEmailBindCode, sendPhoneBindCode, setPassword, updateMyProfile } from '@/api/userController'
import { useLoginUserStore } from '@/stores/loginUser'
import { useEmailCodeCountdown } from '@/composables/useEmailCodeCountdown'

interface DailyStatPoint {
  date: string
  totalTokens: number
  requestCount: number
  successCount: number
  totalCost: number
}

interface PointTransactionRecord {
  id?: number
  changeAmount?: number
  balanceBefore?: number
  balanceAfter?: number
  transactionType?: string
  description?: string
  createTime?: string
}

const router = useRouter()
const route = useRoute()
const loginUserStore = useLoginUserStore()
const loginUser = computed(() => loginUserStore.loginUser)
const summary = ref<API.UserSummaryStatsVO>({})
const membership = ref<MembershipVO>({})
const dailyStats = ref<DailyStatPoint[]>([])
const billDrawerOpen = ref(false)
const billTab = ref('orders')
const orderLoading = ref(false)
const pointTransactionLoading = ref(false)
const rechargeLoading = ref(false)
const billingLoading = ref(false)
const paymentOrders = ref<PaymentOrderVO[]>([])
const pointTransactions = ref<PointTransactionRecord[]>([])
const rechargeRecords = ref<API.RechargeRecord[]>([])
const billingRecords = ref<API.BillingRecord[]>([])
const profileModalOpen = ref(false)
const passwordModalOpen = ref(false)
const profileSaving = ref(false)
const passwordSaving = ref(false)
const emailModalOpen = ref(false)
const phoneModalOpen = ref(false)
const emailCodeSending = ref(false)
const phoneCodeSending = ref(false)
const emailBinding = ref(false)
const phoneBinding = ref(false)
const emailCountdown = useEmailCodeCountdown()
const phoneCountdown = useEmailCodeCountdown()

const profileForm = reactive({
  userName: '',
  userAvatar: '',
  userProfile: '',
})

const passwordForm = reactive({
  userPassword: '',
  checkPassword: '',
})

const emailBindForm = reactive({
  email: '',
  code: '',
})

const phoneBindForm = reactive({
  phone: '',
  code: '',
})

const rechargePagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`,
})

const orderPagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total: number) => `共 ${total} 条`,
})

const pointTransactionPagination = reactive({
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
  cancelled: '已取消',
  coming_soon: '暂未开放',
  refunded: '已退款',
}

const orderTypeTextMap: Record<string, string> = {
  subscription: '套餐',
  points: '积分',
}

const paymentMethodTextMap: Record<string, string> = {
  alipay: '支付宝',
  stripe: 'Stripe',
  wechat: '微信',
}

const pointTransactionTypeTextMap: Record<string, string> = {
  purchase: '购买',
  plan_bonus: '套餐赠送',
  register_bonus: '注册赠送',
  image_consume: '图片扣费',
  admin_adjust: '管理员调整',
}

const pointTransactionColorMap: Record<string, string> = {
  purchase: 'green',
  plan_bonus: 'cyan',
  register_bonus: 'blue',
  image_consume: 'orange',
  admin_adjust: 'purple',
}

const paymentMethodColor = (method?: string) => {
  if (method === 'alipay') return 'blue'
  if (method === 'stripe') return 'purple'
  if (method === 'wechat') return 'green'
  return 'default'
}

const formatSignedNumber = (value?: number) => {
  const amount = Number(value ?? 0)
  return amount > 0 ? `+${formatNumber(amount)}` : formatNumber(amount)
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

const orderColumns = [
  { title: '商品', dataIndex: 'productName', key: 'productName' },
  { title: '类型', key: 'orderType', width: 100 },
  { title: '金额', key: 'amount', width: 120 },
  { title: '支付方式', key: 'paymentMethod', width: 120 },
  { title: '状态', key: 'status', width: 110 },
  { title: '时间', key: 'createTime', width: 180 },
]

const pointTransactionColumns = [
  { title: '积分变动', key: 'changeAmount', width: 130 },
  { title: '类型', key: 'transactionType', width: 130 },
  { title: '变动后余额', key: 'balanceAfter', width: 130 },
  { title: '说明', dataIndex: 'description', key: 'description' },
  { title: '时间', key: 'createTime', width: 180 },
]

const formatNumber = (value?: number) => (value ?? 0).toLocaleString('zh-CN')
const formatCurrencyNumber = (value?: number) => Number(value ?? 0).toFixed(2)
const formatQuota = (value?: number) => (value === -1 ? '无限制' : formatNumber(value))

const summaryCards = computed(() => [
  {
    title: '会员套餐',
    value: membership.value.planName || '免费版',
    extra: `普通剩余 ${formatQuota(membership.value.dailyProRemaining)} / 高级剩余 ${formatQuota(membership.value.dailyAdvancedRemaining)}`,
  },
  {
    title: '积分余额',
    value: formatNumber(membership.value.pointBalance),
    extra: '用于 AI 图片与视频生成',
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
  const [userRes, summaryRes, membershipRes] = await Promise.all([getLoginUser(), getMySummaryStats(), getMyMembership()])
  if (userRes.data.code === 0 && userRes.data.data) {
    loginUserStore.setLoginUser(userRes.data.data)
    syncProfileForm(userRes.data.data)
  }
  if (summaryRes.data.code === 0) summary.value = summaryRes.data.data ?? {}
  if (membershipRes.data.code === 0) membership.value = membershipRes.data.data ?? {}
}

const syncProfileForm = (user: API.LoginUserVO) => {
  profileForm.userName = user.userName ?? ''
  profileForm.userAvatar = user.userAvatar ?? ''
  profileForm.userProfile = user.userProfile ?? ''
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

const loadPaymentOrders = async () => {
  orderLoading.value = true
  try {
    const res = await listMyPaymentOrders({
      pageNum: orderPagination.current,
      pageSize: orderPagination.pageSize,
    })
    if (res.data.code === 0 && res.data.data) {
      paymentOrders.value = res.data.data.records ?? []
      orderPagination.total = res.data.data.totalRow ?? 0
    } else {
      message.error(res.data.message ?? '加载支付订单失败')
    }
  } finally {
    orderLoading.value = false
  }
}

const loadPointTransactions = async () => {
  pointTransactionLoading.value = true
  try {
    const res = await listMyPointTransactions({
      pageNum: pointTransactionPagination.current,
      pageSize: pointTransactionPagination.pageSize,
    })
    if (res.data.code === 0 && res.data.data) {
      pointTransactions.value = res.data.data.records ?? []
      pointTransactionPagination.total = res.data.data.totalRow ?? 0
    } else {
      message.error(res.data.message ?? '加载积分流水失败')
    }
  } finally {
    pointTransactionLoading.value = false
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

const goMembership = () => {
  void router.push({ name: 'membership' })
}

const openBillDrawer = async () => {
  billDrawerOpen.value = true
  await Promise.all([loadPaymentOrders(), loadPointTransactions(), loadRechargeRecords(), loadBillingRecords()])
}

const handleBillDrawerChange = (open: boolean) => {
  if (open) return
  billTab.value = 'orders'
}

const handleRechargeTableChange = (pag: { current: number; pageSize: number }) => {
  rechargePagination.current = pag.current
  rechargePagination.pageSize = pag.pageSize
  void loadRechargeRecords()
}

const handleOrderTableChange = (pag: { current: number; pageSize: number }) => {
  orderPagination.current = pag.current
  orderPagination.pageSize = pag.pageSize
  void loadPaymentOrders()
}

const handlePointTransactionTableChange = (pag: { current: number; pageSize: number }) => {
  pointTransactionPagination.current = pag.current
  pointTransactionPagination.pageSize = pag.pageSize
  void loadPointTransactions()
}

const handleBillingTableChange = (pag: { current: number; pageSize: number }) => {
  billingPagination.current = pag.current
  billingPagination.pageSize = pag.pageSize
  void loadBillingRecords()
}

const submitProfile = async () => {
  profileSaving.value = true
  try {
    const res = await updateMyProfile({ ...profileForm })
    if (res.data.code === 0 && res.data.data) {
      loginUserStore.setLoginUser(res.data.data)
      syncProfileForm(res.data.data)
      profileModalOpen.value = false
      message.success('个人资料已更新')
    } else {
      message.error(res.data.message ?? '个人资料更新失败')
    }
  } finally {
    profileSaving.value = false
  }
}

const submitPassword = async () => {
  if (!passwordForm.userPassword || passwordForm.userPassword.length < 8) {
    message.warning('密码长度不能小于8位')
    return
  }
  if (passwordForm.userPassword !== passwordForm.checkPassword) {
    message.warning('两次密码不一致')
    return
  }
  passwordSaving.value = true
  try {
    const res = await setPassword({ ...passwordForm })
    if (res.data.code === 0) {
      loginUserStore.setLoginUser({
        ...loginUserStore.loginUser,
        hasPassword: true,
        needSetPassword: false,
      })
      passwordForm.userPassword = ''
      passwordForm.checkPassword = ''
      passwordModalOpen.value = false
      message.success('密码已保存')
    } else {
      message.error(res.data.message ?? '密码保存失败')
    }
  } finally {
    passwordSaving.value = false
  }
}

const openEmailModal = () => {
  emailBindForm.email = loginUser.value.userEmail ?? ''
  emailBindForm.code = ''
  emailModalOpen.value = true
}

const openPhoneModal = () => {
  phoneBindForm.phone = loginUser.value.userPhone ?? ''
  phoneBindForm.code = ''
  phoneModalOpen.value = true
}

const sendBindEmailCode = async () => {
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(emailBindForm.email)) {
    message.warning('请输入正确的邮箱')
    return
  }
  emailCodeSending.value = true
  try {
    const res = await sendEmailBindCode({ email: emailBindForm.email.trim(), scene: 'bind' })
    if (res.data.code === 0) {
      message.success('验证码已发送')
      emailCountdown.start()
    } else {
      message.error(res.data.message ?? '验证码发送失败')
    }
  } finally {
    emailCodeSending.value = false
  }
}

const sendBindPhoneCode = async () => {
  if (!/^1[3-9]\d{9}$/.test(phoneBindForm.phone)) {
    message.warning('请输入正确的手机号')
    return
  }
  phoneCodeSending.value = true
  try {
    const res = await sendPhoneBindCode({ phone: phoneBindForm.phone.trim() })
    if (res.data.code === 0) {
      if (res.data.data?.mockMode && res.data.data.devCode) {
        phoneBindForm.code = res.data.data.devCode
        message.info(`本地开发验证码：${res.data.data.devCode}`)
      } else {
        message.success(res.data.data?.message ?? '验证码已发送')
      }
      phoneCountdown.start()
    } else {
      message.error(res.data.message ?? '验证码发送失败')
    }
  } finally {
    phoneCodeSending.value = false
  }
}

const submitEmailBind = async () => {
  if (!emailBindForm.email || !emailBindForm.code) {
    message.warning('请输入邮箱和验证码')
    return
  }
  emailBinding.value = true
  try {
    const res = await bindEmail({ email: emailBindForm.email.trim(), code: emailBindForm.code.trim() })
    if (res.data.code === 0 && res.data.data) {
      loginUserStore.setLoginUser(res.data.data)
      emailModalOpen.value = false
      message.success('邮箱已更新')
    } else {
      message.error(res.data.message ?? '邮箱绑定失败')
    }
  } finally {
    emailBinding.value = false
  }
}

const submitPhoneBind = async () => {
  if (!phoneBindForm.phone || !phoneBindForm.code) {
    message.warning('请输入手机号和验证码')
    return
  }
  phoneBinding.value = true
  try {
    const res = await bindPhone({ phone: phoneBindForm.phone.trim(), code: phoneBindForm.code.trim() })
    if (res.data.code === 0 && res.data.data) {
      loginUserStore.setLoginUser(res.data.data)
      phoneModalOpen.value = false
      message.success('手机号已更新')
    } else {
      message.error(res.data.message ?? '手机号绑定失败')
    }
  } finally {
    phoneBinding.value = false
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
    void router.replace({ name: 'membership' })
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
.account-card { margin-bottom: 18px; border-radius: 18px; }
.account-main { display: flex; align-items: center; gap: 18px; flex-wrap: wrap; }
.account-info { flex: 1; min-width: 240px; }
.account-name { font-size: 20px; font-weight: 700; color: #111827; }
.account-meta { margin-top: 10px; display: flex; gap: 8px; align-items: center; flex-wrap: wrap; color: #64748b; }
.code-row { display: grid; grid-template-columns: minmax(0, 1fr) auto; gap: 10px; }
.summary-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 16px; margin-bottom: 18px; }
.summary-card { border-radius: 18px; background: linear-gradient(180deg, #ffffff, #f8fbff); }
.summary-title { color: #64748b; font-size: 13px; }
.summary-value { margin-top: 10px; font-size: 28px; font-weight: 700; color: #0f172a; }
.summary-extra { margin-top: 10px; font-size: 12px; color: #94a3b8; line-height: 1.6; }
.chart-card { border-radius: 18px; }
.chart-head { display: flex; justify-content: space-between; gap: 16px; margin-bottom: 20px; flex-wrap: wrap; }
.card-title { font-size: 18px; font-weight: 700; color: #0f172a; }
.card-desc { margin-top: 4px; color: #6b7280; }
.date-range { display: flex; align-items: end; gap: 12px; flex-wrap: wrap; }
.date-item { display: flex; flex-direction: column; gap: 6px; font-size: 12px; color: #64748b; }
.date-input { min-width: 160px; height: 38px; padding: 0 12px; border-radius: 10px; border: 1px solid #dbe4f0; background: #fff; }
.point-plus { color: #059669; font-weight: 700; }
.point-minus { color: #ea580c; font-weight: 700; }
@media (max-width: 960px) { .summary-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
@media (max-width: 640px) { .summary-grid { grid-template-columns: 1fr; } }
</style>
