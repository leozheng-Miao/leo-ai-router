<script setup lang="ts">
import { useLoginUserStore } from '@/stores/loginUser'
import {
  RocketOutlined,
  ApiOutlined,
  ThunderboltOutlined,
  SafetyOutlined,
} from '@ant-design/icons-vue'

const loginUserStore = useLoginUserStore()

const features = [
  {
    icon: ApiOutlined,
    title: 'API Key 管理',
    desc: '创建并管理您的 API Key，轻松接入 AI 能力',
    color: '#3b82f6',
    bg: '#eff6ff',
  },
  {
    icon: ThunderboltOutlined,
    title: '多模型路由',
    desc: '智能路由到最优 AI 模型，兼容 OpenAI 协议',
    color: '#8b5cf6',
    bg: '#f5f3ff',
  },
  {
    icon: SafetyOutlined,
    title: '用量统计',
    desc: '实时查看 Token 消耗，掌控每一次调用记录',
    color: '#10b981',
    bg: '#ecfdf5',
  },
]
</script>

<template>
  <div id="homePage">
    <!-- Hero -->
    <section class="hero">
      <div class="hero-badge">
        <RocketOutlined style="font-size: 12px" />
        <span>生产级 AI 路由网关</span>
      </div>
      <h1 class="hero-title">
        统一接入<br />
        <span class="gradient-text">多种 AI 模型</span>
      </h1>
      <p class="hero-desc">
        兼容 OpenAI 协议，一个 API Key 调用所有主流模型<br />
        稳定、高效、可观测
      </p>
      <div class="hero-actions">
        <template v-if="loginUserStore.loginUser.id">
          <a-button type="primary" size="large" class="btn-primary" href="/dashboard">
            进入控制台
          </a-button>
        </template>
        <template v-else>
          <a-button type="primary" size="large" class="btn-primary" href="/user/register">
            免费开始
          </a-button>
          <a-button size="large" class="btn-ghost" href="/user/login"> 登录账号 </a-button>
        </template>
      </div>
    </section>

    <!-- 欢迎条（已登录） -->
    <section v-if="loginUserStore.loginUser.id" class="welcome-bar">
      <div class="container">
        <span class="welcome-emoji">👋</span>
        <span
          >欢迎回来，<strong>{{ loginUserStore.loginUser.userName }}</strong></span
        >
      </div>
    </section>

    <!-- Features -->
    <section class="features">
      <div class="container">
        <h2 class="section-title">核心能力</h2>
        <div class="feature-grid">
          <div v-for="f in features" :key="f.title" class="feature-card">
            <div class="feature-icon" :style="{ background: f.bg, color: f.color }">
              <component :is="f.icon" style="font-size: 22px" />
            </div>
            <h3 class="feature-title">{{ f.title }}</h3>
            <p class="feature-desc">{{ f.desc }}</p>
          </div>
        </div>
      </div>
    </section>

    <!-- CTA -->
    <section v-if="!loginUserStore.loginUser.id" class="cta">
      <div class="container cta-inner">
        <h2 class="cta-title">立即体验</h2>
        <p class="cta-desc">注册即可获得免费配额，无需信用卡</p>
        <a-button type="primary" size="large" class="btn-primary" href="/user/register">
          免费注册
        </a-button>
      </div>
    </section>
  </div>
</template>

<style scoped>
#homePage {
  min-height: calc(100vh - 64px - 61px);
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
  padding: 100px 24px 80px;
  background: linear-gradient(160deg, #f0f9ff 0%, #faf5ff 50%, #f0fdf4 100%);
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: rgba(59, 130, 246, 0.1);
  color: #3b82f6;
  border: 1px solid rgba(59, 130, 246, 0.2);
  border-radius: 20px;
  padding: 4px 14px;
  font-size: 13px;
  font-weight: 500;
  margin-bottom: 28px;
}

.hero-title {
  font-size: 56px;
  font-weight: 800;
  line-height: 1.2;
  margin: 0 0 20px;
  color: #111827;
  letter-spacing: -1px;
}

.gradient-text {
  background: linear-gradient(135deg, #3b82f6, #8b5cf6);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.hero-desc {
  font-size: 18px;
  color: #6b7280;
  line-height: 1.8;
  margin: 0 0 40px;
}

.hero-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  flex-wrap: wrap;
}

.btn-primary {
  height: 48px;
  padding: 0 32px;
  border-radius: 24px;
  font-size: 16px;
  font-weight: 600;
  background: linear-gradient(135deg, #3b82f6, #8b5cf6);
  border: none;
}

.btn-primary:hover {
  opacity: 0.88;
}

.btn-ghost {
  height: 48px;
  padding: 0 32px;
  border-radius: 24px;
  font-size: 16px;
  border: 1.5px solid #d1d5db;
  color: #374151;
  background: #fff;
}

/* Welcome bar */
.welcome-bar {
  background: #fff;
  border-bottom: 1px solid #f3f4f6;
  padding: 14px 24px;
  font-size: 15px;
  color: #374151;
}

.welcome-bar .container {
  display: flex;
  align-items: center;
  gap: 8px;
}

.welcome-emoji {
  font-size: 18px;
}

/* Features */
.features {
  padding: 80px 0;
}

.section-title {
  font-size: 32px;
  font-weight: 700;
  text-align: center;
  color: #111827;
  margin: 0 0 48px;
}

.feature-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
}

.feature-card {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 16px;
  padding: 32px 28px;
  transition:
    transform 0.25s,
    box-shadow 0.25s;
}

.feature-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 16px 40px rgba(0, 0, 0, 0.08);
}

.feature-icon {
  width: 52px;
  height: 52px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 20px;
}

.feature-title {
  font-size: 18px;
  font-weight: 600;
  color: #111827;
  margin: 0 0 10px;
}

.feature-desc {
  font-size: 14px;
  color: #6b7280;
  line-height: 1.7;
  margin: 0;
}

/* CTA */
.cta {
  background: linear-gradient(135deg, #3b82f6, #8b5cf6);
  padding: 80px 24px;
  text-align: center;
}

.cta-title {
  font-size: 36px;
  font-weight: 700;
  color: #fff;
  margin: 0 0 12px;
}

.cta-desc {
  font-size: 16px;
  color: rgba(255, 255, 255, 0.82);
  margin: 0 0 32px;
}

.cta .btn-primary {
  background: #fff;
  color: #3b82f6;
  font-weight: 700;
}

/* 响应式 */
@media (max-width: 768px) {
  .hero-title {
    font-size: 36px;
  }
  .hero-desc {
    font-size: 15px;
  }
  .feature-grid {
    grid-template-columns: 1fr;
  }
  .hero {
    padding: 60px 20px 50px;
  }
}
</style>
