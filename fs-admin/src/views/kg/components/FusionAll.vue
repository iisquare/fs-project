<script setup lang="ts">
/**
 * 知识融合 - 同本体内实体去重
 *
 * 流程：配置规则 → 扫描生成候选 → 人工审核（左右并排对比）→ 合并。
 * 策略：候选一律人工确认，不做自动合并；属性冲突保留非空值；不提供撤销，合并过程留有记录。
 */
import { computed, onMounted, ref } from 'vue'
import type { FormInstance, TableInstance } from 'element-plus'
import { ElMessage, ElNotification } from 'element-plus'
import * as ElementPlusIcons from '@element-plus/icons-vue'
import { useRoute, useRouter } from 'vue-router'
import ApiUtil from '@/utils/ApiUtil'
import TableUtil from '@/utils/TableUtil'
import RouteUtil from '@/utils/RouteUtil'
import DateUtil from '@/utils/DateUtil'
import FusionApi from '@/api/kg/FusionApi'
import OntologyApi from '@/api/kg/OntologyApi'
import LayoutHeading from '@/components/Layout/LayoutHeading.vue'
import LayoutHelp from '@/components/Layout/LayoutHelp.vue'

const route = useRoute()
const router = useRouter()
const props = defineProps<{ tab?: string }>()
const activeTab = ref(props.tab ?? 'rule')
const tabOfPath = (path: string) => path.includes('/candidate') ? 'candidate' : (path.includes('/record') ? 'record' : 'rule')
const pageMeta = computed(() => ({
  rule: { title: '融合规则', description: '配置参与匹配的字段与权重和相似度阈值，扫描生成候选' },
  candidate: { title: '候选审核', description: '候选一律人工确认：属性冲突保留非空值，确认后执行合并且不可撤销' },
  record: { title: '融合记录', description: '记录每次合并的双方数据与关系处理结果，仅用于追溯' },
}[activeTab.value as string] ?? { title: '', description: '' }))

const ontologies = ref<any[]>([])
const handleOntologies = () => {
  OntologyApi.list({ page: 1, pageSize: 200 }).then((result: any) => {
    ontologies.value = ApiUtil.data(result)?.rows ?? []
  }).catch(() => {})
}

/* ---------------- 规则 ---------------- */

const ruleLoading = ref(false)
const ruleRows = ref<any[]>([])
const ruleFormVisible = ref(false)
const ruleFormLoading = ref(false)
const ruleFormRef = ref<FormInstance>()
const ruleForm = ref<any>({ id: 0, name: '', ontologyId: null, entityLabel: '', primaryField: '', fields: [], threshold: 0.75, scanLimit: 5000, status: 1 })
const model = ref<any>({ entities: [], relationships: [] })

const entityOptions = computed(() => model.value.entities ?? [])
const entityFields = computed(() => entityOptions.value.find((item: any) => item.label === ruleForm.value.entityLabel)?.fields ?? [])
const ontologyName = (id: any) => ontologies.value.find((item: any) => item.id === id)?.name ?? id

const loadRules = () => {
  ruleLoading.value = true
  FusionApi.ruleList({}, { warning: false }).then((result: any) => {
    ruleRows.value = ApiUtil.data(result) ?? []
  }).catch(() => {}).finally(() => {
    ruleLoading.value = false
  })
}

const handleRuleAdd = () => {
  ruleForm.value = { id: 0, name: '', ontologyId: null, entityLabel: '', primaryField: '', fields: [], threshold: 0.75, scanLimit: 5000, status: 1 }
  model.value = { entities: [], relationships: [] }
  ruleFormVisible.value = true
}

const handleRuleEdit = (row: any) => {
  ruleForm.value = Object.assign({}, row, { fields: (row.fields ?? []).map((item: any) => ({ name: item.name, weight: item.weight ?? 1 })) })
  ruleFormVisible.value = true
  handleOntologyChange(row.ontologyId, true)
}

const handleOntologyChange = (id: any, keepField: boolean) => {
  if (!keepField) {
    ruleForm.value.entityLabel = ''
    ruleForm.value.primaryField = ''
    ruleForm.value.fields = []
  }
  model.value = { entities: [], relationships: [] }
  if (!id) return
  OntologyApi.model({ id }, { warning: false }).then((result: any) => {
    model.value = ApiUtil.data(result) ?? { entities: [], relationships: [] }
    if (!ruleForm.value.entityLabel && entityOptions.value.length > 0) {
      ruleForm.value.entityLabel = entityOptions.value[0].label
    }
    syncPrimaryField()
  }).catch(() => {})
}

const syncPrimaryField = () => {
  const entity = entityOptions.value.find((item: any) => item.label === ruleForm.value.entityLabel)
  if (entity) {
    ruleForm.value.primaryField = entity.primaryField || ruleForm.value.primaryField
    const names = (entity.fields ?? []).map((item: any) => item.name)
    ruleForm.value.fields = (ruleForm.value.fields ?? []).filter((item: any) => names.includes(item.name))
  }
}

const handleEntityChange = () => {
  ruleForm.value.fields = []
  syncPrimaryField()
}

const handleRuleSubmit = () => {
  const fields = (ruleForm.value.fields ?? []).filter((item: any) => item.name)
  if (!ruleForm.value.name) return ElMessage.warning('请填写规则名称')
  if (!ruleForm.value.ontologyId) return ElMessage.warning('请选择本体')
  if (!ruleForm.value.entityLabel) return ElMessage.warning('请选择实体')
  if (fields.length === 0) return ElMessage.warning('请选择参与匹配的字段')
  ruleFormLoading.value = true
  FusionApi.ruleSave(Object.assign({}, ruleForm.value, { fields }), { success: true }).then(() => {
    ruleFormVisible.value = false
    loadRules()
  }).catch((result: any) => {
    ElMessage.warning(ApiUtil.message(result))
  }).finally(() => {
    ruleFormLoading.value = false
  })
}

const handleRuleDelete = (row: any) => {
  TableUtil.confirm(`确认删除规则[${row.name}]？`, '删除规则').then(() => {
    FusionApi.ruleDelete({ ids: [row.id] }, { success: true }).then(() => loadRules()).catch(() => {})
  }).catch(() => {})
}

/* ---------------- 扫描与候选 ---------------- */

const scanning = ref(false)
const candidateLoading = ref(false)
const candidateRows = ref<any[]>([])
const candidateSelection = ref<any[]>([])
const candidateFilters = ref<any>({ ruleId: null, status: 'PENDING', keyword: '' })

const scan = (row: any) => {
  scanning.value = true
  FusionApi.scan({ ruleId: row.id }, { warning: false }).then((result: any) => {
    const data = ApiUtil.data(result) ?? {}
    ElMessage.success(`扫描 ${data.nodeCount ?? 0} 个节点，生成 ${data.candidateCount ?? 0} 个候选`)
    router.push({ path: '/kg/fusion/candidate', query: { ruleId: String(row.id) } })
  }).catch((result: any) => {
    ElNotification({ title: '扫描失败', message: ApiUtil.message(result), type: 'error' })
  }).finally(() => {
    scanning.value = false
  })
}

const loadCandidates = () => {
  candidateLoading.value = true
  FusionApi.candidateList({
    ruleId: candidateFilters.value.ruleId || undefined,
    status: candidateFilters.value.status || undefined,
    keyword: candidateFilters.value.keyword || undefined,
    page: 1,
    pageSize: 50,
  }, { warning: false }).then((result: any) => {
    candidateRows.value = ApiUtil.data(result)?.rows ?? []
  }).catch(() => {}).finally(() => {
    candidateLoading.value = false
  })
}

const handleReject = (rows: any[]) => {
  if (!rows.length) return
  TableUtil.confirm(`确认所选 ${rows.length} 组数据不是同一实体？`, '标记为不同实体').then(() => {
    FusionApi.reject({ ids: rows.map((row: any) => row.id) }, { success: true }).then(() => loadCandidates()).catch(() => {})
  }).catch(() => {})
}

/* ---------------- 审核与合并 ---------------- */

const reviewVisible = ref(false)
const reviewLoading = ref(false)
const review = ref<any>(null)
const keepSide = ref('LEFT')
const merging = ref(false)

const handleReview = (row: any) => {
  reviewVisible.value = true
  reviewLoading.value = true
  keepSide.value = 'LEFT'
  review.value = null
  FusionApi.candidateDetail({ id: row.id }, { warning: false }).then((result: any) => {
    review.value = ApiUtil.data(result)
  }).catch((result: any) => {
    ElMessage.warning(ApiUtil.message(result))
  }).finally(() => {
    reviewLoading.value = false
  })
}

const handleMerge = () => {
  TableUtil.confirm(`将把另一条数据合并到所选保留的数据，合并后不可撤销。确认执行？`, '合并确认').then(() => {
    merging.value = true
    FusionApi.merge({ id: review.value.id, keepSide: keepSide.value }).then((result: any) => {
      const data = ApiUtil.data(result) ?? {}
      ElNotification({
        title: '合并完成',
        message: `保留 ${data.keepKey}，合并 ${data.mergedKey}；转移关系 ${data.relationsMoved} 条，合并重复关系 ${data.relationsMerged} 条，删除自环 ${data.relationsRemoved} 条，补齐标签 ${data.labelsMerged} 个`,
        type: 'success',
      })
      reviewVisible.value = false
      loadCandidates()
    }).catch((result: any) => {
      ElNotification({ title: '合并失败', message: ApiUtil.message(result), type: 'error' })
    }).finally(() => {
      merging.value = false
    })
  }).catch(() => {})
}

/* ---------------- 融合记录 ---------------- */

const recordLoading = ref(false)
const recordRows = ref<any[]>([])
const loadRecords = () => {
  recordLoading.value = true
  FusionApi.recordList({ page: 1, pageSize: 50 }, { warning: false }).then((result: any) => {
    recordRows.value = ApiUtil.data(result)?.rows ?? []
  }).catch(() => {}).finally(() => {
    recordLoading.value = false
  })
}

const statusText = (status: string) => ({ PENDING: '待确认', MERGED: '已合并', REJECTED: '不同实体' }[status] ?? status)
const statusType = (status: string): any => ({ PENDING: 'warning', MERGED: 'success', REJECTED: 'info' }[status] ?? 'info')

onMounted(() => {
  activeTab.value = props.tab ?? tabOfPath(route.path)
  handleOntologies()
  loadRules()
  if (activeTab.value === 'candidate') {
    const ruleId = Number(route.query.ruleId)
    candidateFilters.value.ruleId = Number.isFinite(ruleId) && ruleId > 0 ? ruleId : null
    loadCandidates()
  }
  if (activeTab.value === 'record') loadRecords()
})
</script>

<template>
  <LayoutHeading :title="pageMeta.title" :description="pageMeta.description" />

  <el-card v-if="activeTab === 'rule'" :bordered="false" shadow="never" class="fs-table-card">
    <div class="fs-table-toolbar flex-between">
      <el-space>
        <el-button v-permit="'kg:fusion:scan'" type="success" :icon="ElementPlusIcons.Plus" @click="handleRuleAdd">新建规则</el-button>
      </el-space>
      <el-space>
        <button-refresh @click="loadRules" :loading="ruleLoading" />
      </el-space>
    </div>
    <el-table v-loading="ruleLoading" :data="ruleRows" :border="true" table-layout="auto">
      <el-table-column prop="name" label="规则名称" min-width="160px" />
      <el-table-column label="本体" width="160px"><template #default="scope">{{ ontologyName(scope.row.ontologyId) }}</template></el-table-column>
      <el-table-column prop="entityLabel" label="实体标签" width="150px" />
      <el-table-column label="参与字段" min-width="200px">
        <template #default="scope">
          <el-tag v-for="item in (scope.row.fields ?? [])" :key="item.name" size="small" effect="plain" style="margin-right: 4px">
            {{ item.name }}<span v-if="item.weight">（{{ item.weight }}）</span>
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="threshold" label="阈值" width="90px" />
      <el-table-column prop="scanLimit" label="扫描上限" width="100px" />
      <el-table-column label="操作" width="200px" fixed="right">
        <template #default="scope">
          <el-space>
            <el-button link type="primary" :loading="scanning" @click="scan(scope.row)">扫描候选</el-button>
            <el-button v-permit="'kg:fusion:scan'" link type="primary" @click="handleRuleEdit(scope.row)">编辑</el-button>
            <el-button v-permit="'kg:fusion:scan'" link type="danger" @click="handleRuleDelete(scope.row)">删除</el-button>
          </el-space>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="还没有融合规则，先新建规则再扫描候选">
          <el-button v-permit="'kg:fusion:scan'" type="primary" :icon="ElementPlusIcons.Plus" @click="handleRuleAdd">新建规则</el-button>
        </el-empty>
      </template>
    </el-table>
  </el-card>

  <el-card v-if="activeTab === 'candidate'" :bordered="false" shadow="never" class="fs-table-card">
    <div class="fs-table-toolbar flex-between">
      <el-space wrap>
        <el-select v-model="candidateFilters.ruleId" clearable placeholder="全部规则" style="width: 180px">
          <el-option value="" label="全部规则" />
          <el-option v-for="item in ruleRows" :key="item.id" :value="item.id" :label="item.name" />
        </el-select>
        <el-select v-model="candidateFilters.status" clearable placeholder="全部状态" style="width: 140px">
          <el-option value="" label="全部状态" />
          <el-option value="PENDING" label="待确认" />
          <el-option value="MERGED" label="已合并" />
          <el-option value="REJECTED" label="不同实体" />
        </el-select>
        <el-input v-model="candidateFilters.keyword" placeholder="主键值" clearable style="width: 160px" @keyup.enter="loadCandidates" />
        <el-button type="primary" @click="loadCandidates">查询</el-button>
      </el-space>
      <el-space>
        <el-button v-permit="'kg:fusion:merge'" :disabled="!candidateSelection.length" @click="handleReject(candidateSelection)">标记为不同实体</el-button>
        <button-refresh @click="loadCandidates" :loading="candidateLoading" />
      </el-space>
    </div>
    <el-table v-loading="candidateLoading" :data="candidateRows" :border="true" table-layout="auto" @selection-change="(value: any) => candidateSelection = value">
      <el-table-column type="selection" width="45px" />
      <el-table-column prop="leftKey" label="数据A" min-width="140px" />
      <el-table-column prop="rightKey" label="数据B" min-width="140px" />
      <el-table-column label="相似度" width="110px">
        <template #default="scope">{{ (scope.row.score * 100).toFixed(1) }}%</template>
      </el-table-column>
      <el-table-column label="状态" width="110px">
        <template #default="scope"><el-tag :type="statusType(scope.row.status)" effect="plain">{{ statusText(scope.row.status) }}</el-tag></template>
      </el-table-column>
      <el-table-column label="生成时间" width="170px">
        <template #default="scope">{{ DateUtil.format(scope.row.createdTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="160px" fixed="right">
        <template #default="scope">
          <el-space>
            <el-button link type="primary" @click="handleReview(scope.row)">审核</el-button>
            <el-button v-if="scope.row.status === 'PENDING'" v-permit="'kg:fusion:merge'" link @click="handleReject([scope.row])">不同实体</el-button>
          </el-space>
        </template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无候选，请在规则页执行扫描" />
      </template>
    </el-table>
  </el-card>

  <el-card v-if="activeTab === 'record'" :bordered="false" shadow="never" class="fs-table-card">
    <div class="fs-table-toolbar flex-between">
      <span class="dm-form-tip">融合记录仅用于追溯，不提供撤销</span>
      <button-refresh @click="loadRecords" :loading="recordLoading" />
    </div>
    <el-table v-loading="recordLoading" :data="recordRows" :border="true" table-layout="auto">
      <el-table-column type="expand" width="45px">
        <template #default="scope">
          <div class="fusion-snapshot">
            <div><strong>保留前：</strong>{{ scope.row.keepSnapshot }}</div>
            <div><strong>合并前：</strong>{{ scope.row.mergedSnapshot }}</div>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="entityLabel" label="实体标签" width="150px" />
      <el-table-column prop="keepKey" label="保留数据" min-width="140px" />
      <el-table-column prop="mergedKey" label="被合并数据" min-width="140px" />
      <el-table-column prop="relationsMoved" label="转移关系" width="100px" />
      <el-table-column prop="relationsMerged" label="合并重复" width="100px" />
      <el-table-column prop="relationsRemoved" label="删除自环" width="100px" />
      <el-table-column prop="labelsMerged" label="补齐标签" width="100px" />
      <el-table-column label="合并时间" width="170px">
        <template #default="scope">{{ DateUtil.format(scope.row.createdTime) }}</template>
      </el-table-column>
      <template #empty>
        <el-empty description="暂无融合记录，确认合并后会在这里留痕" />
      </template>
    </el-table>
  </el-card>

  <el-drawer v-model="ruleFormVisible" size="560px" :close-on-click-modal="false" :destroy-on-close="true">
    <template #header="{ titleId, titleClass }">
      <h4 :id="titleId" :class="titleClass">{{ ruleForm.id ? '编辑融合规则' : '新建融合规则' }}</h4>
    </template>
    <el-form ref="ruleFormRef" :model="ruleForm" label-position="top">
      <el-form-item required>
        <template #label><span>规则名称</span><LayoutHelp text="用于区分不同的匹配策略，例如「人员去重」" /></template>
        <el-input v-model="ruleForm.name" placeholder="例如：人员去重" />
      </el-form-item>
      <el-form-item required>
        <template #label><span>本体</span><LayoutHelp text="融合限定在同一本体内进行，不支持跨本体合并" /></template>
        <el-select v-model="ruleForm.ontologyId" filterable placeholder="请选择本体" @change="(value: any) => handleOntologyChange(value, false)">
          <el-option v-for="item in ontologies" :key="item.id" :value="item.id" :label="item.name" />
        </el-select>
      </el-form-item>
      <el-form-item required>
        <template #label><span>实体</span><LayoutHelp text="选择要去做重的实体标签，候选只在该实体的数据之间生成" /></template>
        <el-select v-model="ruleForm.entityLabel" filterable placeholder="请选择实体" @change="handleEntityChange">
          <el-option v-for="item in entityOptions" :key="item.label" :value="item.label" :label="item.name || item.label" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <template #label><span>主键字段</span><LayoutHelp text="默认取实体的主键字段，用于定位与合并数据" /></template>
        <el-input v-model="ruleForm.primaryField" placeholder="默认取实体主键字段" />
      </el-form-item>
      <el-form-item required>
        <template #label><span>匹配字段</span><LayoutHelp text="参与相似度计算的字段；未填写的字段不参与比较" /></template>
        <el-select v-model="ruleForm.fields" multiple filterable value-key="name" placeholder="选择参与匹配的字段">
          <el-option v-for="item in entityFields" :key="item.name" :value="{ name: item.name, weight: 1 }" :label="item.title || item.name" />
        </el-select>
      </el-form-item>
      <el-form-item v-if="(ruleForm.fields ?? []).length">
        <template #label><span>字段权重</span><LayoutHelp text="权重越高，该字段对总分影响越大；默认 1" /></template>
        <el-table :data="ruleForm.fields" size="small" border>
          <el-table-column prop="name" label="字段" width="140px" />
          <el-table-column label="权重">
            <template #default="scope">
              <el-input-number v-model="scope.row.weight" :min="1" :max="10" :controls="false" style="width: 100px" />
            </template>
          </el-table-column>
        </el-table>
      </el-form-item>
      <el-form-item>
        <template #label><span>相似度阈值</span><LayoutHelp text="达到阈值才会生成候选，建议 0.7 以上；阈值越低召回越多、人工审核成本越高" /></template>
        <el-input-number v-model="ruleForm.threshold" :min="0.1" :max="1" :step="0.05" :controls="false" style="width: 120px" />
      </el-form-item>
      <el-form-item>
        <template #label><span>扫描上限</span><LayoutHelp text="单次扫描的节点数量上限，超出部分不会被扫描" /></template>
        <el-input-number v-model="ruleForm.scanLimit" :min="10" :max="20000" :controls="false" style="width: 120px" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-space>
        <el-button @click="ruleFormVisible = false">取消</el-button>
        <el-button type="primary" :loading="ruleFormLoading" @click="handleRuleSubmit">确定</el-button>
      </el-space>
    </template>
  </el-drawer>

  <el-drawer v-model="reviewVisible" size="900px" title="融合审核">
    <div v-loading="reviewLoading">
      <template v-if="review">
        <el-alert type="info" :closable="false" show-icon class="mb-10"
          :title="`数据A：${review.leftKey}　数据B：${review.rightKey}　相似度：${(review.score * 100).toFixed(1)}%`" />
        <el-radio-group v-model="keepSide" class="mb-10">
          <el-radio-button value="LEFT">保留数据A</el-radio-button>
          <el-radio-button value="RIGHT">保留数据B</el-radio-button>
        </el-radio-group>
        <span class="dm-form-tip">属性冲突时保留非空值；两侧都有值时保留所选方</span>
        <el-table :data="review.fields" size="small" border>
          <el-table-column label="字段" width="150px">
            <template #default="scope">{{ scope.row.title || scope.row.name }}</template>
          </el-table-column>
          <el-table-column label="数据A" min-width="200px">
            <template #default="scope">
              <span :class="{ 'fusion-empty': !scope.row.left }">{{ scope.row.left || '（空）' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="数据B" min-width="200px">
            <template #default="scope">
              <span :class="{ 'fusion-empty': !scope.row.right }">{{ scope.row.right || '（空）' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="相似度" width="100px">
            <template #default="scope">{{ (scope.row.similarity * 100).toFixed(0) }}%</template>
          </el-table-column>
        </el-table>
        <el-space class="mt-10">
          <el-button v-permit="'kg:fusion:merge'" type="primary" :loading="merging" @click="handleMerge">确认合并</el-button>
          <el-button v-permit="'kg:fusion:merge'" @click="handleReject([{ id: review.id }]); reviewVisible = false">不是同一实体</el-button>
          <el-button @click="reviewVisible = false">稍后处理</el-button>
        </el-space>
      </template>
    </div>
  </el-drawer>
</template>

<style lang="scss" scoped>
.fs-table-card {
  margin-bottom: 16px;
  :deep(.el-card__body) {
    padding-bottom: 16px;
  }
}
.dm-form-tip { font-size: 12px; color: var(--el-text-color-secondary); margin-left: 8px; }
.fusion-empty { color: var(--el-text-color-placeholder); }
.fusion-snapshot {
  padding: 10px 12px;
  border: 1px dashed var(--el-border-color-lighter);
  border-radius: 6px;
  background: var(--el-fill-color-blank);
  font-size: 12px;
  line-height: 1.7;
  word-break: break-all;
  div { margin: 4px 0; }
  strong {
    margin-right: 4px;
    font-weight: 500;
    color: var(--el-text-color-secondary);
  }
}
.mb-10 { margin-bottom: 10px; }
.mt-10 { margin-top: 10px; }
</style>
