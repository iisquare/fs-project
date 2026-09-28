<script setup lang="ts">
/**
 * 流程统计 - 按 时间 / 流程 / 用户 / 对话类型 / 对话状态 / 执行状态 / 时间聚合层级 筛选，
 * 统计会话数量（时间轴）、对话轮次数量（时间轴）、流程排名与用户排名。
 *
 * 口径：
 * - 会话取编排会话（按创建时间），对话轮次取运行日志（一轮对话一条）；
 *   两侧按同一套「流程 + 用户 + 类型 + 删除状态」条件过滤，图上的会话数与轮次数才对得齐；
 * - 删除状态与列表页的「全部 / 已删除 / 未删除」一致：会话被删除时，消息与运行日志一并打删除标记，
 *   所以轮次侧的状态会跟着会话走；
 * - 执行状态：轮次看运行日志自身的状态，会话看它自己的轮次——有一轮失败即为失败，
 *   全成功才算成功，所以「成功 / 失败」两张时间轴与两张排名都是同一套口径；
 * - 执行状态分布图不带状态筛选（其余筛选照常生效）：先记分布再按筛选决定算不算数，
 *   筛「失败」时图上仍能看到成功与失败各占多少；
 * - 时间聚合层级（小时 / 天 / 周 / 月）由后端按东八区切分，周取周一；
 * - 默认近一周：统计页一打开就该有结论，全量口径没人看。
 */
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import AgenticApi from '@/api/agent/AgenticApi'
import UserApi from '@/api/member/UserApi'
import ApiUtil from '@/utils/ApiUtil'
import DateUtil from '@/utils/DateUtil'
import RouteUtil from '@/utils/RouteUtil'

const route = useRoute()
const router = useRouter()

const DAY = 24 * 60 * 60 * 1000
/** 与后端一致的区间上限：整体 1 年，按小时 31 天（先在前端拦一道，不必等接口返回） */
const MAX_RANGE = 366 * DAY
const MAX_HOUR_RANGE = 31 * DAY
/** 图上最多画几条排名：全画会把横向条压成细线，完整的量在下方表格里 */
const RANK_LIMIT = 10
/** 图表配色：前几个取 Element Plus 的语义色，与页面其它部分一致 */
const COLORS = ['#409eff', '#67c23a', '#e6a23c', '#909399', '#f56c6c', '#79bbff', '#95d475', '#eebe77', '#b1b3b8', '#f89898']

/** 默认近一周（含今天共 7 天） */
const defaultRange = (): string[] => [
  DateUtil.format(Date.now() - 6 * DAY, 'yyyy-MM-dd 00:00:00'),
  DateUtil.format(Date.now(), 'yyyy-MM-dd 23:59:59'),
]

const loading = ref(false)
const data = ref<any>({})
const filters = ref<any>(RouteUtil.query2filter(route, {
  range: defaultRange(),
  agenticId: '',
  uid: '',
  type: '',
  deleted: '',
  status: '',
  aggregation: 'day',
}, false))

/** 「YYYY-MM-DD HH:mm:ss」按本地时区解析：直接交给 new Date 在部分浏览器上会被当成 UTC */
const millis = (value: any, fallback: number) => {
  if (!value) return fallback
  const time = new Date(String(value).replace(/-/g, '/')).getTime()
  return isNaN(time) ? fallback : time
}

const buildParam = () => {
  const range: string[] = filters.value.range ?? []
  const now = Date.now()
  return {
    beginTime: millis(range[0], now - 6 * DAY),
    endTime: millis(range[1], now),
    agenticId: Number(filters.value.agenticId) || 0,
    uid: Number(filters.value.uid) || 0,
    type: filters.value.type || '',
    deleted: filters.value.deleted || '',
    status: filters.value.status || '',
    aggregation: filters.value.aggregation || 'day',
  }
}

const load = (warning = false) => {
  loading.value = true
  AgenticApi.statistic(buildParam(), { warning, error: false }).then((result: any) => {
    data.value = ApiUtil.data(result) ?? {}
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}

const handleSearch = () => {
  const param = buildParam()
  if (param.endTime - param.beginTime > MAX_RANGE) {
    ElMessage.warning('统计区间不能超过 1 年')
    return
  }
  if ('hour' === param.aggregation && param.endTime - param.beginTime > MAX_HOUR_RANGE) {
    ElMessage.warning('按小时统计的区间不能超过 31 天，请改用按天')
    return
  }
  RouteUtil.filter2query(route, router, filters.value)
  load(true)
}
const handleReset = () => {
  filters.value = {
    range: defaultRange(), agenticId: '', uid: '', type: '', deleted: '', status: '', aggregation: 'day',
  }
  handleSearch()
}

/* ------------------------------- 概览 ------------------------------- */

const summary = computed<any>(() => data.value?.summary ?? {})
/** 毫秒转秒：统计里看的是「平均」量级，留一位小数够用 */
const renderSeconds = (value: any) => {
  const ms = Number(value ?? 0)
  return ms < 1 ? '—' : (ms / 1000).toFixed(1) + 's'
}
const overview = computed(() => {
  const item = summary.value
  const sessions = Number(item.sessions ?? 0)
  const rounds = Number(item.rounds ?? 0)
  return [
    { label: '会话数量', value: sessions },
    { label: '对话轮次', value: rounds },
    { label: '成功轮次', value: Number(item.succeeded ?? 0) },
    { label: '失败轮次', value: Number(item.failed ?? 0) },
    { label: '参与用户', value: Number(item.users ?? 0) },
    { label: '覆盖流程', value: Number(item.flows ?? 0) },
    { label: '平均轮次/会话', value: sessions < 1 ? '—' : (rounds / sessions).toFixed(1) },
    { label: '对话成功率', value: rounds < 1 ? '—' : Math.round(Number(item.successRate ?? 0)) + '%' },
    { label: '平均耗时', value: renderSeconds(item.avgDuration) },
  ]
})

/** 空数据时不画图：只留空状态提示，免得同时出现一个空坐标系 */
const hasTimeline = computed(() => (data.value?.timeline ?? []).length > 0)
const hasTypes = computed(() => (data.value?.types ?? [])
  .some((row: any) => Number(row.sessions ?? 0) + Number(row.rounds ?? 0) > 0))
const hasStatuses = computed(() => (data.value?.statuses ?? [])
  .some((row: any) => Number(row.rounds ?? 0) > 0))
/** 当前执行状态筛选的文案：分布图不跟筛选走，筛选生效时要把它说清楚 */
const statusText = computed(() => ('success' === filters.value.status ? '成功'
  : ('failed' === filters.value.status ? '失败' : '')))

/* ------------------------------- 图表 ------------------------------- */

/** 容器用函数 ref 收进来：四张图共用一套渲染与尺寸监听，不必各写一遍 */
const doms = new Map<string, HTMLDivElement>()
/** 实例按「容器元素」索引：ResizeObserver 回调里拿到的就是元素，用它才好反查 */
const instances = new Map<HTMLElement, echarts.ECharts>()
let observer: ResizeObserver | null = null

/** 收掉某个容器上的实例：元素被 v-if 换掉或卸载时，实例与尺寸监听都要一起释放 */
const disposeChart = (el: HTMLElement) => {
  const chart = instances.get(el)
  if (!chart) return
  chart.dispose()
  instances.delete(el)
  observer?.unobserve(el)
}

const bind = (key: string) => (el: any) => {
  const old = doms.get(key)
  if (el) {
    if (old && old !== el) disposeChart(old)
    doms.set(key, el as HTMLDivElement)
  } else if (old) {
    disposeChart(old)
    doms.delete(key)
  }
}
const bindSessions = bind('sessions')
const bindRounds = bind('rounds')
const bindTypes = bind('types')
const bindStatuses = bind('statuses')
const bindFlows = bind('flows')
const bindUsers = bind('users')

const renderChart = (key: string, option: any) => {
  const el = doms.get(key)
  if (!el || !option) return
  let chart = instances.get(el)
  if (!chart) {
    chart = echarts.init(el)
    instances.set(el, chart)
    observer?.observe(el)
  }
  chart.setOption(option, true)
}

/**
 * 时间轴：成功 / 失败堆叠成一根柱（量本身不大，柱顶直接标总数），
 * 明细（成功、失败、参与用户、覆盖流程）都在 tooltip 里给全，不必再画第二条轴
 */
const timelineOption = (
  rows: any[],
  totalKey: string,
  totalName: string,
  groups: Array<{ key: string; name: string; color: string }>,
  extras: Array<{ key: string; name: string }>,
) => ({
  tooltip: {
    trigger: 'axis',
    axisPointer: { type: 'shadow' },
    formatter: (params: any[]) => {
      const row = rows[params?.[0]?.dataIndex ?? 0]
      if (!row) return ''
      return [
        String(row.time ?? ''),
        `${totalName}合计：${row[totalKey] ?? 0}`,
        ...groups.map((group) => `${group.name}：${row[group.key] ?? 0}`),
        ...extras.map((extra) => `${extra.name}：${row[extra.key] ?? 0}`),
      ].join('<br/>')
    },
  },
  legend: { top: 0, itemWidth: 10, itemHeight: 10, textStyle: { fontSize: 11 } },
  grid: { left: 8, right: 16, top: 32, bottom: 8, containLabel: true },
  xAxis: {
    type: 'category',
    data: rows.map((row: any) => row.time),
    axisLabel: { fontSize: 11, interval: 'auto', rotate: rows.length > 10 ? 30 : 0, hideOverlap: true },
  },
  yAxis: { type: 'value', minInterval: 1, axisLabel: { fontSize: 11 }, splitLine: { lineStyle: { type: 'dashed' } } },
  series: groups.map((group, index) => ({
    name: group.name,
    type: 'bar',
    stack: 'status',
    barMaxWidth: 26,
    itemStyle: { color: group.color },
    // 总数标在最上面那一段的柱顶：堆叠柱的顶端就是合计
    label: {
      show: index === groups.length - 1 && rows.length <= 20,
      position: 'top',
      fontSize: 11,
      color: '#909399',
    },
    data: rows.map((row: any) => Number(row[group.key] ?? 0)),
  })),
})

/** 对话类型分布：只有调试运行与发布应用两种，饼图看占比，数字在标签上 */
const typeOption = (rows: any[]) => ({
  tooltip: {
    trigger: 'item',
    formatter: (params: any) => {
      const row = rows[params.dataIndex] ?? {}
      return [`${row.typeText ?? row.type ?? ''}`, `会话数量：${row.sessions ?? 0}`, `对话轮次：${row.rounds ?? 0}`].join('<br/>')
    },
  },
  legend: { bottom: 0, itemWidth: 10, itemHeight: 10, textStyle: { fontSize: 11 } },
  series: [{
    type: 'pie',
    radius: ['38%', '64%'],
    center: ['50%', '44%'],
    label: { fontSize: 11, formatter: '{b}: {c}' },
    data: rows.map((row: any, index: number) => ({
      name: row.typeText || row.type || '未知',
      value: Number(row.sessions ?? 0),
      itemStyle: { color: COLORS[(index + 2) % COLORS.length] },
    })),
  }],
})

/**
 * 执行状态分布：按对话轮次切分（轮次才是真正的执行状态），会话数量在 tooltip 里一并给出；
 * 配色固定成功绿、失败红，与上面两张时间轴的堆叠色一致
 */
const statusOption = (rows: any[]) => ({
  tooltip: {
    trigger: 'item',
    formatter: (params: any) => {
      const row = rows[params.dataIndex] ?? {}
      return [
        `${row.statusText ?? ''}`,
        `对话轮次：${row.rounds ?? 0}`,
        `会话数量：${row.sessions ?? 0}`,
      ].join('<br/>')
    },
  },
  legend: { bottom: 0, itemWidth: 10, itemHeight: 10, textStyle: { fontSize: 11 } },
  series: [{
    type: 'pie',
    radius: ['38%', '64%'],
    center: ['50%', '44%'],
    label: { fontSize: 11, formatter: '{b}: {c}' },
    data: rows.map((row: any) => ({
      name: row.statusText || `状态#${row.status}`,
      value: Number(row.rounds ?? 0),
      itemStyle: { color: 1 === Number(row.status) ? COLORS[1] : COLORS[4] },
    })),
  }],
})

/** 排名：横向条从下往上画（量大的在最上面），桶尾标数 */
const rankOption = (rows: any[], label: (row: any) => string, name: string) => {
  const top = rows.slice(0, RANK_LIMIT)
  const list = [...top].reverse()
  return {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params: any[]) => {
        const row = list[params?.[0]?.dataIndex ?? 0]
        if (!row) return ''
        return [
          `${label(row)}`,
          `会话数量：${row.sessions ?? 0}`,
          `对话轮次：${row.rounds ?? 0}`,
          row.createdUid === undefined ? `参与用户：${row.users ?? 0}` : `覆盖流程：${row.flows ?? 0}`,
        ].join('<br/>')
      },
    },
    grid: { left: 8, right: 48, top: 8, bottom: 8, containLabel: true },
    xAxis: { type: 'value', minInterval: 1, axisLabel: { fontSize: 11 }, splitLine: { lineStyle: { type: 'dashed' } } },
    yAxis: { type: 'category', data: list.map(label), axisLabel: { fontSize: 11, width: 140, overflow: 'truncate' } },
    series: [{
      name,
      type: 'bar',
      barMaxWidth: 16,
      itemStyle: { color: COLORS[0], borderRadius: [0, 3, 3, 0] },
      label: { show: true, position: 'right', fontSize: 11, color: '#909399' },
      data: list.map((row: any) => Number(row.sessions ?? 0)),
    }],
  }
}

const renderAll = () => {
  const timeline = data.value?.timeline ?? []
  const extras = [{ key: 'users', name: '参与用户' }, { key: 'flows', name: '覆盖流程' }]
  renderChart('sessions', timelineOption(timeline, 'sessions', '会话数量', [
    { key: 'succeededSessions', name: '成功会话', color: COLORS[1] },
    { key: 'failedSessions', name: '失败会话', color: COLORS[4] },
  ], extras))
  renderChart('rounds', timelineOption(timeline, 'rounds', '对话轮次', [
    { key: 'succeeded', name: '成功轮次', color: COLORS[1] },
    { key: 'failed', name: '失败轮次', color: COLORS[4] },
  ], extras))
  renderChart('types', typeOption(data.value?.types ?? []))
  renderChart('statuses', statusOption(data.value?.statuses ?? []))
  renderChart('flows', rankOption(data.value?.flows ?? [], (row: any) => row.agenticName || `流程#${row.agenticId}`, '会话数量'))
  renderChart('users', rankOption(data.value?.users ?? [], (row: any) => row.createdUserInfo?.name || `用户#${row.createdUid}`, '会话数量'))
}

watch(data, () => nextTick(renderAll), { deep: true })

onMounted(() => {
  observer = new ResizeObserver((entries) => {
    entries.forEach((entry) => instances.get(entry.target as HTMLElement)?.resize())
  })
  load()
  nextTick(renderAll)
})

onBeforeUnmount(() => {
  observer?.disconnect()
  observer = null
  instances.forEach((chart) => chart.dispose())
  instances.clear()
})
</script>

<template>
  <div class="stat-page" v-loading="loading">
    <!-- 筛选：时间 / 流程 / 用户 / 类型 / 状态 / 聚合层级，默认近一周；沿用列表页统一的 form-search 栅格 -->
    <el-card class="stat-card fs-table-search" :bordered="false" shadow="never">
      <form-search :model="filters">
        <form-search-item label="时间" prop="range">
          <el-date-picker
            v-model="filters.range"
            type="datetimerange"
            :clearable="false"
            :format="DateUtil.moment()"
            value-format="YYYY-MM-DD HH:mm:ss"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间" />
        </form-search-item>
        <form-search-item label="流程" prop="agenticId">
          <form-select v-model="filters.agenticId" :callback="AgenticApi.list" clearable filterable placeholder="全部流程" />
        </form-search-item>
        <form-search-item label="用户" prop="uid">
          <form-select v-model="filters.uid" :callback="UserApi.list" clearable filterable placeholder="全部用户" />
        </form-search-item>
        <form-search-item>
          <el-button type="primary" @click="handleSearch" :loading="loading">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </form-search-item>
        <form-search-item label="对话类型" prop="type">
          <el-select v-model="filters.type" clearable placeholder="全部类型">
            <el-option value="published" label="发布应用" />
            <el-option value="draft" label="调试运行" />
          </el-select>
        </form-search-item>
        <form-search-item label="对话状态" prop="deleted">
          <el-select v-model="filters.deleted" clearable placeholder="全部状态">
            <el-option value="" label="全部" />
            <el-option value="without" label="未删除" />
            <el-option value="only" label="已删除" />
          </el-select>
        </form-search-item>
        <form-search-item label="执行状态" prop="status">
          <el-select v-model="filters.status" clearable placeholder="全部">
            <el-option value="" label="全部" />
            <el-option value="success" label="成功" />
            <el-option value="failed" label="失败" />
          </el-select>
        </form-search-item>
        <form-search-item label="聚合层级" prop="aggregation">
          <el-select v-model="filters.aggregation" clearable>
            <el-option value="hour" label="按小时" />
            <el-option value="day" label="按天" />
            <el-option value="week" label="按周" />
            <el-option value="month" label="按月" />
          </el-select>
        </form-search-item>
      </form-search>
    </el-card>

    <!-- 概览 -->
    <el-card class="stat-card" :bordered="false" shadow="never">
      <div class="overview">
        <div class="overview-item" v-for="item in overview" :key="item.label">
          <span class="overview-value">{{ item.value }}</span>
          <span class="overview-label">{{ item.label }}</span>
        </div>
        <span class="overview-tip">统计条件下的全部会话与对话轮次</span>
      </div>
    </el-card>

    <!-- 时间轴：会话数量与对话轮次各一张，横轴同一聚合层级 -->
    <el-card class="stat-card" :bordered="false" shadow="never">
      <div class="section-title">时间趋势与分布</div>
      <el-row :gutter="12">
        <el-col :xs="24" :md="16" v-if="hasTimeline">
          <div class="chart-title">会话数量 <span class="chart-tip">成功会话 / 失败会话</span></div>
          <div class="chart" :ref="bindSessions"></div>
        </el-col>
        <el-col :xs="24" :md="8" v-if="hasTypes">
          <div class="chart-title">对话类型分布</div>
          <div class="chart" :ref="bindTypes"></div>
        </el-col>
        <el-col :xs="24" :md="16" v-if="hasTimeline">
          <div class="chart-title">对话轮次 <span class="chart-tip">成功轮次 / 失败轮次</span></div>
          <div class="chart" :ref="bindRounds"></div>
        </el-col>
        <el-col :xs="24" :md="8" v-if="hasStatuses">
          <div class="chart-title">
            执行状态分布
            <span class="chart-tip">
              {{ statusText ? `已按「${statusText}」筛选，此图为筛选前的盘面` : '按对话轮次统计，不含状态筛选' }}
            </span>
          </div>
          <div class="chart" :ref="bindStatuses"></div>
        </el-col>
      </el-row>
      <el-empty description="该条件下没有会话记录" :image-size="70" v-if="!(data.timeline ?? []).length && !loading" />
    </el-card>

    <!-- 流程排名 -->
    <el-card class="stat-card" :bordered="false" shadow="never">
      <div class="section-title">流程排名</div>
      <el-row :gutter="12">
        <el-col :span="24" v-if="(data.flows ?? []).length">
          <div class="chart-title">
            会话数量
            <span class="chart-tip">最多展示前 {{ RANK_LIMIT }} 个流程，完整的量见下表</span>
          </div>
          <div class="chart is-tall" :ref="bindFlows"></div>
        </el-col>
      </el-row>
      <el-table class="section-table" :data="data.flows ?? []" size="small" border table-layout="auto">
        <el-table-column label="流程" min-width="200">
          <template #default="scope">{{ scope.row.agenticName || `流程#${scope.row.agenticId}` }}</template>
        </el-table-column>
        <el-table-column prop="sessions" label="会话数量" width="100" />
        <el-table-column prop="rounds" label="对话轮次" width="100" />
        <el-table-column prop="succeeded" label="成功轮次" width="100" />
        <el-table-column prop="failed" label="失败轮次" width="100" />
        <el-table-column prop="users" label="参与用户" width="100" />
        <el-table-column label="成功率" width="100">
          <template #default="scope">
            <span class="muted" v-if="!scope.row.rounds">—</span>
            <span v-else>{{ Math.round(scope.row.successRate ?? 0) }}%</span>
          </template>
        </el-table-column>
        <el-table-column label="平均耗时" width="110">
          <template #default="scope">{{ renderSeconds(scope.row.avgDuration) }}</template>
        </el-table-column>
        <el-table-column label="最近对话" width="170">
          <template #default="scope">{{ DateUtil.format(scope.row.lastTime) || '—' }}</template>
        </el-table-column>
      </el-table>
      <el-empty description="该条件下没有流程记录" :image-size="70" v-if="!(data.flows ?? []).length && !loading" />
    </el-card>

    <!-- 用户排名 -->
    <el-card class="stat-card" :bordered="false" shadow="never">
      <div class="section-title">用户排名</div>
      <el-row :gutter="12">
        <el-col :span="24" v-if="(data.users ?? []).length">
          <div class="chart-title">
            会话数量
            <span class="chart-tip">最多展示前 {{ RANK_LIMIT }} 位用户，完整的量见下表</span>
          </div>
          <div class="chart is-tall" :ref="bindUsers"></div>
        </el-col>
      </el-row>
      <el-table class="section-table" :data="data.users ?? []" size="small" border table-layout="auto">
        <el-table-column label="用户" min-width="200">
          <template #default="scope">{{ scope.row.createdUserInfo?.name || `用户#${scope.row.createdUid}` }}</template>
        </el-table-column>
        <el-table-column prop="sessions" label="会话数量" width="100" />
        <el-table-column prop="rounds" label="对话轮次" width="100" />
        <el-table-column prop="flows" label="覆盖流程" width="100" />
        <el-table-column prop="succeeded" label="成功轮次" width="100" />
        <el-table-column prop="failed" label="失败轮次" width="100" />
        <el-table-column label="成功率" width="100">
          <template #default="scope">
            <span class="muted" v-if="!scope.row.rounds">—</span>
            <span v-else>{{ Math.round(scope.row.successRate ?? 0) }}%</span>
          </template>
        </el-table-column>
        <el-table-column label="最近对话" width="170">
          <template #default="scope">{{ DateUtil.format(scope.row.lastTime) || '—' }}</template>
        </el-table-column>
      </el-table>
      <el-empty description="该条件下没有用户记录" :image-size="70" v-if="!(data.users ?? []).length && !loading" />
    </el-card>
  </div>
</template>

<style lang="scss" scoped>
.stat-page {
  .stat-card {
    & + .stat-card {
      margin-top: 12px;
    }
    :deep(.el-card__body) {
      padding: 14px 16px;
    }
  }
  /* 筛选卡用的是列表页通用的 fs-table-search：它自带 margin-bottom，
     这里归零，让相邻卡的 margin-top 统一管行距，免得两处叠加 */
  .stat-card.fs-table-search {
    margin-bottom: 0;
  }
  /* 下拉类控件占满栅格；日期区间由 FormSearchItem 自带的宽度撑满 */
  :deep(.el-form-item__content > .el-select) {
    width: 100%;
  }
  .el-row {
    row-gap: 12px;
  }
}
/* 图表标题：与表格的 section-title 区分开，字号更小、颜色更浅 */
.chart-title {
  @include flex-start();
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 4px;
  font-size: 13px;
  color: var(--el-text-color-regular);
  .chart-tip {
    font-size: 12px;
    color: var(--el-text-color-placeholder);
  }
}
.chart {
  width: 100%;
  height: 240px;
  &.is-tall {
    height: 320px;
  }
}
/* 概览：数字一排，末尾提示靠右 */
.overview {
  @include flex-start();
  flex-wrap: wrap;
  gap: 16px 40px;
  .overview-item {
    @include flex-start-column();
    flex: none;
    gap: 2px;
    .overview-value {
      font-size: 22px;
      font-weight: 600;
      line-height: 1.2;
      color: var(--el-text-color-primary);
      font-variant-numeric: tabular-nums;
    }
    .overview-label {
      font-size: 12px;
      color: var(--el-text-color-placeholder);
    }
  }
  .overview-tip {
    margin-left: auto;
    font-size: 12px;
    color: var(--el-text-color-placeholder);
  }
}
.section-title {
  margin-bottom: 10px;
  font-size: 14px;
  font-weight: 500;
  color: var(--el-text-color-primary);
}
/* 表格单独一行，与上方图表留开 */
.section-table {
  margin-top: 12px;
}
.muted {
  font-size: 12px;
  color: var(--el-text-color-placeholder);
}
</style>
