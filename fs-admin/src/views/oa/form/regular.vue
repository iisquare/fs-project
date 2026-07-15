<script setup lang="ts">
import { onMounted, ref } from 'vue'
import type { FormInstance, TableInstance } from 'element-plus'
import RouteUtil from '@/utils/RouteUtil'
import { useRoute, useRouter } from 'vue-router'
import ApiUtil from '@/utils/ApiUtil'
import TableUtil from '@/utils/TableUtil'
import FormRegularApi from '@/api/oa/FormRegularApi'

const route = useRoute()
const router = useRouter()
const tableRef = ref<TableInstance>()
const loading = ref(false)
const searchable = ref(true)
const columns = ref([
  { prop: 'id', label: 'ID' },
  { prop: 'name', label: '名称' },
  { prop: 'label', label: '标签' },
  { prop: 'tooltip', label: '提示' },
  { prop: 'sort', label: '排序' },
  { prop: 'statusText', label: '状态' },
])
const config = ref({
  ready: false,
  status: {},
})
const rows = ref([])
const filterRef = ref<FormInstance>()
const filters = ref(RouteUtil.query2filter(route, {}))
const pagination = ref(RouteUtil.pagination(filters.value))
const selection: any = ref([])

const formRef = ref<FormInstance>()
const formVisible = ref(false)
const formLoading = ref(false)
const form = ref<any>({})
const formRules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  label: [{ required: true, message: '请输入标签', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
}

const infoVisible = ref(false)
const infoRow = ref<any>({})

const testVisible = ref(false)
const testLoading = ref(false)
const test = ref({ regex: '', content: '', result: '' })

const handleRefresh = (filter2query: boolean, keepPage: boolean) => {
  tableRef.value?.clearSelection()
  Object.assign(filters.value, RouteUtil.pagination2filter(pagination.value, keepPage))
  filter2query && RouteUtil.filter2query(route, router, filters.value)
  loading.value = true
  FormRegularApi.list(filters.value).then((result: any) => {
    RouteUtil.result2pagination(pagination.value, result)
    rows.value = result.data.rows
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}

onMounted(() => {
  handleRefresh(false, true)
  FormRegularApi.config().then((result: any) => {
    Object.assign(config.value, { ready: true }, ApiUtil.data(result))
  }).catch(() => {})
})

const handleAdd = () => {
  form.value = { name: 'regex:', sort: 0 }
  formVisible.value = true
}

const handleEdit = (scope: any) => {
  form.value = Object.assign({}, scope.row, { status: scope.row.status + '' })
  formVisible.value = true
}

const handleShow = (scope: any) => {
  infoRow.value = Object.assign({}, scope.row, {
    description: scope.row.description ? scope.row.description : '暂无',
  })
  infoVisible.value = true
}

const handleSubmit = () => {
  formRef.value?.validate((valid: boolean) => {
    if (!valid || formLoading.value) return
    formLoading.value = true
    FormRegularApi.save(form.value, { success: true }).then(() => {
      formVisible.value = false
      handleRefresh(false, true)
    }).catch(() => {}).finally(() => {
      formLoading.value = false
    })
  })
}

const handleDelete = () => {
  TableUtil.selection(selection.value).then((ids: any) => {
    loading.value = true
    FormRegularApi.delete(ids, { success: true }).then(() => {
      handleRefresh(false, true)
    }).catch(() => {}).finally(() => {
      loading.value = false
    })
  }).catch(() => {})
}

const handleTest = (scope?: any) => {
  test.value = {
    regex: scope?.row?.regex ?? '',
    content: '',
    result: '',
  }
  testVisible.value = true
}

const handleRunTest = () => {
  if (testLoading.value) return
  testLoading.value = true
  FormRegularApi.test(test.value, { success: true }).then((result: any) => {
    test.value.result = JSON.stringify(result)
  }).catch(() => {}).finally(() => {
    testLoading.value = false
  })
}
</script>

<template>
  <el-card :bordered="false" shadow="never" class="fs-table-search" v-show="searchable">
    <form-search ref="filterRef" :model="filters">
      <form-search-item label="标签" prop="label">
        <el-input v-model="filters.label" clearable />
      </form-search-item>
      <form-search-item label="正则" prop="regex">
        <el-input v-model="filters.regex" clearable />
      </form-search-item>
      <form-search-item label="提示" prop="tooltip">
        <el-input v-model="filters.tooltip" clearable />
      </form-search-item>
      <form-search-item label="名称" prop="name">
        <el-input v-model="filters.name" clearable />
      </form-search-item>
      <form-search-item label="状态" prop="status">
        <el-select v-model="filters.status" placeholder="请选择" clearable>
          <el-option v-for="(value, key) in config.status" :key="key" :value="key" :label="value" />
        </el-select>
      </form-search-item>
      <form-search-item>
        <el-button type="primary" @click="handleRefresh(true, false)">查询</el-button>
        <el-button @click="filterRef?.resetFields()">重置</el-button>
        <el-button @click="() => handleTest()">校验</el-button>
      </form-search-item>
    </form-search>
  </el-card>
  <el-card :bordered="false" shadow="never" class="fs-table-card">
    <div class="fs-table-toolbar flex-between">
      <el-space>
        <button-add v-permit="'oa:formRegular:add'" @click="handleAdd" />
        <button-delete v-permit="'oa:formRegular:delete'" :disabled="selection.length === 0" @click="handleDelete" />
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
      <el-table-column type="selection" />
      <TableColumn :columns="columns" />
      <el-table-column label="操作">
        <template #default="scope">
          <el-button link v-permit="'oa:formRegular:'" @click="() => handleShow(scope)">查看</el-button>
          <el-button link v-permit="'oa:formRegular:modify'" @click="() => handleEdit(scope)">编辑</el-button>
          <el-button link @click="() => handleTest(scope)">校验</el-button>
        </template>
      </el-table-column>
    </el-table>
    <TablePagination v-model="pagination" @change="handleRefresh(true, true)" />
  </el-card>

  <el-dialog v-model="formVisible" :title="form.id ? `信息修改 - ${form.id}` : '信息添加'" :close-on-click-modal="false" width="500">
    <el-form ref="formRef" :model="form" :rules="formRules" label-width="80px">
      <el-form-item label="ID" prop="id">
        <el-input v-model="form.id" :disabled="!!form.id" />
      </el-form-item>
      <el-form-item label="名称" prop="name">
        <el-input v-model="form.name" />
      </el-form-item>
      <el-form-item label="标签" prop="label">
        <el-input v-model="form.label" />
      </el-form-item>
      <el-form-item label="正则" prop="regex">
        <el-input v-model="form.regex" />
      </el-form-item>
      <el-form-item label="提示" prop="tooltip">
        <el-input v-model="form.tooltip" />
      </el-form-item>
      <el-form-item label="排序">
        <el-input-number v-model="form.sort" :min="0" :max="200" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="form.status" placeholder="请选择">
          <el-option v-for="(value, key) in config.status" :key="key" :value="key" :label="value" />
        </el-select>
      </el-form-item>
      <el-form-item label="描述">
        <el-input type="textarea" v-model="form.description" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="formVisible = false">取消</el-button>
      <el-button type="primary" @click="handleSubmit" :loading="formLoading">确定</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="infoVisible" :title="`信息查看 - ${infoRow.id}`" width="500">
    <el-descriptions :column="1" border>
      <el-descriptions-item label="名称">{{ infoRow.name }}</el-descriptions-item>
      <el-descriptions-item label="标签">{{ infoRow.label }}</el-descriptions-item>
      <el-descriptions-item label="正则">{{ infoRow.regex }}</el-descriptions-item>
      <el-descriptions-item label="提示">{{ infoRow.tooltip }}</el-descriptions-item>
      <el-descriptions-item label="排序">{{ infoRow.sort }}</el-descriptions-item>
      <el-descriptions-item label="状态">{{ infoRow.statusText }}</el-descriptions-item>
      <el-descriptions-item label="描述">{{ infoRow.description }}</el-descriptions-item>
      <el-descriptions-item label="创建者">{{ infoRow.createdUidName }}</el-descriptions-item>
      <el-descriptions-item label="创建时间">{{ infoRow.createdTime }}</el-descriptions-item>
      <el-descriptions-item label="修改者">{{ infoRow.updatedUidName }}</el-descriptions-item>
      <el-descriptions-item label="修改时间">{{ infoRow.updatedTime }}</el-descriptions-item>
    </el-descriptions>
  </el-dialog>

  <el-dialog v-model="testVisible" title="正则校验" :close-on-click-modal="false" width="600">
    <el-form label-width="60px">
      <el-form-item label="正则">
        <el-input type="textarea" v-model="test.regex" clearable :rows="2" />
      </el-form-item>
      <el-form-item label="内容">
        <el-input type="textarea" v-model="test.content" clearable :rows="3" />
      </el-form-item>
      <el-form-item label="结果">
        <el-input type="textarea" v-model="test.result" :rows="3" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="testVisible = false">取消</el-button>
      <el-button type="primary" @click="handleRunTest" :loading="testLoading">校验</el-button>
    </template>
  </el-dialog>
</template>

<style lang="scss" scoped>
</style>
