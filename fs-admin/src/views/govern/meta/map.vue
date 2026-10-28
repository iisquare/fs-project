<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import MetaApi from '@/api/govern/MetaApi'
import ApiUtil from '@/utils/ApiUtil'
import LayoutHeading from '@/components/Layout/LayoutHeading.vue'
import GovernPageShell from '@/views/govern/components/GovernPageShell.vue'

const router = useRouter()
const keyword = ref('')
const loading = ref(false)
const relationChart = ref<HTMLElement>()
const typeChart = ref<HTMLElement>()
let relationInstance: echarts.ECharts | null = null
let typeInstance: echarts.ECharts | null = null
const statistic = ref<any>({
  modelCount: 0,
  relationCount: 0,
  relationModelCount: 0,
  relationColumnCount: 0,
  relations: [],
  types: [],
  modelSources: [],
  modelTargets: [],
  columnSources: [],
  columnTargets: [],
})

const relationRows = computed(() => statistic.value.relations ?? [])
const typeRows = computed(() => statistic.value.types ?? [])
const rankingPanels = computed(() => [
  { title: '模型出度', rows: statistic.value.modelSources ?? [], text: (item: any) => `${item.sourceCatalog}${item.sourceModel}` },
  { title: '模型入度', rows: statistic.value.modelTargets ?? [], text: (item: any) => `${item.targetCatalog}${item.targetModel}` },
  { title: '字段出度', rows: statistic.value.columnSources ?? [], text: (item: any) => `${item.sourceCatalog}${item.sourceModel}/${item.sourceColumn}` },
  { title: '字段入度', rows: statistic.value.columnTargets ?? [], text: (item: any) => `${item.targetCatalog}${item.targetModel}/${item.targetColumn}` },
])

const render = () => {
  if (relationChart.value) {
    relationInstance ??= echarts.init(relationChart.value)
    relationInstance.setOption({
      tooltip: { trigger: 'item' },
      legend: { bottom: 0, type: 'scroll' },
      series: [{
        type: 'pie',
        radius: ['42%', '70%'],
        center: ['50%', '44%'],
        itemStyle: { borderRadius: 5, borderColor: '#fff', borderWidth: 2 },
        data: relationRows.value.map((item: any) => ({ name: item.relation, value: item.ct })),
      }],
    })
  }
  if (typeChart.value) {
    typeInstance ??= echarts.init(typeChart.value)
    typeInstance.setOption({
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      grid: { left: 20, right: 20, top: 20, bottom: 20, containLabel: true },
      xAxis: { type: 'value' },
      yAxis: { type: 'category', data: typeRows.value.map((item: any) => item.type).reverse() },
      series: [{
        type: 'bar',
        barMaxWidth: 26,
        itemStyle: { color: '#409eff', borderRadius: [0, 5, 5, 0] },
        data: typeRows.value.map((item: any) => item.ct).reverse(),
      }],
    })
  }
}

const load = () => {
  loading.value = true
  MetaApi.statistic({}, { warning: false }).then((result: any) => {
    statistic.value = Object.assign({}, statistic.value, ApiUtil.data(result) ?? {})
    nextTick(render)
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}

const search = () => {
  router.push({ path: '/govern/meta/list', query: { keyword: keyword.value } }).catch(() => {})
}

const resize = () => {
  relationInstance?.resize()
  typeInstance?.resize()
}

onMounted(() => {
  load()
  window.addEventListener('resize', resize)
})
onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  relationInstance?.dispose()
  typeInstance?.dispose()
})
</script>

<template>
  <GovernPageShell title="数据地图" description="查看元模型、字段类型与关系分布，快速定位治理对象">
  <el-card shadow="never" class="search-card">
    <el-input v-model="keyword" clearable placeholder="输入模型或字段名称" @keyup.enter="search">
      <template #prepend>元数据检索</template>
      <template #append><el-button @click="search">检索</el-button></template>
    </el-input>
  </el-card>

  <div class="stats" v-loading="loading">
    <el-card shadow="never"><span>模型数量</span><strong>{{ statistic.modelCount }}</strong></el-card>
    <el-card shadow="never"><span>关系数量</span><strong>{{ statistic.relationCount }}</strong></el-card>
    <el-card shadow="never"><span>模型关系</span><strong>{{ statistic.relationModelCount }}</strong></el-card>
    <el-card shadow="never"><span>字段关系</span><strong>{{ statistic.relationColumnCount }}</strong></el-card>
  </div>

  <div class="charts">
    <el-card shadow="never">
      <LayoutHeading title="关系类型分布" />
      <div ref="relationChart" class="chart" />
    </el-card>
    <el-card shadow="never">
      <LayoutHeading title="字段类型分布" />
      <div ref="typeChart" class="chart" />
    </el-card>
  </div>

  <div class="ranking">
    <el-card shadow="never" v-for="panel in rankingPanels" :key="panel.title">
      <LayoutHeading :title="panel.title" />
      <el-empty v-if="!(panel.rows ?? []).length" :image-size="58" description="暂无数据" />
      <div v-for="(item, index) in panel.rows ?? []" :key="index" class="rank-row">
        <span>{{ panel.text(item) }}</span>
        <el-tag effect="plain">{{ item.ct }}</el-tag>
      </div>
    </el-card>
  </div>
  </GovernPageShell>
</template>

<style scoped>
.search-card { margin-bottom: 15px; }
.stats, .charts { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 15px; }
.stats > :deep(.el-card__body) { display: flex; align-items: baseline; justify-content: space-between; }
.stats span { color: #606266; font-size: 13px; }
.stats strong { color: #1f2d3d; font-size: 30px; }
.charts { grid-template-columns: repeat(2, minmax(0, 1fr)); margin-top: 15px; }
.chart { height: 300px; }
.ranking { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 15px; margin-top: 15px; }
.rank-row { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding: 8px 0; border-bottom: 1px solid var(--el-border-color-lighter); color: #606266; }
.rank-row:last-child { border-bottom: 0; }
.rank-row span { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
@media (max-width: 1000px) {
  .stats, .charts, .ranking { grid-template-columns: 1fr 1fr; }
}
</style>
