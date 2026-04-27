<template>
  <div class="image-page">
    <section class="generate-panel">
      <div class="panel-header">
        <div>
          <div class="panel-title">AI 绘图</div>
          <div class="panel-desc">输入提示词并选择模型，生成图片后可在右侧查看历史记录。</div>
        </div>
      </div>

      <a-card :bordered="false" class="generate-card">
        <a-form layout="vertical">
          <a-form-item label="提示词" required>
            <a-textarea
              v-model:value="form.prompt"
              :rows="7"
              :maxlength="1200"
              show-count
              placeholder="例如：黄昏时分的未来主义海边城市，霓虹灯反射在潮湿路面上，电影感构图，超高细节"
            />
          </a-form-item>

          <a-form-item label="模型选择">
            <a-radio-group v-model:value="form.model" class="model-group">
              <label
                v-for="item in imageModels"
                :key="item.modelKey"
                class="model-card"
                :class="{ active: form.model === item.modelKey }"
              >
                <input v-model="form.model" type="radio" :value="item.modelKey" class="model-radio" />
                <div class="model-head">
                  <span class="model-name">{{ item.modelName || item.modelKey }}</span>
                  <span class="model-provider">{{ item.providerDisplayName || item.providerName }}</span>
                </div>
                <div class="model-meta">
                  <span>{{ item.description || '图片生成模型' }}</span>
                  <span class="model-price">{{ formatPrice(item.inputPrice) }}</span>
                </div>
              </label>
            </a-radio-group>
          </a-form-item>

          <div class="config-grid">
            <a-form-item label="尺寸">
              <a-select v-model:value="form.size">
                <a-select-option v-for="item in sizeOptions" :key="item" :value="item">
                  {{ item }}
                </a-select-option>
              </a-select>
            </a-form-item>
            <a-form-item label="质量">
              <a-select v-model:value="form.quality">
                <a-select-option value="standard">standard</a-select-option>
                <a-select-option value="hd">hd</a-select-option>
              </a-select>
            </a-form-item>
          </div>

          <div class="estimate-box">
            <div class="estimate-label">预估费用</div>
            <div class="estimate-value">¥{{ estimatedCost }}</div>
            <div class="estimate-desc">
              当前按所选模型单张图片费用估算，实际扣费以服务端返回结果为准。
            </div>
          </div>

          <div class="generate-actions">
            <a-space>
              <a-button type="primary" size="large" :loading="generating" @click="handleGenerate">
                {{ generating ? '生成中...' : '立即生成' }}
              </a-button>
              <a-button size="large" @click="resetForm">重置</a-button>
            </a-space>
          </div>

          <div v-if="latestPreview" class="latest-preview">
            <div class="preview-head">
              <span>最近生成结果</span>
              <a-button type="link" @click="openPreview(latestPreview)">预览大图</a-button>
            </div>
            <img :src="latestPreview" alt="latest result" class="preview-image" @click="openPreview(latestPreview)" />
          </div>
        </a-form>
      </a-card>
    </section>

    <aside class="history-panel">
      <div class="history-head">
        <div>
          <div class="panel-title">历史记录</div>
          <div class="panel-desc">最近的图片生成任务和状态会显示在这里。</div>
        </div>
        <a-button size="small" @click="loadRecords">刷新</a-button>
      </div>

      <a-card :bordered="false" class="history-card">
        <div v-if="records.length === 0 && !historyLoading" class="empty-state">
          暂无图片生成记录
        </div>

        <div v-else class="history-list">
          <article v-for="record in records" :key="record.id" class="history-item">
            <div class="history-layout">
              <div class="history-thumb-shell">
                <img
                  v-if="record.status === 'success' && getRecordImage(record)"
                  :src="getRecordImage(record) || ''"
                  alt="generated"
                  class="thumb-image"
                  @click="openPreview(getRecordImage(record) || '')"
                />
                <div v-else class="thumb-fallback">
                  {{ record.status === 'success' ? '暂无缩略图' : '失败' }}
                </div>
              </div>

              <div class="history-content">
                <div class="history-item-head">
                  <div class="history-title-row">
                    <div class="history-prompt">{{ record.prompt || '无提示词' }}</div>
                    <a-tag :color="record.status === 'success' ? 'green' : 'red'">
                      {{ record.status === 'success' ? '成功' : '失败' }}
                    </a-tag>
                  </div>
                  <span class="history-time">{{ formatTime(record.createTime) }}</span>
                </div>

                <div class="history-meta history-meta--stacked">
                  <span>模型：{{ record.modelKey || '-' }}</span>
                  <span>尺寸：{{ record.size || '-' }}</span>
                  <span>费用：¥{{ Number(record.cost ?? 0).toFixed(4) }}</span>
                  <span>时间：{{ formatTime(record.createTime) }}</span>
                </div>

                <div v-if="record.status !== 'success'" class="error-box">
                  {{ record.errorMessage || '生成失败' }}
                </div>
              </div>

              <div class="history-side">
                <div class="history-side-time">{{ formatTime(record.createTime) }}</div>
                <div class="history-actions history-actions--icon">
                <a-button
                  type="text"
                  class="icon-btn"
                  :disabled="!getRecordImage(record)"
                  @click="getRecordImage(record) ? openPreview(getRecordImage(record) || '') : showDetail(record)"
                >
                  <EyeOutlined />
                </a-button>
                <a-button
                  type="text"
                  class="icon-btn"
                  :disabled="!(record.status === 'success' && getRecordImage(record))"
                  @click="downloadImage(record)"
                >
                  <DownloadOutlined />
                </a-button>
                </div>
              </div>
            </div>
          </article>
        </div>

        <a-pagination
          v-if="pagination.total > pagination.pageSize"
          class="history-pagination"
          :current="pagination.current"
          :page-size="pagination.pageSize"
          :total="pagination.total"
          :show-size-changer="true"
          @change="handlePageChange"
        />
      </a-card>
    </aside>

    <a-modal v-model:open="previewOpen" title="图片预览" width="920px" :footer="null">
      <img v-if="previewUrl" :src="previewUrl" alt="preview" class="preview-modal-image" />
    </a-modal>

    <a-modal v-model:open="detailOpen" title="生成记录详情" width="760px" :footer="null">
      <a-descriptions v-if="detailRecord" bordered :column="2" size="small">
        <a-descriptions-item label="提示词" :span="2">{{ detailRecord.prompt || '-' }}</a-descriptions-item>
        <a-descriptions-item label="模型">{{ detailRecord.modelKey || '-' }}</a-descriptions-item>
        <a-descriptions-item label="状态">{{ detailRecord.status || '-' }}</a-descriptions-item>
        <a-descriptions-item label="尺寸">{{ detailRecord.size || '-' }}</a-descriptions-item>
        <a-descriptions-item label="质量">{{ detailRecord.quality || '-' }}</a-descriptions-item>
        <a-descriptions-item label="费用">¥{{ Number(detailRecord.cost ?? 0).toFixed(2) }}</a-descriptions-item>
        <a-descriptions-item label="耗时">{{ detailRecord.duration ?? 0 }} ms</a-descriptions-item>
        <a-descriptions-item label="时间">{{ formatTime(detailRecord.createTime) }}</a-descriptions-item>
        <a-descriptions-item label="修订提示词" :span="2">{{ detailRecord.revisedPrompt || '-' }}</a-descriptions-item>
        <a-descriptions-item label="错误信息" :span="2">{{ detailRecord.errorMessage || '-' }}</a-descriptions-item>
      </a-descriptions>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { DownloadOutlined, EyeOutlined } from '@ant-design/icons-vue'
import { generateImage, getMyRecords } from '@/api/imageController'
import { listActiveModelsByType } from '@/api/modelController'

const generating = ref(false)
const historyLoading = ref(false)
const imageModels = ref<API.ModelVO[]>([])
const records = ref<API.ImageGenerationRecord[]>([])
const latestPreview = ref('')
const previewOpen = ref(false)
const previewUrl = ref('')
const detailOpen = ref(false)
const detailRecord = ref<API.ImageGenerationRecord>()
const imageCacheKey = 'leo_ai_router_image_history_cache'

const sizeOptions = ['1024*1024', '1280*720', '720*1280']

const form = reactive<API.ImageGenerationRequest>({
  prompt: '',
  model: '',
  size: '1024*1024',
  quality: 'standard',
  response_format: 'url',
  n: 1,
})

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
})

const selectedModel = computed(() =>
  imageModels.value.find((item) => item.modelKey === form.model),
)

const estimatedCost = computed(() => Number(selectedModel.value?.inputPrice ?? 0).toFixed(2))

const formatPrice = (value?: number) => `¥${Number(value ?? 0).toFixed(2)} / 张`

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

const normalizeImageUrl = (raw?: string) => {
  if (!raw) return ''
  if (raw.startsWith('data:image')) return raw
  if (/^https?:\/\//i.test(raw)) return raw
  if (/^[A-Za-z0-9+/=]+$/.test(raw)) return `data:image/png;base64,${raw}`
  return raw
}

const readImageCache = (): Record<string, string> => {
  try {
    const raw = localStorage.getItem(imageCacheKey)
    return raw ? JSON.parse(raw) : {}
  } catch {
    return {}
  }
}

const writeImageCache = (cache: Record<string, string>) => {
  localStorage.setItem(imageCacheKey, JSON.stringify(cache))
}

const updateCacheByRecord = (recordId: number | string | undefined, imageUrl: string) => {
  if (!recordId || !imageUrl) return
  const cache = readImageCache()
  cache[String(recordId)] = imageUrl
  writeImageCache(cache)
}

const getRecordImage = (record: API.ImageGenerationRecord) =>
  normalizeImageUrl(record.imageUrl || record.imageData || readImageCache()[String(record.id ?? '')])

const loadModels = async () => {
  const res = await listActiveModelsByType({ modelType: 'image' })
  if (res.data.code === 0) {
    imageModels.value = (res.data.data ?? []).sort((a, b) => Number(a.inputPrice ?? 0) - Number(b.inputPrice ?? 0))
    const firstModel = imageModels.value[0]
    if (!form.model && firstModel?.modelKey) {
      form.model = firstModel.modelKey
    }
  } else {
    message.error(res.data.message ?? '加载图片模型失败')
  }
}

const loadRecords = async () => {
  historyLoading.value = true
  try {
    const res = await getMyRecords({
      pageNum: pagination.current,
      pageSize: pagination.pageSize,
    })
    if (res.data.code === 0 && res.data.data) {
      records.value = (res.data.data.records ?? []).map((record) => {
        const cachedImage = getRecordImage(record)
        if (record.id && cachedImage && !record.imageUrl && !record.imageData) {
          return {
            ...record,
            imageUrl: cachedImage,
          }
        }
        return record
      })
      pagination.total = res.data.data.totalRow ?? 0
    } else {
      message.error(res.data.message ?? '加载历史记录失败')
    }
  } finally {
    historyLoading.value = false
  }
}

const handleGenerate = async () => {
  if (!form.prompt?.trim()) {
    message.warning('请输入提示词')
    return
  }
  if (!form.model) {
    message.warning('请选择图片模型')
    return
  }
  generating.value = true
  try {
    const promptText = form.prompt.trim()
    const res = await generateImage({
      prompt: promptText,
      model: form.model,
      size: form.size,
      quality: form.quality,
      response_format: 'url',
      n: 1,
    })
    const images = res.data?.data ?? []
    if (images.length > 0) {
      const firstImage = normalizeImageUrl(images[0]?.url || images[0]?.b64Json)
      latestPreview.value = firstImage
      if (firstImage) {
        previewUrl.value = firstImage
      }
      await loadRecords()
      const latestRecord = records.value.find(
        (item) =>
          item.prompt === promptText &&
          item.modelKey === form.model &&
          item.status === 'success',
      )
      if (latestRecord?.id && firstImage) {
        updateCacheByRecord(latestRecord.id, firstImage)
        latestRecord.imageUrl = latestRecord.imageUrl || firstImage
      }
      message.success('图片生成成功')
    } else {
      message.warning('生成成功，但未返回图片')
    }
  } catch (error: any) {
    const errorMessage = error?.response?.data?.message || error?.message || '图片生成失败'
    message.error(errorMessage)
    await loadRecords()
  } finally {
    generating.value = false
  }
}

const resetForm = () => {
  form.prompt = ''
  form.size = '1024*1024'
  form.quality = 'standard'
}

const openPreview = (url: string) => {
  previewUrl.value = url
  previewOpen.value = true
}

const showDetail = (record: API.ImageGenerationRecord) => {
  detailRecord.value = record
  detailOpen.value = true
}

const downloadImage = async (record: API.ImageGenerationRecord) => {
  const url = getRecordImage(record)
  if (!url) {
    message.warning('当前记录没有可下载的图片')
    return
  }
  try {
    const fileName = `${record.modelKey || 'image'}-${record.id || Date.now()}.png`
    if (url.startsWith('data:image')) {
      const link = document.createElement('a')
      link.href = url
      link.download = fileName
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      return
    }
    const response = await fetch(url)
    const blob = await response.blob()
    const blobUrl = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = blobUrl
    link.download = fileName
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(blobUrl)
  } catch {
    window.open(url, '_blank', 'noopener')
    message.info('当前图片已在新窗口打开，请使用浏览器下载')
  }
}

const handlePageChange = (page: number, pageSize: number) => {
  pagination.current = page
  pagination.pageSize = pageSize
  void loadRecords()
}

onMounted(() => {
  void Promise.all([loadModels(), loadRecords()])
})
</script>

<style scoped>
.image-page { max-width: 1320px; margin: 0 auto; padding: 28px 24px 40px; display: grid; grid-template-columns: minmax(0, 1.2fr) 420px; gap: 20px; align-items: start; }
.panel-header, .history-head { display: flex; justify-content: space-between; gap: 12px; align-items: start; margin-bottom: 14px; }
.panel-title { font-size: 24px; font-weight: 700; color: #0f172a; }
.panel-desc { margin-top: 6px; color: #64748b; line-height: 1.7; }
.generate-card, .history-card { border-radius: 22px; background: linear-gradient(180deg, #ffffff, #f8fbff); }
.model-group { width: 100%; display: grid; gap: 12px; }
.model-card { display: block; position: relative; padding: 16px 18px; border-radius: 16px; border: 1px solid #dbe4f0; background: #fff; cursor: pointer; transition: all 0.18s; }
.model-card.active { border-color: #2563eb; box-shadow: 0 0 0 4px rgba(37, 99, 235, 0.08); background: #f8fbff; }
.model-radio { position: absolute; opacity: 0; pointer-events: none; }
.model-head { display: flex; justify-content: space-between; gap: 12px; align-items: center; }
.model-name { font-size: 16px; font-weight: 700; color: #0f172a; }
.model-provider { font-size: 12px; color: #2563eb; background: #eff6ff; border-radius: 999px; padding: 4px 10px; }
.model-meta { margin-top: 10px; display: flex; justify-content: space-between; gap: 12px; color: #64748b; font-size: 13px; }
.model-price { color: #ea580c; font-weight: 700; }
.config-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 14px; }
.estimate-box { margin-top: 10px; padding: 16px 18px; border-radius: 18px; background: linear-gradient(135deg, #0f172a, #1d4ed8); color: #fff; }
.estimate-label { font-size: 13px; color: rgba(255,255,255,0.72); }
.estimate-value { margin-top: 8px; font-size: 34px; font-weight: 700; }
.estimate-desc { margin-top: 8px; color: rgba(255,255,255,0.76); line-height: 1.7; }
.generate-actions { margin-top: 18px; }
.latest-preview { margin-top: 22px; }
.preview-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; color: #0f172a; font-weight: 600; }
.preview-image { width: 100%; border-radius: 18px; background: #f1f5f9; border: 1px solid #dbe4f0; cursor: zoom-in; }
.history-list { display: flex; flex-direction: column; gap: 14px; }
.history-item { padding: 18px 16px; border-radius: 18px; border: 1px solid #e5edf7; background: #fff; }
.history-layout { display: grid; grid-template-columns: 84px minmax(0, 1fr) 132px; gap: 16px; align-items: start; }
.history-thumb-shell { width: 84px; height: 84px; border-radius: 12px; overflow: hidden; background: #f8fafc; border: 1px solid #dbe4f0; }
.history-item-head { display: flex; justify-content: flex-start; gap: 12px; align-items: start; }
.history-title-row { display: flex; gap: 10px; align-items: flex-start; flex-wrap: wrap; }
.history-time { color: #94a3b8; font-size: 12px; }
.history-side { display: flex; flex-direction: column; align-items: flex-end; gap: 14px; min-width: 132px; }
.history-side-time { color: #94a3b8; font-size: 12px; line-height: 1.5; text-align: right; word-break: break-word; }
.history-prompt { font-size: 15px; line-height: 1.7; color: #0f172a; font-weight: 600; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.history-meta { margin-top: 10px; display: flex; flex-wrap: wrap; gap: 8px 14px; font-size: 12px; color: #64748b; }
.history-meta--stacked { flex-direction: column; gap: 4px; }
.thumb-image { width: 100%; height: 100%; object-fit: cover; cursor: zoom-in; background: #f8fafc; display: block; }
.thumb-fallback { width: 100%; height: 100%; display: grid; place-items: center; font-size: 12px; color: #94a3b8; background: linear-gradient(180deg, #f8fafc, #eef2ff); }
.error-box { margin-top: 10px; padding: 8px 10px; border-radius: 10px; background: #fff1f2; color: #be123c; font-size: 12px; line-height: 1.7; }
.history-actions { margin-top: 12px; display: flex; gap: 10px; }
.history-actions--icon { margin-top: 0; justify-content: flex-end; width: 100%; }
.icon-btn { color: #60a5fa; }
.icon-btn:disabled { color: #cbd5e1; }
.history-pagination { margin-top: 18px; text-align: right; }
.empty-state { padding: 36px 18px; text-align: center; color: #94a3b8; }
.preview-modal-image { width: 100%; border-radius: 16px; }
@media (max-width: 1120px) { .image-page { grid-template-columns: 1fr; } }
@media (max-width: 640px) { .config-grid { grid-template-columns: 1fr; } .image-page { padding: 20px 16px 32px; } .history-layout { grid-template-columns: 1fr; } .history-thumb-shell { width: 100%; height: 180px; } .history-side { align-items: flex-start; min-width: 0; } .history-side-time { text-align: left; } .history-actions--icon { justify-content: flex-start; } }
</style>
