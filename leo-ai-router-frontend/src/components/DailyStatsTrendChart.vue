<template>
  <div ref="containerRef" class="chart-shell">
    <svg :width="width" :height="height" class="chart-svg">
      <line
        v-for="tick in ticks"
        :key="`grid-${tick}`"
        :x1="padding.left"
        :x2="width - padding.right"
        :y1="yLeft(tick)"
        :y2="yLeft(tick)"
        class="grid-line"
      />
      <line :x1="padding.left" :x2="padding.left" :y1="padding.top" :y2="chartBottom" class="axis-line" />
      <line :x1="width - padding.right" :x2="width - padding.right" :y1="padding.top" :y2="chartBottom" class="axis-line" />
      <line :x1="padding.left" :x2="width - padding.right" :y1="chartBottom" :y2="chartBottom" class="axis-line" />

      <text
        v-for="tick in ticks"
        :key="`left-${tick}`"
        :x="padding.left - 10"
        :y="yLeft(tick) + 4"
        text-anchor="end"
        class="axis-text"
      >
        {{ formatNumber(tick) }}
      </text>

      <text
        v-for="tick in rightTicks"
        :key="`right-${tick}`"
        :x="width - padding.right + 10"
        :y="yRight(tick) + 4"
        text-anchor="start"
        class="axis-text"
      >
        {{ formatCost(tick) }}
      </text>

      <g v-for="point in normalizedData" :key="point.date">
        <rect
          :x="point.x - barWidth / 2"
          :y="yLeft(point.requestCount)"
          :width="barWidth"
          :height="Math.max(chartBottom - yLeft(point.requestCount), 2)"
          class="bar-rect"
        />
        <text :x="point.x" :y="chartBottom + 18" text-anchor="middle" class="axis-text axis-text--bottom">
          {{ point.date.slice(5) }}
        </text>
      </g>

      <polyline :points="tokenPolyline" class="line-token" />
      <polyline :points="costPolyline" class="line-cost" />

      <circle v-for="point in normalizedData" :key="`t-${point.date}`" :cx="point.x" :cy="yLeft(point.totalTokens)" r="4" class="dot-token" />
      <circle v-for="point in normalizedData" :key="`c-${point.date}`" :cx="point.x" :cy="yRight(point.totalCost)" r="4" class="dot-cost" />

      <rect
        v-for="point in normalizedData"
        :key="`h-${point.date}`"
        :x="point.x - hoverWidth / 2"
        :y="padding.top"
        :width="hoverWidth"
        :height="chartBottom - padding.top"
        class="hover-zone"
        @mousemove="showTooltip($event, point)"
        @mouseleave="hideTooltip"
      />
    </svg>

    <div v-if="tooltip.visible" class="tooltip" :style="tooltipStyle">
      <div class="tooltip-title">{{ tooltip.point?.date }}</div>
      <div class="tooltip-row">Token: {{ formatNumber(tooltip.point?.totalTokens ?? 0) }}</div>
      <div class="tooltip-row">请求数: {{ formatNumber(tooltip.point?.requestCount ?? 0) }}</div>
      <div class="tooltip-row">成功数: {{ formatNumber(tooltip.point?.successCount ?? 0) }}</div>
      <div class="tooltip-row">费用: {{ formatCost(tooltip.point?.totalCost ?? 0) }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'

interface DailyPoint {
  date: string
  totalTokens: number
  requestCount: number
  successCount: number
  totalCost: number
}

const props = defineProps<{ data: DailyPoint[] }>()

const containerRef = ref<HTMLDivElement | null>(null)
const width = ref(920)
const height = 360
const padding = { top: 20, right: 72, bottom: 44, left: 64 }
const chartBottom = height - padding.bottom

const tooltip = reactive<{
  visible: boolean
  x: number
  y: number
  point: DailyPoint | null
}>({
  visible: false,
  x: 0,
  y: 0,
  point: null,
})

const normalizedData = computed(() => {
  const chartWidth = Math.max(width.value - padding.left - padding.right, 220)
  const step = props.data.length > 1 ? chartWidth / (props.data.length - 1) : chartWidth / 2
  return props.data.map((item, index) => ({
    ...item,
    x: padding.left + (props.data.length > 1 ? index * step : chartWidth / 2),
  }))
})

const maxLeftValue = computed(() => Math.max(...props.data.flatMap((item) => [item.totalTokens, item.requestCount]), 1))
const maxRightValue = computed(() => Math.max(...props.data.map((item) => item.totalCost), 0.01))

const ticks = computed(() => {
  const step = maxLeftValue.value / 4
  return [0, 1, 2, 3, 4].map((index) => Math.round(step * index))
})

const rightTicks = computed(() => {
  const step = maxRightValue.value / 4
  return [0, 1, 2, 3, 4].map((index) => Number((step * index).toFixed(2)))
})

const yLeft = (value: number) => chartBottom - (value / maxLeftValue.value) * (chartBottom - padding.top)
const yRight = (value: number) => chartBottom - (value / maxRightValue.value) * (chartBottom - padding.top)

const tokenPolyline = computed(() => normalizedData.value.map((item) => `${item.x},${yLeft(item.totalTokens)}`).join(' '))
const costPolyline = computed(() => normalizedData.value.map((item) => `${item.x},${yRight(item.totalCost)}`).join(' '))
const hoverWidth = computed(() => {
  const chartWidth = Math.max(width.value - padding.left - padding.right, 220)
  return props.data.length > 1 ? chartWidth / props.data.length : chartWidth
})
const barWidth = computed(() => Math.max(Math.min(hoverWidth.value * 0.36, 28), 10))

const tooltipStyle = computed(() => ({ left: `${tooltip.x}px`, top: `${tooltip.y}px` }))

const formatNumber = (value: number) => value.toLocaleString('zh-CN')
const formatCost = (value: number) => `¥${value.toFixed(2)}`

const updateSize = () => {
  if (containerRef.value) {
    width.value = Math.max(containerRef.value.clientWidth, 320)
  }
}

const showTooltip = (event: MouseEvent, point: DailyPoint) => {
  if (!containerRef.value) return
  const rect = containerRef.value.getBoundingClientRect()
  tooltip.visible = true
  tooltip.x = event.clientX - rect.left + 16
  tooltip.y = event.clientY - rect.top + 16
  tooltip.point = point
}

const hideTooltip = () => {
  tooltip.visible = false
}

onMounted(() => {
  updateSize()
  window.addEventListener('resize', updateSize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', updateSize)
})
</script>

<style scoped>
.chart-shell { position: relative; width: 100%; min-height: 360px; }
.chart-svg { display: block; overflow: visible; }
.grid-line { stroke: var(--leo-border); stroke-dasharray: 4 4; }
.axis-line { stroke: var(--leo-border-strong); }
.axis-text { fill: var(--leo-text-secondary); font-size: 12px; }
.axis-text--bottom { fill: var(--leo-text-tertiary); }
.bar-rect { fill: rgba(36, 91, 255, 0.14); }
.line-token { fill: none; stroke: var(--leo-primary); stroke-width: 3; }
.line-cost { fill: none; stroke: var(--leo-warning); stroke-width: 3; }
.dot-token { fill: var(--leo-primary); }
.dot-cost { fill: var(--leo-warning); }
.hover-zone { fill: transparent; }
.tooltip { position: absolute; pointer-events: none; z-index: 2; min-width: 156px; border-radius: var(--leo-radius-md); padding: 12px 14px; background: rgba(15, 23, 42, 0.94); color: #e2e8f0; box-shadow: var(--leo-shadow-pop); }
.tooltip-title { font-weight: 700; margin-bottom: 8px; }
.tooltip-row { font-size: 12px; line-height: 1.7; }
</style>
