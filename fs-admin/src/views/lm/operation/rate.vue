<script setup lang="ts">
import { onMounted, ref } from 'vue';
import type { FormInstance, TableInstance } from 'element-plus';
import { useRoute, useRouter } from 'vue-router';
import RateApi from '@/api/lm/RateApi';
import ApiUtil from '@/utils/ApiUtil';
import DateUtil from '@/utils/DateUtil';
import RouteUtil from '@/utils/RouteUtil';
import TableUtil from '@/utils/TableUtil';

const route = useRoute()
const router = useRouter()
const tableRef = ref<TableInstance>()
const loading = ref(false)
const searchable = ref(true)
const columns = ref([
  { prop: 'id', label: 'ID' },
  { prop: 'name', label: '名称' },
  { prop: 'requestCount', label: '请求数量' },
  { prop: 'requestInterval', label: '请求间隔(秒)' },
  { prop: 'tokenCount', label: '词元数量' },
  { prop: 'tokenInterval', label: '词元间隔(秒)' },
  { prop: 'creditCount', label: '积分数量' },
  { prop: 'creditInterval', label: '积分间隔(秒)' },
  { prop: 'sort', label: '排序' },
  { prop: 'statusText', label: '状态' },
  { prop: 'description', label: '描述', hide: true },
  { prop: 'createdTime', label: '创建时间', formatter: DateUtil.render, hide: true },
  { prop: 'updatedTime', label: '修改时间', formatter: DateUtil.render, hide: true },
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
  Object.assign(filters.value, RouteUtil.pagination2filter(pagination.value, keepPage))
  filter2query && RouteUtil.filter2query(route, router, filters.value)
  loading.value = true
  RateApi.list(filters.value).then((result: any) => {
    RouteUtil.result2pagination(pagination.value, result)
    rows.value = result.data.rows
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}

onMounted(() => {
  handleRefresh(false, true)
  RateApi.config().then((result: any) => {
    Object.assign(config.value, { ready: true }, ApiUtil.data(result))
  }).catch(() => {})
})

const infoVisible = ref(false)
const formVisible = ref(false)
const formLoading = ref(false)
const form: any = ref({})
const formRef = ref<FormInstance>()
const rules = ref({
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
})

const handleAdd = () => {
  form.value = {
    status: '1',
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
  })
  formVisible.value = true
}

const handleSubmit = () => {
  formRef.value?.validate((valid: boolean) => {
    if (!valid || formLoading.value) return
    formLoading.value = true
    RateApi.save(form.value, { success: true }).then(() => {
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
    RateApi.delete(ids, { success: true }).then(() => {
      handleRefresh(false, true)
    }).catch(() => {})
  }).catch(() => {})
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
        <el-button type="primary" @click="handleRefresh(true, false)">查询</el-button>
        <el-button @click="filterRef?.resetFields()">重置</el-button>
      </form-search-item>
    </form-search>
  </el-card>

  <el-card :bordered="false" shadow="never" class="fs-table-card">
    <div class="fs-table-toolbar flex-between">
      <el-space>
        <button-add v-permit="'lm:rate:add'" @click="handleAdd" />
        <button-delete v-permit="'lm:rate:delete'" :disabled="selection.length === 0" @click="handleDelete" />
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
      <TableColumn :columns="columns" />
      <el-table-column label="操作" width="120">
        <template #default="scope">
          <el-button link @click="handleShow(scope)" v-permit="'lm:rate:'">查看</el-button>
          <el-button link @click="handleEdit(scope)" v-permit="'lm:rate:modify'">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>
    <TablePagination v-model="pagination" @change="handleRefresh(true, true)" />
  </el-card>

  <el-drawer v-model="infoVisible" :title="'信息查看 - ' + form.id" size="60%">
    <el-descriptions border :column="2">
      <el-descriptions-item label="名称">{{ form.name }}</el-descriptions-item>
      <el-descriptions-item label="状态">{{ form.statusText }}</el-descriptions-item>
      <el-descriptions-item label="间隔内请求数量">{{ form.requestCount }}</el-descriptions-item>
      <el-descriptions-item label="请求间隔(秒)">{{ form.requestInterval }}</el-descriptions-item>
      <el-descriptions-item label="间隔内词元数量">{{ form.tokenCount }}</el-descriptions-item>
      <el-descriptions-item label="词元间隔(秒)">{{ form.tokenInterval }}</el-descriptions-item>
      <el-descriptions-item label="间隔内积分数量">{{ form.creditCount }}</el-descriptions-item>
      <el-descriptions-item label="积分间隔(秒)">{{ form.creditInterval }}</el-descriptions-item>
      <el-descriptions-item label="排序" :span="2">{{ form.sort }}</el-descriptions-item>
      <el-descriptions-item label="描述" :span="2">{{ form.description || '暂无' }}</el-descriptions-item>
      <el-descriptions-item label="创建者">{{ form.createdUserInfo?.name ?? form.createdUid }}</el-descriptions-item>
      <el-descriptions-item label="创建时间">{{ DateUtil.format(form.createdTime) }}</el-descriptions-item>
      <el-descriptions-item label="修改者">{{ form.updatedUserInfo?.name ?? form.updatedUid }}</el-descriptions-item>
      <el-descriptions-item label="修改时间">{{ DateUtil.format(form.updatedTime) }}</el-descriptions-item>
    </el-descriptions>
  </el-drawer>

  <el-drawer v-model="formVisible" :close-on-click-modal="false" :show-close="false" :destroy-on-close="true" size="60%">
    <template #header="{ close, titleId, titleClass }">
      <h4 :id="titleId" :class="titleClass">{{ '速率限制' + (form.id ? ('修改 - ' + form.id) : '添加') }}</h4>
      <el-space>
        <el-button type="primary" @click="handleSubmit" :loading="formLoading">确定</el-button>
        <el-button @click="close">取消</el-button>
      </el-space>
    </template>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="auto">
      <el-descriptions border :column="2">
        <el-descriptions-item label="名称">
          <el-form-item prop="name" class="form-item-reset">
            <el-input v-model="form.name" />
          </el-form-item>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-form-item prop="status" class="form-item-reset">
            <el-select v-model="form.status" placeholder="请选择">
              <el-option v-for="(value, key) in config.status" :key="key" :value="key" :label="value" />
            </el-select>
          </el-form-item>
        </el-descriptions-item>
        <el-descriptions-item label="间隔内请求数量">
          <el-input-number v-model="form.requestCount" :min="0" />
        </el-descriptions-item>
        <el-descriptions-item label="请求间隔">
          <el-space>
            <el-input-number v-model="form.requestInterval" :min="0" />
            <span>秒</span>
          </el-space>
        </el-descriptions-item>
        <el-descriptions-item label="间隔内词元数量">
          <el-input-number v-model="form.tokenCount" :min="0" />
        </el-descriptions-item>
        <el-descriptions-item label="词元间隔">
          <el-space>
            <el-input-number v-model="form.tokenInterval" :min="0" />
            <span>秒</span>
          </el-space>
        </el-descriptions-item>
        <el-descriptions-item label="间隔内积分数量">
          <el-input-number v-model="form.creditCount" :min="0" />
        </el-descriptions-item>
        <el-descriptions-item label="积分间隔">
           <el-space>
             <el-input-number v-model="form.creditInterval" :min="0" />
             <span>秒</span>
           </el-space>
        </el-descriptions-item>
        <el-descriptions-item label="排序" :span="2">
          <el-input-number v-model="form.sort" :min="0" />
        </el-descriptions-item>
        <el-descriptions-item label="描述" :span="2">
          <el-input type="textarea" v-model="form.description" :rows="4" />
        </el-descriptions-item>
      </el-descriptions>
    </el-form>
  </el-drawer>
</template>

<style lang="scss" scoped>
</style>
