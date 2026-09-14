<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import type { FormInstance, TableInstance } from 'element-plus'
import { ElMessage } from 'element-plus'
import * as ElementPlusIcons from '@element-plus/icons-vue'
import RouteUtil from '@/utils/RouteUtil'
import { useRoute, useRouter } from 'vue-router'
import ApiUtil from '@/utils/ApiUtil'
import TableUtil from '@/utils/TableUtil'
import FormApi from '@/api/oa/FormApi'
import config from '@/designer/FlexForm/config'
import FlexForm from '@/designer/FlexForm/FlexForm.vue'
import ListFilter from '@/designer/FlexForm/ListFilter.vue'
import ListSorter from '@/designer/FlexForm/ListSorter.vue'
import ListViewer from '@/designer/FlexForm/ListViewer.vue'

const route = useRoute()
const router = useRouter()
const tableRef = ref<TableInstance>()
const loading = ref(false)
const columns = ref<any[]>([])
const filterRef = ref<FormInstance>()
const filters = ref<any>({ condition: [], sort: [], column: [] })
const pagination = ref(RouteUtil.pagination(filters.value))
const selection: any = ref([])
const formRows = ref<any[]>([])
const frame = ref<any>({ id: 0, name: '未就绪', widgets: [], options: {} })
const fields = ref<any>({ filter: [], sorter: [], viewer: [] })
const authority = ref<any>(config.exhibition.authorityEmpty())

const infoVisible = ref(false)
const formVisible = ref(false)
const formLoading = ref(false)
const formRef = ref<any>()
const form = ref<any>({})

const rows = computed(() => {
  const widgets = frame.value?.widgets
  if (!widgets) return []
  const all = [config.idField].concat(widgets, config.reservedFields)
  return config.validator.pretty(all, formRows.value)
})

const handleRefresh = (filter2query: boolean, keepPage: boolean) => {
  columns.value = config.exhibition.tableColumns(filters.value.column)
  tableRef.value?.clearSelection()
  Object.assign(filters.value, RouteUtil.pagination2filter(pagination.value, keepPage))
  filter2query && RouteUtil.filter2query(route, router, filters.value)
  loading.value = true
  FormApi.list(Object.assign({}, filters.value, { frameId: frame.value.id })).then((result: any) => {
    RouteUtil.result2pagination(pagination.value, result)
    formRows.value = result.data.rows
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}

const handleLoad = () => {
  loading.value = true
  FormApi.frame({ id: route.query.id }).then((result: any) => {
    if (!result.data) return
    const data = ApiUtil.data(result)
    frame.value = Object.assign({}, data, {
      widgets: data.widgets || [],
      options: data.options || {},
    })
    const widgets = frame.value.widgets
    const reserved = config.reservedFields
    fields.value.filter = config.exhibition.operateFields(widgets, 'filterable', false)
      .concat(config.exhibition.operateFields(reserved, 'filterable', false))
    fields.value.sorter = config.exhibition.operateFields(widgets, 'sortable', false)
      .concat(config.exhibition.operateFields(reserved, 'sortable', false))
    fields.value.viewer = config.exhibition.operateFields(widgets, 'viewable', true)
      .concat(config.exhibition.operateFields(reserved, 'viewable', true))
    let pageSize = 5
    if (frame.value.options.pageSize > 0) pageSize = frame.value.options.pageSize
    Object.assign(filters.value, RouteUtil.query2filter(route, { page: 1, pageSize }))
    Object.assign(pagination.value, RouteUtil.pagination(filters.value))
    if (route.query[RouteUtil.filterKey]) { // 采用地址栏中的筛选配置
      filters.value.column = config.exhibition.mergeColumnItem(fields.value.viewer, filters.value.column)
      filters.value.condition = filters.value.condition || []
      filters.value.sort = filters.value.sort || []
    } else { // 采用表单默认配置
      filters.value.sort = config.exhibition.parseSortor(frame.value.options.sort)
      filters.value.column = config.exhibition.mergeColumnItem(fields.value.viewer,
        config.exhibition.parseColumnSorted(frame.value.options.column))
    }
    const authorityFields = config.exhibition.authorityFields(widgets)
    authority.value = config.exhibition.authorityEmpty()
    authority.value.fields = authorityFields
    authority.value.view = config.exhibition.authority(authorityFields, {}, { viewable: true })
    authority.value.edit = config.exhibition.authority(authorityFields, {}, Object.fromEntries(new Map(
      Object.entries(config.exhibition.authorityDefaults).map((item: any) => [item[0], true])
    )))
    handleRefresh(false, true)
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}

onMounted(() => {
  handleLoad()
})

const handleAdd = () => {
  form.value = {}
  formVisible.value = true
}

const handleEdit = (scope: any) => {
  form.value = formRows.value[scope.$index]
  formVisible.value = true
}

const handleShow = (scope: any) => {
  form.value = formRows.value[scope.$index]
  infoVisible.value = true
}

const handleSubmit = (force: boolean) => {
  formRef.value?.validate((valid: boolean) => {
    if (!valid) {
      ElMessage.warning('数据校验不通过')
      if (!force) return
    }
    if (formLoading.value) return
    formLoading.value = true
    FormApi.save({ frameId: frame.value.id, form: form.value }, { success: true }).then(() => {
      formVisible.value = false
      handleRefresh(false, true)
    }).catch(() => {}).finally(() => {
      formLoading.value = false
    })
  })
}

const handleDelete = () => {
  TableUtil.selection(selection.value, '_id').then((ids: any) => {
    loading.value = true
    FormApi.delete({ frameId: frame.value.id, ids }, { success: true }).then(() => {
      handleRefresh(false, true)
    }).catch(() => {
      loading.value = false
    })
  }).catch(() => {})
}
</script>

<template>
  <el-card :bordered="false" shadow="never" class="fs-table-search">
    <form-search ref="filterRef" :model="filters">
      <form-search-item :label="frame.name">
        <el-space>
          <el-popover placement="bottom-start" trigger="click" :width="820">
            <template #reference>
              <el-button :icon="ElementPlusIcons.Filter">筛选条件</el-button>
            </template>
            <ListFilter v-model="filters.condition" :fields="fields.filter" :config="config" />
          </el-popover>
          <el-popover placement="bottom-start" trigger="click" :width="430">
            <template #reference>
              <el-button :icon="ElementPlusIcons.Sort">排序字段</el-button>
            </template>
            <ListSorter v-model="filters.sort" :fields="fields.sorter" :config="config" />
          </el-popover>
          <el-popover placement="bottom-start" trigger="click" :width="400">
            <template #reference>
              <el-button :icon="ElementPlusIcons.View">展示字段</el-button>
            </template>
            <ListViewer v-model="filters.column" :fields="fields.viewer" :config="config" />
          </el-popover>
          <el-button type="primary" :icon="ElementPlusIcons.Search" @click="handleRefresh(true, false)" :loading="loading">查询</el-button>
        </el-space>
      </form-search-item>
    </form-search>
  </el-card>
  <el-card :bordered="false" shadow="never" class="fs-table-card">
    <div class="fs-table-toolbar flex-between">
      <el-space>
        <button-add v-permit="'oa:formData:add'" @click="handleAdd" />
        <button-delete v-permit="'oa:formData:delete'" :disabled="selection.length === 0" @click="handleDelete" />
      </el-space>
      <el-space>
        <button-refresh @click="handleRefresh(true, true)" :loading="loading" />
      </el-space>
    </div>
    <el-table
      ref="tableRef"
      :data="rows"
      :row-key="(record: any) => record._id"
      :border="true"
      v-loading="loading"
      table-layout="auto"
      @selection-change="(newSelection: any) => selection = newSelection"
    >
      <el-table-column type="selection" />
      <TableColumn :columns="columns" />
      <el-table-column label="操作">
        <template #default="scope">
          <el-button link v-permit="'oa:formData:'" @click="() => handleShow(scope)">查看</el-button>
          <el-button link v-permit="'oa:formData:modify'" @click="() => handleEdit(scope)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>
    <TablePagination v-model="pagination" @change="handleRefresh(true, true)" />
  </el-card>

  <el-dialog v-model="infoVisible" :title="`信息查看 - ${form._id}`" :destroy-on-close="true" width="818">
    <FlexForm v-model="form" :config="config" :frame="frame" :authority="authority.view" />
    <template #footer>
      <el-button @click="infoVisible = false">关闭</el-button>
    </template>
  </el-dialog>

  <el-dialog
    v-model="formVisible"
    :title="form._id ? `信息修改 - ${form._id}` : '信息添加'"
    :close-on-click-modal="false"
    :destroy-on-close="true"
    width="818">
    <FlexForm ref="formRef" v-model="form" :config="config" :frame="frame" :authority="authority.edit" />
    <template #footer>
      <el-button @click="formVisible = false">取消</el-button>
      <el-button type="primary" @click="() => handleSubmit(false)" :loading="formLoading">确定</el-button>
      <el-button type="danger" @click="() => handleSubmit(true)" :loading="formLoading">强制提交</el-button>
    </template>
  </el-dialog>
</template>

<style lang="scss" scoped>
</style>
