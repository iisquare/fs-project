<script setup lang="ts">
/**
 * 结构管理 - 索引管理 / 约束管理（按菜单拆分为独立页面，通过 mode 区分）
 *
 * 交互约定：
 * 1. 列表支持多选批量删除、来源标识（本体生成/手工创建/未纳管/系统内置）、展开查看创建语句；
 * 2. 新建抽屉支持实时CQL预览与创建前预检；
 * 3. 预检发现问题时给出明确原因，仍需创建时二次确认；
 * 4. 结构对账为独立页面，见 SchemaReconcile.vue。
 */
import { computed, onMounted, ref, watch } from 'vue'
import type { FormInstance, TableInstance } from 'element-plus'
import { ElMessage, ElNotification } from 'element-plus'
import * as ElementPlusIcons from '@element-plus/icons-vue'
import { useRoute, useRouter } from 'vue-router'
import ApiUtil from '@/utils/ApiUtil'
import TableUtil from '@/utils/TableUtil'
import RouteUtil from '@/utils/RouteUtil'
import DateUtil from '@/utils/DateUtil'
import SchemaApi from '@/api/kg/SchemaApi'
import LayoutIcon from '@/components/Layout/LayoutIcon.vue'
import LayoutHeading from '@/components/Layout/LayoutHeading.vue'
import LayoutHelp from '@/components/Layout/LayoutHelp.vue'

const route = useRoute()
const router = useRouter()
const props = defineProps<{ mode?: string }>()
const tableRef = ref<TableInstance>()
const loading = ref(false)
const searchable = ref(true)
const rows = ref<any[]>([])
const selection = ref<any[]>([])
const capabilities = ref<any>({})

const filterRef = ref<FormInstance>()
const filters = ref<any>(RouteUtil.query2filter(route, {
  name: '', label: '', subType: '', source: '', ontologyType: ''
}, false))

const typeLabels: any = {
  RANGE: '范围索引', TEXT: '文本索引', POINT: '点索引', LOOKUP: '令牌查找',
  UNIQUE: '唯一约束', NOT_NULL: '存在性约束', KEY: '节点键约束',
  RELATIONSHIP_KEY: '关系键约束', TYPE: '类型约束',
}
const sourceLabels: any = {
  ontology: '本体生成', manual: '手工创建', unmanaged: '未纳管', system: '系统内置',
}
const sourceTypes: any = {
  ontology: 'success', manual: 'primary', unmanaged: 'info', system: 'warning',
}
const enterpriseTypes = ['NOT_NULL', 'KEY', 'RELATIONSHIP_KEY', 'TYPE']

const indexColumns = ref([
  { prop: 'name', label: '名称', slot: 'name', width: '280px' },
  { prop: 'definition.subType', label: '类型', width: '110px', formatter: (row: any) => typeLabels[row.definition?.subType] ?? row.definition?.subType },
  { prop: 'definition.ontologyType', label: '作用对象', width: '100px', formatter: (row: any) => row.definition?.ontologyType === 'REL' ? '关系' : '节点' },
  { prop: 'definition.label', label: '标签', width: '160px' },
  { prop: 'definition.fields', label: '字段', formatter: (row: any) => (row.definition?.fields ?? []).join(', ') },
  { prop: 'state', label: '状态', width: '100px' },
  { prop: 'populationPercent', label: '填充率', width: '100px', hide: true, formatter: (row: any) => row.populationPercent == null ? '' : (row.populationPercent * 100).toFixed(2) + '%' },
  { prop: 'indexProvider', label: '提供者', width: '180px', hide: true },
  { prop: 'owningConstraint', label: '所属约束', width: '180px', hide: true },
  { prop: 'updatedTime', label: '更新时间', width: '170px', hide: true, formatter: (row: any) => DateUtil.format(row.updatedTime || row.createdTime) },
])
const constraintColumns = ref([
  { prop: 'name', label: '名称', slot: 'name', width: '280px' },
  { prop: 'definition.subType', label: '类型', width: '120px', formatter: (row: any) => typeLabels[row.definition?.subType] ?? row.definition?.subType },
  { prop: 'definition.ontologyType', label: '作用对象', width: '100px', formatter: (row: any) => row.definition?.ontologyType === 'REL' ? '关系' : '节点' },
  { prop: 'definition.label', label: '标签', width: '160px' },
  { prop: 'definition.fields', label: '字段', formatter: (row: any) => (row.definition?.fields ?? []).join(', ') },
  { prop: 'state', label: '状态', width: '100px' },
  { prop: 'ownedIndex', label: '支撑索引', width: '240px', hide: true },
  { prop: 'updatedTime', label: '更新时间', width: '170px', hide: true, formatter: (row: any) => DateUtil.format(row.updatedTime || row.createdTime) },
])
const kind = computed(() => props.mode === 'constraint' ? 'CONSTRAINT' : 'INDEX')
const columns = computed(() => kind.value === 'CONSTRAINT' ? constraintColumns.value : indexColumns.value)

const handleRefresh = () => {
  tableRef.value?.clearSelection()
  selection.value = []
  loading.value = true
  SchemaApi.show(Object.assign({ kind: kind.value }, filters.value)).then((result: any) => {
    rows.value = ApiUtil.data(result)?.rows ?? []
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}

const handleCapabilities = () => {
  SchemaApi.capabilities({}, { warning: false }).then((result: any) => {
    capabilities.value = ApiUtil.data(result) ?? {}
  }).catch(() => {})
}

onMounted(() => {
  handleCapabilities()
  handleRefresh()
})

/* ---------------- 新建结构 ---------------- */

const formVisible = ref(false)
const formLoading = ref(false)
const prechecking = ref(false)
const precheck = ref<any>(null)
const formRef = ref<FormInstance>()
const form = ref<any>({
  kind: 'INDEX', ontologyType: 'NODE', subType: 'RANGE',
  label: '', extraLabels: [], fields: [], name: '', propertyType: 'STRING', ontologyId: null,
})
const model = ref<any>({ entities: [], relationships: [] })

const handleAdd = () => {
  form.value = {
    kind: kind.value, ontologyType: 'NODE',
    subType: kind.value === 'INDEX' ? 'RANGE' : 'UNIQUE',
    label: '', extraLabels: [], fields: [], name: '', propertyType: 'STRING', ontologyId: null,
  }
  precheck.value = null
  formVisible.value = true
}
const singleField = computed(() => form.value.kind === 'CONSTRAINT' && ['NOT_NULL', 'TYPE'].includes(form.value.subType))
const indexTypeOptions = computed(() => ['RANGE', 'TEXT', 'POINT', 'LOOKUP'].map(value => ({ value, label: `${value} - ${typeLabels[value]}` })))
const constraintTypeOptions = computed(() => ['UNIQUE', 'NOT_NULL', 'KEY', 'RELATIONSHIP_KEY', 'TYPE'].map(value => {
  const disabled = enterpriseTypes.includes(value) && !capabilities.value.enterprise
  return { value, label: typeLabels[value] + (enterpriseTypes.includes(value) ? '（企业版）' : ''), disabled }
}))

const previewStatement = computed(() => {
  const value = form.value
  const name = value.name || '待生成'
  const node = value.ontologyType !== 'REL'
  const fields = value.fields ?? []
  if (value.kind === 'INDEX') {
    const type = value.subType && value.subType !== 'RANGE' ? value.subType + ' ' : ''
    let target
    if (value.subType === 'LOOKUP') {
      target = node ? 'FOR (n) ON EACH labels(n)' : 'FOR ()-[r]-() ON EACH type(r)'
    } else {
      const properties = fields.map((field: string) => 'nor.`' + field + '`').join(', ')
      target = (node ? 'FOR (nor:`' + value.label + '`) ON (' : 'FOR ()-[nor:`' + value.label + '`]-() ON (') + properties + ')'
    }
    return 'CREATE ' + type + 'INDEX `' + name + '` IF NOT EXISTS ' + target
  }
  const target = node ? 'FOR (nor:`' + value.label + '`)' : 'FOR ()-[nor:`' + value.label + '`]-()'
  const properties = fields.map((field: string) => 'nor.`' + field + '`')
  const property = properties.length > 1 ? '(' + properties.join(', ') + ')' : (properties[0] ?? 'nor.``')
  const suffix: any = {
    UNIQUE: ' IS UNIQUE', NOT_NULL: ' IS NOT NULL', KEY: ' IS NODE KEY',
    RELATIONSHIP_KEY: ' IS RELATIONSHIP KEY', TYPE: ' IS :: ' + (value.propertyType || 'STRING'),
  }
  return 'CREATE CONSTRAINT `' + name + '` IF NOT EXISTS ' + target + ' REQUIRE ' + property + (suffix[value.subType] ?? '')
})

watch(form, () => { precheck.value = null }, { deep: true })
watch(singleField, (value) => {
  if (value && form.value.fields.length > 1) form.value.fields = form.value.fields.slice(0, 1)
})

const generateName = () => {
  const prefix = form.value.kind === 'INDEX' ? 'index' : 'constraint'
  const scope = form.value.ontologyType === 'NODE' ? 'node' : 'rel'
  const fields = (form.value.fields ?? []).join('_')
  form.value.name = [prefix, scope, form.value.label, fields].filter(Boolean).join('_').toLowerCase()
}

const payload = () => {
  const value = { ...form.value }
  value.labels = [value.label, ...(value.extraLabels ?? [])].filter(Boolean)
  if (value.kind === 'INDEX' && value.subType === 'LOOKUP') {
    value.label = ''
    value.labels = []
    value.fields = []
  }
  return value
}

const handlePrecheck = () => {
  prechecking.value = true
  SchemaApi.precheck(payload(), { warning: false }).then((result: any) => {
    precheck.value = ApiUtil.data(result)
  }).catch((result: any) => {
    precheck.value = null
    ElMessage.warning(ApiUtil.message(result))
  }).finally(() => {
    prechecking.value = false
  })
}

const handleSubmit = () => {
  formRef.value?.validate((valid: boolean) => {
    if (!valid || formLoading.value) return
    formLoading.value = true
    SchemaApi.precheck(payload(), { warning: false }).then((result: any) => {
      const data = ApiUtil.data(result)
      precheck.value = data
      if ((data?.errors ?? []).length > 0) {
        formLoading.value = false
        TableUtil.confirm(`预检发现 ${data.errors.length} 项问题：\n${data.errors.join('\n')}\n\n确认仍然创建？`, '预检未通过').then(() => {
          doCreate(true)
        }).catch(() => {})
      } else {
        doCreate(false)
      }
    }).catch((result: any) => {
      formLoading.value = false
      ElMessage.warning(ApiUtil.message(result))
    })
  })
}

const doCreate = (force: boolean) => {
  formLoading.value = true
  SchemaApi.create(Object.assign(payload(), { force }), { success: true }).then(() => {
    formVisible.value = false
    handleRefresh()
  }).catch(() => {}).finally(() => {
    formLoading.value = false
  })
}

/* ---------------- 删除 ---------------- */

const detailVisible = ref(false)
const detail = ref<any>(null)
const handleDetail = (row: any) => {
  detail.value = row
  detailVisible.value = true
}

const handleDelete = (targets: any[]) => {
  if (!targets.length) return
  const names = targets.map(item => item.name)
  const message = `确认删除以下 ${names.length} 个结构？\n${names.slice(0, 10).join('、')}${names.length > 10 ? ' 等' : ''}\n\n提示：删除唯一约束时会一并移除其支撑索引。`
  TableUtil.confirm(message, '删除确认').then(() => {
    loading.value = true
    SchemaApi.batch({ drops: targets.map(item => ({ name: item.name, kind: item.kind })) }).then((result: any) => {
      const results = ApiUtil.data(result) ?? []
      const failed = results.filter((item: any) => item.code !== 0)
      const warned = results.filter((item: any) => item.code === 0 && item.message)
      const describe = (item: any) => {
        const data = item.data
        const target = typeof data === 'string' ? data : (data?.statement ?? data?.name ?? '')
        return `${target} ${item.message ?? ''}`.trim()
      }
      if (failed.length > 0) {
        ElNotification({
          title: `删除完成：成功 ${results.length - failed.length} 个，失败 ${failed.length} 个`,
          message: failed.map(describe).join('\n'),
          type: 'warning',
        })
      } else if (warned.length > 0) {
        ElNotification({
          title: `已删除 ${results.length} 个结构`,
          message: warned.map(describe).join('\n'),
          type: 'warning',
        })
      } else {
        ElMessage.success(`已删除 ${results.length} 个结构`)
      }
    }).catch((result: any) => {
      ElNotification({ title: '删除失败', message: ApiUtil.message(result), type: 'error' })
    }).finally(() => {
      loading.value = false
      handleRefresh()
    })
  }).catch(() => {})
}

const handleDeleteSelection = () => handleDelete(selection.value)

const handleCopy = (text: string) => {
  navigator.clipboard?.writeText(text).then(() => ElMessage.success('已复制到剪贴板')).catch(() => ElMessage.warning('复制失败，请手动选择'))
}

const handlePlan = () => { router.push('/kg/modeling/schema') }
</script>

<template>
  <LayoutHeading
    :title="kind === 'CONSTRAINT' ? '约束管理' : '索引管理'"
    :description="kind === 'CONSTRAINT'
      ? '独立管理图数据库约束，支持唯一、存在性、键、类型等类型的增删与预检'
      : '独立管理图数据库索引，支持范围、文本、点、令牌查找及复合索引的增删与预检'"
  >
    <template #extra>
      <el-space>
        <el-tag v-if="capabilities.version" type="info" effect="plain">Neo4j {{ capabilities.version }}</el-tag>
        <el-tag :type="capabilities.enterprise ? 'success' : 'warning'" effect="plain">
          {{ capabilities.enterprise ? '企业版' : '社区版' }}
        </el-tag>
        <el-tooltip v-if="!capabilities.enterprise" content="存在性、键、类型约束为企业版特性，社区版不可用" placement="top">
          <LayoutIcon name="QuestionFilled" />
        </el-tooltip>
      </el-space>
    </template>
  </LayoutHeading>

  <el-card :bordered="false" shadow="never" class="fs-table-search" v-loading="false">
    <form-search ref="filterRef" v-show="searchable" :model="filters">
      <form-search-item label="名称" prop="name">
        <el-input v-model="filters.name" clearable placeholder="支持模糊匹配" @keyup.enter="handleRefresh()" />
      </form-search-item>
      <form-search-item label="标签" prop="label">
        <el-input v-model="filters.label" clearable placeholder="节点标签或关系类型" @keyup.enter="handleRefresh()" />
      </form-search-item>
      <form-search-item label="类型" prop="subType">
        <el-select v-model="filters.subType" clearable placeholder="全部" style="width: 160px">
          <el-option v-for="(label, value) in typeLabels" :key="value" :value="value" :label="label" />
        </el-select>
      </form-search-item>
      <form-search-item label="来源" prop="source">
        <el-select v-model="filters.source" clearable placeholder="全部" style="width: 140px">
          <el-option v-for="(label, value) in sourceLabels" :key="value" :value="value" :label="label" />
        </el-select>
      </form-search-item>
      <form-search-item>
        <el-button type="primary" @click="handleRefresh()">查询</el-button>
        <el-button @click="filterRef?.resetFields()">重置</el-button>
      </form-search-item>
    </form-search>
  </el-card>

  <el-card :bordered="false" shadow="never" class="fs-table-card pb-20">
    <div class="fs-table-toolbar flex-between">
      <el-space>
        <el-button v-permit="'kg:ontology:schema'" type="success" :icon="ElementPlusIcons.Plus" @click="handleAdd">
          {{ kind === 'CONSTRAINT' ? '新建约束' : '新建索引' }}
        </el-button>
        <el-tooltip :disabled="selection.length > 0" content="请先勾选需要删除的结构" placement="top">
          <span>
            <el-button v-permit="'kg:ontology:schema'" type="danger" :icon="ElementPlusIcons.Delete" :disabled="selection.length === 0" @click="handleDeleteSelection">
              批量删除<template v-if="selection.length">（{{ selection.length }}）</template>
            </el-button>
          </span>
        </el-tooltip>
        <el-button v-permit="'kg:ontology:schema'" @click="handlePlan">结构对账</el-button>
      </el-space>
      <el-space>
        <button-search @click="searchable = !searchable" />
        <button-refresh @click="handleRefresh()" :loading="loading" />
        <TableColumnSetting v-if="kind === 'CONSTRAINT'" v-model="constraintColumns" :table="tableRef" />
        <TableColumnSetting v-else v-model="indexColumns" :table="tableRef" />
      </el-space>
    </div>
    <el-table
      ref="tableRef"
      v-loading="loading"
      :data="rows"
      :row-key="(record: any) => record.name"
      :border="true"
      table-layout="auto"
      @selection-change="(value: any) => selection = value"
    >
      <el-table-column type="selection" width="45px" />
      <el-table-column type="expand">
        <template #default="scope">
          <el-descriptions border :column="1" label-width="120px" class="schema-detail">
            <el-descriptions-item label="创建语句">
              <div class="schema-sql">
                <code>{{ scope.row.definition?.statement }}</code>
                <el-button link :icon="ElementPlusIcons.CopyDocument" @click.stop="handleCopy(scope.row.definition?.statement)">复制</el-button>
              </div>
            </el-descriptions-item>
            <el-descriptions-item label="字段与对象">
              {{ scope.row.definition?.ontologyType === 'REL' ? '关系' : '节点' }} / {{ scope.row.definition?.label }}
              / {{ (scope.row.definition?.fields ?? []).join(', ') || '无' }}
            </el-descriptions-item>
            <el-descriptions-item label="来源">
              {{ sourceLabels[scope.row.source] ?? scope.row.source }}
              <span v-if="scope.row.ontologyId">（本体 #{{ scope.row.ontologyId }}）</span>
              <span v-else-if="scope.row.source === 'unmanaged'">，未在系统登记，可能是手工创建或在功能上线前创建</span>
            </el-descriptions-item>
          </el-descriptions>
        </template>
      </el-table-column>
      <TableColumn :columns="columns">
        <template #name="scope">
          <div class="schema-name">
            <span class="schema-name__text">{{ scope.row.name }}</span>
            <el-tag size="small" effect="plain" :type="sourceTypes[scope.row.source] ?? 'info'">
              {{ sourceLabels[scope.row.source] ?? scope.row.source }}
            </el-tag>
          </div>
        </template>
      </TableColumn>
      <el-table-column label="操作" width="140px" fixed="right">
        <template #default="scope">
          <el-space>
            <el-button link type="primary" @click="handleDetail(scope.row)">详情</el-button>
            <el-button v-permit="'kg:ontology:schema'" link type="danger" @click="handleDelete([scope.row])">删除</el-button>
          </el-space>
        </template>
      </el-table-column>
      <template #empty>
        <div class="schema-empty">
            <div>{{ kind === 'CONSTRAINT' ? '暂无约束' : '暂无索引' }}</div>
          <div class="schema-empty__tip">可以直接新建，也可以选择本体后一键生成建议方案</div>
          <el-space>
            <el-button type="primary" @click="handleAdd">新建</el-button>
            <el-button @click="handlePlan">结构对账</el-button>
          </el-space>
        </div>
      </template>
    </el-table>
  </el-card>

  <el-drawer v-model="formVisible" size="520px" :close-on-click-modal="false" :destroy-on-close="true">
    <template #header="{ titleId, titleClass }">
      <h4 :id="titleId" :class="titleClass">{{ form.kind === 'CONSTRAINT' ? '新建约束' : '新建索引' }}</h4>
    </template>
    <el-form ref="formRef" :model="form" label-position="top">
      <el-divider content-position="left">结构定义</el-divider>
      <el-form-item required>
        <template #label><span>结构类型</span><LayoutHelp text="索引与约束独立管理，创建后会自动登记并纳入结构对账" /></template>
        <el-radio-group v-model="form.kind">
          <el-radio-button value="INDEX">索引</el-radio-button>
          <el-radio-button value="CONSTRAINT">约束</el-radio-button>
        </el-radio-group>
      </el-form-item>
      <el-form-item required>
        <template #label><span>作用对象</span><LayoutHelp text="选择作用于节点标签还是关系类型" /></template>
        <el-radio-group v-model="form.ontologyType" @change="() => { form.label = ''; form.fields = [] }">
          <el-radio-button value="NODE">节点</el-radio-button>
          <el-radio-button value="REL">关系</el-radio-button>
        </el-radio-group>
      </el-form-item>
      <el-form-item required>
        <template #label><span>类型</span><LayoutHelp text="存在性、键、类型约束为企业版特性，社区版不可用" /></template>
        <el-select v-if="form.kind === 'INDEX'" v-model="form.subType">
          <el-option v-for="item in indexTypeOptions" :key="item.value" :value="item.value" :label="item.label" />
        </el-select>
        <el-select v-else v-model="form.subType">
          <el-option v-for="item in constraintTypeOptions" :key="item.value" :value="item.value" :label="item.label" :disabled="item.disabled" />
        </el-select>
      </el-form-item>
      <el-form-item v-if="form.kind === 'CONSTRAINT' && form.subType === 'TYPE'" required>
        <template #label><span>属性类型</span><LayoutHelp text="属性类型约束要求该字段的值必须为指定类型" /></template>
        <el-select v-model="form.propertyType">
          <el-option v-for="item in ['STRING', 'BOOLEAN', 'INTEGER', 'FLOAT', 'DATE', 'LOCAL DATETIME', 'ZONED DATETIME', 'DURATION', 'POINT']" :key="item" :value="item" :label="item" />
        </el-select>
      </el-form-item>
      <el-form-item v-if="form.subType !== 'LOOKUP'" required>
        <template #label><span>标签</span><LayoutHelp text="节点标签或关系类型；不受本体限制，可自由输入" /></template>
        <el-select v-model="form.label" filterable allow-create default-first-option placeholder="节点标签或关系类型" />
      </el-form-item>
      <el-form-item v-if="form.subType !== 'LOOKUP'">
        <template #label><span>标签组合</span><LayoutHelp text="选填，作用于多个标签的组合，例如 Person:Employee" /></template>
        <el-select v-model="form.extraLabels" multiple filterable allow-create default-first-option placeholder="选填，作用于多个标签的组合，例如 Person:Employee">
          <el-option v-for="item in form.extraLabels" :key="item" :value="item" :label="item" />
        </el-select>
      </el-form-item>
      <el-form-item v-if="form.subType !== 'LOOKUP'" required>
        <template #label><span>字段</span><LayoutHelp text="复合索引按顺序生效；存在性与类型约束仅支持单个字段" /></template>
        <el-select v-model="form.fields" :multiple="!singleField" filterable allow-create default-first-option placeholder="字段名称，复合索引按顺序生效" />
      </el-form-item>
      <el-form-item required>
        <template #label><span>名称</span><LayoutHelp text="图数据库中唯一，只能由字母、数字、下划线组成" /></template>
        <el-input v-model="form.name" placeholder="图数据库中唯一，字母数字下划线">
          <template #append><el-button @click="generateName">自动生成</el-button></template>
        </el-input>
      </el-form-item>
      <el-form-item label="语句预览">
        <div class="schema-preview"><code>{{ previewStatement }}</code></div>
      </el-form-item>
      <el-form-item>
        <template #label><span>创建预检</span><LayoutHelp text="预检会检查重复数据、等价结构与影响范围；发现问题时可确认后强制执行" /></template>
        <el-button :loading="prechecking" @click="handlePrecheck">执行预检</el-button>
      </el-form-item>
      <el-form-item v-if="precheck" label="预检结果">
        <div class="schema-precheck">
          <el-alert v-if="(precheck.errors ?? []).length" type="error" :closable="false" title="存在问题">
            <div v-for="(item, index) in precheck.errors" :key="index">{{ item }}</div>
          </el-alert>
          <el-alert v-if="(precheck.warnings ?? []).length" type="warning" :closable="false" title="提示" style="margin-top: 6px">
            <div v-for="(item, index) in precheck.warnings" :key="index">{{ item }}</div>
          </el-alert>
          <div v-if="precheck.affected != null" class="schema-form-tip">影响数据量：{{ precheck.affected }} 条</div>
          <el-table v-if="(precheck.duplicates ?? []).length" :data="precheck.duplicates" size="small" border style="margin-top: 6px">
            <el-table-column v-for="field in Object.keys(precheck.duplicates[0] ?? {})" :key="field" :prop="field" :label="field" />
          </el-table>
        </div>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-space>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="formLoading" @click="handleSubmit">确定</el-button>
      </el-space>
    </template>
  </el-drawer>

  <el-drawer v-model="detailVisible" size="480px" title="结构详情">
    <el-descriptions v-if="detail" :column="1" border label-width="110px">
      <el-descriptions-item label="名称">{{ detail.name }}</el-descriptions-item>
      <el-descriptions-item label="类型">{{ typeLabels[detail.definition?.subType] ?? detail.definition?.subType }}</el-descriptions-item>
      <el-descriptions-item label="作用对象">{{ detail.definition?.ontologyType === 'REL' ? '关系' : '节点' }} / {{ detail.definition?.label }}</el-descriptions-item>
      <el-descriptions-item label="字段">{{ (detail.definition?.fields ?? []).join(', ') || '无' }}</el-descriptions-item>
      <el-descriptions-item label="来源">{{ sourceLabels[detail.source] ?? detail.source }}</el-descriptions-item>
      <el-descriptions-item label="状态">{{ detail.state ?? '-' }}</el-descriptions-item>
      <el-descriptions-item label="创建语句">
        <div class="schema-sql">
          <code>{{ detail.definition?.statement }}</code>
          <el-button link :icon="ElementPlusIcons.CopyDocument" @click="handleCopy(detail.definition?.statement)">复制</el-button>
        </div>
      </el-descriptions-item>
    </el-descriptions>
    <template #footer>
      <el-space>
      <el-button v-permit="'kg:ontology:schema'" type="danger" @click="handleDelete([detail]); detailVisible = false">删除该结构</el-button>
        <el-button @click="detailVisible = false">关闭</el-button>
      </el-space>
    </template>
  </el-drawer>
</template>

<style lang="scss" scoped>
.schema-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  .schema-tabs { flex: 1; }
  :deep(.el-tabs__header) { margin-bottom: 0; }
}
.schema-capability { padding-left: 12px; }
.schema-name {
  display: flex;
  align-items: center;
  gap: 8px;
  &__text { font-weight: 500; }
}
.schema-sql {
  display: flex;
  align-items: center;
  gap: 8px;
  code { word-break: break-all; }
}
.schema-preview {
  width: 100%;
  padding: 8px 10px;
  background: var(--el-fill-color-light);
  border-radius: 4px;
  code { word-break: break-all; color: var(--el-color-primary); }
}
.schema-form-tip { font-size: 12px; color: var(--el-text-color-secondary); }
.schema-empty {
  padding: 20px 0;
  color: var(--el-text-color-secondary);
  &__tip { margin: 6px 0 12px; font-size: 12px; }
}
.schema-precheck { width: 100%; }
.schema-detail { padding: 8px 12px; }
</style>
