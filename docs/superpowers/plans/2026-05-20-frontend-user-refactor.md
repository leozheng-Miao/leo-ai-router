# LeoAI Router 用户端前端重构 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 LeoAI Router 用户端重构为统一的白底 SaaS 控制台风格，先完成首页、控制台、聊天、绘图、会员、个人中心、API Keys 和历史记录。

**Architecture:** 保留 Vue 3 + TypeScript + Vite + Pinia + Vue Router，不改后端 API、认证、SSE、会员权益和积分业务。新增轻量视觉组件与用户端壳层，Ant Design Vue 仅继续承担复杂交互组件，页面按 dashboard、home、chat、membership、image、user-center 分批落地。

**Tech Stack:** Vue 3, TypeScript, Vite, Pinia, Vue Router, Ant Design Vue, CSS variables, existing generated API clients.

---

## File Structure

Create:

- `leo-ai-router-frontend/src/styles/tokens.css`: 全局 design tokens、基础布局变量和通用 utility class。
- `leo-ai-router-frontend/src/components/ui/AppButton.vue`: 自研主按钮/次按钮/轻量按钮。
- `leo-ai-router-frontend/src/components/ui/AppPanel.vue`: 统一面板容器。
- `leo-ai-router-frontend/src/components/ui/MetricCard.vue`: 指标卡。
- `leo-ai-router-frontend/src/components/ui/ModelStatusCard.vue`: 模型状态卡。
- `leo-ai-router-frontend/src/components/ui/EmptyState.vue`: 空状态。
- `leo-ai-router-frontend/src/components/ui/ErrorPanel.vue`: 加载失败状态。
- `leo-ai-router-frontend/src/components/ui/SidebarNav.vue`: 用户中心左侧导航。
- `leo-ai-router-frontend/src/layouts/UserCenterLayout.vue`: `/profile`、`/keys`、`/history`、`/membership` 复用壳层。
- `leo-ai-router-frontend/src/pages/DashboardPage.vue`: 新控制台概览页。

Modify:

- `leo-ai-router-frontend/src/main.ts`: 引入 `styles/tokens.css`。
- `leo-ai-router-frontend/src/App.vue`: 移除旧主题渐变 token，交给 tokens.css。
- `leo-ai-router-frontend/src/router/index.ts`: 新增 `/dashboard`，保持原有用户端路由。
- `leo-ai-router-frontend/src/components/GlobalHeader.vue`: 重构顶部导航与头像菜单。
- `leo-ai-router-frontend/src/layouts/BasicLayout.vue`: 适配新 App Shell。
- `leo-ai-router-frontend/src/pages/HomePage.vue`: 改为参考图首页/概览风格。
- `leo-ai-router-frontend/src/pages/chat/ChatPage.vue`: 改为三栏工作台，不改 SSE 与消息业务逻辑。
- `leo-ai-router-frontend/src/pages/image/ImageGenerationPage.vue`: 改为绘图工作台，不改生成业务逻辑。
- `leo-ai-router-frontend/src/pages/user/MembershipPage.vue`: 改为用户中心 + 支付摘要布局。
- `leo-ai-router-frontend/src/pages/user/ProfilePage.vue`: 放入用户中心壳层并统一视觉。
- `leo-ai-router-frontend/src/pages/user/ApiKeyPage.vue`: 放入用户中心壳层并统一视觉。
- `leo-ai-router-frontend/src/pages/user/HistoryPage.vue`: 放入用户中心壳层并统一视觉。
- `leo-ai-router-frontend/src/components/DailyStatsTrendChart.vue`: 调整颜色、边框、tooltip 与新 tokens 对齐。

Verification commands:

```bash
cd leo-ai-router-frontend
npm run type-check
npm run build-only
```

Use bundled Node if the system Node is too old:

```bash
cd leo-ai-router-frontend
PATH=/Users/zhengsmacbook/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/bin:$PATH npm run type-check
PATH=/Users/zhengsmacbook/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/bin:$PATH npm run build-only
```

---

### Task 1: Design Tokens and Shared UI Components

**Files:**
- Create: `leo-ai-router-frontend/src/styles/tokens.css`
- Create: `leo-ai-router-frontend/src/components/ui/AppButton.vue`
- Create: `leo-ai-router-frontend/src/components/ui/AppPanel.vue`
- Create: `leo-ai-router-frontend/src/components/ui/MetricCard.vue`
- Create: `leo-ai-router-frontend/src/components/ui/ModelStatusCard.vue`
- Create: `leo-ai-router-frontend/src/components/ui/EmptyState.vue`
- Create: `leo-ai-router-frontend/src/components/ui/ErrorPanel.vue`
- Modify: `leo-ai-router-frontend/src/main.ts`
- Modify: `leo-ai-router-frontend/src/App.vue`
- Test: `leo-ai-router-frontend/src/components/ui/*.vue`

- [ ] **Step 1: Create `tokens.css`**

```css
:root {
  --leo-bg-page: #f6f8fc;
  --leo-bg-panel: #ffffff;
  --leo-bg-muted: #f9fafd;
  --leo-bg-active: #eef3ff;
  --leo-primary: #245bff;
  --leo-primary-hover: #1d4ed8;
  --leo-primary-soft: #eaf0ff;
  --leo-text-primary: #0f172a;
  --leo-text-secondary: #667085;
  --leo-text-tertiary: #98a2b3;
  --leo-border: #e6eaf2;
  --leo-border-strong: #c8d2e5;
  --leo-success: #12b76a;
  --leo-warning: #f79009;
  --leo-danger: #f04438;
  --leo-radius-sm: 6px;
  --leo-radius-md: 8px;
  --leo-radius-lg: 12px;
  --leo-shadow-pop: 0 16px 40px rgba(15, 23, 42, 0.12);
  --leo-header-height: 56px;
  --leo-page-max: 1440px;
}

* {
  box-sizing: border-box;
}

html,
body,
#app {
  min-height: 100%;
}

body {
  margin: 0;
  background: var(--leo-bg-page);
  color: var(--leo-text-primary);
  font-family:
    -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', 'Oxygen', 'Ubuntu', 'Cantarell',
    'Fira Sans', 'Droid Sans', 'Helvetica Neue', sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

.leo-page {
  width: min(100%, var(--leo-page-max));
  margin: 0 auto;
  padding: 24px;
}

.leo-workspace {
  width: 100%;
  min-height: calc(100vh - var(--leo-header-height));
  padding: 16px;
}

.leo-card-grid {
  display: grid;
  gap: 12px;
}
```

- [ ] **Step 2: Import tokens in `main.ts`**

```ts
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import Antd from 'ant-design-vue'
import 'ant-design-vue/dist/reset.css'
import './styles/tokens.css'

import App from './App.vue'
import router from './router'

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.use(Antd)

app.mount('#app')
```

- [ ] **Step 3: Simplify `App.vue` global styles**

Keep only the app mount and remove old gradient-heavy theme variables.

```vue
<script setup lang="ts">
import BasicLayout from '@/layouts/BasicLayout.vue'
import { onMounted } from 'vue'
import { useLoginUserStore } from '@/stores/loginUser'

const loginUserStore = useLoginUserStore()

onMounted(async () => {
  await loginUserStore.fetchLoginUser()
})
</script>

<template>
  <BasicLayout />
</template>
```

- [ ] **Step 4: Add shared components**

`AppPanel.vue`:

```vue
<template>
  <section class="app-panel" :class="{ 'app-panel--flush': flush }">
    <slot />
  </section>
</template>

<script setup lang="ts">
defineProps<{ flush?: boolean }>()
</script>

<style scoped>
.app-panel {
  background: var(--leo-bg-panel);
  border: 1px solid var(--leo-border);
  border-radius: var(--leo-radius-md);
  padding: 18px;
}
.app-panel--flush {
  padding: 0;
}
</style>
```

`MetricCard.vue`:

```vue
<template>
  <div class="metric-card">
    <span class="metric-label">{{ label }}</span>
    <strong class="metric-value">{{ value }}</strong>
    <span v-if="trend" class="metric-trend" :class="trendTone">{{ trend }}</span>
  </div>
</template>

<script setup lang="ts">
defineProps<{ label: string; value: string | number; trend?: string; trendTone?: 'success' | 'danger' | 'muted' }>()
</script>

<style scoped>
.metric-card {
  min-height: 108px;
  padding: 18px;
  background: var(--leo-bg-panel);
  border: 1px solid var(--leo-border);
  border-radius: var(--leo-radius-md);
}
.metric-label { color: var(--leo-text-secondary); font-size: 13px; }
.metric-value { display: block; margin-top: 14px; color: var(--leo-text-primary); font-size: 28px; line-height: 1; }
.metric-trend { display: block; margin-top: 12px; font-size: 12px; color: var(--leo-text-tertiary); }
.metric-trend.success { color: var(--leo-success); }
.metric-trend.danger { color: var(--leo-danger); }
.metric-trend.muted { color: var(--leo-text-tertiary); }
</style>
```

- [ ] **Step 5: Run type-check**

Run:

```bash
cd leo-ai-router-frontend
PATH=/Users/zhengsmacbook/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/bin:$PATH npm run type-check
```

Expected: command exits 0.

- [ ] **Step 6: Commit**

```bash
git add leo-ai-router-frontend/src/styles/tokens.css leo-ai-router-frontend/src/components/ui leo-ai-router-frontend/src/main.ts leo-ai-router-frontend/src/App.vue
git commit -m "feat: add frontend design tokens and ui primitives"
```

---

### Task 2: App Shell, Navigation, User Center Layout, and Router

**Files:**
- Create: `leo-ai-router-frontend/src/components/ui/SidebarNav.vue`
- Create: `leo-ai-router-frontend/src/layouts/UserCenterLayout.vue`
- Modify: `leo-ai-router-frontend/src/router/index.ts`
- Modify: `leo-ai-router-frontend/src/components/GlobalHeader.vue`
- Modify: `leo-ai-router-frontend/src/layouts/BasicLayout.vue`

- [ ] **Step 1: Add `/dashboard` route**

Add this route after `/` in `router/index.ts`:

```ts
{
  path: '/dashboard',
  name: 'dashboard',
  component: () => import('@/pages/DashboardPage.vue'),
},
```

- [ ] **Step 2: Replace top navigation labels**

In `GlobalHeader.vue`, use this user-facing nav order:

```ts
const navItems = computed(() => [
  { label: '首页', path: '/' },
  { label: '控制台', path: '/dashboard', auth: true },
  { label: '在线对话', path: '/chat' },
  { label: 'AI 绘图', path: '/images' },
  { label: '会员充值', path: '/membership' },
])
```

Keep admin management links out of the top navigation in phase one; admin routes stay reachable by direct URL and are redesigned in phase two.

- [ ] **Step 3: Create `SidebarNav.vue`**

```vue
<template>
  <nav class="sidebar-nav">
    <RouterLink v-for="item in items" :key="item.path" :to="item.path" class="sidebar-nav__item">
      <component :is="item.icon" v-if="item.icon" />
      <span>{{ item.label }}</span>
    </RouterLink>
  </nav>
</template>

<script setup lang="ts">
defineProps<{ items: Array<{ label: string; path: string; icon?: unknown }> }>()
</script>

<style scoped>
.sidebar-nav {
  display: grid;
  gap: 4px;
}
.sidebar-nav__item {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 38px;
  padding: 0 12px;
  border-radius: var(--leo-radius-sm);
  color: var(--leo-text-secondary);
  text-decoration: none;
  font-size: 14px;
}
.sidebar-nav__item.router-link-active {
  color: var(--leo-primary);
  background: var(--leo-bg-active);
}
</style>
```

- [ ] **Step 4: Create `UserCenterLayout.vue`**

```vue
<template>
  <div class="user-center-layout">
    <aside class="user-center-sidebar">
      <RouterLink to="/" class="user-center-brand">LeoAI Router</RouterLink>
      <SidebarNav :items="items" />
    </aside>
    <main class="user-center-content">
      <slot />
    </main>
  </div>
</template>

<script setup lang="ts">
import { ApiOutlined, BarChartOutlined, CreditCardOutlined, UserOutlined } from '@ant-design/icons-vue'
import SidebarNav from '@/components/ui/SidebarNav.vue'

const items = [
  { label: '个人中心', path: '/profile', icon: UserOutlined },
  { label: 'API Keys', path: '/keys', icon: ApiOutlined },
  { label: '会员充值', path: '/membership', icon: CreditCardOutlined },
  { label: '使用统计', path: '/history', icon: BarChartOutlined },
]
</script>

<style scoped>
.user-center-layout {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr);
  min-height: calc(100vh - var(--leo-header-height));
  background: var(--leo-bg-page);
}
.user-center-sidebar {
  border-right: 1px solid var(--leo-border);
  background: var(--leo-bg-panel);
  padding: 18px 12px;
}
.user-center-brand {
  display: block;
  margin: 0 8px 22px;
  color: var(--leo-text-primary);
  font-weight: 700;
  text-decoration: none;
}
.user-center-content {
  min-width: 0;
  padding: 24px;
}
@media (max-width: 900px) {
  .user-center-layout { grid-template-columns: 1fr; }
  .user-center-sidebar { border-right: 0; border-bottom: 1px solid var(--leo-border); }
}
</style>
```

- [ ] **Step 5: Run type-check**

Run:

```bash
cd leo-ai-router-frontend
PATH=/Users/zhengsmacbook/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/bin:$PATH npm run type-check
```

Expected: command exits 0.

- [ ] **Step 6: Commit**

```bash
git add leo-ai-router-frontend/src/components/GlobalHeader.vue leo-ai-router-frontend/src/components/ui/SidebarNav.vue leo-ai-router-frontend/src/layouts/BasicLayout.vue leo-ai-router-frontend/src/layouts/UserCenterLayout.vue leo-ai-router-frontend/src/router/index.ts
git commit -m "feat: update frontend app shell navigation"
```

---

### Task 3: Dashboard Page and Chart Styling

**Files:**
- Create: `leo-ai-router-frontend/src/pages/DashboardPage.vue`
- Modify: `leo-ai-router-frontend/src/components/DailyStatsTrendChart.vue`

- [ ] **Step 1: Build dashboard data loader**

Use existing APIs only:

```ts
import { computed, onMounted, ref } from 'vue'
import { getMyDailyStats, getMySummaryStats } from '@/api/statsController'
import { getMyMembership, type MembershipVO } from '@/api/membershipController'
import { listAvailableModels } from '@/api/modelController'

const loading = ref(false)
const error = ref('')
const summary = ref<API.UserSummaryStatsVO>({})
const dailyStats = ref<any[]>([])
const membership = ref<MembershipVO>({})
const models = ref<API.ModelVO[]>([])

const dashboardMetrics = computed(() => [
  { label: '接入模型', value: models.value.length },
  { label: '今日请求', value: Number(summary.value.todayRequests ?? 0).toLocaleString('zh-CN') },
  { label: '成功率', value: `${Number(summary.value.successRate ?? 0).toFixed(2)}%` },
  { label: '平均延迟', value: `${Number(summary.value.averageLatency ?? 0).toFixed(0)}ms` },
  { label: '积分余额', value: Number(membership.value.pointBalance ?? 0).toLocaleString('zh-CN') },
])

const loadDashboard = async () => {
  loading.value = true
  error.value = ''
  try {
    const endDate = new Date()
    const startDate = new Date()
    startDate.setDate(endDate.getDate() - 6)
    const [summaryRes, dailyRes, membershipRes, modelRes] = await Promise.all([
      getMySummaryStats(),
      getMyDailyStats({
        startDate: startDate.toISOString().slice(0, 10),
        endDate: endDate.toISOString().slice(0, 10),
      }),
      getMyMembership(),
      listAvailableModels(),
    ])
    if (summaryRes.data.code === 0) summary.value = summaryRes.data.data ?? {}
    if (dailyRes.data.code === 0) dailyStats.value = (dailyRes.data.data ?? []) as any[]
    if (membershipRes.data.code === 0) membership.value = membershipRes.data.data ?? {}
    if (modelRes.data.code === 0) models.value = modelRes.data.data ?? []
  } catch {
    error.value = '控制台数据加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void loadDashboard()
})
```

- [ ] **Step 2: Use shared components in dashboard template**

Include `MetricCard`, `AppPanel`, `ErrorPanel`, `EmptyState`, `ModelStatusCard`, and `DailyStatsTrendChart`. Keep missing fields as `0` or empty state; do not invent backend data.

- [ ] **Step 3: Update `DailyStatsTrendChart.vue` colors**

Use token colors:

```css
.grid-line { stroke: var(--leo-border); stroke-dasharray: 4 4; }
.axis-line { stroke: var(--leo-border-strong); }
.axis-text { fill: var(--leo-text-secondary); font-size: 12px; }
.bar-rect { fill: rgba(36, 91, 255, 0.14); }
.line-token { fill: none; stroke: var(--leo-primary); stroke-width: 3; }
.line-cost { fill: none; stroke: var(--leo-warning); stroke-width: 3; }
.dot-token { fill: var(--leo-primary); }
.dot-cost { fill: var(--leo-warning); }
.tooltip { border-radius: var(--leo-radius-md); background: rgba(15, 23, 42, 0.94); box-shadow: var(--leo-shadow-pop); }
```

- [ ] **Step 4: Run type-check**

Run:

```bash
cd leo-ai-router-frontend
PATH=/Users/zhengsmacbook/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/bin:$PATH npm run type-check
```

Expected: command exits 0.

- [ ] **Step 5: Commit**

```bash
git add leo-ai-router-frontend/src/pages/DashboardPage.vue leo-ai-router-frontend/src/components/DailyStatsTrendChart.vue
git commit -m "feat: add user dashboard overview"
```

---

### Task 4: Home Page Refactor

**Files:**
- Modify: `leo-ai-router-frontend/src/pages/HomePage.vue`

- [ ] **Step 1: Replace marketing hero with reference-aligned hero**

Use a two-column hero: left product copy and actions, right SDK code panel. Use model health cards and dashboard-like sections below.

- [ ] **Step 2: Keep login-aware actions**

Use:

```vue
<RouterLink v-if="loginUserStore.loginUser.id" to="/dashboard" class="hero-primary">进入控制台</RouterLink>
<template v-else>
  <RouterLink to="/user/register" class="hero-primary">开始使用</RouterLink>
  <RouterLink to="/user/login" class="hero-secondary">登录账号</RouterLink>
</template>
```

- [ ] **Step 3: Keep static demo metrics clearly marked as product display data**

Use static arrays for reference-style product display only. Do not claim runtime data if no API is called.

```ts
const modelHealth = [
  { name: 'DeepSeek', successRate: '98.5%', latency: '812ms', status: '健康' },
  { name: '通义千问', successRate: '97.6%', latency: '623ms', status: '健康' },
  { name: '智谱 GLM', successRate: '99.1%', latency: '742ms', status: '健康' },
  { name: 'OpenAI', successRate: '98.9%', latency: '721ms', status: '健康' },
  { name: 'Gemini', successRate: '97.2%', latency: '856ms', status: '健康' },
]
```

- [ ] **Step 4: Run type-check**

Run:

```bash
cd leo-ai-router-frontend
PATH=/Users/zhengsmacbook/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/bin:$PATH npm run type-check
```

Expected: command exits 0.

- [ ] **Step 5: Commit**

```bash
git add leo-ai-router-frontend/src/pages/HomePage.vue
git commit -m "feat: redesign frontend home page"
```

---

### Task 5: Chat Three-Column Workspace

**Files:**
- Modify: `leo-ai-router-frontend/src/pages/chat/ChatPage.vue`

- [ ] **Step 1: Preserve business logic**

Before editing, identify and keep these functions and refs intact:

- `loadConversations`
- `selectConversation`
- `handleCreateConversation`
- `handleDeleteConversation`
- `sendMessage`
- `stopGeneration`
- `extractSseEvents`
- `parseSseEvent`
- `renderContent`
- `selectedRoutingStrategy`
- `selectedModel`
- `enableReasoning`
- membership/model lock helpers

- [ ] **Step 2: Restructure template into three columns**

Use these top-level containers:

```vue
<div class="chat-workspace">
  <aside class="conversation-rail">...</aside>
  <main class="chat-stage">...</main>
  <aside class="route-panel">...</aside>
</div>
```

Left rail keeps conversation list and create/search/delete. Center keeps message list and composer. Right panel owns route strategy, model selection, membership quota, deep thinking, and temperature.

- [ ] **Step 3: Keep message composer API unchanged**

Do not change request construction rules. Fixed model is only sent when `selectedRoutingStrategy === 'fixed'`; otherwise route strategy is sent without forcing default `gpt-5.5`.

- [ ] **Step 4: Keep Markdown class contract**

Do not remove these rendered classes because existing CSS and renderer rely on them:

- `.code-block`
- `.inline-code`
- table wrapper classes used by `renderContent`

- [ ] **Step 5: Run type-check**

Run:

```bash
cd leo-ai-router-frontend
PATH=/Users/zhengsmacbook/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/bin:$PATH npm run type-check
```

Expected: command exits 0.

- [ ] **Step 6: Commit**

```bash
git add leo-ai-router-frontend/src/pages/chat/ChatPage.vue
git commit -m "feat: redesign chat workspace"
```

---

### Task 6: Membership Billing Page

**Files:**
- Modify: `leo-ai-router-frontend/src/pages/user/MembershipPage.vue`

- [ ] **Step 1: Wrap page in `UserCenterLayout`**

```vue
<template>
  <UserCenterLayout>
    <div class="membership-console">
      ...
    </div>
  </UserCenterLayout>
</template>

<script setup lang="ts">
import UserCenterLayout from '@/layouts/UserCenterLayout.vue'
...
</script>
```

- [ ] **Step 2: Keep current order creation functions unchanged**

Do not change:

- `listMembershipPlans`
- `listPointPackages`
- `getMyMembership`
- `createSubscriptionOrder`
- `createPointsOrder`
- `submitOrder`

- [ ] **Step 3: Convert layout to account overview + catalog + payment summary**

Top overview uses `membership.planName`, `dailyProRemaining`, `dailyAdvancedRemaining`, and `pointBalance`. Main area uses plan cards and point cards. Right summary uses `currentName`, `currentPrice`, and `paymentMethod`.

- [ ] **Step 4: Preserve payment method behavior**

Wechat remains disabled/unavailable:

```ts
if (paymentMethod.value === 'wechat') {
  message.info('微信支付暂未开放，请选择支付宝或 Stripe')
  return
}
```

- [ ] **Step 5: Run type-check**

Run:

```bash
cd leo-ai-router-frontend
PATH=/Users/zhengsmacbook/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/bin:$PATH npm run type-check
```

Expected: command exits 0.

- [ ] **Step 6: Commit**

```bash
git add leo-ai-router-frontend/src/pages/user/MembershipPage.vue
git commit -m "feat: redesign membership billing page"
```

---

### Task 7: Image Generation Workspace

**Files:**
- Modify: `leo-ai-router-frontend/src/pages/image/ImageGenerationPage.vue`

- [ ] **Step 1: Preserve generation logic**

Do not change:

- `generateImage`
- `getMyRecords`
- `listAvailableModels`
- `getMyMembership`
- `canUseImageModel`
- `isMemberOnlyImageModel`
- `isGeminiImageModel`
- `handleGenerate`
- `downloadImage`

- [ ] **Step 2: Restructure into prompt panel, model settings, result/history**

Use:

```vue
<div class="image-workspace">
  <main class="image-main">...</main>
  <aside class="image-settings">...</aside>
</div>
```

The main area contains prompt, generate button, latest preview, and history list. The right settings area contains model cards, size, quality, points estimate, and access warning.

- [ ] **Step 3: Preserve Gemini parameter warning**

Keep the exact user-facing meaning that Gemini image models do not use size/quality and server defaults are applied.

- [ ] **Step 4: Run type-check**

Run:

```bash
cd leo-ai-router-frontend
PATH=/Users/zhengsmacbook/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/bin:$PATH npm run type-check
```

Expected: command exits 0.

- [ ] **Step 5: Commit**

```bash
git add leo-ai-router-frontend/src/pages/image/ImageGenerationPage.vue
git commit -m "feat: redesign image generation workspace"
```

---

### Task 8: User Center Pages

**Files:**
- Modify: `leo-ai-router-frontend/src/pages/user/ProfilePage.vue`
- Modify: `leo-ai-router-frontend/src/pages/user/ApiKeyPage.vue`
- Modify: `leo-ai-router-frontend/src/pages/user/HistoryPage.vue`

- [ ] **Step 1: Wrap all pages in `UserCenterLayout`**

Use this pattern in all three pages:

```vue
<template>
  <UserCenterLayout>
    <div class="user-page-content">
      ...
    </div>
  </UserCenterLayout>
</template>

<script setup lang="ts">
import UserCenterLayout from '@/layouts/UserCenterLayout.vue'
...
</script>
```

- [ ] **Step 2: Profile keeps account binding flows**

Do not change:

- edit profile modal
- bind/change email
- bind/change phone
- set/change password
- bill drawer tabs
- daily stats date range

Remove no backend cost-accounting path. Do not add old token asset cards.

- [ ] **Step 3: API Keys keeps one-time key display and revoke flow**

Do not change:

- `listMyApiKeys`
- `createApiKey`
- `revokeApiKey`
- one-time full key modal warning
- revoke confirmation

- [ ] **Step 4: History keeps admin/user API split**

Keep:

```ts
const res = loginUserStore.loginUser.userRole === 'admin' ? await pageHistory(payload) : await pageMyHistory(payload)
```

- [ ] **Step 5: Run type-check**

Run:

```bash
cd leo-ai-router-frontend
PATH=/Users/zhengsmacbook/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/bin:$PATH npm run type-check
```

Expected: command exits 0.

- [ ] **Step 6: Commit**

```bash
git add leo-ai-router-frontend/src/pages/user/ProfilePage.vue leo-ai-router-frontend/src/pages/user/ApiKeyPage.vue leo-ai-router-frontend/src/pages/user/HistoryPage.vue
git commit -m "feat: unify user center pages"
```

---

### Task 9: User-Side Final Verification

**Files:**
- Verify only unless build failures require narrow fixes.

- [ ] **Step 1: Run full frontend verification**

Run:

```bash
cd leo-ai-router-frontend
PATH=/Users/zhengsmacbook/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/bin:$PATH npm run type-check
PATH=/Users/zhengsmacbook/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/bin:$PATH npm run build-only
```

Expected: both commands exit 0.

- [ ] **Step 2: Start local dev server**

Run:

```bash
cd leo-ai-router-frontend
PATH=/Users/zhengsmacbook/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/bin:$PATH npm run dev -- --host 127.0.0.1
```

Expected: Vite prints a local URL such as `http://127.0.0.1:5173/`.

- [ ] **Step 3: Browser smoke check**

Open and inspect:

- `/`
- `/dashboard`
- `/chat`
- `/images`
- `/membership`
- `/profile`
- `/keys`
- `/history`

Check:

- no blank pages
- no obvious overlap at desktop width
- top navigation active states work
- user center left navigation works
- chat composer remains visible
- membership payment summary remains visible
- image model lock/access warning remains visible

- [ ] **Step 4: Stop dev server**

Stop the Vite session with Ctrl-C in the running terminal.

- [ ] **Step 5: Commit verification-only fixes if any**

Only if narrow fixes were required:

```bash
git add leo-ai-router-frontend/src
git commit -m "fix: polish user frontend verification issues"
```

---

## Self-Review

Spec coverage:

- 用户端页面范围 covered by Tasks 3-8.
- Vue 3 + TypeScript + Vite retained by all tasks.
- Ant Design Vue kept for complex components in Tasks 2, 5, 6, 7, 8.
- 新 `/dashboard` covered by Task 3 and route in Task 2.
- 白底 SaaS tokens and shared components covered by Task 1.
- Chat 三栏 with existing SSE/Markdown/business logic covered by Task 5.
- Membership 套餐 + 积分 + 支付摘要 covered by Task 6.
- Image model constraints covered by Task 7.
- Profile/API Keys/History user center shell covered by Task 8.
- Verification covered by Task 9.

Red-flag scan:

- No unfinished requirement markers are intentionally present.

Type consistency:

- Imports use existing `@/*` alias.
- Existing API client names match current generated files.
- New component paths are under `src/components/ui`.
- `UserCenterLayout` is used only by user-center pages and membership.
