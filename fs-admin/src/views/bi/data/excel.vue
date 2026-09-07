<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import type { FormInstance, TableInstance, UploadRawFile } from 'element-plus';
import * as ElementPlusIcons from '@element-plus/icons-vue';
import RouteUtil from '@/utils/RouteUtil'
import { useRoute, useRouter } from 'vue-router';
import DataExcelApi from '@/api/bi/DataExcelApi';
import ApiUtil from '@/utils/ApiUtil';
import DateUtil from '@/utils/DateUtil';
import TableUtil from '@/utils/TableUtil';
import DataSchemaTable from '@/components/Data/DataSchemaTable.vue';
import DataFieldSelect from '@/components/Data/DataFieldSelect.vue';

const route = useRoute()
const router = useRouter()
const tableRef = ref<TableInstance>()
const loading = ref(false)
const searchable = ref(true)
const columns = ref([
  { prop: 'id', label: 'ID' },
  { prop: 'name', label: '名称' },
  { prop: 'pks', label: '主键', slot: 'pks' },
  { prop: 'sort', label: '排序' },
  { prop: 'statusText', label: '状态' },
  { prop: 'description', label: '描述', hide: true },
])
const config: any = ref({
  ready: false,
  modes: {},
  status: {},
  fieldTypes: [],
})
const rows = ref([])
const filterRef = ref<FormInstance>()
const filters = ref(RouteUtil.query2filter(route, { advanced: false, roleIds: [] }))
const pagination = ref(RouteUtil.pagination(filters.value))
const selection: any = ref([])
const handleRefresh = (filter2query: boolean, keepPage: boolean) => {
  tableRef.value?.clearSelection()
  Object.assign(filters.value, RouteUtil.pagination2filter(pagination.value, keepPage))
  filter2query && RouteUtil.filter2query(route, router, filters.value)
  loading.value = true
  DataExcelApi.list(filters.value).then((result: any) => {
    RouteUtil.result2pagination(pagination.value, result)
    rows.value = result.data.rows
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}
onMounted(() => {
  handleRefresh(false, true)
  DataExcelApi.config().then((result: any) => {
    Object.assign(config.value, { ready: true }, ApiUtil.data(result))
  }).catch(() => {})
})
const infoVisible = ref(false)
const formVisible = ref(false)
const formLoading = ref(false)
const form: any = ref({})
const formRef: any = ref<FormInstance>()
const rules = ref({
  name: [{ required: true, message: '请输入数据集名称', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
})
const handleAdd = () => {
  form.value = {
    status: '1',
    fields: [],
  }
  formVisible.value = true
}
const handleShow = (scope: any) => {
  form.value = Object.assign({}, scope.row)
  infoVisible.value = true
}
const handleEdit = (scope: any) => {
  form.value = Object.assign({}, scope.row, {
    status: scope.row.status + '',
    labels: scope.row.labels || [],
    roleIds: scope.row.roleIds || [],
  })
  formVisible.value = true
}
const handleSubmit = () => {
  formRef.value?.validate((valid: boolean) => {
    if (!valid || formLoading.value) return
    formLoading.value = true
    DataExcelApi.save(form.value, { success: true }).then((result: any) => {
      Object.assign(form.value, {
        id: ApiUtil.data(result).id,
      })
      handleRefresh(false, true)
    }).catch(() => {}).finally(() => {
      formLoading.value = false
    })
  })
}
const handleDelete = () => {
  TableUtil.selection(selection.value).then((ids: any) => {
    loading.value = true
    DataExcelApi.delete(ids, { success: true }).then(() => {
      handleRefresh(false, true)
    }).catch(() => {
      loading.value = false
    })
  }).catch(() => {})
}
const active = ref('table')
const mode = ref('overwrite')
const tableDataRef = ref<TableInstance>()
const dataLoading = ref(false)
const dataRows = ref([])
const dataSelection = ref([])
const dataPagination = ref(RouteUtil.pagination({}))
const dataColumns = computed(() => {
  return form.value.fields?.map((item: any) => {
    return { prop: item.name, label: item.title }
  }) || []
})
watch(() => [form.value.id, active.value], () => {
  if (form.value.id && active.value === 'data') {
    handleDataRefresh()
  }
})
const handleBeforeUpload = async (rawFile: UploadRawFile) => {
  if (dataLoading.value) return
  dataLoading.value = true
  DataExcelApi.upload({ id: form.value.id, mode: mode.value, file: rawFile }, { success: true }).then(() => {
    handleDataRefresh()
  }).catch(() => {
    dataLoading.value = false
  })
  return false
}
const handleDataRefresh = () => {
  tableDataRef.value?.clearSelection()
  dataLoading.value = true
  const params = Object.assign({ id: form.value.id }, RouteUtil.pagination2filter(dataPagination.value, true))
  DataExcelApi.dataList(params).then((result: any) => {
    RouteUtil.result2pagination(dataPagination.value, result)
    dataRows.value = result.data.rows
  }).catch(() => {}).finally(() => {
    dataLoading.value = false
  })
}
const handleDataDelete = () => {
  TableUtil.selection(dataSelection.value, '_id').then((ids: any) => {
    dataLoading.value = true
    DataExcelApi.dataDelete(form.value.id, ids, { success: true }).then(() => {
      handleDataRefresh()
    }).catch(() => {
      dataLoading.value = false
    })
  }).catch(() => {})
}
const handleTemplate = (scope: any) => {
  window.open(`${import.meta.env.VITE_APP_API_URL}/bi/dataExcel/template?id=${scope.row.id}`, '_blank')
}
</script>

<template>
  <el-card :bordered="false" shadow="never" class="fs-table-search" v-show="searchable">
    <form-search ref="filterRef" :model="filters">
      <form-search-item label="名称" prop="name">
        <el-input v-model="filters.name" clearable />
      </form-search-item>
      <form-search-item label="状态" prop="status">
        <el-select v-model="filters.status" placeholder="请选择" clearable>
          <el-option v-for="(value, key) in config.status" :key="key" :value="key" :label="value" />
        </el-select>
      </form-search-item>
      <form-search-item>
        <el-button type="primary" @click="handleRefresh(true, false)" :loading="loading">查询</el-button>
        <el-button @click="filterRef?.resetFields()">重置</el-button>
      </form-search-item>
    </form-search>
  </el-card>
  <el-card :bordered="false" shadow="never" class="fs-table-card">
    <div class="fs-table-toolbar flex-between">
      <el-space>
        <button-add v-permit="'bi:dataExcel:add'" @click="handleAdd" />
        <button-delete v-permit="'bi:dataExcel:delete'" :disabled="selection.length === 0" @click="handleDelete" />
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
      @selection-change="(s: any) => selection = s"
    >
      <el-table-column type="selection" />
      <TableColumn :columns="columns">
        <template #pks="scope">
          <DataFieldSelect :model-value="scope.row.pks" multiple />
        </template>
        <template #labels="scope">
          <el-space><el-tag v-for="item in scope.row.labels" :key="item">{{ item }}</el-tag></el-space>
        </template>
        <template #role="scope">
          <el-space><el-tag v-for="item in scope.row.roles" :key="item.id">{{ item.name }}</el-tag></el-space>
        </template>
      </TableColumn>
      <el-table-column label="操作">
        <template #default="scope">
          <el-button link @click="handleShow(scope)" v-permit="'bi:dataExcel:'">查看</el-button>
          <el-button link @click="handleEdit(scope)" v-permit="'bi:dataExcel:modify'">编辑</el-button>
          <el-button link @click="handleTemplate(scope)" v-permit="'bi:dataExcel:'">下载模板</el-button>
        </template>
      </el-table-column>
    </el-table>
    <TablePagination v-model="pagination" @change="handleRefresh(true, true)" />
  </el-card>
  <el-drawer v-model="infoVisible" :title="'信息查看 - ' + form.id" size="60%">
    <layout-heading title="基础信息" />
    <el-descriptions :column="2" label-width="120px" border>
      <el-descriptions-item label="名称" :span="2">{{ form.name }}</el-descriptions-item>
      <el-descriptions-item label="状态">{{ form.statusText }}</el-descriptions-item>
      <el-descriptions-item label="排序">{{ form.sort }}</el-descriptions-item>
      <el-descriptions-item label="描述" :span="2">{{ form.description || '暂无' }}</el-descriptions-item>
      <el-descriptions-item label="创建者">{{ form.createdUserInfo?.name }}</el-descriptions-item>
      <el-descriptions-item label="创建时间">{{ DateUtil.format(form.createdTime) }}</el-descriptions-item>
      <el-descriptions-item label="修改者">{{ form.updatedUserInfo?.name }}</el-descriptions-item>
      <el-descriptions-item label="修改时间">{{ DateUtil.format(form.updatedTime) }}</el-descriptions-item>
      <el-descriptions-item label="主键" :span="2">
        <DataFieldSelect v-if="form.pks?.length" :model-value="form.pks" multiple />
        <span v-else>暂无</span>
      </el-descriptions-item>
    </el-descriptions>
    <layout-heading title="字段配置" style="margin-top:15px" />
    <DataSchemaTable v-model="form.fields" :types="config.fieldTypes" />
  </el-drawer>
  <el-drawer v-model="formVisible" :close-on-click-modal="false" :show-close="false" :destroy-on-close="true" size="60%">
    <template #header="{ close, titleId, titleClass }">
      <h4 :id="titleId" :class="titleClass">{{ '信息' + (form.id ? ('修改 - ' + form.id) : '添加') }}</h4>
      <el-space>
        <el-button type="primary" @click="handleSubmit" :loading="formLoading">保存</el-button>
        <el-button @click="close">关闭</el-button>
      </el-space>
    </template>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="auto">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="名称" prop="name" :span="2">
          <el-input v-model="form.name" />
        </el-descriptions-item>
        <el-descriptions-item label="排序">
          <form-input-number v-model="form.sort" />
        </el-descriptions-item>
        <el-descriptions-item label="状态" prop="status">
          <el-select v-model="form.status" placeholder="请选择">
            <el-option v-for="(value, key) in config.status" :key="key" :value="key" :label="value" />
          </el-select>
        </el-descriptions-item>
        <el-descriptions-item label="描述" :span="2">
          <el-input type="textarea" v-model="form.description" />
        </el-descriptions-item>
        <el-descriptions-item label="主键" :span="2">
          <DataFieldSelect v-model="form.pks" v-model:fields="form.fields" editable multiple />
        </el-descriptions-item>
      </el-descriptions>
    </el-form>
    <el-tabs v-model="active">
      <el-tab-pane label="字段列表" name="table">
        <DataSchemaTable v-model="form.fields" :types="config.fieldTypes" editable />
      </el-tab-pane>
      <el-tab-pane label="字段编辑器" name="schema">
        <DataSchemaText v-model="form.fields" :types="config.fieldTypes" />
      </el-tab-pane>
      <el-tab-pane label="数据管理" name="data" class="fs-table-card">
        <div class="fs-table-toolbar flex-between">
          <el-space>
            <button-delete :disabled="dataSelection.length === 0" @click="handleDataDelete" />
          </el-space>
          <el-space>
            <el-radio-group v-model="mode">
              <el-radio v-for="(value, key) in config.modes" :key="key" :value="key" :label="value" />
            </el-radio-group>
            <el-upload :show-file-list="false" :before-upload="handleBeforeUpload" :disabled="dataLoading" accept=".xlsx,.xls">
              <el-button type="primary" :loading="dataLoading" :icon="ElementPlusIcons.UploadFilled">上传Excel</el-button>
            </el-upload>
          </el-space>
        </div>
        <el-table
          ref="tableDataRef"
          :data="dataRows"
          :row-key="(record: any) => record._id"
          :border="true"
          v-loading="dataLoading"
          table-layout="auto"
          @selection-change="(s: any) => dataSelection = s"
        >
          <el-table-column type="selection" />
          <TableColumn :columns="dataColumns"></TableColumn>
        </el-table>
        <TablePagination v-model="dataPagination" @change="handleDataRefresh" />
      </el-tab-pane>
    </el-tabs>
  </el-drawer>
</template>

<style lang="scss" scoped>
.el-descriptions {
  margin-bottom: 15px;
}
</style>
