<template>
  <div class="membership-console">
    <section class="account-overview">
      <div>
        <div class="eyebrow">Billing console</div>
        <h1>会员与积分</h1>
        <p>套餐负责聊天和 API 中转权益，积分负责图片生成消耗。</p>
      </div>
      <div class="overview-grid">
        <div class="overview-item">
          <span>当前套餐</span>
          <strong>{{ membership.planName || '免费版' }}</strong>
        </div>
        <div class="overview-item">
          <span>普通剩余</span>
          <strong>{{ formatLimit(membership.dailyProRemaining) }}</strong>
        </div>
        <div class="overview-item">
          <span>高级剩余</span>
          <strong>{{ formatLimit(membership.dailyAdvancedRemaining) }}</strong>
        </div>
        <div class="overview-item">
          <span>积分余额</span>
          <strong>{{ formatNumber(membership.pointBalance) }}</strong>
        </div>
      </div>
    </section>

    <section class="billing-layout">
      <main class="catalog-panel">
        <div class="catalog-head">
          <a-segmented v-model:value="activeTab" :options="tabOptions" />
          <span class="catalog-note">
            {{ activeTab === 'plans' ? '套餐用于聊天和 API 中转额度' : '积分用于图片生成，永久有效' }}
          </span>
        </div>

        <div v-if="activeTab === 'plans'" class="plan-table">
          <button
            v-for="plan in plans"
            :key="plan.planCode"
            type="button"
            class="plan-row"
            :class="{ selected: selectedPlan?.planCode === plan.planCode }"
            @click="selectedPlan = plan"
          >
            <div class="plan-main">
              <div class="plan-title">
                <CrownOutlined />
                <strong>{{ plan.planName }}</strong>
                <a-tag v-if="plan.lifetime === 1" color="blue">长期</a-tag>
              </div>
              <p>{{ plan.description || '适合持续使用多模型聊天的账号' }}</p>
            </div>
            <div class="plan-metrics">
              <div><span>普通/日</span><strong>{{ formatLimit(plan.dailyProLimit) }}</strong></div>
              <div><span>高级/日</span><strong>{{ formatLimit(plan.dailyAdvancedLimit) }}</strong></div>
              <div><span>赠送积分</span><strong>{{ formatNumber(plan.bonusPoints) }}</strong></div>
            </div>
            <div class="plan-price">
              <strong>¥{{ formatMoney(plan.price) }}</strong>
              <span>{{ plan.lifetime === 1 ? '永久' : `${plan.durationDays || 0} 天` }}</span>
            </div>
          </button>
        </div>

        <div v-else class="point-grid">
          <button
            v-for="pkg in pointPackages"
            :key="pkg.packageCode"
            type="button"
            class="point-card"
            :class="{ selected: selectedPackage?.packageCode === pkg.packageCode }"
            @click="selectedPackage = pkg"
          >
            <div class="point-head">
              <WalletOutlined />
              <a-tag v-if="pkg.badge" color="blue">{{ pkg.badge }}</a-tag>
            </div>
            <strong>{{ formatNumber(pkg.points) }}</strong>
            <span>积分</span>
            <div class="point-price">¥{{ formatMoney(pkg.price) }}</div>
            <p>¥{{ pointUnitPrice(pkg) }}/积分</p>
          </button>
        </div>
      </main>

      <aside class="summary-panel">
        <div class="summary-title">支付摘要</div>
        <div class="summary-product">
          <span>{{ activeTab === 'plans' ? '订阅套餐' : '积分包' }}</span>
          <strong>{{ currentName }}</strong>
        </div>
        <div class="summary-lines">
          <div>
            <span>商品金额</span>
            <strong>¥{{ formatMoney(currentPrice) }}</strong>
          </div>
          <div>
            <span>支付方式</span>
            <strong>{{ paymentMethodText[paymentMethod] }}</strong>
          </div>
        </div>
        <a-radio-group v-model:value="paymentMethod" class="pay-methods">
          <a-radio-button value="alipay">支付宝</a-radio-button>
          <a-radio-button value="stripe">Stripe</a-radio-button>
          <a-tooltip title="微信支付暂未开放">
            <a-radio-button value="wechat" disabled>微信</a-radio-button>
          </a-tooltip>
        </a-radio-group>
        <a-button
          type="primary"
          block
          size="large"
          class="pay-button"
          :loading="Boolean(submitting)"
          @click="submitOrder"
        >
          立即支付 ¥{{ formatMoney(currentPrice) }}
        </a-button>
        <div class="summary-footer">
          <CheckCircleOutlined />
          支付成功后权益实时到账，可在个人中心账单查看记录。
        </div>
      </aside>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { CheckCircleOutlined, CrownOutlined, WalletOutlined } from '@ant-design/icons-vue'
import {
  getMyMembership,
  listMembershipPlans,
  listPointPackages,
  type MembershipVO,
  type PointPackage,
  type SubscriptionPlan,
} from '@/api/membershipController'
import { createPointsOrder, createSubscriptionOrder, type PaymentOrderVO } from '@/api/paymentOrderController'

const tabOptions = [
  { label: '套餐订阅', value: 'plans' },
  { label: '购买积分', value: 'points' },
]

const paymentMethodText: Record<string, string> = {
  alipay: '支付宝',
  stripe: 'Stripe',
  wechat: '微信',
}

const activeTab = ref<'plans' | 'points'>('plans')
const paymentMethod = ref<'alipay' | 'stripe' | 'wechat'>('alipay')
const plans = ref<SubscriptionPlan[]>([])
const pointPackages = ref<PointPackage[]>([])
const membership = ref<MembershipVO>({})
const selectedPlan = ref<SubscriptionPlan>()
const selectedPackage = ref<PointPackage>()
const submitting = ref('')

const currentName = computed(() =>
  activeTab.value === 'plans'
    ? selectedPlan.value?.planName || '未选择套餐'
    : selectedPackage.value?.packageName || '未选择积分包',
)

const currentPrice = computed(() =>
  activeTab.value === 'plans'
    ? selectedPlan.value?.price
    : selectedPackage.value?.price,
)

const formatNumber = (value?: number) => Number(value ?? 0).toLocaleString('zh-CN')
const formatMoney = (value?: number) => Number(value ?? 0).toFixed(2)
const formatLimit = (value?: number) => (value === -1 ? '无限' : `${formatNumber(value)}次`)
const pointUnitPrice = (pkg: PointPackage) => {
  if (!pkg.points || !pkg.price) return '0.000'
  return (pkg.price / pkg.points).toFixed(3)
}

const loadData = async () => {
  const [planRes, packageRes, membershipRes] = await Promise.all([
    listMembershipPlans(),
    listPointPackages(),
    getMyMembership(),
  ])
  if (planRes.data.code === 0) {
    plans.value = (planRes.data.data ?? []).filter((item) => item.planCode !== 'free')
    selectedPlan.value = plans.value[0]
  }
  if (packageRes.data.code === 0) {
    pointPackages.value = packageRes.data.data ?? []
    selectedPackage.value = pointPackages.value[0]
  }
  if (membershipRes.data.code === 0) {
    membership.value = membershipRes.data.data ?? {}
  }
}

const submitOrder = async () => {
  if (activeTab.value === 'plans' && !selectedPlan.value?.planCode) {
    message.warning('请选择套餐')
    return
  }
  if (activeTab.value === 'points' && !selectedPackage.value?.packageCode) {
    message.warning('请选择积分包')
    return
  }
  if (paymentMethod.value === 'wechat') {
    message.info('微信支付暂未开放，请选择支付宝或 Stripe')
    return
  }
  submitting.value = paymentMethod.value
  try {
    const res =
      activeTab.value === 'plans'
        ? await createSubscriptionOrder({ planCode: selectedPlan.value?.planCode, paymentMethod: paymentMethod.value })
        : await createPointsOrder({ packageCode: selectedPackage.value?.packageCode, paymentMethod: paymentMethod.value })
    if (res.data.code !== 0 || !res.data.data) {
      message.error(res.data.message ?? '创建支付订单失败')
      return
    }
    handlePayment(res.data.data)
  } finally {
    submitting.value = ''
  }
}

const handlePayment = (order: PaymentOrderVO) => {
  if (order.displayType === 'disabled' || order.status === 'coming_soon') {
    message.info('该支付方式暂未开放')
    return
  }
  if (order.displayType === 'redirect_url' && order.redirectUrl) {
    window.location.href = order.redirectUrl
    return
  }
  if (order.displayType === 'form_html' && order.formHtml) {
    const payWindow = window.open('', '_blank')
    if (!payWindow) {
      message.warning('浏览器拦截了支付窗口，请允许弹窗后重试')
      return
    }
    payWindow.document.open()
    payWindow.document.write(order.formHtml)
    payWindow.document.close()
    return
  }
  message.error('未获取到有效的支付信息')
}

watch(activeTab, (value) => {
  if (value === 'plans' && !selectedPlan.value && plans.value.length > 0) {
    selectedPlan.value = plans.value[0]
  }
  if (value === 'points' && !selectedPackage.value && pointPackages.value.length > 0) {
    selectedPackage.value = pointPackages.value[0]
  }
})

onMounted(loadData)
</script>

<style scoped>
.membership-console {
  min-height: calc(100vh - var(--leo-header-height) - 50px);
  color: var(--leo-text-primary);
}

.account-overview {
  display: grid;
  grid-template-columns: minmax(260px, 1fr) minmax(520px, 1.4fr);
  gap: 20px;
  align-items: end;
  margin-bottom: 18px;
}

.eyebrow {
  color: var(--leo-primary);
  font-size: 12px;
  font-weight: 800;
  text-transform: uppercase;
}

h1 {
  margin: 6px 0 6px;
  font-size: 30px;
  line-height: 1.2;
}

p {
  margin: 0;
  color: var(--leo-text-secondary);
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.overview-item {
  border: 1px solid var(--leo-border);
  border-radius: var(--leo-radius-md);
  background: var(--leo-bg-panel);
  padding: 12px 14px;
}

.overview-item span {
  display: block;
  color: var(--leo-text-secondary);
  font-size: 12px;
}

.overview-item strong {
  display: block;
  margin-top: 6px;
  font-size: 18px;
}

.billing-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 18px;
  align-items: start;
}

.catalog-panel,
.summary-panel {
  border: 1px solid var(--leo-border);
  border-radius: var(--leo-radius-md);
  background: var(--leo-bg-panel);
}

.catalog-panel {
  padding: 18px;
}

.catalog-head {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  align-items: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.catalog-note {
  color: var(--leo-text-secondary);
  font-size: 13px;
}

.plan-table {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.plan-row {
  width: 100%;
  border: 1px solid var(--leo-border);
  border-radius: var(--leo-radius-md);
  background: var(--leo-bg-panel);
  padding: 16px;
  display: grid;
  grid-template-columns: minmax(220px, 1.3fr) minmax(320px, 1.5fr) 128px;
  gap: 16px;
  text-align: left;
  cursor: pointer;
  transition: border-color 0.16s, box-shadow 0.16s, background 0.16s;
}

.plan-row:hover,
.plan-row.selected,
.point-card:hover,
.point-card.selected {
  border-color: var(--leo-primary);
  background: var(--leo-bg-active);
  box-shadow: 0 8px 26px rgba(36, 91, 255, 0.08);
}

.plan-title {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--leo-text-primary);
}

.plan-title :deep(svg) {
  color: var(--leo-primary);
}

.plan-main p {
  margin-top: 8px;
  font-size: 13px;
  line-height: 1.6;
}

.plan-metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.plan-metrics div {
  border-radius: var(--leo-radius-md);
  background: var(--leo-bg-muted);
  padding: 10px;
}

.plan-metrics span,
.plan-price span {
  display: block;
  color: var(--leo-text-secondary);
  font-size: 12px;
}

.plan-metrics strong,
.plan-price strong {
  display: block;
  margin-top: 5px;
  color: var(--leo-text-primary);
}

.plan-price {
  text-align: right;
}

.plan-price strong {
  color: var(--leo-primary);
  font-size: 24px;
}

.point-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.point-card {
  min-height: 188px;
  border: 1px solid var(--leo-border);
  border-radius: var(--leo-radius-md);
  background: var(--leo-bg-panel);
  padding: 16px;
  text-align: left;
  cursor: pointer;
  transition: border-color 0.16s, box-shadow 0.16s, background 0.16s;
}

.point-head {
  min-height: 24px;
  display: flex;
  justify-content: space-between;
  gap: 8px;
  color: var(--leo-primary);
}

.point-card > strong {
  display: block;
  margin-top: 18px;
  font-size: 30px;
  color: var(--leo-text-primary);
}

.point-card > span,
.point-card p {
  color: var(--leo-text-secondary);
  font-size: 13px;
}

.point-price {
  margin-top: 18px;
  color: var(--leo-primary);
  font-size: 22px;
  font-weight: 800;
}

.summary-panel {
  position: sticky;
  top: 76px;
  padding: 18px;
}

.summary-title {
  font-weight: 800;
  margin-bottom: 16px;
}

.summary-product {
  border: 1px solid var(--leo-border);
  border-radius: var(--leo-radius-md);
  background: var(--leo-bg-muted);
  padding: 14px;
}

.summary-product span,
.summary-lines span {
  color: var(--leo-text-secondary);
  font-size: 12px;
}

.summary-product strong {
  display: block;
  margin-top: 6px;
  font-size: 18px;
}

.summary-lines {
  margin: 16px 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.summary-lines div {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.pay-methods {
  width: 100%;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  margin-bottom: 14px;
}

.pay-methods :deep(.ant-radio-button-wrapper) {
  text-align: center;
}

.pay-button {
  border-radius: var(--leo-radius-md);
  font-weight: 700;
}

.summary-footer {
  margin-top: 14px;
  display: flex;
  align-items: flex-start;
  gap: 8px;
  color: var(--leo-text-secondary);
  font-size: 12px;
  line-height: 1.6;
}

.summary-footer :deep(svg) {
  margin-top: 3px;
  color: var(--leo-success);
}

@media (max-width: 1100px) {
  .account-overview,
  .billing-layout {
    grid-template-columns: 1fr;
  }

  .summary-panel {
    position: static;
  }

  .plan-row {
    grid-template-columns: 1fr;
  }

  .plan-price {
    text-align: left;
  }
}

@media (max-width: 720px) {
  .overview-grid,
  .point-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .plan-metrics {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 480px) {
  .overview-grid,
  .point-grid,
  .pay-methods {
    grid-template-columns: 1fr;
  }
}
</style>
