<template>
  <div class="image-page">
    <div class="image-workspace">
      <main class="image-main">
        <section class="workspace-panel prompt-panel">
          <div class="panel-header">
            <div>
              <div class="panel-kicker">Image Generation</div>
              <div class="panel-title">AI 绘图</div>
              <div class="panel-desc">输入提示词，右侧设置模型与参数。</div>
            </div>
          </div>

          <a-form layout="vertical">
            <a-form-item label="提示词" required>
              <a-textarea
                v-model:value="form.prompt"
                :rows="8"
                :maxlength="1200"
                show-count
                placeholder="例如：黄昏时分的未来主义海边城市，霓虹灯反射在潮湿路面上，电影感构图，超高细节"
              />
            </a-form-item>

            <div class="generate-actions">
              <a-space wrap>
                <a-button
                  type="primary"
                  size="large"
                  :loading="generating"
                  :disabled="!canGenerateImage"
                  @click="handleGenerate"
                >
                  {{ generating ? '生成中...' : '立即生成' }}
                </a-button>
                <a-button size="large" @click="resetForm">重置</a-button>
              </a-space>
              <div v-if="imageAccessWarning" class="access-warning">{{ imageAccessWarning }}</div>
            </div>
          </a-form>
        </section>

        <section class="workspace-panel latest-panel">
          <div class="section-head">
            <div>
              <div class="section-title">最近生成结果</div>
              <div class="section-desc">生成成功后可在这里快速预览。</div>
            </div>
            <a-button v-if="latestPreview" type="link" @click="openPreview(latestPreview)">预览大图</a-button>
          </div>

          <div v-if="latestPreview" class="latest-preview">
            <img :src="latestPreview" alt="latest result" class="preview-image" @click="openPreview(latestPreview)" />
          </div>
          <div v-else class="preview-empty">暂无最新图片</div>
        </section>

        <section class="workspace-panel history-panel">
          <div class="history-head">
            <div>
              <div class="section-title">历史记录</div>
              <div class="section-desc">最近的图片生成任务和状态会显示在这里。</div>
            </div>
            <a-button size="small" @click="loadRecords">刷新</a-button>
          </div>

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
                  <div class="history-title-row">
                    <div class="history-prompt">{{ record.prompt || '无提示词' }}</div>
                    <a-tag :color="record.status === 'success' ? 'green' : 'red'">
                      {{ record.status === 'success' ? '成功' : '失败' }}
                    </a-tag>
                  </div>

                  <div class="history-meta">
                    <span>模型：{{ record.modelKey || '-' }}</span>
                    <span>尺寸：{{ record.size || '-' }}</span>
                    <span>费用：¥{{ Number(record.cost ?? 0).toFixed(4) }}</span>
                    <span>时间：{{ formatTime(record.createTime) }}</span>
                  </div>

                  <div v-if="record.status !== 'success'" class="error-box">
                    {{ record.errorMessage || '生成失败' }}
                  </div>
                </div>

                <div class="history-actions">
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
                  <a-button size="small" @click="showDetail(record)">详情</a-button>
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
        </section>
      </main>

      <aside class="image-settings">
        <section class="workspace-panel settings-panel">
          <div class="section-title">生成设置</div>

          <a-form layout="vertical">
            <a-form-item label="模型选择">
              <a-radio-group v-model:value="form.model" class="model-group">
                <label
                  v-for="item in imageModels"
                  :key="item.modelKey"
                  class="model-card"
                  :class="{ active: form.model === item.modelKey, disabled: !canUseImageModel(item) }"
                >
                  <input
                    v-model="form.model"
                    type="radio"
                    :value="item.modelKey"
                    :disabled="!canUseImageModel(item)"
                    class="model-radio"
                  />
                  <div class="model-head">
                    <span class="model-name">{{ item.modelName || item.modelKey }}</span>
                    <span class="model-provider">{{ item.providerDisplayName || item.providerName }}</span>
                  </div>
                  <div class="model-meta">
                    <span>{{ item.description || '图片生成模型' }}</span>
                    <span class="model-price">{{ formatPointCost(item) }}</span>
                  </div>
                  <div v-if="isMemberOnlyImageModel(item)" class="model-lock" :class="{ open: canUseImageModel(item) }">
                    <LockOutlined />
                    {{ canUseImageModel(item) ? '会员模型，当前可用' : '会员模型，请先开通套餐' }}
                  </div>
                </label>
              </a-radio-group>
            </a-form-item>

            <div class="config-grid">
              <a-form-item label="尺寸">
                <a-select v-model:value="form.size" :disabled="isGeminiImageModel">
                  <a-select-option v-for="item in sizeOptions" :key="item" :value="item">
                    {{ item }}
                  </a-select-option>
                </a-select>
              </a-form-item>
              <a-form-item label="质量">
                <a-select v-model:value="form.quality" :disabled="isGeminiImageModel">
                  <a-select-option value="low">low</a-select-option>
                  <a-select-option value="medium">medium</a-select-option>
                  <a-select-option value="high">high</a-select-option>
                </a-select>
              </a-form-item>
            </div>
            <div v-if="isGeminiImageModel" class="config-hint">
              Gemini 图片模型当前不使用尺寸和质量参数，服务端会按模型默认策略生成单图，并以 Base64 结果返回。
            </div>

            <div class="estimate-box">
              <div class="estimate-label">预估积分</div>
              <div class="estimate-value">{{ estimatedPoints }} 积分</div>
              <div class="estimate-desc">
                积分余额 {{ formatNumber(membership.pointBalance) }}，图片生成成功后扣除；失败不扣。
              </div>
            </div>

            <div v-if="imageAccessWarning" class="settings-warning">{{ imageAccessWarning }}</div>
          </a-form>
        </section>
      </aside>
    </div>

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
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { DownloadOutlined, EyeOutlined, LockOutlined } from '@ant-design/icons-vue'
import { generateImage, getMyRecords } from '@/api/imageController'
import { listAvailableModels } from '@/api/modelController'
import { getMyMembership, type MembershipVO } from '@/api/membershipController'

const generating = ref(false)
const historyLoading = ref(false)
const imageModels = ref<API.ModelVO[]>([])
const membership = ref<MembershipVO>({})
const records = ref<API.ImageGenerationRecord[]>([])
const latestPreview = ref('')
const previewOpen = ref(false)
const previewUrl = ref('')
const detailOpen = ref(false)
const detailRecord = ref<API.ImageGenerationRecord>()
const imageCacheKey = 'leo_ai_router_image_history_cache'

const defaultImageSizes = ['1024x1024', '1280x720', '720x1280']
const openAiImage15Sizes = ['1024x1024', '1536x1024', '1024x1536']
const openAiImage2Sizes = ['auto', '1024x1024', '1536x1024', '1024x1536', '2048x2048', '2048x1152']

const form = reactive<API.ImageGenerationRequest>({
  prompt: '',
  model: '',
  size: '1024x1024',
  quality: 'medium',
  response_format: 'b64_json',
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

const isPaidMember = computed(() => {
  const planCode = membership.value.planCode
  return Boolean(planCode && planCode !== 'free' && membership.value.status !== 'free')
})

const isGeminiImageModel = computed(() => {
  const providerName = selectedModel.value?.providerName?.toLowerCase?.() ?? ''
  return providerName === 'gemini' || providerName === 'google'
})

const sizeOptions = computed(() => {
  if (selectedModel.value?.modelKey === 'gpt-image-2') {
    return openAiImage2Sizes
  }
  if (selectedModel.value?.modelKey === 'gpt-image-1.5') {
    return openAiImage15Sizes
  }
  if (isGeminiImageModel.value) {
    return ['1024x1024']
  }
  return defaultImageSizes
})

const estimatedPoints = computed(() => Number(selectedModel.value?.pointCost ?? 0) * Number(form.n ?? 1))

const canGenerateImage = computed(() => {
  if (!selectedModel.value) {
    return false
  }
  if (!canUseImageModel(selectedModel.value)) {
    return false
  }
  return Number(membership.value.pointBalance ?? 0) >= estimatedPoints.value
})

const imageAccessWarning = computed(() => {
  if (!selectedModel.value) {
    return '请选择图片模型'
  }
  if (!canUseImageModel(selectedModel.value)) {
    return '当前图片模型仅限会员使用，请先开通套餐'
  }
  if (Number(membership.value.pointBalance ?? 0) < estimatedPoints.value) {
    return '积分余额不足，请先购买积分'
  }
  return ''
})

const formatPrice = (value?: number) => `¥${Number(value ?? 0).toFixed(2)} / 张`
const formatNumber = (value?: number) => Number(value ?? 0).toLocaleString('zh-CN')
const formatPointCost = (model: API.ModelVO) => {
  const cost = Number(model.pointCost ?? 0)
  return cost > 0 ? `${cost} 积分 / 张` : formatPrice(model.inputPrice)
}

const isMemberOnlyImageModel = (model?: API.ModelVO) => {
  const providerName = model?.providerName?.toLowerCase?.() ?? ''
  const modelKey = model?.modelKey?.toLowerCase?.() ?? ''
  return providerName.includes('openai') || providerName.includes('gemini') || providerName.includes('google')
    || modelKey.includes('gpt') || modelKey.includes('gemini')
}

const canUseImageModel = (model?: API.ModelVO) => {
  if (!model) {
    return false
  }
  return !isMemberOnlyImageModel(model) || isPaidMember.value
}

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

watch(selectedModel, (model) => {
  const options = sizeOptions.value
  if (options.length > 0 && !options.includes(form.size || '')) {
    form.size = options[0]
  }
  if (!model) return
  if (isGeminiImageModel.value) {
    form.size = '1024x1024'
    form.quality = 'medium'
    return
  }
  if (form.quality !== 'low' && form.quality !== 'medium' && form.quality !== 'high') {
    form.quality = 'medium'
  }
})

const loadModels = async () => {
  const res = await listAvailableModels()
  if (res.data.code === 0) {
    imageModels.value = (res.data.data ?? [])
      .filter((item) => item.modelType === 'image')
      .sort((a, b) => Number(a.inputPrice ?? 0) - Number(b.inputPrice ?? 0))
    const firstModel = imageModels.value.find(canUseImageModel) ?? imageModels.value[0]
    if (!form.model && firstModel?.modelKey) {
      form.model = firstModel.modelKey
    }
  } else {
    message.error(res.data.message ?? '加载图片模型失败')
  }
}

const loadMembership = async () => {
  const res = await getMyMembership()
  if (res.data.code === 0) {
    membership.value = res.data.data ?? {}
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
  if (!selectedModel.value || !canUseImageModel(selectedModel.value)) {
    message.warning('当前图片模型仅限会员使用，请先开通套餐')
    return
  }
  if (Number(membership.value.pointBalance ?? 0) < estimatedPoints.value) {
    message.warning('积分余额不足，请先购买积分')
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
      response_format: 'b64_json',
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
  form.size = sizeOptions.value[0] ?? '1024x1024'
  form.quality = 'medium'
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
  void Promise.all([loadMembership(), loadModels(), loadRecords()])
})
</script>

<style scoped>
.image-page { width: min(100%, var(--leo-page-max)); margin: 0 auto; padding: 24px; }
.image-workspace { display: grid; grid-template-columns: minmax(0, 1fr) 360px; gap: 16px; align-items: start; }
.image-main { display: grid; gap: 16px; min-width: 0; }
.image-settings { position: sticky; top: calc(var(--leo-header-height) + 16px); min-width: 0; }
.workspace-panel { background: var(--leo-bg-panel); border: 1px solid var(--leo-border); border-radius: var(--leo-radius-md); box-shadow: 0 8px 24px rgba(15, 23, 42, 0.06); padding: 18px; }
.panel-header, .history-head, .section-head { display: flex; justify-content: space-between; gap: 12px; align-items: flex-start; margin-bottom: 14px; }
.panel-kicker { color: var(--leo-primary); font-size: 12px; font-weight: 700; text-transform: uppercase; }
.panel-title { margin-top: 4px; color: var(--leo-text-primary); font-size: 24px; font-weight: 700; line-height: 1.3; }
.panel-desc, .section-desc { margin-top: 4px; color: var(--leo-text-secondary); font-size: 13px; line-height: 1.6; }
.section-title { color: var(--leo-text-primary); font-size: 16px; font-weight: 700; line-height: 1.4; }
.settings-panel { padding-bottom: 12px; }
.model-group { width: 100%; display: grid; gap: 10px; }
.model-card { display: block; position: relative; padding: 12px; border-radius: var(--leo-radius-md); border: 1px solid var(--leo-border); background: var(--leo-bg-panel); cursor: pointer; transition: border-color 0.18s ease, box-shadow 0.18s ease, background 0.18s ease; }
.model-card.active { border-color: var(--leo-primary); box-shadow: 0 0 0 3px rgba(36, 91, 255, 0.1); background: var(--leo-bg-active); }
.model-card.disabled { cursor: not-allowed; background: var(--leo-bg-muted); border-style: dashed; opacity: 0.76; }
.model-radio { position: absolute; opacity: 0; pointer-events: none; }
.model-head { display: flex; justify-content: space-between; gap: 10px; align-items: flex-start; }
.model-name { color: var(--leo-text-primary); font-size: 14px; font-weight: 700; line-height: 1.4; }
.model-provider { flex: none; color: var(--leo-primary); background: var(--leo-primary-soft); border-radius: 999px; padding: 3px 8px; font-size: 12px; line-height: 1.3; }
.model-meta { margin-top: 8px; display: grid; gap: 6px; color: var(--leo-text-secondary); font-size: 12px; line-height: 1.5; }
.model-price { color: var(--leo-warning); font-weight: 700; }
.model-lock { margin-top: 8px; display: flex; align-items: center; gap: 6px; color: var(--leo-warning); font-size: 12px; line-height: 1.5; }
.model-lock.open { color: var(--leo-success); }
.config-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; }
.config-hint { margin: -2px 0 12px; padding: 10px 12px; border-radius: var(--leo-radius-md); background: var(--leo-bg-muted); color: var(--leo-text-secondary); font-size: 12px; line-height: 1.6; border: 1px dashed var(--leo-border-strong); }
.estimate-box { margin-top: 4px; padding: 14px; border-radius: var(--leo-radius-md); background: var(--leo-bg-muted); border: 1px solid var(--leo-border); }
.estimate-label { color: var(--leo-text-secondary); font-size: 12px; }
.estimate-value { margin-top: 4px; color: var(--leo-primary); font-size: 28px; font-weight: 700; line-height: 1.2; }
.estimate-desc { margin-top: 8px; color: var(--leo-text-secondary); font-size: 12px; line-height: 1.6; }
.generate-actions { margin-top: 16px; display: flex; justify-content: space-between; gap: 12px; align-items: center; flex-wrap: wrap; }
.access-warning, .settings-warning { color: var(--leo-warning); font-size: 13px; line-height: 1.6; }
.settings-warning { margin-top: 12px; padding: 10px 12px; border-radius: var(--leo-radius-md); background: #fff7ed; border: 1px solid #fed7aa; }
.latest-preview { overflow: hidden; border-radius: var(--leo-radius-md); border: 1px solid var(--leo-border); background: var(--leo-bg-muted); }
.preview-image { display: block; width: 100%; max-height: 520px; object-fit: contain; cursor: zoom-in; background: var(--leo-bg-muted); }
.preview-empty { height: 260px; display: grid; place-items: center; border: 1px dashed var(--leo-border-strong); border-radius: var(--leo-radius-md); background: var(--leo-bg-muted); color: var(--leo-text-tertiary); }
.history-list { display: flex; flex-direction: column; gap: 10px; }
.history-item { padding: 12px; border-radius: var(--leo-radius-md); border: 1px solid var(--leo-border); background: var(--leo-bg-panel); }
.history-layout { display: grid; grid-template-columns: 92px minmax(0, 1fr) auto; gap: 12px; align-items: start; }
.history-thumb-shell { width: 92px; height: 92px; border-radius: var(--leo-radius-md); overflow: hidden; background: var(--leo-bg-muted); border: 1px solid var(--leo-border); }
.history-title-row { display: flex; gap: 8px; align-items: flex-start; flex-wrap: wrap; }
.history-prompt { min-width: 180px; flex: 1; color: var(--leo-text-primary); font-size: 14px; font-weight: 600; line-height: 1.6; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.history-meta { margin-top: 8px; display: flex; flex-wrap: wrap; gap: 6px 12px; color: var(--leo-text-secondary); font-size: 12px; line-height: 1.5; }
.thumb-image { width: 100%; height: 100%; object-fit: cover; cursor: zoom-in; background: var(--leo-bg-muted); display: block; }
.thumb-fallback { width: 100%; height: 100%; display: grid; place-items: center; color: var(--leo-text-tertiary); background: var(--leo-bg-muted); font-size: 12px; }
.error-box { margin-top: 8px; padding: 8px 10px; border-radius: var(--leo-radius-md); background: #fff1f2; color: var(--leo-danger); font-size: 12px; line-height: 1.6; }
.history-actions { display: flex; gap: 6px; align-items: center; justify-content: flex-end; }
.icon-btn { color: var(--leo-primary); }
.icon-btn:disabled { color: var(--leo-text-tertiary); }
.history-pagination { margin-top: 16px; text-align: right; }
.empty-state { padding: 34px 18px; text-align: center; color: var(--leo-text-tertiary); }
.preview-modal-image { width: 100%; border-radius: var(--leo-radius-md); }
@media (max-width: 1120px) {
  .image-workspace { grid-template-columns: 1fr; }
  .image-settings { position: static; }
}
@media (max-width: 720px) {
  .image-page { padding: 16px; }
  .workspace-panel { padding: 14px; }
  .config-grid, .history-layout { grid-template-columns: 1fr; }
  .history-thumb-shell { width: 100%; height: 180px; }
  .history-actions { justify-content: flex-start; flex-wrap: wrap; }
  .generate-actions { align-items: flex-start; }
}
</style>
