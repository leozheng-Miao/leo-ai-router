<template>
  <a-layout-footer class="footer">
    <div class="footer-inner">
      <div class="footer-left">
        <div class="footer-brand">
          <svg width="20" height="20" viewBox="0 0 28 28" fill="none">
            <rect width="28" height="28" rx="7" fill="url(#footerGrad)" />
            <path
              d="M8 9h12M8 14h8M8 19h12"
              stroke="#fff"
              stroke-width="2"
              stroke-linecap="round"
            />
            <defs>
              <linearGradient
                id="footerGrad"
                x1="0"
                y1="0"
                x2="28"
                y2="28"
                gradientUnits="userSpaceOnUse"
              >
                <stop stop-color="#2563EB" />
                <stop offset="1" stop-color="#7C3AED" />
              </linearGradient>
            </defs>
          </svg>
          <span class="footer-brand-name">LeoAI Router</span>
        </div>
        <p class="footer-desc">企业级 AI 模型路由网关，兼容 OpenAI 协议</p>
      </div>

      <div class="footer-links">
        <div class="footer-col">
          <div class="col-title">产品</div>
          <a href="#" class="col-link">模型广场</a>
          <a href="#" class="col-link">价格方案</a>
          <a href="#" class="col-link">使用文档</a>
        </div>
        <div class="footer-col">
          <div class="col-title">开发者</div>
          <a href="#" class="col-link">API 参考</a>
          <a href="#" class="col-link">快速开始</a>
          <a href="#" class="col-link">SDK 下载</a>
        </div>
        <div class="footer-col">
          <div class="col-title">关于</div>
          <a href="#" class="col-link">服务条款</a>
          <a href="#" class="col-link">隐私政策</a>
          <a href="#" class="col-link">联系我们</a>
        </div>
      </div>
    </div>

    <div class="footer-bottom">
      <span>© {{ new Date().getFullYear() }} LeoAI Router. All rights reserved.</span>
      <div class="footer-status">
        <span class="status-dot"></span>
        <span>所有系统正常运行</span>
      </div>
    </div>
  </a-layout-footer>
</template>

<script setup lang="ts"></script>

<style scoped>
.footer {
  background: #111827;
  padding: 48px 0 0;
  margin-top: auto;
}

.footer-inner {
  max-width: 1280px;
  margin: 0 auto;
  padding: 0 24px 40px;
  display: flex;
  justify-content: space-between;
  gap: 48px;
}

.footer-brand {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.footer-brand-name {
  font-size: 15px;
  font-weight: 700;
  color: #fff;
}

.footer-desc {
  font-size: 13px;
  color: #9ca3af;
  margin: 0;
  max-width: 220px;
  line-height: 1.6;
}

.footer-links {
  display: flex;
  gap: 64px;
}

.footer-col {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.col-title {
  font-size: 12px;
  font-weight: 600;
  color: #f9fafb;
  text-transform: uppercase;
  letter-spacing: 0.8px;
  margin-bottom: 4px;
}

.col-link {
  font-size: 13px;
  color: #9ca3af;
  text-decoration: none;
  transition: color 0.15s;
}

.col-link:hover {
  color: #e5e7eb;
}

.footer-bottom {
  max-width: 1280px;
  margin: 0 auto;
  padding: 16px 24px;
  border-top: 1px solid #1f2937;
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
  color: #6b7280;
}

.footer-status {
  display: flex;
  align-items: center;
  gap: 6px;
}

.status-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #10b981;
  box-shadow: 0 0 0 2px rgba(16, 185, 129, 0.2);
}
</style>
