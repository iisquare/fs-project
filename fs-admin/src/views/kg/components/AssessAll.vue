<script setup lang="ts">
/**
 * 知识评估 - 按本体定义检查图数据质量
 *
 * 检查项全部来自本体定义：主键与必填字段缺失、主键重复、标题字段为空、
 * 标签一致性、属性类型一致性、未声明的属性、孤立节点、关系端点标签一致性。
 * 计数类检查为精确统计，类型与冗余属性为抽样检查（结果中标注）。
 */
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { useRoute } from 'vue-router'
import ApiUtil from '@/utils/ApiUtil'
import DateUtil from '@/utils/DateUtil'
import AssessApi from '@/api/kg/AssessApi'
import OntologyApi from '@/api/kg/OntologyApi'
import LayoutHeading from '@/components/Layout/LayoutHeading.vue'

const router = useRouter()
const route = useRoute()
const props = defineProps<{ tab?: string }>()
const activeTab = ref(props.tab ?? 'run')
const tabOfPath = (path: string) => path.includes('/history') ? 'history' : 'run'
const pageMeta = computed(() => activeTab.value === 'history'
  ? { title: '评估历史', description: '查看历次评估的得分、数据量与问题数，可回看当次明细' }
  : { title: '执行评估', description: '依据本体定义检查图数据的完整性、唯一性、一致性与规范性' })

const ontologies = ref<any[]>([])
const ontologyId = ref<any>(null)
const sample = ref(2000)
const running = ref(false)
const result = ref<any>(null)
const historyRows = ref<any[]>([])
const historyLoading = ref(false)
const expanded = ref<string[]>([])

const entityScopes = computed(() => (result.value?.scopes ?? []).filter((item: any) => item.kind === 'ENTITY'))
const relationshipScopes = computed(() => (result.value?.scopes ?? []).filter((item: any) => item.kind === 'RELATIONSHIP'))

const loadOntologies = () => {
  OntologyApi.list({ page: 1, pageSize: 200 }).then((response: any) => {
    ontologies.value = ApiUtil.data(response)?.rows ?? []
    if (!ontologyId.value && ontologies.value.length > 0) ontologyId.value = ontologies.value[0].id
    // 本体默认值就绪后再加载历史，进入菜单即可看到数据
    if (activeTab.value === 'history') loadHistory()
  }).catch(() => {})
}

const handleOntologyChange = () => {
  if (activeTab.value === 'history') loadHistory()
}

const run = () => {
  if (!ontologyId.value) return ElMessage.warning('请选择本体')
  running.value = true
  AssessApi.run({ ontologyId: ontologyId.value, sample: sample.value }, { warning: false }).then((response: any) => {
    result.value = ApiUtil.data(response)
    expanded.value = entityScopes.value.slice(0, 1).map((item: any) => item.label)
    ElMessage.success(`评估完成，总分 ${result.value?.score ?? 0}`)
  }).catch((error: any) => {
    ElMessage.warning(ApiUtil.message(error))
  }).finally(() => {
    running.value = false
  })
}

const loadHistory = () => {
  if (!ontologyId.value) return
  historyLoading.value = true
  AssessApi.history({ ontologyId: ontologyId.value }, { warning: false }).then((response: any) => {
    historyRows.value = ApiUtil.data(response) ?? []
  }).catch(() => {}).finally(() => {
    historyLoading.value = false
  })
}

const loadDetail = (row: any) => {
  router.push({ path: '/kg/assess/run', query: { recordId: String(row.id) } })
}

const scoreColor = (score: number) => score >= 90 ? '#67c23a' : (score >= 70 ? '#e6a23c' : '#f56c6c')
const rowClass = (row: any) => row.issues > 0 ? 'assess-issue-row' : ''

const goData = (scope: any, sampleItem: any) => {
  if (scope?.kind !== 'ENTITY') return
  const properties = sampleItem?.n?.properties ?? {}
  const primaryValue = scope.primaryField ? properties[scope.primaryField] : undefined
  const values = Object.values(properties)
    .filter((value: any) => value !== null && value !== undefined && typeof value !== 'object' && String(value) !== '')
  const candidates = [primaryValue, sampleItem?.key, values.length ? values[0] : undefined, '']
  const id = candidates.find((value: any) => value !== undefined && value !== null && String(value) !== '') ?? ''
  router.push({ path: '/kg/extraction/data', query: { ontologyId: String(ontologyId.value), entity: scope.label, id: String(id ?? '') } })
}

/**
 * 问题样例的标识
 *
 * 样例有三种形态：节点/关系样例 {n}/{r}、抽样样例 {elementId,key}、重复值样例 {value}。
 * elementId 仅用于数据排查，统一作为标签展示，没有元素时退回重复值或主键值。
 */
const sampleText = (item: any) => {
  if (!item) return ''
  const cell = item.n ?? item.r ?? item.a ?? item.b
  const value = [cell?.elementId, item.elementId, item.value, item.key]
    .find((current: any) => current !== undefined && current !== null && String(current) !== '')
  return undefined === value ? '' : String(value)
}

/**
 * 问题样例的说明：后端给出的提示、主键值或节点属性
 */
const sampleDetail = (item: any) => {
  const parts: string[] = []
  if (item?.message) parts.push(item.message)
  if (item?.key !== undefined && item?.key !== null && String(item.key) !== '') parts.push(`主键：${item.key}`)
  const cell = item?.n ?? item?.r ?? item?.a ?? item?.b
  if (!parts.length && cell?.properties) parts.push(JSON.stringify(cell.properties))
  return parts.join('；')
}

onMounted(() => {
  activeTab.value = props.tab ?? tabOfPath(route.path)
  loadOntologies()
  if (activeTab.value !== 'history') {
    const recordId = Number(route.query.recordId)
    if (Number.isFinite(recordId) && recordId > 0) {
      AssessApi.detail({ id: recordId }, { warning: false }).then((response: any) => {
        result.value = ApiUtil.data(response)
      }).catch(() => {})
    }
  }
})
</script>

<template>
  <LayoutHeading :title="pageMeta.title" :description="pageMeta.description">
    <template #extra>
      <el-space wrap>
        <span class="assess-label">本体</span>
        <el-select v-model="ontologyId" filterable placeholder="请选择本体" style="width: 220px" @change="handleOntologyChange">
          <el-option v-for="item in ontologies" :key="item.id" :value="item.id" :label="item.name" />
        </el-select>
        <template v-if="activeTab === 'run'">
          <span class="assess-label">抽样条数</span>
          <el-input-number v-model="sample" :min="100" :max="20000" :controls="false" style="width: 110px" />
          <el-button v-permit="'kg:assess:run'" type="primary" :loading="running" @click="run">执行评估</el-button>
        </template>
        <el-button v-if="activeTab === 'history'" v-permit="'kg:assess:history'" :loading="historyLoading" @click="loadHistory">刷新</el-button>
      </el-space>
    </template>
  </LayoutHeading>

  <el-card v-if="activeTab === 'run' && result" :bordered="false" shadow="never" class="fs-table-card">
    <div class="assess-summary">
      <div class="assess-summary__score">
        <div class="assess-summary__value" :style="{ color: scoreColor(result.score) }">{{ result.score }}</div>
        <div class="assess-summary__tip">综合得分</div>
      </div>
      <div class="assess-summary__item">
        <div class="assess-summary__label">实体数据</div>
        <div class="assess-summary__number">{{ result.nodeCount }}</div>
      </div>
      <div class="assess-summary__item">
        <div class="assess-summary__label">关系数据</div>
        <div class="assess-summary__number">{{ result.relationshipCount }}</div>
      </div>
      <div class="assess-summary__item">
        <div class="assess-summary__label">问题总数</div>
        <div class="assess-summary__number" :style="{ color: result.issueCount > 0 ? '#f56c6c' : '#67c23a' }">{{ result.issueCount }}</div>
      </div>
      <div class="assess-summary__item">
        <div class="assess-summary__label">抽样条数</div>
        <div class="assess-summary__number">{{ result.sample }}</div>
      </div>
    </div>
    <el-alert v-if="result.issues?.length" type="warning" :closable="false" title="本体定义存在问题，评估结果可能受影响" class="mt-10">
      <div v-for="(item, index) in result.issues" :key="index">{{ item }}</div>
    </el-alert>
  </el-card>

  <el-card v-if="activeTab === 'run' && result" :bordered="false" shadow="never" class="fs-table-card">
    <el-tabs>
      <el-tab-pane :label="`实体（${entityScopes.length}）`">
        <el-collapse v-model="expanded">
          <el-collapse-item v-for="scope in entityScopes" :key="scope.label" :name="scope.label">
            <template #title>
              <span class="assess-scope">{{ scope.name }}（{{ scope.label }}）</span>
              <el-tag size="small" effect="plain" style="margin-left: 8px">{{ scope.count }} 条</el-tag>
              <el-tag size="small" :type="scope.issueCount > 0 ? 'warning' : 'success'" effect="plain" style="margin-left: 6px">
                问题 {{ scope.issueCount }}
              </el-tag>
              <el-tag size="small" :color="scoreColor(scope.score)" effect="dark" style="margin-left: 6px">得分 {{ scope.score }}</el-tag>
            </template>
            <el-table :data="scope.items" size="small" border :row-class-name="rowClass">
              <el-table-column type="expand" width="45px">
                <template #default="scope2">
                  <div v-if="!(scope2.row.samples ?? []).length" class="assess-empty">无问题样例</div>
                  <div v-for="(item, index) in (scope2.row.samples ?? [])" :key="index" class="assess-sample">
                    <el-tag v-if="sampleText(item)" size="small" type="warning" effect="plain">{{ sampleText(item) }}</el-tag>
                    <span v-if="sampleDetail(item)" class="assess-sample__detail">{{ sampleDetail(item) }}</span>
                    <el-button v-if="scope.kind === 'ENTITY' && (item.n || item.key || item.elementId)" link type="primary" @click="goData(scope, item)">去数据管理</el-button>
                  </div>
                </template>
              </el-table-column>
              <el-table-column prop="message" label="检查项" min-width="240px" />
              <el-table-column label="范围" width="110px">
                <template #default="scope2">
                  <el-tag size="small" type="info" effect="plain">{{ scope2.row.sampled ? '抽样' : '全量' }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="total" label="检查总量" width="110px" />
              <el-table-column prop="issues" label="问题数" width="100px" />
              <el-table-column label="得分" width="100px">
                <template #default="scope2">{{ scope2.row.score }}</template>
              </el-table-column>
            </el-table>
          </el-collapse-item>
        </el-collapse>
      </el-tab-pane>
      <el-tab-pane :label="`关系（${relationshipScopes.length}）`">
        <el-collapse>
          <el-collapse-item v-for="scope in relationshipScopes" :key="scope.label" :name="scope.label">
            <template #title>
              <span class="assess-scope">{{ scope.name }}（{{ scope.label }}）</span>
              <el-tag size="small" effect="plain" style="margin-left: 8px">{{ scope.count }} 条</el-tag>
              <el-tag size="small" :type="scope.issueCount > 0 ? 'warning' : 'success'" effect="plain" style="margin-left: 6px">
                问题 {{ scope.issueCount }}
              </el-tag>
              <el-tag size="small" :color="scoreColor(scope.score)" effect="dark" style="margin-left: 6px">得分 {{ scope.score }}</el-tag>
            </template>
            <el-table :data="scope.items" size="small" border :row-class-name="rowClass">
              <el-table-column type="expand" width="45px">
                <template #default="scope2">
                  <div v-if="!(scope2.row.samples ?? []).length" class="assess-empty">无问题样例</div>
                  <div v-for="(item, index) in (scope2.row.samples ?? [])" :key="index" class="assess-sample">
                    <el-tag v-if="sampleText(item)" size="small" type="warning" effect="plain">{{ sampleText(item) }}</el-tag>
                    <span v-if="sampleDetail(item)" class="assess-sample__detail">{{ sampleDetail(item) }}</span>
                  </div>
                </template>
              </el-table-column>
              <el-table-column prop="message" label="检查项" min-width="240px" />
              <el-table-column label="范围" width="110px">
                <template #default="scope2">
                  <el-tag size="small" type="info" effect="plain">{{ scope2.row.sampled ? '抽样' : '全量' }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="total" label="检查总量" width="110px" />
              <el-table-column prop="issues" label="问题数" width="100px" />
              <el-table-column label="得分" width="100px">
                <template #default="scope2">{{ scope2.row.score }}</template>
              </el-table-column>
            </el-table>
          </el-collapse-item>
        </el-collapse>
      </el-tab-pane>
    </el-tabs>
  </el-card>

  <el-empty v-if="activeTab === 'run' && !result" description="选择本体后执行评估，将按本体定义检查图数据质量" />

  <el-card v-if="activeTab === 'history'" :bordered="false" shadow="never" class="fs-table-card">
    <div class="fs-table-toolbar flex-between">
      <span class="assess-label">评估历史仅用于追溯，不提供撤销</span>
    </div>
    <el-table v-loading="historyLoading" :data="historyRows" size="small" border>
      <el-table-column label="评估时间" width="170px">
        <template #default="scope">{{ DateUtil.format(scope.row.createdTime) }}</template>
      </el-table-column>
      <el-table-column prop="ontologyName" label="本体" min-width="140px" />
      <el-table-column label="得分" width="100px">
        <template #default="scope">
          <span :style="{ color: scoreColor(scope.row.score) }">{{ scope.row.score }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="nodeCount" label="实体数据" width="100px" />
      <el-table-column prop="relationshipCount" label="关系数据" width="100px" />
      <el-table-column prop="issueCount" label="问题数" width="90px" />
      <el-table-column label="操作" width="100px" fixed="right">
        <template #default="scope">
          <el-button link type="primary" @click="loadDetail(scope.row)">查看明细</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<style lang="scss" scoped>
.fs-table-card {
  margin-bottom: 16px;
  :deep(.el-card__body) {
    padding-bottom: 16px;
  }
}
.assess-label { color: var(--el-text-color-secondary); font-size: 13px; }
.assess-summary {
  display: flex;
  align-items: center;
  gap: 24px;
  flex-wrap: wrap;
  padding: 16px 0;
  &__score {
    padding: 8px 24px;
    border-right: 1px solid var(--el-border-color-lighter);
  }
  &__value { font-size: 40px; font-weight: 700; line-height: 1.1; }
  &__tip { font-size: 12px; color: var(--el-text-color-secondary); }
  &__item { min-width: 110px; }
  &__label { font-size: 13px; color: var(--el-text-color-secondary); }
  &__number { font-size: 22px; font-weight: 600; }
}
.assess-scope { font-weight: 600; }
  .assess-sample {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 5px 0;
    font-size: 12px;
    line-height: 1.6;
    word-break: break-all;
    & + .assess-sample { border-top: 1px dashed var(--el-border-color-lighter); }
    &__detail { flex: 1; min-width: 0; color: var(--el-text-color-regular); }
  }
  .assess-empty { padding: 4px 0; font-size: 12px; color: var(--el-text-color-secondary); }
.mt-10 { margin-top: 10px; }
:deep(.assess-issue-row) { background: var(--el-color-warning-light-9); }
</style>
