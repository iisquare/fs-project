<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { FormInstance, TableInstance } from 'element-plus'
import { Sort } from '@element-plus/icons-vue'
import ModelRelationApi from '@/api/govern/ModelRelationApi'
import ApiUtil from '@/utils/ApiUtil'
import RouteUtil from '@/utils/RouteUtil'
import TableUtil from '@/utils/TableUtil'
import GovernPageShell from '@/views/govern/components/GovernPageShell.vue'

const route = useRoute()
const router = useRouter()
const tableRef = ref<TableInstance>()
const loading = ref(false)
const searchable = ref(true)
const rows = ref<any[]>([])
const selection = ref<any[]>([])
const filters = ref<any>(RouteUtil.query2filter(route, {
  advanced: false,
  page: 1,
  pageSize: 30,
}))
const pagination = ref<any>(RouteUtil.pagination(filters.value))
const filterRef = ref<FormInstance>()
const config = ref<any>({ relations: {} })
const columns = ref([
  { prop: 'sourceCatalog', label: '来源包', minWidth: 130 },
  { prop: 'sourceModel', label: '来源模型', minWidth: 130 },
  { prop: 'sourceColumn', label: '来源字段', minWidth: 120, formatter: (row: any) => row.sourceColumn || '模型级' },
  { prop: 'relation', label: '关系', width: 110, slot: 'relation' },
  { prop: 'targetCatalog', label: '目标包', minWidth: 130 },
  { prop: 'targetModel', label: '目标模型', minWidth: 130 },
  { prop: 'targetColumn', label: '目标字段', minWidth: 120, formatter: (row: any) => row.targetColumn || '模型级' },
])

const refresh = (filter2query: boolean, keepPage = true) => {
  tableRef.value?.clearSelection()
  Object.assign(filters.value, RouteUtil.pagination2filter(pagination.value, keepPage))
  if (filter2query) RouteUtil.filter2query(route, router, filters.value)
  loading.value = true
  ModelRelationApi.list(filters.value).then((result: any) => {
    RouteUtil.result2pagination(pagination.value, result)
    rows.value = ApiUtil.data(result)?.rows ?? []
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}

const exchange = () => {
  const source = {
    catalog: filters.value.sourceCatalog,
    model: filters.value.sourceModel,
    column: filters.value.sourceColumn,
  }
  filters.value.sourceCatalog = filters.value.targetCatalog
  filters.value.sourceModel = filters.value.targetModel
  filters.value.sourceColumn = filters.value.targetColumn
  filters.value.targetCatalog = source.catalog
  filters.value.targetModel = source.model
  filters.value.targetColumn = source.column
}

const formVisible = ref(false)
const formLoading = ref(false)
const formRef = ref<FormInstance>()
const form = ref<any>({})
const rules = {
  sourceCatalog: [{ required: true, message: '请输入来源包', trigger: 'blur' }],
  sourceModel: [{ required: true, message: '请输入来源模型', trigger: 'blur' }],
  relation: [{ required: true, message: '请选择关系类型', trigger: 'change' }],
  targetCatalog: [{ required: true, message: '请输入目标包', trigger: 'blur' }],
  targetModel: [{ required: true, message: '请输入目标模型', trigger: 'blur' }],
}

const edit = (row: any = {}) => {
  form.value = Object.assign({
    relation: 'depend',
    sourceCatalog: '/',
    targetCatalog: '/',
  }, row)
  formVisible.value = true
}

const save = () => {
  formRef.value?.validate((valid: boolean) => {
    if (!valid || formLoading.value) return
    formLoading.value = true
    ModelRelationApi.save(form.value, { success: true }).then(() => {
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
    ModelRelationApi.delete(ids, { success: true }).then(() => refresh(false)).catch(() => {
      loading.value = false
    })
  }).catch(() => {})
}

const relationName = (key: string) => config.value.relations?.[key]?.name || config.value.relations?.[key] || key

onMounted(() => {
  refresh(false)
  ModelRelationApi.config({ warning: false }).then((result: any) => {
    config.value = ApiUtil.data(result) ?? config.value
  }).catch(() => {})
})
</script>

<template>
  <GovernPageShell title="模型关系" description="维护模型与字段之间的继承、依赖、关联等关系，为血缘和影响分析提供依据">
  <el-card shadow="never" class="search-card" v-show="searchable">
    <form-search ref="filterRef" :model="filters">
      <form-search-item label="所属包" prop="catalog">
        <el-input v-model="filters.catalog" clearable />
      </form-search-item>
      <form-search-item label="模型编码" prop="model">
        <el-input v-model="filters.model" clearable />
      </form-search-item>
      <form-search-item label="字段编码" prop="column">
        <el-input v-model="filters.column" clearable />
      </form-search-item>
      <form-search-item label="关系类型" prop="relation">
        <el-select v-model="filters.relation" clearable placeholder="全部关系">
          <el-option v-for="(value, key) in config.relations" :key="key" :label="`${key} - ${value.name || value}`" :value="key" />
        </el-select>
      </form-search-item>
      <form-search-item label="来源包" prop="sourceCatalog">
        <el-input v-model="filters.sourceCatalog" clearable />
      </form-search-item>
      <form-search-item label="来源模型" prop="sourceModel">
        <el-input v-model="filters.sourceModel" clearable />
      </form-search-item>
      <form-search-item label="来源字段" prop="sourceColumn">
        <el-input v-model="filters.sourceColumn" clearable />
      </form-search-item>
      <form-search-item label="目标包" prop="targetCatalog">
        <el-input v-model="filters.targetCatalog" clearable />
      </form-search-item>
      <form-search-item label="目标模型" prop="targetModel">
        <el-input v-model="filters.targetModel" clearable />
      </form-search-item>
      <form-search-item label="目标字段" prop="targetColumn">
        <el-input v-model="filters.targetColumn" clearable />
      </form-search-item>
      <form-search-item>
        <el-button type="primary" :loading="loading" @click="refresh(true, false)">查询</el-button>
        <el-button :icon="Sort" @click="exchange">交换</el-button>
        <el-button @click="filterRef?.resetFields()">重置</el-button>
      </form-search-item>
    </form-search>
  </el-card>

  <el-card shadow="never" class="fs-table-card">
    <div class="fs-table-toolbar flex-between">
      <el-space>
        <button-add v-permit="'govern:modelRelation:add'" @click="edit()" />
        <button-delete v-permit="'govern:modelRelation:delete'" :disabled="selection.length === 0" @click="remove" />
      </el-space>
      <el-space>
        <button-search @click="searchable = !searchable" />
        <button-refresh :loading="loading" @click="refresh(true)" />
        <TableColumnSetting v-model="columns" :table="tableRef" :loading="loading" />
      </el-space>
    </div>
    <el-table
      ref="tableRef"
      v-loading="loading"
      :data="rows"
      row-key="id"
      border
      @selection-change="(value: any[]) => selection = value"
    >
      <el-table-column type="selection" width="45" />
      <TableColumn :columns="columns">
        <template #relation="scope">
          <el-tag effect="plain">{{ relationName(scope.row.relation) }}</el-tag>
        </template>
      </TableColumn>
      <el-table-column label="描述" prop="description" min-width="180" show-overflow-tooltip />
      <el-table-column label="操作" width="90" fixed="right">
        <template #default="{ row }">
          <el-button link v-permit="'govern:modelRelation:modify'" @click="edit(row)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>
    <TablePagination v-model="pagination" :loading="loading" @change="refresh(true)" />
  </el-card>

  <el-dialog v-model="formVisible" :title="form.id ? '编辑模型关系' : '新增模型关系'" width="760px">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
      <el-row :gutter="18">
        <el-col :span="8">
          <el-form-item label="来源包" prop="sourceCatalog">
            <el-input v-model="form.sourceCatalog" placeholder="/domain/" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="来源模型" prop="sourceModel">
            <el-input v-model="form.sourceModel" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="来源字段">
            <el-input v-model="form.sourceColumn" placeholder="模型关系可留空" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="关系类型" prop="relation">
            <el-select v-model="form.relation">
              <el-option v-for="(value, key) in config.relations" :key="key" :label="`${key} - ${value.name || value}`" :value="key" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="目标包" prop="targetCatalog">
            <el-input v-model="form.targetCatalog" placeholder="/domain/" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="目标模型" prop="targetModel">
            <el-input v-model="form.targetModel" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="目标字段">
            <el-input v-model="form.targetColumn" placeholder="模型关系可留空" />
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="描述">
            <el-input v-model="form.description" type="textarea" :rows="3" />
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>
    <template #footer>
      <el-space>
        <el-button type="primary" :loading="formLoading" @click="save">保存</el-button>
        <el-button @click="formVisible = false">取消</el-button>
      </el-space>
    </template>
  </el-dialog>
  </GovernPageShell>
</template>
