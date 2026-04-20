<template>
  <div class="home-page">
    <!-- Hero -->
    <section class="hero">
      <div class="hero-badge">
        <span class="badge-dot"></span>
        支持 DeepSeek · 通义千问 · GLM · 更多模型持续接入
      </div>
      <h1 class="hero-title">
        统一接入，智能路由<br />
        <span class="hero-gradient">多种主流 AI 模型</span>
      </h1>
      <p class="hero-subtitle">
        兼容 OpenAI 协议 · 一个 API Key 调用全部模型<br />
        成本优先 / 延迟优先 / 自动 Fallback，开箱即用
      </p>
      <div class="hero-actions">
        <template v-if="loginUserStore.loginUser.id">
          <RouterLink to="/dashboard" class="btn-primary">
            <DashboardOutlined /> 进入控制台
          </RouterLink>
        </template>
        <template v-else>
          <RouterLink to="/user/register" class="btn-primary"> 免费开始使用 → </RouterLink>
          <RouterLink to="/user/login" class="btn-secondary">登录账号</RouterLink>
        </template>
      </div>

      <!-- Code preview -->
      <div class="hero-code">
        <div class="code-header">
          <div class="code-dots">
            <span class="dot red"></span>
            <span class="dot yellow"></span>
            <span class="dot green"></span>
          </div>
          <span class="code-title">快速接入示例</span>
          <a-tag color="green" style="margin-left: auto; font-size: 11px">兼容 OpenAI SDK</a-tag>
        </div>
        <pre
          class="code-body"
        ><code><span class="c-keyword">from</span> openai <span class="c-keyword">import</span> OpenAI

client = OpenAI(
    api_key=<span class="c-str">"sk-your-leo-api-key"</span>,
    base_url=<span class="c-str">"https://api.leoai.cn/v1"</span>
)

response = client.chat.completions.create(
    model=<span class="c-str">"auto"</span>,  <span class="c-comment"># 自动路由到最优模型</span>
    messages=[{<span class="c-str">"role"</span>: <span class="c-str">"user"</span>, <span class="c-str">"content"</span>: <span class="c-str">"Hello!"</span>}]
)</code></pre>
      </div>
    </section>

    <!-- Stats -->
    <section class="stats-section">
      <div class="container">
        <div class="stats-grid">
          <div v-for="stat in stats" :key="stat.label" class="stat-item">
            <div class="stat-value">{{ stat.value }}</div>
            <div class="stat-label">{{ stat.label }}</div>
          </div>
        </div>
      </div>
    </section>

    <!-- Features -->
    <section class="features-section">
      <div class="container">
        <div class="section-header">
          <div class="section-label">核心能力</div>
          <h2 class="section-title">为开发者和企业设计的 AI 网关</h2>
          <p class="section-desc">从接入到上线，一站式解决模型调用、稳定性、可观测性问题</p>
        </div>
        <div class="features-grid">
          <div v-for="f in features" :key="f.title" class="feature-card">
            <div class="feature-icon" :style="{ background: f.iconBg }">
              <component :is="f.icon" :style="{ color: f.iconColor, fontSize: '20px' }" />
            </div>
            <h3 class="feature-title">{{ f.title }}</h3>
            <p class="feature-desc">{{ f.desc }}</p>
            <ul class="feature-list">
              <li v-for="point in f.points" :key="point">
                <CheckCircleFilled class="check-icon" />
                {{ point }}
              </li>
            </ul>
          </div>
        </div>
      </div>
    </section>

    <!-- Models -->
    <section class="models-section">
      <div class="container">
        <div class="section-header">
          <div class="section-label">已接入模型</div>
          <h2 class="section-title">覆盖主流大模型，持续扩展</h2>
        </div>
        <div class="models-grid">
          <div v-for="m in models" :key="m.name" class="model-card">
            <div class="model-logo" :style="{ background: m.bg }">{{ m.abbr }}</div>
            <div class="model-info">
              <div class="model-name">{{ m.name }}</div>
              <div class="model-desc">{{ m.desc }}</div>
            </div>
            <a-tag :color="m.statusColor" class="model-status">{{ m.status }}</a-tag>
          </div>
        </div>
      </div>
    </section>

    <!-- CTA -->
    <section v-if="!loginUserStore.loginUser.id" class="cta-section">
      <div class="container">
        <div class="cta-card">
          <h2 class="cta-title">立即开始，免费使用</h2>
          <p class="cta-desc">注册即可获得免费调用配额，无需信用卡</p>
          <RouterLink to="/user/register" class="btn-cta">免费注册 →</RouterLink>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import {
  DashboardOutlined,
  ThunderboltOutlined,
  SafetyOutlined,
  ApiOutlined,
  BarChartOutlined,
  CheckCircleFilled,
  SwapOutlined,
} from '@ant-design/icons-vue'
import { useLoginUserStore } from '@/stores/loginUser'

const loginUserStore = useLoginUserStore()

const stats = [
  { value: '9+', label: '接入模型数' },
  { value: '3种', label: '路由策略' },
  { value: '99.9%', label: '可用性 SLA' },
  { value: '< 50ms', label: '平均路由延迟' },
]

const features = [
  {
    icon: SwapOutlined,
    iconBg: '#eff6ff',
    iconColor: '#2563eb',
    title: '智能模型路由',
    desc: '多维度路由策略，自动选择最优模型',
    points: ['成本优先 / 延迟优先 / 轮询', '自动 Fallback 容灾切换', '固定模型 / 综合评分路由'],
  },
  {
    icon: ApiOutlined,
    iconBg: '#f5f3ff',
    iconColor: '#7c3aed',
    title: 'OpenAI 协议兼容',
    desc: '无需修改代码，一键切换',
    points: ['完全兼容 OpenAI SDK', '支持流式输出 SSE', '支持深度思考模型'],
  },
  {
    icon: SafetyOutlined,
    iconBg: '#ecfdf5',
    iconColor: '#059669',
    title: '高可用与安全',
    desc: '企业级稳定性保障',
    points: ['健康检查 + 自动摘除', 'API Key 管理与鉴权', 'IP 黑名单 + 限流保护'],
  },
  {
    icon: BarChartOutlined,
    iconBg: '#fff7ed',
    iconColor: '#ea580c',
    title: '全链路可观测',
    desc: '每一次调用都可追踪',
    points: ['Token 消耗实时统计', '请求日志与链路追踪', '模型成本分析报表'],
  },
]

const models = [
  {
    abbr: 'DS',
    name: 'DeepSeek',
    desc: 'deepseek-chat · deepseek-reasoner',
    bg: 'linear-gradient(135deg,#e0f2fe,#bae6fd)',
    status: '已接入',
    statusColor: 'green',
  },
  {
    abbr: 'QW',
    name: '通义千问',
    desc: 'qwen-plus · qwen-max · qwen-turbo',
    bg: 'linear-gradient(135deg,#fef3c7,#fde68a)',
    status: '已接入',
    statusColor: 'green',
  },
  {
    abbr: 'ZP',
    name: '智谱 AI',
    desc: 'glm-4.7 · glm-4.7-flash',
    bg: 'linear-gradient(135deg,#f0fdf4,#bbf7d0)',
    status: '已接入',
    statusColor: 'green',
  },
  {
    abbr: 'GPT',
    name: 'OpenAI',
    desc: 'gpt-4o · gpt-4-turbo',
    bg: 'linear-gradient(135deg,#f5f3ff,#ddd6fe)',
    status: '计划中',
    statusColor: 'orange',
  },
]
</script>

<style scoped>
.home-page {
  min-height: calc(100vh - 56px);
  background: #fafafa;
}

.container {
  max-width: 1100px;
  margin: 0 auto;
  padding: 0 24px;
}

/* Hero */
.hero {
  text-align: center;
  padding: 80px 24px 64px;
  background: linear-gradient(180deg, #f8faff 0%, #fafafa 100%);
  border-bottom: 1px solid #e5e7eb;
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 20px;
  padding: 5px 14px;
  font-size: 12px;
  color: #6b7280;
  font-weight: 500;
  margin-bottom: 28px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
}

.badge-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #10b981;
  box-shadow: 0 0 0 2px rgba(16, 185, 129, 0.2);
  flex-shrink: 0;
}

.hero-title {
  font-size: 52px;
  font-weight: 800;
  line-height: 1.15;
  color: #111827;
  margin: 0 0 20px;
  letter-spacing: -1.5px;
}

.hero-gradient {
  background: linear-gradient(135deg, #2563eb 0%, #7c3aed 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.hero-subtitle {
  font-size: 17px;
  color: #6b7280;
  line-height: 1.8;
  margin: 0 0 36px;
}

.hero-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-bottom: 52px;
}

.btn-primary {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  height: 44px;
  padding: 0 24px;
  border-radius: 10px;
  background: linear-gradient(135deg, #2563eb, #7c3aed);
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  text-decoration: none;
  transition: opacity 0.15s;
  border: none;
}

.btn-primary:hover {
  opacity: 0.88;
}

.btn-secondary {
  display: inline-flex;
  align-items: center;
  height: 44px;
  padding: 0 24px;
  border-radius: 10px;
  background: #fff;
  color: #374151;
  font-size: 15px;
  font-weight: 500;
  text-decoration: none;
  border: 1px solid #e5e7eb;
  transition: all 0.15s;
}

.btn-secondary:hover {
  border-color: #d1d5db;
  background: #f9fafb;
}

/* Code block */
.hero-code {
  max-width: 600px;
  margin: 0 auto;
  background: #1e1e2e;
  border-radius: 12px;
  overflow: hidden;
  text-align: left;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
  border: 1px solid #2d2d3d;
}

.code-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: #16161f;
  border-bottom: 1px solid #2d2d3d;
}

.code-dots {
  display: flex;
  gap: 6px;
}
.dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}
.dot.red {
  background: #ff5f57;
}
.dot.yellow {
  background: #febc2e;
}
.dot.green {
  background: #28c840;
}

.code-title {
  font-size: 12px;
  color: #6b7280;
  font-weight: 500;
}

.code-body {
  margin: 0;
  padding: 20px 20px 24px;
  font-family: 'JetBrains Mono', 'Fira Code', monospace;
  font-size: 13px;
  line-height: 1.7;
  color: #cdd6f4;
  overflow-x: auto;
}

.c-keyword {
  color: #cba6f7;
}
.c-str {
  color: #a6e3a1;
}
.c-comment {
  color: #6c7086;
}

/* Stats */
.stats-section {
  padding: 40px 0;
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 1px;
  background: #e5e7eb;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  overflow: hidden;
}

.stat-item {
  padding: 28px 24px;
  text-align: center;
  background: #fff;
}

.stat-value {
  font-size: 32px;
  font-weight: 800;
  color: #111827;
  letter-spacing: -1px;
  line-height: 1;
  margin-bottom: 6px;
}

.stat-label {
  font-size: 13px;
  color: #9ca3af;
  font-weight: 500;
}

/* Features */
.features-section {
  padding: 80px 0;
  background: #fafafa;
}

.section-header {
  text-align: center;
  margin-bottom: 48px;
}

.section-label {
  display: inline-block;
  font-size: 12px;
  font-weight: 600;
  color: #2563eb;
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  border-radius: 20px;
  padding: 3px 12px;
  margin-bottom: 16px;
  letter-spacing: 0.5px;
  text-transform: uppercase;
}

.section-title {
  font-size: 32px;
  font-weight: 700;
  color: #111827;
  margin: 0 0 12px;
  letter-spacing: -0.5px;
}

.section-desc {
  font-size: 15px;
  color: #6b7280;
  margin: 0;
}

.features-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
}

.feature-card {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 14px;
  padding: 28px;
  transition: all 0.2s;
}

.feature-card:hover {
  border-color: #bfdbfe;
  box-shadow: 0 8px 24px rgba(37, 99, 235, 0.06);
  transform: translateY(-2px);
}

.feature-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 16px;
}

.feature-title {
  font-size: 17px;
  font-weight: 700;
  color: #111827;
  margin: 0 0 8px;
}

.feature-desc {
  font-size: 13px;
  color: #9ca3af;
  margin: 0 0 16px;
}

.feature-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.feature-list li {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #374151;
}

.check-icon {
  color: #10b981;
  font-size: 13px;
  flex-shrink: 0;
}

/* Models */
.models-section {
  padding: 80px 0;
  background: #fff;
  border-top: 1px solid #e5e7eb;
}

.models-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.model-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  background: #fff;
  transition: all 0.15s;
}

.model-card:hover {
  border-color: #d1d5db;
  background: #f9fafb;
}

.model-logo {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 700;
  color: #374151;
  flex-shrink: 0;
}

.model-info {
  flex: 1;
  min-width: 0;
}
.model-name {
  font-size: 14px;
  font-weight: 600;
  color: #111827;
}
.model-desc {
  font-size: 12px;
  color: #9ca3af;
  margin-top: 2px;
}
.model-status {
  flex-shrink: 0;
  font-size: 11px;
}

/* CTA */
.cta-section {
  padding: 80px 0;
  background: #fafafa;
}

.cta-card {
  background: linear-gradient(135deg, #1e3a8a 0%, #4c1d95 100%);
  border-radius: 20px;
  padding: 64px 48px;
  text-align: center;
}

.cta-title {
  font-size: 34px;
  font-weight: 800;
  color: #fff;
  margin: 0 0 12px;
  letter-spacing: -0.5px;
}

.cta-desc {
  font-size: 15px;
  color: rgba(255, 255, 255, 0.75);
  margin: 0 0 32px;
}

.btn-cta {
  display: inline-block;
  height: 46px;
  line-height: 46px;
  padding: 0 28px;
  border-radius: 10px;
  background: #fff;
  color: #2563eb;
  font-size: 15px;
  font-weight: 700;
  text-decoration: none;
  transition: opacity 0.15s;
}

.btn-cta:hover {
  opacity: 0.92;
}

/* Responsive */
@media (max-width: 768px) {
  .hero-title {
    font-size: 34px;
  }
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .features-grid {
    grid-template-columns: 1fr;
  }
  .models-grid {
    grid-template-columns: 1fr;
  }
}
</style>
