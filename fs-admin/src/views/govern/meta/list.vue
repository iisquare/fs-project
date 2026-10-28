<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { FormInstance } from 'element-plus'
import { ArrowRight, Connection, FolderOpened, Grid, List } from '@element-plus/icons-vue'
import MetaApi from '@/api/govern/MetaApi'
import ApiUtil from '@/utils/ApiUtil'
import RouteUtil from '@/utils/RouteUtil'
import GovernPageShell from '@/views/govern/components/GovernPageShell.vue'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const rows = ref<any[]>([])
const filterRef = ref<FormInstance>()
const filters = ref<any>({
  page: 1,
  pageSize: 15,
  keyword: String(route.query.keyword || ''),
  prefix: String(route.query.prefix || ''),
  mold: String(route.query.mold || ''),
})
const pagination = ref<any>(RouteUtil.pagination(filters.value))

const stripHighlight = (value: any) => {
  let result = String(value ?? '')
  if (Array.isArray(value)) result = value.join('')
  return result.replace(/<[^>]+>/g, '').replace(/&lt;/g, '<').replace(/&gt;/g, '>').replace(/&amp;/g, '&')
}

const highlightParts = (value: any) => {
  const text = stripHighlight(value)
  const keyword = String(filters.value.keyword || '').trim()
  if (!keyword) return [{ text, matched: false }]
  const escaped = keyword.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  const expression = new RegExp(`(${escaped})`, 'ig')
  return text.split(expression).filter(Boolean).map(part => ({
    text: part,
    matched: part.toLowerCase() === keyword.toLowerCase(),
  }))
}

const refresh = (keepPage = true) => {
  Object.assign(filters.value, RouteUtil.pagination2filter(pagination.value, keepPage))
  const query = { ...filters.value }
  delete query.page
  delete query.pageSize
  router.replace({ path: route.path, query }).catch(() => {})
  loading.value = true
  MetaApi.search(Object.assign({ highlight: 'code,name,description' }, filters.value)).then((result: any) => {
    RouteUtil.result2pagination(pagination.value, result)
    rows.value = ApiUtil.data(result)?.rows ?? []
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}

const modelOf = (item: any) => {
  const paths = String(item.id || '').split('/')
  if (item.mold === 'catalog') {
    paths.pop()
    return { catalog: paths.join('/') + '/', code: paths.pop(), column: '' }
  }
  const column = item.mold === 'column' ? paths.pop() : ''
  const code = paths.pop()
  return { catalog: paths.join('/') + '/', code, column }
}

const navigate = (kind: string, item: any) => {
  const model = modelOf(item)
  if (kind === 'influence') {
    return router.push({
      path: '/govern/meta/influence',
      query: { catalog: model.catalog, model: model.code, code: model.column },
    }).catch(() => {})
  }
  if (kind === 'blood') {
    return router.push({
      path: '/govern/meta/blood',
      query: { catalog: model.catalog, code: model.code, column: model.column },
    }).catch(() => {})
  }
  router.push({
    path: '/govern/meta/detail',
    query: { catalog: model.catalog, code: model.code, column: model.column },
  }).catch(() => {})
}

const moldText = (mold: string) => ({
  catalog: '目录',
  table: '模型',
  column: '字段',
}[mold] || mold)

const moldClass = (mold: string) => ({
  catalog: 'is-catalog',
  table: 'is-table',
  column: 'is-column',
}[mold] || '')

const moldIcon = (mold: string) => ({
  catalog: FolderOpened,
  table: Grid,
  column: List,
}[mold] || Grid)

const codeLabel = (mold: string) => ({
  catalog: '目录编码',
  table: '模型编码',
  column: '字段编码',
}[mold] || '编码')

const primaryValue = (row: any) => {
  const name = stripHighlight(row.highlight?.name ?? row.name)
  return name ? (row.highlight?.name ?? row.name) : (row.highlight?.code ?? row.code)
}

const changeMold = (value: any) => {
  filters.value.mold = value
  refresh(false)
}

onMounted(() => refresh(true))
</script>

<template>
  <GovernPageShell title="元数据检索" description="按模型、字段和包路径检索已采集的元数据，并继续查看详情或分析关系">
    <el-card shadow="never" class="search-card">
      <form-search ref="filterRef" :model="filters">
        <form-search-item label="关键词" prop="keyword">
          <el-input v-model="filters.keyword" clearable placeholder="编码、名称或描述" @keyup.enter="refresh(false)" />
        </form-search-item>
        <form-search-item label="包前缀" prop="prefix">
          <el-input v-model="filters.prefix" clearable placeholder="例如 /domain/" @keyup.enter="refresh(false)" />
        </form-search-item>
        <form-search-item label="类型" prop="mold">
          <el-select v-model="filters.mold" clearable placeholder="全部类型">
            <el-option label="目录" value="catalog" />
            <el-option label="模型" value="table" />
            <el-option label="字段" value="column" />
          </el-select>
        </form-search-item>
        <form-search-item>
          <el-button type="primary" :loading="loading" @click="refresh(false)">检索</el-button>
          <el-button @click="filterRef?.resetFields()">重置</el-button>
        </form-search-item>
      </form-search>
    </el-card>

    <el-card shadow="never" class="result-panel">
      <div class="result-panel__header">
        <div class="result-panel__summary">
          <div>
            <strong>检索结果</strong>
            <span>{{ pagination.total }} 条匹配记录</span>
          </div>
          <el-radio-group :model-value="filters.mold" size="small" @change="changeMold">
            <el-radio-button value="">全部</el-radio-button>
            <el-radio-button value="catalog">目录</el-radio-button>
            <el-radio-button value="table">模型</el-radio-button>
            <el-radio-button value="column">字段</el-radio-button>
          </el-radio-group>
        </div>
        <button-refresh :loading="loading" @click="refresh(true)" />
      </div>

      <div v-loading="loading" class="meta-result-list">
        <el-empty v-if="!loading && rows.length === 0" description="没有找到匹配的元数据" :image-size="86" />
        <article
          v-for="row in rows"
          :key="row.id"
          :class="['meta-result', moldClass(row.mold)]"
          @click="navigate('detail', row)"
        >
          <div class="meta-result__identity">
            <div class="meta-result__icon">
              <component :is="moldIcon(row.mold)" />
            </div>
            <div class="meta-result__type">
              <strong>{{ moldText(row.mold) }}</strong>
            </div>
          </div>
          <div class="meta-result__content">
            <div class="meta-result__title">
              <span class="meta-result__name">
                <template v-for="(part, index) in highlightParts(primaryValue(row))" :key="index">
                  <mark v-if="part.matched">{{ part.text }}</mark><template v-else>{{ part.text }}</template>
                </template>
              </span>
              <span class="meta-result__code">
                （{{ codeLabel(row.mold) }}：
                <code>
                  <template v-for="(part, index) in highlightParts(row.highlight?.code ?? row.code)" :key="index">
                    <mark v-if="part.matched">{{ part.text }}</mark><template v-else>{{ part.text }}</template>
                  </template>
                </code>）
              </span>
            </div>
            <p class="meta-result__description">
              <template v-for="(part, index) in highlightParts(row.highlight?.description ?? row.description)" :key="index">
                <mark v-if="part.matched">{{ part.text }}</mark><template v-else>{{ part.text }}</template>
              </template>
              <template v-if="!stripHighlight(row.highlight?.description ?? row.description)">暂无描述</template>
            </p>
            <div class="meta-result__facts">
              <div class="meta-result__path">
                <el-icon><Connection /></el-icon>
                <code>{{ row.id }}</code>
              </div>
            </div>
          </div>
          <div class="meta-result__actions" @click.stop>
            <el-button v-if="row.mold === 'column'" text @click="navigate('influence', row)">影响</el-button>
            <el-button text @click="navigate('blood', row)">血缘</el-button>
            <el-tooltip content="查看详情">
              <el-button circle :icon="ArrowRight" @click="navigate('detail', row)" />
            </el-tooltip>
          </div>
        </article>
      </div>

      <TablePagination v-model="pagination" :loading="loading" @change="refresh(true)" />
    </el-card>
  </GovernPageShell>
</template>

<style scoped>
.result-panel :deep(.el-card__body) {
  padding: 0 18px;
}
.result-panel__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  min-height: 58px;
}
.result-panel__summary {
  display: flex;
  align-items: center;
  gap: 22px;
}
.result-panel__summary > div {
  display: flex;
  align-items: baseline;
  gap: 10px;
}
.result-panel__header strong {
  color: var(--el-text-color-primary);
  font-size: 15px;
}
.result-panel__header span {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
.meta-result-list {
  min-height: 220px;
  border-top: 1px solid var(--el-border-color-lighter);
}
.meta-result {
  position: relative;
  display: grid;
  grid-template-columns: 88px minmax(0, 1fr) 180px;
  align-items: center;
  gap: 20px;
  min-height: 88px;
  min-width: 0;
  padding: 14px 8px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  cursor: pointer;
  transition: background-color 0.2s;
}
.meta-result:last-child {
  border-bottom: none;
}
.meta-result:hover {
  background: var(--el-fill-color-ultra-light);
}
.meta-result__identity {
  display: flex;
  flex: none;
  align-items: center;
  gap: 11px;
  width: 88px;
}
.meta-result__icon {
  display: flex;
  flex: none;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  border-radius: 9px;
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
  font-size: 19px;
}
.meta-result.is-catalog .meta-result__icon {
  background: var(--el-color-warning-light-9);
  color: var(--el-color-warning);
}
.meta-result.is-column .meta-result__icon {
  background: var(--el-color-success-light-9);
  color: var(--el-color-success);
}
.meta-result__type {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.meta-result__type strong {
  color: var(--el-text-color-primary);
  font-size: 13px;
}
.meta-result__content {
  flex: 1;
  min-width: 0;
}
.meta-result__title {
  display: flex;
  align-items: center;
  gap: 4px;
  min-width: 0;
}
.meta-result__name {
  overflow: hidden;
  color: var(--el-text-color-primary);
  font-size: 15px;
  font-weight: 650;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.meta-result__code {
  display: inline-flex;
  align-items: center;
  flex: none;
  max-width: 42%;
  overflow: hidden;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.meta-result__code code {
  overflow: hidden;
  color: var(--el-text-color-regular);
  font-family: var(--el-font-family);
  text-overflow: ellipsis;
}
.meta-result__description {
  display: -webkit-box;
  min-height: 20px;
  margin-top: 3px;
  overflow: hidden;
  color: var(--el-text-color-secondary);
  font-size: 13px;
  line-height: 20px;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}
.meta-result__facts {
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
  margin-top: 5px;
}
.meta-result__path {
  display: flex;
  align-items: center;
  gap: 5px;
  min-width: 0;
  font-size: 12px;
}
.meta-result__path .el-icon {
  flex: none;
  color: var(--el-text-color-placeholder);
}
.meta-result__path code {
  overflow: hidden;
  color: var(--el-text-color-placeholder);
  text-overflow: ellipsis;
  white-space: nowrap;
}
.meta-result mark {
  padding: 0 2px;
  border-radius: 2px;
  background: #fff1b8;
  color: #b45309;
}
.meta-result__actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 4px;
  min-height: 38px;
  padding-left: 16px;
  border-left: 1px solid var(--el-border-color-lighter);
}
.meta-result__actions :deep(.el-button + .el-button) {
  margin-left: 0;
}
.result-panel :deep(.el-pagination) {
  padding: 12px 0 14px;
  justify-content: flex-end;
}
@media (max-width: 960px) {
  .result-panel__summary {
    align-items: flex-start;
    flex-direction: column;
    gap: 8px;
  }
  .result-panel__header {
    align-items: flex-start;
    padding: 14px 0;
  }
  .meta-result {
    grid-template-columns: 88px minmax(0, 1fr);
    align-items: center;
  }
  .meta-result__actions {
    grid-column: 2;
    justify-self: end;
    justify-content: flex-end;
  }
}
@media (max-width: 640px) {
  .meta-result__title {
    align-items: flex-start;
    flex-wrap: wrap;
  }
  .meta-result__identity {
    grid-column: 1 / -1;
  }
  .meta-result__content {
    grid-column: 1 / -1;
  }
  .meta-result__actions {
    grid-column: 1 / -1;
  }
}
</style>
