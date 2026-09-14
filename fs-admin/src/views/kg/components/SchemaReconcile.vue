<script setup lang="ts">
/**
 * 结构对账 - 已登记结构与数据库实际结构的比对
 *
 * 索引与约束独立管理：结构在"索引管理/约束管理"中创建并自动登记，
 * 本页负责发现漂移（登记了但库里没有、同名不同结构、库里有但未登记）并支持重建与登记。
 */
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElNotification } from 'element-plus'
import ApiUtil from '@/utils/ApiUtil'
import TableUtil from '@/utils/TableUtil'
import SchemaApi from '@/api/kg/SchemaApi'
import OntologyApi from '@/api/kg/OntologyApi'
import LayoutHeading from '@/components/Layout/LayoutHeading.vue'

const props = defineProps<{
  capabilities?: any,
  ontologies?: any[],
}>()

const ontologyOptions = ref<any[]>(props.ontologies ?? [])
const loadOntologies = () => {
  if (ontologyOptions.value.length > 0) return
  OntologyApi.list({ page: 1, pageSize: 200 }, { warning: false }).then((result: any) => {
    ontologyOptions.value = ApiUtil.data(result)?.rows ?? []
  }).catch(() => {})
}

const typeLabels: any = {
  RANGE: '范围索引', TEXT: '文本索引', POINT: '点索引', LOOKUP: '令牌查找',
  UNIQUE: '唯一约束', NOT_NULL: '存在性约束', KEY: '节点键约束',
  RELATIONSHIP_KEY: '关系键约束', TYPE: '类型约束',
}

const ontologyId = ref<any>(null)
const loading = ref(false)
const applying = ref(false)
const diff = ref<any>({ missing: [], matched: [], conflict: [], unmanaged: [], issues: [] })
const results = ref<any[]>([])
const missingSelection = ref<any[]>([])
const diffed = ref(false)
const activePanels = ref<string[]>(['missing'])

const counts = computed(() => ({
  missing: (diff.value.missing ?? []).length,
  matched: (diff.value.matched ?? []).length,
  conflict: (diff.value.conflict ?? []).length,
  unmanaged: (diff.value.unmanaged ?? []).length,
}))
const consistent = computed(() => diffed.value && counts.value.missing === 0 && counts.value.conflict === 0)

const typeText = (row: any) => typeLabels[row?.subType] ?? row?.subType ?? ''
const scopeText = (row: any) => row?.ontologyType === 'REL' ? '关系' : '节点'
const fieldsText = (row: any) => (row?.fields ?? []).join(', ') || '无'
const labelsText = (row: any) => (row?.labels ?? []).join(':') || '-'

const handleDiff = () => {
  loading.value = true
  SchemaApi.diff({ ontologyId: ontologyId.value || undefined }, { warning: false }).then((result: any) => {
    const data = ApiUtil.data(result) ?? {}
    diff.value = {
      missing: data.missing ?? [], matched: data.matched ?? [],
      conflict: data.conflict ?? [], unmanaged: data.unmanaged ?? [],
      issues: (data.issues ?? []).map((item: any) => String(item)),
    }
    missingSelection.value = []
    diffed.value = true
    activePanels.value = [counts.value.missing > 0 ? 'missing' : 'unmanaged']
  }).catch((result: any) => {
    ElMessage.warning(ApiUtil.message(result))
  }).finally(() => {
    loading.value = false
  })
}

const applyResult = (result: any) => {
  const data = ApiUtil.data(result) ?? {}
  results.value = data.results ?? []
  const failed = results.value.filter((item: any) => item.code !== 0)
  if (failed.length > 0) {
    ElNotification({
      title: `执行完成：成功 ${results.value.length - failed.length} 条，失败 ${failed.length} 条`,
      message: failed.map((item: any) => `${item.action} ${item.message}`).join('\n'),
      type: 'warning',
    })
  } else if (results.value.length > 0) {
    ElMessage.success(`执行完成，共 ${results.value.length} 条`)
  } else {
    ElMessage.info('没有需要重建的结构')
  }
}

const handleRebuildAll = () => {
  TableUtil.confirm(`将按登记定义重建 ${counts.value.missing} 个缺失结构，确认执行？`, '重建结构').then(() => {
    applying.value = true
    SchemaApi.apply({ ontologyId: ontologyId.value || undefined }).then((result: any) => {
      applyResult(result)
    }).catch((result: any) => {
      ElNotification({ title: '执行失败', message: ApiUtil.message(result), type: 'error' })
    }).finally(() => {
      applying.value = false
      handleDiff()
    })
  }).catch(() => {})
}

const handleRebuildSelected = () => {
  if (!missingSelection.value.length) return
  TableUtil.confirm(`将重建所选 ${missingSelection.value.length} 个结构，确认执行？`, '重建结构').then(() => {
    applying.value = true
    SchemaApi.batch({ creates: missingSelection.value }).then((result: any) => {
      applyResult({ code: ApiUtil.code(result), message: ApiUtil.message(result), data: ApiUtil.data(result) })
    }).catch((result: any) => {
      ElNotification({ title: '执行失败', message: ApiUtil.message(result), type: 'error' })
    }).finally(() => {
      applying.value = false
      handleDiff()
    })
  }).catch(() => {})
}

const handleAttach = (row: any) => {
  TableUtil.confirm(`将现有结构[${row.name}]登记为手工创建，之后对账会把它视为已纳管。确认继续？`, '登记结构').then(() => {
    SchemaApi.attach({ name: row.name }, { success: true }).then(() => {
      handleDiff()
    }).catch((result: any) => {
      ElMessage.warning(ApiUtil.message(result))
    })
  }).catch(() => {})
}

/** 全库结构漂移扫描 */
const scanLoading = ref(false)
const scanRows = ref<any[]>([])
const scanVisible = ref(false)

const handleScan = () => {
  scanVisible.value = true
  scanLoading.value = true
  SchemaApi.scan({}, { warning: false }).then((result: any) => {
    scanRows.value = ApiUtil.data(result) ?? []
  }).catch((result: any) => {
    ElMessage.warning(ApiUtil.message(result))
  }).finally(() => {
    scanLoading.value = false
  })
}

onMounted(() => {
  loadOntologies()
  handleDiff()
})
</script>

<template>
  <LayoutHeading
    title="结构对账"
    description="索引与约束独立管理，创建时自动登记；本页发现并修复漂移：登记了但库里没有、同名不同结构、库里有但未登记"
  />

  <el-card :bordered="false" shadow="never" class="fs-table-card">
    <div class="diff-toolbar">
      <el-space>
        <el-select v-model="ontologyId" clearable filterable placeholder="按登记来源筛选（可选）" style="width: 240px" @change="handleDiff">
          <el-option v-for="item in ontologyOptions" :key="item.id" :value="item.id" :label="item.name" />
        </el-select>
        <el-button type="primary" :loading="loading" @click="handleDiff">重新对账</el-button>
        <el-button :loading="scanLoading" @click="handleScan">全库漂移扫描</el-button>
      </el-space>
      <el-button type="success" :loading="applying" :disabled="!diffed || counts.missing === 0" @click="handleRebuildAll">
        重建全部缺失（{{ counts.missing }}）
      </el-button>
    </div>
    <el-alert v-if="diff.issues?.length" type="warning" :closable="false" title="登记定义存在问题" class="mt-10">
      <div v-for="(item, index) in diff.issues" :key="index">{{ item }}</div>
    </el-alert>
  </el-card>

  <el-card v-if="diffed" :bordered="false" shadow="never" class="fs-table-card">
    <div class="diff-summary">
      <div class="diff-summary__item diff-summary__item--missing">
        <div class="diff-summary__label">缺失</div>
        <div class="diff-summary__value">{{ counts.missing }}</div>
        <div class="diff-summary__tip">已登记但数据库中不存在</div>
      </div>
      <div class="diff-summary__item diff-summary__item--conflict">
        <div class="diff-summary__label">结构不一致</div>
        <div class="diff-summary__value">{{ counts.conflict }}</div>
        <div class="diff-summary__tip">同名但定义与数据库不同</div>
      </div>
      <div class="diff-summary__item diff-summary__item--matched">
        <div class="diff-summary__label">已一致</div>
        <div class="diff-summary__value">{{ counts.matched }}</div>
        <div class="diff-summary__tip">登记与实际一致</div>
      </div>
      <div class="diff-summary__item diff-summary__item--unmanaged">
        <div class="diff-summary__label">未纳管</div>
        <div class="diff-summary__value">{{ counts.unmanaged }}</div>
        <div class="diff-summary__tip">数据库中未登记的结构</div>
      </div>
    </div>
    <el-alert v-if="consistent" type="success" :closable="false" title="登记结构与数据库一致，无需处理" class="mt-10" />
    <el-collapse v-model="activePanels" class="mt-10 pb-10">
      <el-collapse-item name="missing">
        <template #title>
          <span class="diff-title">缺失（{{ counts.missing }}）</span>
          <el-button v-if="missingSelection.length" link type="primary" class="ml-10" @click.stop="handleRebuildSelected">
            重建所选（{{ missingSelection.length }}）
          </el-button>
        </template>
        <el-table :data="diff.missing" size="small" border @selection-change="(value: any) => missingSelection = value">
          <el-table-column type="selection" width="45px" />
          <el-table-column prop="name" label="名称" width="280px" />
          <el-table-column label="类型" width="120px"><template #default="scope">{{ typeText(scope.row) }}</template></el-table-column>
          <el-table-column label="作用对象" width="100px"><template #default="scope">{{ scopeText(scope.row) }}</template></el-table-column>
          <el-table-column label="标签" width="180px"><template #default="scope">{{ labelsText(scope.row) }}</template></el-table-column>
          <el-table-column label="字段" min-width="160px"><template #default="scope">{{ fieldsText(scope.row) }}</template></el-table-column>
        </el-table>
      </el-collapse-item>
      <el-collapse-item name="conflict">
        <template #title><span class="diff-title">结构不一致（{{ counts.conflict }}）</span></template>
        <el-alert type="warning" :closable="false" show-icon
          title="同名结构在数据库中的定义与登记不一致，请确认后手工处理（删除后重建，或更新登记）" class="mb-10" />
        <el-table :data="diff.conflict" size="small" border>
          <el-table-column prop="name" label="名称" width="260px" />
          <el-table-column label="类型" width="120px"><template #default="scope">{{ typeText(scope.row) }}</template></el-table-column>
          <el-table-column label="标签" width="180px"><template #default="scope">{{ labelsText(scope.row) }}</template></el-table-column>
          <el-table-column label="登记字段" min-width="140px"><template #default="scope">{{ fieldsText(scope.row) }}</template></el-table-column>
          <el-table-column label="数据库语句" min-width="320px">
            <template #default="scope"><code>{{ scope.row.existingStatement }}</code></template>
          </el-table-column>
        </el-table>
      </el-collapse-item>
      <el-collapse-item name="matched">
        <template #title><span class="diff-title">已一致（{{ counts.matched }}）</span></template>
        <el-table :data="diff.matched" size="small" border>
          <el-table-column prop="name" label="名称" width="280px" />
          <el-table-column label="类型" width="120px"><template #default="scope">{{ typeText(scope.row) }}</template></el-table-column>
          <el-table-column label="标签" width="180px"><template #default="scope">{{ labelsText(scope.row) }}</template></el-table-column>
          <el-table-column label="字段" min-width="160px"><template #default="scope">{{ fieldsText(scope.row) }}</template></el-table-column>
          <el-table-column prop="existingName" label="数据库中名称" width="220px" />
        </el-table>
      </el-collapse-item>
      <el-collapse-item name="unmanaged">
        <template #title><span class="diff-title">未纳管（{{ counts.unmanaged }}）</span></template>
        <el-alert type="info" :closable="false" show-icon
          title="这些结构存在于数据库中但未登记，可能是手工创建或系统内置；登记后即纳入对账" class="mb-10" />
        <el-table :data="diff.unmanaged" size="small" border>
          <el-table-column prop="name" label="名称" width="260px" />
          <el-table-column label="类型" width="120px"><template #default="scope">{{ typeText(scope.row) }}</template></el-table-column>
          <el-table-column label="标签" width="180px"><template #default="scope">{{ labelsText(scope.row) }}</template></el-table-column>
          <el-table-column label="字段" min-width="140px"><template #default="scope">{{ fieldsText(scope.row) }}</template></el-table-column>
          <el-table-column label="操作" width="100px" fixed="right">
            <template #default="scope">
              <el-button link type="primary" @click="handleAttach(scope.row)">登记</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-collapse-item>
    </el-collapse>
  </el-card>

  <el-card v-if="results.length" :bordered="false" shadow="never" class="fs-table-card">
    <template #header><span class="diff-title">执行结果</span></template>
    <el-table :data="results" size="small" border>
      <el-table-column prop="action" label="动作" width="100px">
        <template #default="scope">
          <el-tag :type="scope.row.action === 'CREATE' ? 'success' : 'danger'" effect="plain">{{ scope.row.action === 'CREATE' ? '创建' : '删除' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="结果" width="100px">
        <template #default="scope">
          <el-tag :type="scope.row.code === 0 ? 'success' : 'danger'">{{ scope.row.code === 0 ? '成功' : '失败' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="message" label="信息" />
      <el-table-column label="语句" min-width="320px">
        <template #default="scope"><code>{{ scope.row.data }}</code></template>
      </el-table-column>
    </el-table>
  </el-card>

  <el-drawer v-model="scanVisible" size="680px" title="全库结构漂移扫描">
    <el-alert type="info" :closable="false" show-icon
      title="按登记来源分组统计：已登记结构中数据库缺失或同名不一致的数量" class="mb-10" />
    <el-table v-loading="scanLoading" :data="scanRows" size="small" border>
      <el-table-column prop="name" label="登记来源" min-width="180px" />
      <el-table-column prop="registered" label="已登记" width="100px" />
      <el-table-column label="缺失" width="100px">
        <template #default="scope">
          <el-tag :type="scope.row.missing > 0 ? 'warning' : 'success'" size="small" effect="plain">{{ scope.row.missing }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="不一致" width="100px">
        <template #default="scope">
          <el-tag :type="scope.row.conflict > 0 ? 'warning' : 'success'" size="small" effect="plain">{{ scope.row.conflict }}</el-tag>
        </template>
      </el-table-column>
    </el-table>
  </el-drawer>
</template>

<style lang="scss" scoped>
.diff-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px;
  padding: 16px 0 10px;
}
.diff-summary {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  padding: 16px 0 14px;
  &__item {
    flex: 1;
    min-width: 150px;
    padding: 12px 16px;
    border-radius: 6px;
    background: var(--el-fill-color-lighter);
    border-left: 3px solid var(--el-color-info);
  }
  &__label { font-size: 13px; color: var(--el-text-color-secondary); }
  &__value { font-size: 24px; font-weight: 600; line-height: 1.4; }
  &__tip { font-size: 12px; color: var(--el-text-color-secondary); }
  &__item--missing { border-left-color: var(--el-color-warning); .diff-summary__value { color: var(--el-color-warning); } }
  &__item--conflict { border-left-color: var(--el-color-danger); .diff-summary__value { color: var(--el-color-danger); } }
  &__item--matched { border-left-color: var(--el-color-success); .diff-summary__value { color: var(--el-color-success); } }
  &__item--unmanaged { border-left-color: var(--el-color-info); }
}
.diff-title { font-weight: 600; }
.pb-10 { padding-bottom: 10px; }
.mt-10 { margin-top: 10px; }
.mb-10 { margin-bottom: 10px; }
.ml-10 { margin-left: 10px; }
</style>
