<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import type { FormInstance, TableInstance } from 'element-plus';
import RouteUtil from '@/utils/RouteUtil'
import { useRoute, useRouter } from 'vue-router';
import TableUtil from '@/utils/TableUtil';
import { useUserStore } from '@/stores/user';
import Neo4jApi from '@/api/kg/Neo4jApi';
import DateUtil from '@/utils/DateUtil';

const route = useRoute()
const router = useRouter()
const tableRef = ref<TableInstance>()
const loading = ref(false)
const searchable = ref(true)
const columns = ref([
  { prop: 'id', label: 'ID' },
  { prop: 'name', label: '名称' },
  { prop: 'labelsOrTypes', label: '标签' },
  { prop: 'properties', label: '属性' },
  { prop: 'entityType', label: '本体类型' },
  { prop: 'owningConstraint', label: '约束' },
  { prop: 'state', label: '状态', hide: true },
  { prop: 'populationPercent', label: '流行度', hide: true },
  { prop: 'type', label: '索引类型', hide: true },
  { prop: 'indexProvider', label: '提供者', hide: true },
  { prop: 'lastRead', label: '最后读取', formatter: DateUtil.second },
  { prop: 'readCount', label: '读取数量' },
])
const rows = ref([])
const filterRef = ref<FormInstance>()
const filters = ref(RouteUtil.query2filter(route, { advanced: false, agentIds: [] }))
const selection: any = ref(null)
const handleRefresh = (filter2query: boolean, keepPage: boolean) => {
  selection.value = null
  filter2query && RouteUtil.filter2query(route, router, filters.value)
  loading.value = true
  Neo4jApi.showIndex(filters.value).then((result: any) => {
    rows.value = result.data
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}
onMounted(() => {
  handleRefresh(false, true)
})
const indexTypes = {
  RANGE: '范围索引',
  TEXT: '文本索引',
  POINT: '点索引',
  LOOKUP: '令牌查找索引',
}
const handleGenerate = () => {
  form.value.name = ['index', form.value.ontologyType, form.value.label, ...form.value.fields].join('_').toLowerCase()
}
const formVisible = ref(false)
const formLoading = ref(false)
const form: any = ref({ fields: [] })
const formRef: any = ref<FormInstance>()
const rules = computed(() => {
  return Object.assign({
    ontologyType: [{ required: true, message: '请选择本体类型', trigger: 'change' }],
    name: [{ required: true, message: '请输入索引名称', trigger: 'blur' }],
  }, form.value.indexType === 'LOOKUP' ? {} : {
    label: [{ required: true, message: '请输入索引标签', trigger: 'blur' }],
    fields: [{ required: true, message: '请输入索引字段', trigger: 'change' }],
  })
})
const handleAdd = () => {
  form.value = { ontologyType: 'NODE', fields: [] }
  formVisible.value = true
}
const handleSubmit = () => {
  formRef.value?.validate((valid: boolean) => {
    if (!valid || formLoading.value) return
    formLoading.value = true
    Neo4jApi.createIndex(form.value, { success: true }).then(result => {
      handleRefresh(false, true)
      formVisible.value = false
    }).catch(() => {}).finally(() => {
      formLoading.value = false
    })
  })
}
const handleDelete = () => {
  TableUtil.confirm().then(() => {
    const record: any = rows.value[selection.value]
    loading.value = true
    Neo4jApi.dropIndex({ name: record.name }, { success: true }).then(() => {
      handleRefresh(false, true)
    }).catch(() => {
      loading.value = false
    })
  }).catch(() => {})
}
</script>

<template>
  <el-card :bordered="false" shadow="never" class="fs-table-search" v-show="searchable">
    <form-search ref="filterRef" :model="filters">
      <form-search-item label="名称" prop="name">
        <el-input v-model="filters.name" clearable />
      </form-search-item>
      <form-search-item label="标签" prop="label">
        <el-input v-model="filters.label" clearable />
      </form-search-item>
      <form-search-item label="类型" prop="type">
        <el-input v-model="filters.type" clearable />
      </form-search-item>
      <form-search-item>
        <el-button type="primary" @click="handleRefresh(true, false)">查询</el-button>
        <el-button @click="filterRef?.resetFields()">重置</el-button>
      </form-search-item>
    </form-search>
  </el-card>
  <el-card :bordered="false" shadow="never" class="fs-table-card pb-20">
    <div class="fs-table-toolbar flex-between">
      <el-space>
        <button-add v-permit="'kg:neo4j:'" @click="handleAdd" />
        <button-delete v-permit="'kg:neo4j:'" :disabled="selection === null" @click="handleDelete" />
      </el-space>
      <el-space>
        <button-search @click="searchable = !searchable" />
        <button-refresh @click="handleRefresh(true, true)" :loading="loading" />
        <TableColumnSetting v-model="columns" :table="tableRef" />
      </el-space>
    </div>
    <el-table
      ref="tableRef"
      :data="rows"
      :row-key="(record: any) => record.id"
      :border="true"
      v-loading="loading"
      table-layout="auto"
      @selection-change="(newSelection: any) => selection = newSelection"
    >
      <TableRadio v-model="selection" :value="(scope: any) => scope.$index" width="60px" />
      <el-table-column type="expand">
        <template #default="scope">
          <el-descriptions border label-width="130px">
            <el-descriptions-item label="创建语句" :span="3">{{ scope.row.createStatement }}</el-descriptions-item>
          </el-descriptions>
        </template>
      </el-table-column>
      <TableColumn :columns="columns">
        <template #role="scope">
          <el-space><el-tag v-for="item in scope.row.roles" :key="item.id">{{ item.name }}</el-tag></el-space>
        </template>
      </TableColumn>
    </el-table>
  </el-card>
  <el-drawer v-model="formVisible" :close-on-click-modal="false" :show-close="false" :destroy-on-close="true">
    <template #header="{ close, titleId, titleClass }">
      <h4 :id="titleId" :class="titleClass">创建索引</h4>
      <el-space>
        <el-button type="primary" @click="handleSubmit" :loading="formLoading">确定</el-button>
        <el-button @click="close">取消</el-button>
      </el-space>
    </template>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="auto">
      <el-form-item label="索引类型" prop="indexType">
        <el-select v-model="form.indexType" placeholder="自动选择" clearable>
          <el-option v-for="(value, key) in indexTypes" :key="key" :value="key" :label="`${key} - ${value}`" />
        </el-select>
      </el-form-item>
      <el-form-item label="本体类型" prop="ontologyType">
        <el-radio-group v-model="form.ontologyType">
          <el-radio-button label="节点" value="NODE" />
          <el-radio-button label="关系" value="REL" />
        </el-radio-group>
      </el-form-item>
      <el-form-item label="索引名称" prop="name">
        <el-input v-model="form.name">
          <template #append><el-button @click="handleGenerate">生成</el-button></template>
        </el-input>
      </el-form-item>
       <el-divider content-position="left">非LOOKUP索引时填写</el-divider>
      <el-form-item label="索引标签" prop="label">
        <el-input v-model="form.label" />
      </el-form-item>
      <el-form-item label="索引字段" prop="fields">
        <el-select v-model="form.fields" multiple filterable allow-create :reserve-keyword="false" placeholder="输入创建字段" />
      </el-form-item>
    </el-form>
  </el-drawer>
</template>

<style lang="scss" scoped>
</style>
