<script setup lang="ts">
import { onMounted, ref } from 'vue';
import type { FormInstance, TableInstance } from 'element-plus';
import RouteUtil from '@/utils/RouteUtil'
import { useRoute, useRouter } from 'vue-router';
import InterceptApi from '@/api/spider/InterceptApi';
import ApiUtil from '@/utils/ApiUtil';
import TableUtil from '@/utils/TableUtil';

const model: any = defineModel()

const route = useRoute()
const router = useRouter()
const tableRef = ref<TableInstance>()
const loading = ref(false)
const searchable = ref(true)
const columns = ref([
  { prop: 'id', label: 'ID' },
  { prop: 'name', label: '拦截器名称' },
  { prop: 'code', label: '拦截响应状态' },
  { prop: 'charset', label: '页面编码' },
  { prop: 'collection', label: '数据存储' },
  { prop: 'description', label: '描述', hide: true },
  { prop: 'sort', label: '排序' },
  { prop: 'statusText', label: '状态' },
])
const config = ref({
  ready: false,
  status: {},
})
const rows = ref([])
const filterRef = ref<FormInstance>()
const filters = ref(RouteUtil.query2filter(route, { advanced: false }))
const pagination = ref(RouteUtil.pagination(filters.value))
const selection = ref([])
const handleRefresh = (filter2query: boolean, keepPage: boolean) => {
  tableRef.value?.clearSelection()
  Object.assign(filters.value, RouteUtil.pagination2filter(pagination.value, keepPage), { templateId: model.value.id })
  filter2query && RouteUtil.filter2query(route, router, filters.value)
  loading.value = true
  InterceptApi.list(filters.value).then((result: any) => {
    RouteUtil.result2pagination(pagination.value, result)
    rows.value = result.data.rows
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}
onMounted(() => {
  handleRefresh(false, true)
  InterceptApi.config().then((result: any) => {
    Object.assign(config.value, { ready: true }, ApiUtil.data(result))
  }).catch(() => {})
})
const infoVisible = ref(false)
const infoLoading = ref(false)
const formVisible = ref(false)
const formLoading = ref(false)
const form: any = ref({})
const formRef: any = ref<FormInstance>()
const rules = ref({
  name: [{ required: true, message: '请输入拦截器名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入拦截响应状态码', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
})
const handleAdd = () => {
  form.value = {
    templateId: model.value.id,
    status: '1',
  }
  formVisible.value = true
}
const handleShow = (scope: any) => {
  form.value = Object.assign({}, scope.row)
  infoVisible.value = true
}
const handleReload = () => {
  infoLoading.value = true
  InterceptApi.info({ id: form.value.id }).then(result => {
    Object.assign(form.value, ApiUtil.data(result))
  }).catch(() => {}).finally(() => {
    infoLoading.value = false
  })
}
const handleEdit = (scope: any) => {
  form.value = Object.assign({}, scope.row, {
    status: scope.row.status + '',
  })
  formVisible.value = true
}
const handleSubmit = () => {
  formRef.value?.validate((valid: boolean) => {
    if (!valid || formLoading.value) return
    formLoading.value = true
    InterceptApi.save(form.value, { success: true }).then(result => {
      handleRefresh(false, true)
      formVisible.value = false
    }).catch(() => {}).finally(() => {
      formLoading.value = false
    })
  })
}
const handleDelete = () => {
  TableUtil.selection(selection.value).then((ids: any) => {
    loading.value = true
    InterceptApi.delete(ids, { success: true }).then(() => {
      handleRefresh(false, true)
    }).catch(() => {})
  }).catch(() => {})
}
const placeholder = {
  parser: `Jsoup解析器使用方式：
jsoup {
    title: title(),
    -: body() {
        -1: select(".class-object") {
            detail: html(),
            property: select(".class-name") [
                key: select(".class-name").text(),
                value: select(".class-name").text()
            ]
        },
        -2: select(".class-array") [
            href: attr("abs:href"),
            description: select(".class-name:eq(0) > .class-name").html()
        ]
    }
}
`,
  assistant: `辅助器JS脚本：
context: { // 上下文
    url: '', // 实际请求的链接地址
    job: {}, // 作业信息
    task: {}, // 任务信息
    body: '', // 请求结果
    parsed: {}, // 解析结果
    status: 200, // 请求状态
    exception: null, // 异常信息
}
return: { // 返回结果，若为空则不启用助手
    name: '', // 助手名称
    args: {}, // 传递参数
}
`,
  mapper: `映射器JS脚本：
context: { // 上下文
    url: '', // 实际请求的链接地址
    job: {}, // 作业信息
    task: {}, // 任务信息
    body: '', // 请求结果
    parsed: {}, // 解析结果
    status: 200, // 请求状态
    exception: null, // 异常信息
}
return: { // 返回结果
    collect: [{}], // 输出结果
    next: '', // 下步动作，retry - 重试任务，halt - 停顿令牌并重试，discard - 丢弃任务
    halt: 30000, // 停顿时长，默认30秒
}
`
}
</script>

<template>
  <el-card :bordered="false" shadow="never" class="fs-table-search" v-show="searchable">
    <form-search ref="filterRef" :model="filters">
      <form-search-item label="名称" prop="name">
        <el-input v-model="filters.name" clearable />
      </form-search-item>
      <form-search-item label="响应状态" prop="code">
        <el-input v-model="filters.code" clearable />
      </form-search-item>
      <form-search-item label="状态" prop="status">
        <el-select v-model="filters.status" placeholder="请选择" clearable>
          <el-option v-for="(value, key) in config.status" :key="key" :value="key" :label="value" />
        </el-select>
      </form-search-item>
      <form-search-item>
        <el-button type="primary" @click="handleRefresh(true, false)">查询</el-button>
        <el-button @click="filterRef?.resetFields()">重置</el-button>
      </form-search-item>
    </form-search>
  </el-card>
  <el-card :bordered="false" shadow="never" class="fs-table-card">
    <div class="fs-table-toolbar flex-between">
      <el-space>
        <button-add v-permit="'spider:intercept:add'" @click="handleAdd" />
        <button-delete v-permit="'spider:intercept:delete'" :disabled="selection.length === 0" @click="handleDelete" />
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
      :row-key="record => record.id"
      :border="true"
      v-loading="loading"
      table-layout="auto"
      @selection-change="newSelection => selection = newSelection"
    >
      <el-table-column type="selection" />
      <TableColumn :columns="columns">
      </TableColumn>
      <el-table-column label="操作">
        <template #default="scope">
          <el-button link @click="handleEdit(scope)" v-permit="'spider:intercept:modify'">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>
    <TablePagination v-model="pagination" @change="handleRefresh(true, true)" />
  </el-card>
  <el-drawer v-model="formVisible" :close-on-click-modal="false" :show-close="false" :destroy-on-close="true" size="50%">
    <template #header="{ close, titleId, titleClass }">
      <h4 :id="titleId" :class="titleClass">页面配置</h4>
      <el-space>
        <el-button type="primary" @click="handleSubmit" :loading="formLoading">保存</el-button>
        <el-button @click="close">取消</el-button>
      </el-space>
    </template>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="auto">
      <el-descriptions border :column="2">
        <el-descriptions-item label="拦截器名称" prop="name">
          <el-input v-model="form.name" />
        </el-descriptions-item>
        <el-descriptions-item label="响应状态码" prop="code">
          <el-input-number v-model="form.code" placeholder="拦截页面响应状态码" />
        </el-descriptions-item>
        <el-descriptions-item label="排序">
          <el-input-number v-model="form.sort" />
        </el-descriptions-item>
        <el-descriptions-item label="状态" prop="status">
          <el-select v-model="form.status" placeholder="请选择">
            <el-option v-for="(value, key) in config.status" :key="key" :value="key" :label="value" />
          </el-select>
        </el-descriptions-item>
        <el-descriptions-item label="页面编码" prop="charset">
          <el-input v-model="form.charset" placeholder="默认utf-8" />
        </el-descriptions-item>
        <el-descriptions-item label="数据存储" prop="collection">
          <el-input v-model="form.collection" placeholder="采集结果存储到Mongo中的集合名称" />
        </el-descriptions-item>
        <el-descriptions-item label="解析器" :span="2">
          <el-input type="textarea" v-model="form.parser" :placeholder="placeholder.parser" :rows="5" />
        </el-descriptions-item>
        <el-descriptions-item label="辅助器" :span="2">
          <el-input type="textarea" v-model="form.assistant" :placeholder="placeholder.assistant" :rows="5" />
        </el-descriptions-item>
        <el-descriptions-item label="映射器" :span="2">
          <el-input type="textarea" v-model="form.mapper" :placeholder="placeholder.mapper" :rows="5" />
        </el-descriptions-item>
        <el-descriptions-item label="描述" :span="2">
          <el-input type="textarea" v-model="form.description" />
        </el-descriptions-item>
      </el-descriptions>
    </el-form>
  </el-drawer>
</template>

<style lang="scss" scoped>
</style>
