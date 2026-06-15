<script setup lang="ts">
import { onMounted, onUnmounted, ref, nextTick } from 'vue'
import type { FormInstance } from 'element-plus'
import * as echarts from 'echarts'
import { useRoute, useRouter } from 'vue-router'
import RouteUtil from '@/utils/RouteUtil'
import UsageApi from '@/api/lm/UsageApi'
import UserApi from '@/api/member/UserApi'
import ModelApi from '@/api/lm/ModelApi'
import AuthApi from '@/api/lm/AuthApi'
import ProviderApi from '@/api/lm/ProviderApi'

const route = useRoute()
const router = useRouter()

const fmtDate = (d: Date) => {
  const pad = (n: number) => n.toString().padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

const now = new Date()
const monthAgo = new Date(now.getTime() - 30 * 24 * 3600 * 1000)
const dateRange = ref<string[]>([fmtDate(monthAgo), fmtDate(now)])

const loading = ref(false)
const filterRef = ref<FormInstance>()
const filters = ref({
  uid: '',
  place: '',
  authId: '',
  modelId: '',
  providerId: '',
  aggregation: 'day',
})

const statisticData = ref<any>(null)

const handleSearch = () => {
  RouteUtil.filter2query(route, router, { ...filters.value, dateRange: dateRange.value })
  loading.value = true
  const params = {
    ...filters.value,
    beginTime: dateRange.value?.[0] || '',
    endTime: dateRange.value?.[1] || '',
  }
  UsageApi.statistic(params).then((result: any) => {
    statisticData.value = result.data
    nextTick(() => renderCharts())
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}

const handleReset = () => {
  filterRef.value?.resetFields()
}

onMounted(() => {
  const saved = RouteUtil.query2filter(route, {}, false)
  if (saved.dateRange) {
    dateRange.value = saved.dateRange
    delete saved.dateRange
  }
  Object.assign(filters.value, saved)
  handleSearch()
})

// Chart management
const chartInstances: echarts.ECharts[] = []

const disposeCharts = () => {
  chartInstances.forEach(c => c.dispose())
  chartInstances.length = 0
}

onUnmounted(() => disposeCharts())

// Chart refs
const rankingUserRef = ref()
const rankingAuthRef = ref()
const rankingProviderRef = ref()
const rankingModelRef = ref()
const distStatusRef = ref()
const distReasonRef = ref()
const timelineCreditsRef = ref()
const timelineCallsRef = ref()
const timelineTokensRef = ref()

const COLORS = ['#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de', '#3ba272', '#fc8452', '#9a60b4']

const initChart = (el: HTMLElement): echarts.ECharts => {
  const instance = echarts.init(el)
  chartInstances.push(instance)
  return instance
}

// Ranking horizontal bar
const renderRankingBar = (
  container: any,
  data: any[],
  labelFn: (item: any) => string,
  title: string,
) => {
  if (!container.value || !data?.length) return
  const instance = initChart(container.value)
  const reversed = [...data].reverse()
  const labels = reversed.map(labelFn)
  const values = reversed.map((d: any) => d.consumeCredits ?? 0)

  instance.setOption({
    title: { text: title, left: 'center', textStyle: { fontSize: 14 } },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params: any) => {
        const item = params[0]
        const d = reversed[item.dataIndex]
        return [
          `${item.marker} ${item.name}`,
          `积分: ${d.consumeCredits ?? 0}`,
          `调用: ${d.calls ?? 0}`,
          `Token: ${d.tokens ?? 0}`,
        ].join('<br/>')
      },
    },
    grid: { left: 130, right: 90, top: 40, bottom: 20 },
    xAxis: { type: 'value' },
    yAxis: {
      type: 'category',
      data: labels,
      axisLabel: { width: 110, overflow: 'truncate' },
    },
    series: [{
      type: 'bar',
      data: values.map((v, i) => ({ value: v, itemStyle: { color: COLORS[i % COLORS.length] } })),
      label: { show: true, position: 'right', formatter: (p: any) => Number(p.value).toFixed(6) },
    }],
  })
}

// Distribution pie
const renderPie = (
  container: any,
  data: any[],
  nameField: string,
  title: string,
) => {
  if (!container.value || !data?.length) return
  const instance = initChart(container.value)
  const pieData = data.map((d: any, i: number) => ({
    name: d[nameField] || '(空)',
    value: d.count,
    itemStyle: { color: COLORS[i % COLORS.length] },
  }))

  instance.setOption({
    title: { text: title, left: 'center', textStyle: { fontSize: 14 } },
    tooltip: { trigger: 'item' },
    legend: { orient: 'vertical', left: 'left', top: 30 },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      center: ['55%', '55%'],
      data: pieData,
      label: { show: true, formatter: '{b}: {c}' },
      emphasis: { itemStyle: { shadowBlur: 10, shadowOffsetX: 0, shadowColor: 'rgba(0,0,0,0.5)' } },
    }],
  })
}

// Timeline stacked bar
const renderTimelineBar = (
  container: any,
  data: any[],
  seriesKeys: string[],
  seriesLabels: Record<string, string>,
  title: string,
  valueFormatter?: (v: number) => string,
  totalKey?: string,
) => {
  if (!container.value || !data?.length) return
  const instance = initChart(container.value)

  instance.setOption({
    title: { text: title, left: 'center', textStyle: { fontSize: 14 } },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params: any) => {
        const lines: string[] = []
        const item = data[params[0].dataIndex]
        lines.push(`<b>${item.time}</b>`)
        if (totalKey) {
          const totalVal = valueFormatter ? valueFormatter(item[totalKey]) : item[totalKey]
          lines.push(`合计  ${totalVal}`)
        }
        params.forEach((p: any) => {
          const val = valueFormatter ? valueFormatter(p.value) : p.value
          lines.push(`${p.marker} ${p.seriesName}  ${val}`)
        })
        return lines.join('<br/>')
      },
    },
    legend: { top: 30, type: 'scroll' },
    grid: { left: 20, right: 20, top: 70, bottom: 100, containLabel: true },
    xAxis: {
      type: 'category',
      boundaryGap: true,
      data: data.map((d: any) => d.time),
      axisLabel: { rotate: 30, interval: 0, margin: 20 },
    },
    yAxis: { type: 'value' },
    series: seriesKeys.map((key, i) => ({
      name: seriesLabels[key] || key,
      type: 'bar',
      stack: 'stack',
      data: data.map((d: any) => d[key] ?? 0),
      itemStyle: { color: COLORS[i % COLORS.length] },
      emphasis: { focus: 'series' },
    })),
  })
}

const renderCharts = () => {
  disposeCharts()
  const data = statisticData.value
  if (!data) return

  // Ranking
  const ranking = data.ranking
  if (ranking) {
    renderRankingBar(rankingUserRef, ranking.byUser, (d: any) => d.uidUserInfo?.name || `用户#${d.uid}`, '用户排名')
    renderRankingBar(rankingAuthRef, ranking.byAuth, (d: any) => d.authInfo?.name || `密钥#${d.authId}`, '密钥排名')
    renderRankingBar(rankingProviderRef, ranking.byProvider, (d: any) => d.providerInfo?.name || `供应商#${d.providerId}`, '供应商排名')
    renderRankingBar(rankingModelRef, ranking.byModel, (d: any) => d.modelInfo?.name || `模型#${d.modelId}`, '模型排名')
  }

  // Distribution
  const dist = data.distribution
  if (dist) {
    renderPie(distStatusRef, dist.byStatus, 'status', '状态分布')
    renderPie(distReasonRef, dist.byFinishReason, 'finishReason', '完成原因分布')
  }

  // Timeline
  const timeline = data.timeline
  if (timeline) {
    // Credits
    if (timeline.credits?.length) {
      const creditKeys = extractPlaceKeys(timeline.credits)
      renderTimelineBar(timelineCreditsRef, timeline.credits, creditKeys, {}, '消费金额趋势',
        (v: number) => v.toFixed(6), 'total')
    }
    // Calls
    if (timeline.calls?.length) {
      const callKeys = extractPlaceKeys(timeline.calls)
      renderTimelineBar(timelineCallsRef, timeline.calls, callKeys, {}, '请求数量趋势', undefined, 'total')
    }
    // Tokens
    if (timeline.tokens?.length) {
      timeline.tokens.forEach((d: any) => {
        d.inputNoCache = (d.inputTokens ?? 0) - (d.cachedTokens ?? 0)
      })
      const tokenKeys = ['cachedTokens', 'inputNoCache', 'outputTokens']
      const tokenLabels: Record<string, string> = {
        cachedTokens: '输入缓存Token', inputNoCache: '输入Token(非缓存)', outputTokens: '输出Token',
      }
      renderTimelineBar(timelineTokensRef, timeline.tokens, tokenKeys, tokenLabels, 'Token消耗趋势', undefined, 'totalTokens')
    }
  }
}

const extractPlaceKeys = (data: any[]): string[] => {
  const keys = new Set<string>()
  data.forEach((d: any) => {
    Object.keys(d).forEach(k => {
      if (k !== 'time' && k !== 'total') keys.add(k)
    })
  })
  return Array.from(keys)
}
</script>

<template>
  <el-card :bordered="false" shadow="never" class="fs-table-search">
    <div class="search-toolbar">
      <form-search ref="filterRef" :model="filters" class="search-form">
        <form-search-item label="时间范围" prop="dateRange" :span="12">
          <el-date-picker
            v-model="dateRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
            clearable
          />
        </form-search-item>
        <form-search-item label="用户" prop="uid">
          <form-select v-model="filters.uid" :callback="UserApi.list" clearable />
        </form-search-item>
        <form-search-item label="调用服务" prop="place">
          <el-input v-model="filters.place" clearable />
        </form-search-item>
        <form-search-item label="密钥" prop="authId">
          <form-select v-model="filters.authId" :callback="AuthApi.list" clearable />
        </form-search-item>
        <form-search-item label="模型" prop="modelId">
          <form-select v-model="filters.modelId" :callback="ModelApi.list" clearable />
        </form-search-item>
        <form-search-item label="供应商" prop="providerId">
          <form-select v-model="filters.providerId" :callback="ProviderApi.list" clearable />
        </form-search-item>
        <form-search-item label="聚合周期" prop="aggregation">
          <el-select v-model="filters.aggregation" clearable>
            <el-option label="按小时" value="hour" />
            <el-option label="按天" value="day" />
            <el-option label="按月" value="month" />
          </el-select>
        </form-search-item>
      </form-search>
      <el-space direction="vertical">
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-space>
    </div>
  </el-card>

  <el-card v-if="statisticData?.summary" :bordered="false" shadow="never" class="stat-summary">
    <div class="summary-grid">
      <el-statistic title="充值积分" :value="statisticData.summary.rechargeCredits ?? 0" />
      <el-statistic title="消费积分" :value="statisticData.summary.consumeCredits ?? 0" :precision="6" />
      <el-statistic title="调用量" :value="statisticData.summary.calls ?? 0" />
      <el-statistic title="缓存Token" :value="statisticData.summary.cachedTokens ?? 0" />
      <el-statistic title="输入Token" :value="statisticData.summary.inputTokens ?? 0" />
      <el-statistic title="输出Token" :value="statisticData.summary.outputTokens ?? 0" />
      <el-statistic title="总Token" :value="statisticData.summary.totalTokens ?? 0" />
    </div>
  </el-card>

  <el-card v-if="statisticData?.ranking" :bordered="false" shadow="never" class="stat-section">
    <template #header>排名统计</template>
    <el-row :gutter="16">
      <el-col :span="12"><div ref="rankingUserRef" class="chart chart-bar"></div></el-col>
      <el-col :span="12"><div ref="rankingAuthRef" class="chart chart-bar"></div></el-col>
      <el-col :span="12"><div ref="rankingProviderRef" class="chart chart-bar"></div></el-col>
      <el-col :span="12"><div ref="rankingModelRef" class="chart chart-bar"></div></el-col>
    </el-row>
  </el-card>

  <el-card v-if="statisticData?.distribution" :bordered="false" shadow="never" class="stat-section">
    <template #header>分布统计</template>
    <el-row :gutter="16">
      <el-col :span="12"><div ref="distStatusRef" class="chart chart-pie"></div></el-col>
      <el-col :span="12"><div ref="distReasonRef" class="chart chart-pie"></div></el-col>
    </el-row>
  </el-card>

  <el-card v-if="statisticData?.timeline" :bordered="false" shadow="never" class="stat-section">
    <template #header>时间趋势</template>
    <div ref="timelineCreditsRef" class="chart chart-timeline"></div>
    <div ref="timelineCallsRef" class="chart chart-timeline"></div>
    <div ref="timelineTokensRef" class="chart chart-timeline"></div>
  </el-card>
</template>

<style lang="scss" scoped>
.stat-summary {
  margin-top: 12px;
}
.summary-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 16px;
  justify-items: center;

  :deep(.el-statistic) {
    text-align: center;
  }
}
.stat-section {
  margin-top: 12px;

  :deep(.el-row) {
    row-gap: 20px;
  }
}
.chart {
  width: 100%;
}
.chart-bar {
  height: 300px;
}
.chart-pie {
  height: 350px;
}
.chart-timeline {
  height: 350px;
}
.search-toolbar {
  display: flex;
  gap: 16px;
  align-items: flex-start;
  .search-form {
    flex: 1;
  }
}
</style>
