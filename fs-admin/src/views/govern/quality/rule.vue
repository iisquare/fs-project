<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { FormInstance, TableInstance } from 'element-plus'
import QualityRuleApi from '@/api/govern/QualityRuleApi'
import QualityLogicApi from '@/api/govern/QualityLogicApi'
import ApiUtil from '@/utils/ApiUtil'
import RouteUtil from '@/utils/RouteUtil'
import TableUtil from '@/utils/TableUtil'
import GovernPageShell from '@/views/govern/components/GovernPageShell.vue'
import QualityRuleFields from '@/views/govern/components/QualityRuleFields.vue'
import {
  qualityMetrics,
  qualityOperators,
  qualityRuleTypes,
  ruleDefaultContent,
} from '@/views/govern/components/qualityConfig'

const route = useRoute()
const router = useRouter()
const tableRef = ref<TableInstance>()
const loading = ref(false)
const searchable = ref(true)
const rows = ref<any[]>([])
const selection = ref<any[]>([])
const filters = ref<any>(RouteUtil.query2filter(route, { advanced: false, page: 1, pageSize: 20 }))
const pagination = ref<any>(RouteUtil.pagination(filters.value))
const filterRef = ref<FormInstance>()
const config = ref<any>({
  status: { 1: '启用', 2: '关闭' },
  metrics: qualityMetrics,
  operators: qualityOperators,
})
const logicTree = ref<any[]>([])
const columns = ref([
  { prop: 'id', label: 'ID', width: 80 },
  { prop: 'name', label: '规则名称', minWidth: 180 },
  { prop: 'type', label: '规则类型', width: 120, slot: 'type' },
  { prop: 'logicIdName', label: '分类', minWidth: 130 },
  { prop: 'checkTable', label: '检查表', minWidth: 130 },
  { prop: 'checkColumn', label: '检查字段', minWidth: 120 },
  { prop: 'statusText', label: '状态', width: 90, slot: 'status' },
  { prop: 'sort', label: '排序', width: 80 },
])

const typeInfo = (type: string) => qualityRuleTypes.find(item => item.type === type)
const refresh = (filter2query: boolean, keepPage = true) => {
  tableRef.value?.clearSelection()
  Object.assign(filters.value, RouteUtil.pagination2filter(pagination.value, keepPage))
  if (filter2query) RouteUtil.filter2query(route, router, filters.value)
  loading.value = true
  QualityRuleApi.list(filters.value).then((result: any) => {
    RouteUtil.result2pagination(pagination.value, result)
    rows.value = ApiUtil.data(result)?.rows ?? []
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}

const formVisible = ref(false)
const formLoading = ref(false)
const formRef = ref<FormInstance>()
const form = ref<any>({ content: {} })
const rules = {
  name: [{ required: true, message: '请输入规则名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择规则类型', trigger: 'change' }],
  logicId: [{ required: true, message: '请选择所属分类', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
}

const add = () => {
  form.value = {
    type: 'EmptyRule',
    logicId: logicTree.value[0]?.id,
    sort: 0,
    status: '1',
    content: ruleDefaultContent('EmptyRule'),
  }
  formVisible.value = true
}

const edit = (row: any) => {
  let content = {}
  try { content = JSON.parse(row.content || '{}') } catch {}
  form.value = Object.assign({}, row, {
    status: String(row.status),
    content,
  })
  formVisible.value = true
}

const changeType = (type: string) => {
  form.value.content = ruleDefaultContent(type)
}

const save = () => {
  formRef.value?.validate((valid: boolean) => {
    if (!valid || formLoading.value) return
    formLoading.value = true
    QualityRuleApi.save(form.value, { success: true }).then(() => {
      formVisible.value = false
      refresh(false)
    }).catch(() => {}).finally(() => {
      formLoading.value = false
    })
  })
}

const remove = () => {
  if (selection.value.length === 0) return
  TableUtil.selection(selection.value).then((ids: any) => {
    loading.value = true
    QualityRuleApi.delete(ids, { success: true }).then(() => refresh(false)).catch(() => {
      loading.value = false
    })
  }).catch(() => {})
}

onMounted(() => {
  refresh(false)
  QualityRuleApi.config({ warning: false }).then((result: any) => {
    const data = ApiUtil.data(result) ?? {}
    config.value = Object.assign({}, config.value, data)
  }).catch(() => {})
  QualityLogicApi.tree({}, { warning: false }).then((result: any) => {
    logicTree.value = ApiUtil.data(result) ?? []
  }).catch(() => {})
})
</script>

<template>
  <GovernPageShell title="质检规则" description="按及时性、一致性、规范性、准确性等维度构建可执行的字段与记录检查规则">
  <el-card shadow="never" class="search-card" v-show="searchable">
    <form-search ref="filterRef" :model="filters">
      <form-search-item label="所属分类" prop="logicId">
        <el-tree-select v-model="filters.logicId" :data="logicTree" node-key="id" :props="{ label: 'name', children: 'children' }" clearable check-strictly default-expand-all />
      </form-search-item>
      <form-search-item label="规则类型" prop="type">
        <el-select v-model="filters.type" clearable placeholder="全部类型">
          <el-option v-for="item in qualityRuleTypes" :key="item.type" :label="item.label" :value="item.type" />
        </el-select>
      </form-search-item>
      <form-search-item label="规则名称" prop="name"><el-input v-model="filters.name" clearable /></form-search-item>
      <form-search-item label="检查表名" prop="checkTable"><el-input v-model="filters.checkTable" clearable /></form-search-item>
      <form-search-item label="检查字段" prop="checkColumn"><el-input v-model="filters.checkColumn" clearable /></form-search-item>
      <form-search-item label="状态" prop="status">
        <el-select v-model="filters.status" clearable placeholder="全部状态">
          <el-option v-for="(value, key) in config.status" :key="key" :label="value" :value="key" />
        </el-select>
      </form-search-item>
      <form-search-item>
        <el-button type="primary" :loading="loading" @click="refresh(true, false)">查询</el-button>
        <el-button @click="filterRef?.resetFields()">重置</el-button>
      </form-search-item>
    </form-search>
  </el-card>
  <el-card shadow="never" class="fs-table-card">
    <div class="fs-table-toolbar flex-between">
      <el-space>
        <button-add v-permit="'govern:qualityRule:add'" @click="add" />
        <button-delete v-permit="'govern:qualityRule:delete'" :disabled="selection.length === 0" @click="remove" />
      </el-space>
      <el-space>
        <button-search @click="searchable = !searchable" />
        <button-refresh :loading="loading" @click="refresh(true)" />
        <TableColumnSetting v-model="columns" :table="tableRef" :loading="loading" />
      </el-space>
    </div>
    <el-table ref="tableRef" v-loading="loading" :data="rows" row-key="id" border @selection-change="(value: any[]) => selection = value">
      <el-table-column type="selection" width="45" />
      <TableColumn :columns="columns">
        <template #type="scope">
          <el-tooltip :content="typeInfo(scope.row.type)?.description || scope.row.type">
            <el-tag effect="plain">{{ typeInfo(scope.row.type)?.label || scope.row.type }}</el-tag>
          </el-tooltip>
        </template>
        <template #status="scope">
          <el-tag :type="scope.row.status === 1 ? 'success' : 'info'" effect="plain">
            {{ scope.row.statusText || config.status?.[scope.row.status] }}
          </el-tag>
        </template>
      </TableColumn>
      <el-table-column label="操作" width="80" fixed="right">
        <template #default="{ row }"><el-button link v-permit="'govern:qualityRule:modify'" @click="edit(row)">编辑</el-button></template>
      </el-table-column>
    </el-table>
    <TablePagination v-model="pagination" :loading="loading" @change="refresh(true)" />
  </el-card>

  <el-drawer v-model="formVisible" :title="form.id ? '编辑质检规则' : '新增质检规则'" size="82%" :destroy-on-close="true">
    <template #header="{ close, titleId, titleClass }">
      <h4 :id="titleId" :class="titleClass">{{ form.id ? `编辑质检规则 - ${form.id}` : '新增质检规则' }}</h4>
      <el-space>
        <el-button type="primary" :loading="formLoading" @click="save">保存</el-button>
        <el-button @click="close">取消</el-button>
      </el-space>
    </template>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
      <el-row :gutter="18">
        <el-col :span="8"><el-form-item label="规则名称" prop="name"><el-input v-model="form.name" /></el-form-item></el-col>
        <el-col :span="8">
          <el-form-item label="规则类型" prop="type">
            <el-select v-model="form.type" @change="changeType">
              <el-option v-for="item in qualityRuleTypes" :key="item.type" :label="`${item.label} - ${item.description}`" :value="item.type" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="所属分类" prop="logicId">
            <el-tree-select v-model="form.logicId" :data="logicTree" node-key="id" :props="{ label: 'name', children: 'children' }" check-strictly default-expand-all />
          </el-form-item>
        </el-col>
        <el-col :span="8"><el-form-item label="排序"><el-input-number v-model="form.sort" :min="0" /></el-form-item></el-col>
        <el-col :span="8">
          <el-form-item label="状态" prop="status">
            <el-select v-model="form.status">
              <el-option v-for="(value, key) in config.status" :key="key" :label="value" :value="key" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="24"><el-form-item label="描述"><el-input v-model="form.description" type="textarea" :rows="2" /></el-form-item></el-col>
      </el-row>
      <QualityRuleFields :rule="form" :config="config" />
      <el-form-item label="整改建议">
        <el-input v-model="form.suggest" type="textarea" :rows="3" placeholder="可引用 ${checkTable}、${checkColumn}、${referTable}、${referColumn}" />
      </el-form-item>
    </el-form>
  </el-drawer>
  </GovernPageShell>
</template>
