<script setup lang="ts">
import { onMounted, ref } from 'vue';
import type { FormInstance, TableInstance } from 'element-plus';
import { useRoute, useRouter } from 'vue-router';
import UserApi from '@/api/member/UserApi';
import CreditApi from '@/api/lm/CreditApi';
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
  { prop: 'uid', label: 'UID' },
  { prop: 'uidUserInfo.serial', label: '用户账号' },
  { prop: 'uidUserInfo.name', label: '用户昵称' },
  { prop: 'remained', label: '剩余积分' },
  { prop: 'consumed', label: '总消耗' },
  { prop: 'remindEnabled', label: '余额预警' },
  { prop: 'remindThreshold', label: '预警阈值' },
  { prop: 'rateIds', label: '速率限制', slot: 'rateIds' },
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
const filters = ref(RouteUtil.query2filter(route))
const pagination = ref(RouteUtil.pagination(filters.value))
const selection: any = ref([])

const handleRefresh = (filter2query: boolean, keepPage: boolean) => {
  tableRef.value?.clearSelection()
  Object.assign(filters.value, RouteUtil.pagination2filter(pagination.value, keepPage))
  filter2query && RouteUtil.filter2query(route, router, filters.value)
  loading.value = true
  CreditApi.list(filters.value).then((result: any) => {
    RouteUtil.result2pagination(pagination.value, result)
    rows.value = result.data.rows
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}

onMounted(() => {
  handleRefresh(false, true)
  CreditApi.config().then((result: any) => {
    Object.assign(config.value, { ready: true }, ApiUtil.data(result))
  }).catch(() => {})
})

const infoVisible = ref(false)
const formVisible = ref(false)
const formLoading = ref(false)
const form: any = ref({})
const formRef = ref<FormInstance>()
const rules = ref({
  uid: [{ required: true, message: '请选择用户', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
})

const handleAdd = () => {
  form.value = {
    rateIds: [],
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
    CreditApi.save(form.value, { success: true }).then(() => {
      handleRefresh(false, true)
      formVisible.value = false
    }).catch(() => {}).finally(() => {
      formLoading.value = false
    })
  })
}

const handleDelete = () => {
  TableUtil.selection(selection.value, 'uid').then((ids: any) => {
    loading.value = true
    CreditApi.delete(ids, { success: true }).then(() => {
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
      <form-search-item label="用户" prop="uid">
        <form-select v-model="filters.uid" :callback="UserApi.list" clearable />
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
        <button-add v-permit="'lm:credit:add'" @click="handleAdd" />
        <button-delete v-permit="'lm:credit:delete'" :disabled="selection.length === 0" @click="handleDelete" />
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
      :row-key="(record: any) => record.uid"
      :border="true"
      v-loading="loading"
      table-layout="auto"
      @selection-change="(s: any) => selection = s"
    >
      <el-table-column type="selection" />
      <TableColumn :columns="columns">
        <template #rateIds="scope">
          <el-space wrap>
            <el-space><el-tag v-for="item in scope.row.rates" :key="item.id">{{ item.name }}</el-tag></el-space>
          </el-space>
        </template>
      </TableColumn>
      <el-table-column label="操作" width="120">
        <template #default="scope">
          <el-button link @click="handleShow(scope)" v-permit="'lm:credit:'">查看</el-button>
          <el-button link @click="handleEdit(scope)" v-permit="'lm:credit:modify'">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>
    <TablePagination v-model="pagination" @change="handleRefresh(true, true)" />
  </el-card>

  <el-drawer v-model="infoVisible" :title="'信息查看 - ' + form.uid" size="70%">
    <el-descriptions border :column="2">
      <el-descriptions-item label="UID">{{ form.uid }}</el-descriptions-item>
      <el-descriptions-item label="状态">{{ form.statusText }}</el-descriptions-item>
      <el-descriptions-item label="用户账号">{{ form.uidUserInfo?.serial }}</el-descriptions-item>
      <el-descriptions-item label="用户昵称">{{ form.uidUserInfo?.name }}</el-descriptions-item>
      <el-descriptions-item label="剩余积分">{{ form.remained }}</el-descriptions-item>
      <el-descriptions-item label="总消耗">{{ form.consumed }}</el-descriptions-item>
      <el-descriptions-item label="余额预警">{{ form.remindEnabled }}</el-descriptions-item>
      <el-descriptions-item label="预警阈值">{{ form.remindThreshold }}</el-descriptions-item>
      <el-descriptions-item label="排序">{{ form.sort }}</el-descriptions-item>
      <el-descriptions-item label="速率限制" :span="2">
        <el-space><el-tag v-for="item in form.rates" :key="item.id">{{ item.name }}</el-tag></el-space>
      </el-descriptions-item>
      <el-descriptions-item label="描述" :span="2">{{ form.description || '暂无' }}</el-descriptions-item>
      <el-descriptions-item label="创建者">{{ form.createdUserInfo?.name ?? form.createdUid }}</el-descriptions-item>
      <el-descriptions-item label="创建时间">{{ DateUtil.format(form.createdTime) }}</el-descriptions-item>
      <el-descriptions-item label="修改者">{{ form.updatedUserInfo?.name ?? form.updatedUid }}</el-descriptions-item>
      <el-descriptions-item label="修改时间">{{ DateUtil.format(form.updatedTime) }}</el-descriptions-item>
    </el-descriptions>
  </el-drawer>

  <el-drawer v-model="formVisible" :close-on-click-modal="false" :show-close="false" :destroy-on-close="true" size="70%">
    <template #header="{ close, titleId, titleClass }">
      <h4 :id="titleId" :class="titleClass">{{ (form.createdUid ? '积分调整' : '用户开通') }}</h4>
      <el-space>
        <el-button type="primary" @click="handleSubmit" :loading="formLoading">确定</el-button>
        <el-button @click="close">取消</el-button>
      </el-space>
    </template>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="auto">
      <el-descriptions border :column="2">
        <el-descriptions-item label="用户">
          <form-select v-model="form.uid" :callback="UserApi.list" :disabled="!!form.createdUid" clearable />
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-select v-model="form.status" placeholder="请选择">
            <el-option v-for="(value, key) in config.status" :key="key" :value="key" :label="value" />
          </el-select>
        </el-descriptions-item>
        <el-descriptions-item label="剩余积分">
          <el-input v-model="form.remained" />
        </el-descriptions-item>
        <el-descriptions-item label="总消耗">
          <el-input v-model="form.consumed" />
        </el-descriptions-item>
        <el-descriptions-item label="余额预警">
           <el-checkbox v-model="form.remindEnabled">启用余额预警</el-checkbox>
        </el-descriptions-item>
        <el-descriptions-item label="预警阈值">
          <el-input v-model="form.remindThreshold" />
        </el-descriptions-item>
        <el-descriptions-item label="速率限制" :span="2">
          <form-select v-model="form.rateIds" :callback="RateApi.list" multiple clearable />
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
