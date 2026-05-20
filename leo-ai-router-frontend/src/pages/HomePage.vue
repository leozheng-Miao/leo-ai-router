<template>
  <main class="home-page">
    <section class="hero-section">
      <div class="home-shell hero-grid">
        <div class="hero-copy">
          <div class="hero-badge">
            <span class="hero-badge__dot"></span>
            统一 AI 模型网关
          </div>
          <h1 class="hero-title">一次接入，智能调度多家主流 AI 模型</h1>
          <p class="hero-desc">
            Leo AI Router 面向开发者和团队提供统一模型访问、智能路由、Fallback
            容灾与 OpenAI 协议兼容能力，让现有 SDK 调用可以平滑迁移到多模型体系。
          </p>
          <div class="hero-points" aria-label="核心能力">
            <span>OpenAI SDK 兼容</span>
            <span>成本/延迟/可用性路由</span>
            <span>统一 Key 管理</span>
          </div>
          <div class="hero-actions">
            <RouterLink v-if="isLoggedIn" to="/dashboard" class="action action--primary">
              <DashboardOutlined />
              进入控制台
            </RouterLink>
            <template v-else>
              <RouterLink to="/user/register" class="action action--primary">
                开始使用
                <ArrowRightOutlined />
              </RouterLink>
              <RouterLink to="/user/login" class="action action--secondary">登录账号</RouterLink>
            </template>
          </div>
        </div>

        <div class="code-panel" aria-label="OpenAI SDK base_url 示例">
          <div class="code-panel__top">
            <div class="code-tabs">
              <span class="code-tab code-tab--active">Python</span>
              <span class="code-tab">Node.js</span>
              <span class="code-tab">REST</span>
            </div>
            <span class="code-label">OpenAI Compatible</span>
          </div>
          <pre class="code-block"><code><span v-for="line in sdkCode" :key="line">{{ line }}
</span></code></pre>
          <div class="code-footer">
            <span>base_url</span>
            <strong>https://api.leoai.cn/v1</strong>
          </div>
        </div>
      </div>
    </section>

    <section class="home-shell section-block" aria-label="模型健康状态">
      <div class="section-heading">
        <div>
          <span class="section-kicker">Model Health</span>
          <h2>模型状态概览</h2>
        </div>
        <p>以下为产品展示数据，用于说明平台可观测能力，并非实时运行指标。</p>
      </div>
      <div class="model-health-grid">
        <ModelStatusCard
          v-for="model in modelHealth"
          :key="model.name"
          :name="model.name"
          :success-rate="model.successRate"
          :latency="model.latency"
          :status="model.status"
        />
      </div>
    </section>

    <section class="home-shell section-block">
      <div class="section-heading">
        <div>
          <span class="section-kicker">Dashboard Preview</span>
          <h2>平台运营视图</h2>
        </div>
        <p>以统一入口查看模型接入、路由策略、请求趋势和使用分布。</p>
      </div>

      <div class="metrics-grid">
        <MetricCard
          v-for="metric in platformMetrics"
          :key="metric.label"
          :label="metric.label"
          :value="metric.value"
          :trend="metric.trend"
          :trend-tone="metric.trendTone"
        />
      </div>

      <div class="dashboard-grid">
        <AppPanel>
          <div class="panel-title-row">
            <h3>路由策略</h3>
            <span>按业务目标自动选择模型</span>
          </div>
          <div class="strategy-list">
            <div v-for="strategy in routingStrategies" :key="strategy.name" class="strategy-row">
              <div>
                <strong>{{ strategy.name }}</strong>
                <p>{{ strategy.desc }}</p>
              </div>
              <span>{{ strategy.scene }}</span>
            </div>
          </div>
        </AppPanel>

        <AppPanel>
          <div class="panel-title-row">
            <h3>请求趋势</h3>
            <span>近 7 日展示</span>
          </div>
          <div class="trend-bars" aria-label="请求趋势图">
            <div v-for="point in requestTrend" :key="point.day" class="trend-item">
              <div class="trend-track">
                <span class="trend-fill" :style="{ height: `${point.value}%` }"></span>
              </div>
              <span>{{ point.day }}</span>
            </div>
          </div>
        </AppPanel>

        <AppPanel>
          <div class="panel-title-row">
            <h3>模型使用占比</h3>
            <span>产品展示</span>
          </div>
          <div class="usage-panel">
            <div class="donut" aria-hidden="true"></div>
            <div class="usage-list">
              <div v-for="item in usageShare" :key="item.name" class="usage-row">
                <span>
                  <i :style="{ background: item.color }"></i>
                  {{ item.name }}
                </span>
                <strong>{{ item.value }}%</strong>
              </div>
            </div>
          </div>
        </AppPanel>

        <AppPanel>
          <div class="panel-title-row">
            <h3>最新动态</h3>
            <span>平台活动</span>
          </div>
          <div class="activity-list">
            <div v-for="item in latestUpdates" :key="item.title" class="activity-row">
              <span class="activity-dot"></span>
              <div>
                <strong>{{ item.title }}</strong>
                <p>{{ item.desc }}</p>
              </div>
            </div>
          </div>
        </AppPanel>
      </div>
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { ArrowRightOutlined, DashboardOutlined } from '@ant-design/icons-vue'
import AppPanel from '@/components/ui/AppPanel.vue'
import MetricCard from '@/components/ui/MetricCard.vue'
import ModelStatusCard from '@/components/ui/ModelStatusCard.vue'
import { useLoginUserStore } from '@/stores/loginUser'

const loginUserStore = useLoginUserStore()
const isLoggedIn = computed(() => Boolean(loginUserStore.loginUser.id))

const sdkCode = [
  'from openai import OpenAI',
  '',
  'client = OpenAI(',
  '    api_key="sk-your-leo-api-key",',
  '    base_url="https://api.leoai.cn/v1"',
  ')',
  '',
  'response = client.chat.completions.create(',
  '    model="auto",',
  '    messages=[{"role": "user", "content": "Hello"}]',
  ')',
]

const modelHealth = [
  { name: 'DeepSeek', successRate: '98.5%', latency: '812ms', status: '健康' },
  { name: '通义千问', successRate: '97.6%', latency: '623ms', status: '健康' },
  { name: '智谱 GLM', successRate: '99.1%', latency: '742ms', status: '健康' },
  { name: 'OpenAI', successRate: '98.9%', latency: '721ms', status: '健康' },
  { name: 'Gemini', successRate: '97.2%', latency: '856ms', status: '健康' },
]

const platformMetrics: Array<{
  label: string
  value: string
  trend: string
  trendTone: 'success' | 'danger' | 'muted'
}> = [
  { label: '接入模型', value: '12', trend: '覆盖文本与图像', trendTone: 'muted' },
  { label: '路由策略', value: '5', trend: '支持自动 Fallback', trendTone: 'success' },
  { label: '今日请求', value: '128K', trend: '产品展示数据', trendTone: 'muted' },
  { label: '成功率', value: '98.7%', trend: '+0.6%', trendTone: 'success' },
  { label: '平均延迟', value: '751ms', trend: '-42ms', trendTone: 'success' },
  { label: '用户数', value: '3,240', trend: '团队与开发者', trendTone: 'muted' },
]

const routingStrategies = [
  { name: '智能综合路由', desc: '结合可用性、延迟与成功率进行综合评分。', scene: '默认推荐' },
  { name: '成本优先', desc: '优先选择单位调用成本更低的模型供应商。', scene: '批量任务' },
  { name: '延迟优先', desc: '优先使用近期响应速度更快的模型通道。', scene: '交互场景' },
  { name: '固定模型', desc: '为指定业务稳定绑定模型与供应商。', scene: '强一致输出' },
]

const requestTrend = [
  { day: 'Mon', value: 42 },
  { day: 'Tue', value: 56 },
  { day: 'Wed', value: 48 },
  { day: 'Thu', value: 74 },
  { day: 'Fri', value: 68 },
  { day: 'Sat', value: 52 },
  { day: 'Sun', value: 82 },
]

const usageShare = [
  { name: 'DeepSeek', value: 34, color: '#245bff' },
  { name: '通义千问', value: 24, color: '#12b76a' },
  { name: 'OpenAI', value: 18, color: '#f79009' },
  { name: 'Gemini', value: 14, color: '#7c3aed' },
  { name: '其他模型', value: 10, color: '#98a2b3' },
]

const latestUpdates = [
  { title: '新增 Gemini 图像模型入口', desc: '统一在模型广场展示会员可用能力。' },
  { title: '路由策略支持健康权重', desc: '异常通道可自动降权并触发 Fallback。' },
  { title: '控制台指标视图优化', desc: '请求、成本和延迟指标聚合展示。' },
]
</script>

<style scoped>
.home-page {
  min-height: calc(100vh - var(--leo-header-height));
  background: var(--leo-bg-page);
}

.home-shell {
  width: min(100%, 1180px);
  margin: 0 auto;
  padding: 0 24px;
}

.hero-section {
  padding: 56px 0 32px;
  background:
    linear-gradient(180deg, #ffffff 0%, rgba(255, 255, 255, 0) 76%),
    var(--leo-bg-page);
  border-bottom: 1px solid var(--leo-border);
}

.hero-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(420px, 0.9fr);
  gap: 40px;
  align-items: center;
}

.hero-copy {
  min-width: 0;
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 10px;
  color: var(--leo-primary);
  font-size: 13px;
  font-weight: 700;
  line-height: 18px;
  background: var(--leo-primary-soft);
  border: 1px solid #d7e2ff;
  border-radius: var(--leo-radius-md);
}

.hero-badge__dot {
  width: 7px;
  height: 7px;
  background: var(--leo-success);
  border-radius: 999px;
}

.hero-title {
  max-width: 680px;
  margin: 18px 0 0;
  color: var(--leo-text-primary);
  font-size: 48px;
  font-weight: 800;
  line-height: 1.12;
  letter-spacing: 0;
}

.hero-desc {
  max-width: 620px;
  margin: 20px 0 0;
  color: var(--leo-text-secondary);
  font-size: 17px;
  line-height: 1.75;
}

.hero-points {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 22px;
}

.hero-points span {
  padding: 6px 10px;
  color: var(--leo-text-secondary);
  font-size: 13px;
  font-weight: 600;
  line-height: 18px;
  background: #ffffff;
  border: 1px solid var(--leo-border);
  border-radius: var(--leo-radius-md);
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 28px;
}

.action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-width: 116px;
  height: 42px;
  padding: 0 18px;
  font-size: 14px;
  font-weight: 700;
  line-height: 20px;
  text-decoration: none;
  border-radius: var(--leo-radius-md);
  transition:
    border-color 0.16s,
    background 0.16s,
    color 0.16s;
}

.action--primary {
  color: #ffffff;
  background: var(--leo-primary);
  border: 1px solid var(--leo-primary);
}

.action--primary:hover {
  color: #ffffff;
  background: var(--leo-primary-hover);
  border-color: var(--leo-primary-hover);
}

.action--secondary {
  color: var(--leo-text-primary);
  background: #ffffff;
  border: 1px solid var(--leo-border-strong);
}

.action--secondary:hover {
  color: var(--leo-primary);
  border-color: var(--leo-primary);
}

.code-panel {
  min-width: 0;
  overflow: hidden;
  background: #0f172a;
  border: 1px solid #22304a;
  border-radius: var(--leo-radius-md);
  box-shadow: 0 14px 36px rgba(15, 23, 42, 0.14);
}

.code-panel__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px;
  background: #111c33;
  border-bottom: 1px solid #22304a;
}

.code-tabs {
  display: flex;
  gap: 6px;
  min-width: 0;
}

.code-tab,
.code-label {
  display: inline-flex;
  align-items: center;
  height: 26px;
  padding: 0 9px;
  font-size: 12px;
  font-weight: 700;
  line-height: 16px;
  white-space: nowrap;
  border-radius: var(--leo-radius-sm);
}

.code-tab {
  color: #94a3b8;
  background: rgba(148, 163, 184, 0.08);
}

.code-tab--active {
  color: #ffffff;
  background: var(--leo-primary);
}

.code-label {
  flex: 0 0 auto;
  color: #93c5fd;
  background: rgba(36, 91, 255, 0.14);
}

.code-block {
  min-height: 276px;
  margin: 0;
  padding: 18px;
  overflow-x: auto;
  color: #dbeafe;
  font-family: 'JetBrains Mono', 'SFMono-Regular', Consolas, monospace;
  font-size: 13px;
  line-height: 1.7;
}

.code-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 14px;
  color: #94a3b8;
  font-size: 12px;
  line-height: 18px;
  background: #111c33;
  border-top: 1px solid #22304a;
}

.code-footer strong {
  min-width: 0;
  overflow: hidden;
  color: #ffffff;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.section-block {
  padding-top: 36px;
}

.section-block:last-child {
  padding-bottom: 56px;
}

.section-heading {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 16px;
}

.section-kicker {
  display: block;
  margin-bottom: 6px;
  color: var(--leo-primary);
  font-size: 12px;
  font-weight: 800;
  line-height: 16px;
  text-transform: uppercase;
}

.section-heading h2 {
  margin: 0;
  color: var(--leo-text-primary);
  font-size: 24px;
  font-weight: 800;
  line-height: 32px;
}

.section-heading p {
  max-width: 430px;
  margin: 0;
  color: var(--leo-text-secondary);
  font-size: 13px;
  line-height: 20px;
  text-align: right;
}

.model-health-grid,
.metrics-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 12px;
}

.metrics-grid {
  grid-template-columns: repeat(6, minmax(0, 1fr));
  margin-bottom: 12px;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.panel-title-row {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.panel-title-row h3 {
  margin: 0;
  color: var(--leo-text-primary);
  font-size: 16px;
  font-weight: 800;
  line-height: 22px;
}

.panel-title-row span {
  color: var(--leo-text-tertiary);
  font-size: 12px;
  line-height: 18px;
  white-space: nowrap;
}

.strategy-list,
.activity-list {
  display: grid;
  gap: 10px;
}

.strategy-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 12px;
  align-items: center;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--leo-border);
}

.strategy-row:last-child {
  padding-bottom: 0;
  border-bottom: 0;
}

.strategy-row strong,
.activity-row strong {
  color: var(--leo-text-primary);
  font-size: 14px;
  line-height: 20px;
}

.strategy-row p,
.activity-row p {
  margin: 3px 0 0;
  color: var(--leo-text-secondary);
  font-size: 12px;
  line-height: 18px;
}

.strategy-row > span {
  padding: 4px 8px;
  color: var(--leo-primary);
  font-size: 12px;
  font-weight: 700;
  line-height: 16px;
  background: var(--leo-primary-soft);
  border-radius: var(--leo-radius-sm);
}

.trend-bars {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 10px;
  height: 202px;
  align-items: end;
}

.trend-item {
  display: grid;
  gap: 8px;
  min-width: 0;
  color: var(--leo-text-tertiary);
  font-size: 12px;
  line-height: 16px;
  text-align: center;
}

.trend-track {
  position: relative;
  height: 164px;
  overflow: hidden;
  background: var(--leo-bg-muted);
  border: 1px solid var(--leo-border);
  border-radius: var(--leo-radius-sm);
}

.trend-fill {
  position: absolute;
  right: 0;
  bottom: 0;
  left: 0;
  background: linear-gradient(180deg, #5b8cff 0%, var(--leo-primary) 100%);
  border-radius: var(--leo-radius-sm) var(--leo-radius-sm) 0 0;
}

.usage-panel {
  display: grid;
  grid-template-columns: 150px minmax(0, 1fr);
  gap: 22px;
  align-items: center;
}

.donut {
  width: 150px;
  aspect-ratio: 1;
  background: conic-gradient(
    #245bff 0 34%,
    #12b76a 34% 58%,
    #f79009 58% 76%,
    #7c3aed 76% 90%,
    #98a2b3 90% 100%
  );
  border-radius: 999px;
  box-shadow: inset 0 0 0 34px #ffffff;
}

.usage-list {
  display: grid;
  gap: 10px;
}

.usage-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: var(--leo-text-secondary);
  font-size: 13px;
  line-height: 18px;
}

.usage-row span {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.usage-row i {
  width: 8px;
  height: 8px;
  flex: 0 0 auto;
  border-radius: 999px;
}

.usage-row strong {
  color: var(--leo-text-primary);
  font-size: 13px;
}

.activity-row {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 10px;
  align-items: start;
}

.activity-dot {
  width: 8px;
  height: 8px;
  margin-top: 6px;
  background: var(--leo-success);
  border-radius: 999px;
  box-shadow: 0 0 0 4px rgba(18, 183, 106, 0.12);
}

@media (max-width: 1024px) {
  .hero-grid {
    grid-template-columns: 1fr;
  }

  .model-health-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .metrics-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .home-shell {
    padding: 0 16px;
  }

  .hero-section {
    padding-top: 32px;
  }

  .hero-title {
    font-size: 34px;
    line-height: 1.18;
  }

  .hero-desc {
    font-size: 15px;
  }

  .code-panel__top,
  .code-footer,
  .section-heading,
  .panel-title-row,
  .usage-panel {
    align-items: stretch;
    flex-direction: column;
  }

  .code-panel__top,
  .code-footer {
    display: grid;
  }

  .code-tabs {
    overflow-x: auto;
  }

  .section-heading {
    display: grid;
  }

  .section-heading p {
    max-width: none;
    text-align: left;
  }

  .model-health-grid,
  .metrics-grid,
  .dashboard-grid {
    grid-template-columns: 1fr;
  }

  .usage-panel {
    grid-template-columns: 1fr;
  }

  .donut {
    width: 132px;
    justify-self: center;
  }
}

@media (max-width: 480px) {
  .action {
    width: 100%;
  }

  .code-block {
    min-height: 250px;
    padding: 14px;
    font-size: 12px;
  }

  .strategy-row {
    grid-template-columns: 1fr;
    align-items: start;
  }
}
</style>
