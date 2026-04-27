<template>
  <div class="plugin-page">
    <section class="hero-panel">
      <div class="hero-copy">
        <div class="hero-badge">Plugin Center</div>
        <h1 class="hero-title">插件管理</h1>
        <p class="hero-desc">集中管理插件启停、优先级、配置详情与热重载，让插件运行状态一眼可见。</p>
      </div>
      <div class="hero-actions">
        <a-button size="large" @click="fetchPlugins">刷新列表</a-button>
        <a-popconfirm title="确认重载全部插件？" @confirm="handleReloadAll">
          <a-button type="primary" size="large">全部重载</a-button>
        </a-popconfirm>
      </div>
    </section>

    <section class="stats-grid">
      <div class="stat-card stat-card-accent">
        <div class="stat-label">插件总数</div>
        <div class="stat-value">{{ plugins.length }}</div>
        <div class="stat-foot">当前已接入的全部插件</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">启用中</div>
        <div class="stat-value">{{ activeCount }}</div>
        <div class="stat-foot">可立即参与调度与执行</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">已禁用</div>
        <div class="stat-value">{{ inactiveCount }}</div>
        <div class="stat-foot">已保留配置但不会执行</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">插件类型</div>
        <div class="stat-value">{{ typeCount }}</div>
        <div class="stat-foot">按能力标签自动区分颜色</div>
      </div>
    </section>

    <section class="plugin-list-shell">
      <div class="list-header">
        <div>
          <div class="section-title">插件列表</div>
          <div class="section-subtitle">卡片展示更适合查看配置、状态与操作入口</div>
        </div>
        <a-tag color="blue">{{ activeCount }}/{{ plugins.length }} 已启用</a-tag>
      </div>

      <a-spin :spinning="loading">
        <a-empty v-if="!plugins.length" description="暂无插件数据" class="empty-state" />

        <div v-else class="plugin-list">
          <article v-for="plugin in plugins" :key="plugin.id" class="plugin-card">
            <div class="plugin-card-top">
              <div class="plugin-head">
                <div class="plugin-name-block">
                  <div class="plugin-title-row">
                    <h3 class="plugin-title">{{ plugin.pluginName || plugin.pluginKey }}</h3>
                    <a-tag :color="pluginTypeMeta(plugin.pluginType).color">
                      {{ pluginTypeMeta(plugin.pluginType).label }}
                    </a-tag>
                    <a-tag :color="statusMeta(plugin.status).color">
                      {{ statusMeta(plugin.status).label }}
                    </a-tag>
                  </div>
                  <div class="plugin-key">{{ plugin.pluginKey }}</div>
                </div>
              </div>

              <div class="priority-chip">
                <span class="priority-label">优先级</span>
                <span class="priority-value">{{ plugin.priority ?? 0 }}</span>
              </div>
            </div>

            <div class="plugin-summary">
              {{ plugin.description || '暂无描述，当前插件尚未补充详细说明。' }}
            </div>

            <div class="plugin-meta-grid">
              <div class="meta-box">
                <span class="meta-label">配置概览</span>
                <span class="meta-value meta-code">{{ configPreview(plugin.config) }}</span>
              </div>
              <div class="meta-box">
                <span class="meta-label">更新时间</span>
                <span class="meta-value">{{ formatTime(plugin.updateTime) }}</span>
              </div>
              <div class="meta-box">
                <span class="meta-label">创建时间</span>
                <span class="meta-value">{{ formatTime(plugin.createTime) }}</span>
              </div>
            </div>

            <div class="plugin-actions">
              <div class="plugin-actions-left">
                <a-button @click="openConfigModal(plugin)">查看 JSON</a-button>
                <a-button @click="openEditModal(plugin)">编辑配置</a-button>
              </div>
              <div class="plugin-actions-right">
                <a-popconfirm title="确认重载该插件？" @confirm="handleReload(plugin)">
                  <a-button>重载</a-button>
                </a-popconfirm>
                <a-button
                  :type="plugin.status === 'active' ? 'default' : 'primary'"
                  :danger="plugin.status === 'active'"
                  @click="toggleStatus(plugin)"
                >
                  {{ plugin.status === 'active' ? '禁用' : '启用' }}
                </a-button>
              </div>
            </div>
          </article>
        </div>
      </a-spin>
    </section>

    <a-modal
      v-model:open="editOpen"
      title="编辑插件"
      width="760px"
      :confirm-loading="saving"
      @ok="handleSave"
    >
      <a-form layout="vertical">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="插件名称" required>
              <a-input v-model:value="form.pluginName" maxlength="64" show-count />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="优先级" required>
              <a-input-number v-model:value="form.priority" style="width: 100%" :min="0" />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="描述">
              <a-textarea
                v-model:value="form.description"
                :auto-size="{ minRows: 3, maxRows: 5 }"
                maxlength="300"
                show-count
              />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item>
              <template #label>
                <div class="config-label">
                  <span>插件配置(JSON)</span>
                  <a-button type="link" size="small" @click="formatFormConfig">格式化 JSON</a-button>
                </div>
              </template>
              <a-textarea
                v-model:value="form.config"
                :auto-size="{ minRows: 10, maxRows: 16 }"
                placeholder='{"timeout":30000}'
              />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>

    <a-modal v-model:open="configOpen" title="配置详情" width="760px" :footer="null">
      <div class="config-toolbar">
        <a-tag :color="pluginTypeMeta(currentPlugin?.pluginType).color">
          {{ currentPlugin?.pluginName || currentPlugin?.pluginKey }}
        </a-tag>
        <a-button size="small" @click="copyConfig">复制 JSON</a-button>
      </div>
      <pre class="config-json">{{ prettyConfig(currentPlugin?.config) }}</pre>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import {
  disablePlugin,
  enablePlugin,
  listPlugins,
  reloadAllPlugins,
  reloadPlugin,
  updatePlugin,
} from '@/api/pluginController'

const loading = ref(false)
const saving = ref(false)
const editOpen = ref(false)
const configOpen = ref(false)
const plugins = ref<API.PluginConfigVO[]>([])
const currentPlugin = ref<API.PluginConfigVO>()

const form = reactive<API.PluginUpdateRequest>({
  id: undefined,
  pluginName: '',
  description: '',
  config: '',
  priority: 0,
})

const activeCount = computed(() => plugins.value.filter((item) => item.status === 'active').length)
const inactiveCount = computed(() => plugins.value.filter((item) => item.status !== 'active').length)
const typeCount = computed(
  () => new Set(plugins.value.map((item) => item.pluginType || 'unknown')).size,
)

const fetchPlugins = async () => {
  loading.value = true
  try {
    const res = await listPlugins()
    if (res.data.code === 0) {
      plugins.value = (res.data.data ?? []).sort((a, b) => (b.priority ?? 0) - (a.priority ?? 0))
      return
    }
    message.error(res.data.message || '加载插件失败')
  } finally {
    loading.value = false
  }
}

const openEditModal = (plugin: API.PluginConfigVO) => {
  currentPlugin.value = plugin
  form.id = plugin.id
  form.pluginName = plugin.pluginName || ''
  form.description = plugin.description || ''
  form.config = prettyConfig(plugin.config)
  form.priority = plugin.priority ?? 0
  editOpen.value = true
}

const openConfigModal = (plugin: API.PluginConfigVO) => {
  currentPlugin.value = plugin
  configOpen.value = true
}

const handleSave = async () => {
  if (!form.id) {
    message.warning('插件 ID 缺失')
    return
  }
  if (!form.pluginName?.trim()) {
    message.warning('请输入插件名称')
    return
  }
  if (form.config && !isValidJson(form.config)) {
    message.warning('插件配置不是有效的 JSON')
    return
  }
  saving.value = true
  try {
    const payload: API.PluginUpdateRequest = {
      id: form.id,
      pluginName: form.pluginName.trim(),
      description: form.description?.trim(),
      config: form.config?.trim() ? JSON.stringify(JSON.parse(form.config)) : '{}',
      priority: form.priority ?? 0,
    }
    const res = await updatePlugin(payload)
    if (res.data.code === 0) {
      message.success('插件已更新')
      editOpen.value = false
      await fetchPlugins()
      return
    }
    message.error(res.data.message || '更新插件失败')
  } finally {
    saving.value = false
  }
}

const toggleStatus = async (plugin: API.PluginConfigVO) => {
  if (!plugin.pluginKey) {
    message.warning('插件标识缺失')
    return
  }
  const api = plugin.status === 'active' ? disablePlugin : enablePlugin
  const text = plugin.status === 'active' ? '禁用' : '启用'
  const res = await api({ pluginKey: plugin.pluginKey })
  if (res.data.code === 0) {
    message.success(`${text}成功`)
    await fetchPlugins()
    return
  }
  message.error(res.data.message || `${text}失败`)
}

const handleReload = async (plugin: API.PluginConfigVO) => {
  if (!plugin.pluginKey) {
    message.warning('插件标识缺失')
    return
  }
  const res = await reloadPlugin({ pluginKey: plugin.pluginKey })
  if (res.data.code === 0) {
    message.success('插件已重载')
    await fetchPlugins()
    return
  }
  message.error(res.data.message || '重载失败')
}

const handleReloadAll = async () => {
  const res = await reloadAllPlugins()
  if (res.data.code === 0) {
    message.success('全部插件已重载')
    await fetchPlugins()
    return
  }
  message.error(res.data.message || '全部重载失败')
}

const formatFormConfig = () => {
  if (!form.config?.trim()) {
    form.config = '{}'
    return
  }
  try {
    form.config = JSON.stringify(JSON.parse(form.config), null, 2)
  } catch {
    message.warning('当前配置不是合法 JSON')
  }
}

const copyConfig = async () => {
  const text = prettyConfig(currentPlugin.value?.config)
  try {
    await navigator.clipboard.writeText(text)
    message.success('JSON 已复制')
  } catch {
    message.error('复制失败')
  }
}

const isValidJson = (value: string) => {
  try {
    JSON.parse(value)
    return true
  } catch {
    return false
  }
}

const prettyConfig = (value?: string) => {
  if (!value?.trim()) {
    return '{}'
  }
  try {
    return JSON.stringify(JSON.parse(value), null, 2)
  } catch {
    return value
  }
}

const configPreview = (value?: string) => {
  const text = prettyConfig(value).replace(/\s+/g, ' ')
  return text.length > 72 ? `${text.slice(0, 72)}...` : text
}

const formatTime = (value?: string) => {
  if (!value) {
    return '--'
  }
  return value.replace('T', ' ').slice(0, 19)
}

const statusMeta = (status?: string) => {
  if (status === 'active') {
    return { label: '启用', color: 'green' }
  }
  return { label: '禁用', color: 'default' }
}

const pluginTypeMeta = (pluginType?: string) => {
  const key = (pluginType || 'unknown').toLowerCase()
  const colorMap: Record<string, string> = {
    builtin: 'blue',
    llm: 'geekblue',
    image: 'magenta',
    tool: 'cyan',
    workflow: 'purple',
    storage: 'gold',
    vector: 'orange',
    search: 'lime',
    document: 'volcano',
    speech: 'pink',
  }
  return {
    label: pluginType || 'unknown',
    color: colorMap[key] || 'processing',
  }
}

onMounted(fetchPlugins)
</script>

<style>
.plugin-page {
  width: 100%;
  max-width: 1280px;
  margin: 0 auto;
  padding: 32px 24px 56px;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.plugin-page * {
  box-sizing: border-box;
}

.hero-panel {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 24px;
  padding: 28px 30px;
  border-radius: 28px;
  background:
    radial-gradient(circle at top left, rgba(14, 165, 233, 0.16), transparent 34%),
    radial-gradient(circle at bottom right, rgba(59, 130, 246, 0.2), transparent 30%),
    linear-gradient(135deg, #ffffff 0%, #f8fbff 42%, #eef6ff 100%);
  border: 1px solid rgba(148, 163, 184, 0.18);
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.06);
}

.hero-copy {
  max-width: 720px;
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(37, 99, 235, 0.08);
  color: #2563eb;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.hero-title {
  margin: 14px 0 10px;
  font-size: 38px;
  line-height: 1.05;
  font-weight: 800;
  color: #0f172a;
}

.hero-desc {
  margin: 0;
  max-width: 720px;
  font-size: 16px;
  line-height: 1.7;
  color: #64748b;
}

.hero-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.stat-card {
  min-height: 148px;
  padding: 22px 22px 20px;
  border-radius: 22px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.05);
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.stat-card-accent {
  background: linear-gradient(135deg, #0f172a 0%, #1d4ed8 100%);
  border-color: transparent;
}

.stat-card-accent .stat-label,
.stat-card-accent .stat-foot {
  color: rgba(255, 255, 255, 0.72);
}

.stat-card-accent .stat-value {
  color: #ffffff;
}

.stat-label {
  display: block;
  font-size: 14px;
  color: #64748b;
}

.stat-value {
  display: block;
  font-size: 42px;
  line-height: 1;
  font-weight: 800;
  color: #0f172a;
}

.stat-foot {
  display: block;
  font-size: 13px;
  color: #94a3b8;
}

.plugin-list-shell {
  padding: 24px;
  border-radius: 28px;
  background: linear-gradient(180deg, #ffffff 0%, #f8fafc 100%);
  border: 1px solid #e2e8f0;
  box-shadow: 0 14px 36px rgba(15, 23, 42, 0.06);
}

.list-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.section-title {
  font-size: 24px;
  font-weight: 800;
  color: #0f172a;
}

.section-subtitle {
  margin-top: 6px;
  font-size: 14px;
  color: #64748b;
}

.empty-state {
  padding: 48px 0;
}

.plugin-list {
  display: grid;
  gap: 18px;
}

.plugin-card {
  padding: 22px 24px;
  border-radius: 24px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.04);
  transition:
    transform 0.18s ease,
    box-shadow 0.18s ease,
    border-color 0.18s ease;
}

.plugin-card:hover {
  transform: translateY(-2px);
  border-color: rgba(59, 130, 246, 0.28);
  box-shadow: 0 14px 30px rgba(37, 99, 235, 0.1);
}

.plugin-card-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
}

.plugin-name-block {
  min-width: 0;
}

.plugin-title-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.plugin-title {
  margin: 0;
  font-size: 24px;
  line-height: 1.2;
  font-weight: 800;
  color: #0f172a;
}

.plugin-key {
  margin-top: 6px;
  font-size: 14px;
  color: #64748b;
}

.priority-chip {
  padding: 10px 14px;
  min-width: 102px;
  border-radius: 18px;
  background: #eff6ff;
  text-align: right;
  flex-shrink: 0;
}

.priority-label {
  display: block;
  font-size: 12px;
  color: #64748b;
}

.priority-value {
  display: block;
  margin-top: 4px;
  font-size: 28px;
  line-height: 1;
  font-weight: 800;
  color: #1d4ed8;
}

.plugin-summary {
  margin-top: 14px;
  font-size: 15px;
  line-height: 1.7;
  color: #475569;
}

.plugin-meta-grid {
  margin-top: 18px;
  display: grid;
  grid-template-columns: 1.6fr 1fr 1fr;
  gap: 14px;
}

.meta-box {
  padding: 14px 16px;
  border-radius: 18px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
}

.meta-label {
  display: block;
  font-size: 12px;
  color: #64748b;
}

.meta-value {
  display: block;
  margin-top: 8px;
  font-size: 14px;
  line-height: 1.6;
  color: #0f172a;
}

.meta-code {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  word-break: break-word;
}

.plugin-actions {
  margin-top: 18px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.plugin-actions-left,
.plugin-actions-right {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.config-label {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.config-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.config-json {
  margin: 0;
  padding: 16px;
  background: #0f172a;
  color: #dbeafe;
  border-radius: 16px;
  font-size: 13px;
  line-height: 1.6;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-word;
}

@media (max-width: 1100px) {
  .stats-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .plugin-meta-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 900px) {
  .plugin-page {
    padding: 24px 16px 48px;
  }

  .hero-panel,
  .hero-actions,
  .plugin-card-top,
  .plugin-actions,
  .list-header {
    flex-direction: column;
    align-items: stretch;
  }

  .hero-title {
    font-size: 32px;
  }

  .priority-chip {
    text-align: left;
  }
}

@media (max-width: 640px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }
}
</style>
