<script setup lang="ts">
/**
 * 客户端更新统计：参考流程统计页的筛选、概览和明细结构。
 * 默认近 7 天，汇总与明细使用同一组筛选条件。
 */
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ClientVersionApi from '@/api/agent/ClientVersionApi'
import ApiUtil from '@/utils/ApiUtil'
import DateUtil from '@/utils/DateUtil'
import RouteUtil from '@/utils/RouteUtil'

const DAY = 24 * 60 * 60 * 1000
const route = useRoute()
const router = useRouter()
const loading = ref(false)
const data = ref<any>({ rows: [], summary: {} })
const channels = ref<Record<string, string>>({})

const defaultBegin = () => DateUtil.format(Date.now() - 6 * DAY, 'yyyy-MM-dd 00:00:00')

const filters = ref<any>(RouteUtil.query2filter(route, {
  createdTimeBegin: defaultBegin(),
  createdTimeEnd: '',
  targetVersion: '',
  channel: '',
  event: ''
}, false))
const pagination = ref(RouteUtil.pagination({ page: 1, pageSize: 20 }))

const events = [
  { value: 'download_started', label: '开始下载' },
  { value: 'update_failed', label: '更新失败' },
  { value: 'install_failed', label: '安装失败' },
  { value: 'install_success', label: '安装成功' }
]
const EVENT_TYPES: Record<string, 'primary' | 'warning' | 'danger' | 'success' | 'info'> = {
  download_started: 'primary',
  update_failed: 'warning',
  install_failed: 'danger',
  install_success: 'success'
}
const eventText = (value: string) =>
  events.find((item) => item.value === value)?.label || value || '-'
const eventType = (value: string) => EVENT_TYPES[value] || 'info'
const platformText = (value: string) => ({
  win32: 'Windows',
  darwin: 'macOS',
  linux: 'Linux'
})[value] || value || '-'
const channelText = (value: string) => channels.value[value] || value || '-'

const buildParam = () => {
  return {
    createdTimeBegin: filters.value.createdTimeBegin || '',
    createdTimeEnd: filters.value.createdTimeEnd || '',
    targetVersion: filters.value.targetVersion || '',
    channel: filters.value.channel || '',
    event: filters.value.event || '',
    page: pagination.value.currentPage,
    pageSize: pagination.value.pageSize
  }
}

const load = (updateRoute = false) => {
  if (updateRoute) RouteUtil.filter2query(route, router, filters.value)
  loading.value = true
  ClientVersionApi.stats(buildParam()).then((result: any) => {
    RouteUtil.result2pagination(pagination.value, result)
    data.value = ApiUtil.data(result) ?? { rows: [], summary: {} }
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}

const handleSearch = () => {
  pagination.value.currentPage = 1
  load(true)
}

const handleReset = () => {
  filters.value = {
    createdTimeBegin: defaultBegin(),
    createdTimeEnd: '',
    targetVersion: '',
    channel: '',
    event: ''
  }
  handleSearch()
}

const summary = computed<any>(() => data.value.summary ?? {})
const overview = computed(() => [
  { label: '开始下载', value: Number(summary.value.downloadStarted ?? 0), tone: 'primary' },
  { label: '更新失败', value: Number(summary.value.updateFailed ?? 0), tone: 'warning' },
  { label: '安装失败', value: Number(summary.value.installFailed ?? 0), tone: 'danger' },
  { label: '安装成功', value: Number(summary.value.installSuccess ?? 0), tone: 'success' },
  {
    label: '安装成功率',
    value: `${Number(summary.value.installSuccessRate ?? 0)}%`,
    tone: 'success'
  }
])
const rangeText = computed(() => {
  const begin = filters.value.createdTimeBegin || '最早'
  const end = filters.value.createdTimeEnd || '当前'
  return `${begin} 至 ${end}`
})

onMounted(() => {
  ClientVersionApi.config().then((result: any) => {
    channels.value = ApiUtil.data(result)?.channels ?? {}
  }).catch(() => {})
  load()
})
</script>

<template>
  <div class="stat-page">
    <div class="page-heading">
      <div>
        <h2 class="page-title">客户端更新统计</h2>
        <p class="page-desc">
          查看下载、安装成功率和客户端升级明细；汇总与明细均使用当前筛选条件。
        </p>
      </div>
      <el-button @click="router.push('/agent/bot/clientVersion')">返回版本管理</el-button>
    </div>

    <el-card class="filter-card" :bordered="false" shadow="never">
      <div class="card-heading">
        <span class="card-title">筛选条件</span>
        <span class="card-desc">默认展示最近 7 天</span>
      </div>
      <form-search :model="filters">
        <form-search-item label="目标版本" prop="targetVersion">
          <el-input v-model="filters.targetVersion" clearable placeholder="全部版本" />
        </form-search-item>
        <form-search-item label="开始时间" prop="createdTimeBegin">
          <form-date-picker
            v-model="filters.createdTimeBegin"
            placeholder="开始时间"
          />
        </form-search-item>
        <form-search-item label="结束时间" prop="createdTimeEnd">
          <form-date-picker
            v-model="filters.createdTimeEnd"
            placeholder="留空表示截至当前"
          />
        </form-search-item>
        <form-search-item>
          <el-button type="primary" :loading="loading" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </form-search-item>
        <form-search-item label="渠道" prop="channel">
          <el-select v-model="filters.channel" clearable placeholder="全部渠道">
            <el-option
              v-for="(value, key) in channels"
              :key="key"
              :value="key"
              :label="value"
            />
          </el-select>
        </form-search-item>
        <form-search-item label="事件" prop="event">
          <el-select v-model="filters.event" clearable placeholder="全部事件">
            <el-option
              v-for="item in events"
              :key="item.value"
              :value="item.value"
              :label="item.label"
            />
          </el-select>
        </form-search-item>
      </form-search>
    </el-card>

    <div class="overview-grid">
      <el-card
        v-for="item in overview"
        :key="item.label"
        class="overview-card"
        :class="`overview-card--${item.tone}`"
        :bordered="false"
        shadow="never"
      >
        <span class="overview-accent"></span>
        <div class="overview-copy">
          <span class="overview-label">{{ item.label }}</span>
          <strong class="overview-value">{{ item.value }}</strong>
        </div>
      </el-card>
    </div>

    <el-card class="records-card" :bordered="false" shadow="never">
      <div class="records-heading">
        <div>
          <span class="card-title">更新明细</span>
          <span class="card-desc">当前条件下共 {{ pagination.total }} 条记录</span>
        </div>
        <span class="range-text">{{ rangeText }}</span>
      </div>
      <el-table
        v-loading="loading"
        :data="data.rows"
        row-key="id"
        stripe
        table-layout="fixed"
        empty-text="当前条件下暂无更新记录"
      >
        <el-table-column prop="clientId" label="客户端标识" min-width="190" show-overflow-tooltip />
        <el-table-column label="系统" width="100">
          <template #default="scope">{{ platformText(scope.row.platform) }}</template>
        </el-table-column>
        <el-table-column prop="architecture" label="架构" width="90" />
        <el-table-column label="渠道" width="90">
          <template #default="scope">{{ channelText(scope.row.channel) }}</template>
        </el-table-column>
        <el-table-column prop="fromVersion" label="当前版本" width="110" />
        <el-table-column prop="targetVersion" label="目标版本" width="110" />
        <el-table-column label="事件" width="120">
          <template #default="scope">
            <el-tag :type="eventType(scope.row.event)" effect="light" size="small">
              {{ eventText(scope.row.event) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="message"
          label="说明"
          min-width="260"
          show-overflow-tooltip
        />
        <el-table-column label="时间" width="170">
          <template #default="scope">{{ DateUtil.format(scope.row.createdTime) }}</template>
        </el-table-column>
      </el-table>
      <div class="records-footer">
        <TablePagination
          v-model="pagination"
          :loading="loading"
          @change="load(false)"
        />
      </div>
    </el-card>
  </div>
</template>

<style lang="scss" scoped>
.stat-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.page-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  padding: 2px 2px 4px;
}

.page-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  line-height: 1.4;
  color: var(--el-text-color-primary);
}

.page-desc {
  margin: 6px 0 0;
  font-size: 13px;
  line-height: 1.6;
  color: var(--el-text-color-secondary);
}

.filter-card,
.records-card,
.overview-card {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 10px;
  background: var(--el-bg-color);
  box-shadow: 0 1px 2px rgb(0 0 0 / 3%);
}

.filter-card {
  :deep(.el-card__body) {
    padding: 18px 20px 12px;
  }
}

.card-heading,
.records-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;
}

.records-heading {
  margin-bottom: 16px;
}

.card-title {
  display: block;
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.card-desc {
  display: block;
  margin-top: 4px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(150px, 1fr));
  gap: 12px;
}

.overview-card {
  position: relative;
  overflow: hidden;

  :deep(.el-card__body) {
    display: flex;
    align-items: center;
    min-height: 82px;
    padding: 16px 18px 16px 24px;
  }
}

.overview-accent {
  position: absolute;
  top: 16px;
  bottom: 16px;
  left: 0;
  width: 4px;
  border-radius: 0 3px 3px 0;
  background: var(--el-color-primary);
}

.overview-copy {
  display: flex;
  flex-direction: column;
  gap: 7px;
}

.overview-value {
  font-size: 26px;
  font-variant-numeric: tabular-nums;
  font-weight: 650;
  line-height: 1;
  color: var(--el-text-color-primary);
}

.overview-label {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.overview-card--primary .overview-accent {
  background: var(--el-color-primary);
}

.overview-card--warning .overview-accent {
  background: var(--el-color-warning);
}

.overview-card--danger .overview-accent {
  background: var(--el-color-danger);
}

.overview-card--danger .overview-value {
  color: var(--el-color-danger);
}

.overview-card--success .overview-accent {
  background: var(--el-color-success);
}

.overview-card--success .overview-value {
  color: var(--el-color-success);
}

.records-card {
  :deep(.el-card__body) {
    padding: 18px 20px;
  }

  :deep(.el-table) {
    --el-table-header-bg-color: var(--el-fill-color-lighter);
    --el-table-border-color: var(--el-border-color-lighter);
  }

  :deep(.el-table th.el-table__cell) {
    font-weight: 600;
    color: var(--el-text-color-regular);
  }
}

.range-text {
  padding: 5px 10px;
  border-radius: 999px;
  background: var(--el-fill-color-light);
  color: var(--el-text-color-secondary);
  font-size: 12px;
  white-space: nowrap;
}

.records-footer {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

@media (max-width: 1200px) {
  .overview-grid {
    grid-template-columns: repeat(3, minmax(140px, 1fr));
  }
}

@media (max-width: 900px) {
  .overview-grid {
    grid-template-columns: repeat(2, minmax(100px, 1fr));
  }

  .page-heading,
  .records-heading {
    align-items: flex-start;
    flex-direction: column;
  }

  .range-text {
    white-space: normal;
  }
}
</style>
