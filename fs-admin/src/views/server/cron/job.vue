<script setup lang="ts">
import { onMounted, ref } from 'vue';
import type { FormInstance, TableInstance } from 'element-plus';
import RouteUtil from '@/utils/RouteUtil'
import { useRoute, useRouter } from 'vue-router';
import TableUtil from '@/utils/TableUtil';
import CronApi from '@/api/server/CronApi';
import TableRadio from '@/components/Table/TableRadio.vue';

const route = useRoute()
const router = useRouter()
const tableRef = ref<TableInstance>()
const loading = ref(false)
const searchable = ref(true)
const columns = ref([
  { prop: 'name', label: '名称' },
  { prop: 'group', label: '分组' },
  { prop: 'schedule', label: '调度器' },
])
const rows = ref([])
const filterRef = ref<FormInstance>()
const filters = ref(RouteUtil.query2filter(route, { advanced: false, agentIds: [] }))
const pagination = ref(RouteUtil.pagination(filters.value))
const selection: any = ref(null)
const handleRefresh = (filter2query: boolean, keepPage: boolean) => {
  selection.value = null
  Object.assign(filters.value, RouteUtil.pagination2filter(pagination.value, keepPage))
  filter2query && RouteUtil.filter2query(route, router, filters.value)
  loading.value = true
  CronApi.jobList(filters.value).then((result: any) => {
    RouteUtil.result2pagination(pagination.value, result)
    rows.value = result.data.rows
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}
onMounted(() => {
  handleRefresh(false, true)
})
const infoVisible = ref(false)
const formVisible = ref(false)
const formLoading = ref(false)
const form: any = ref({})
const formRef: any = ref<FormInstance>()
const rules = ref({
  name: [{ required: true, message: '请输入作业名称', trigger: 'blur' }],
  group: [{ required: true, message: '请输入作业分组', trigger: 'blur' }],
  cls: [{ required: true, message: '请输入执行器类名称', trigger: 'blur' }]
})
const handleAdd = () => {
  form.value = {}
  formVisible.value = true
}
const handleShow = (scope: any) => {
  form.value = Object.assign({}, scope.row)
  infoVisible.value = true
}
const handleEdit = (scope: any) => {
  form.value = Object.assign({}, scope.row, {})
  formVisible.value = true
}
const handleSubmit = () => {
  formRef.value?.validate((valid: boolean) => {
    if (!valid || formLoading.value) return
    formLoading.value = true
    CronApi.jobSave(form.value, { success: true }).then(result => {
      handleRefresh(false, true)
      formVisible.value = false
    }).catch(() => {}).finally(() => {
      formLoading.value = false
    })
  })
}
const handleDelete = () => {
  TableUtil.confirm().then(() => {
    const record = rows.value[selection.value]
    handleCommand('delete', record)
  }).catch(() => {})
}
const handleCommand = (command: String, record: any, arg = null) => {
  const param = {
    arg,
    command,
    name: record.name,
    group: record.group
  }
  loading.value = true
  formLoading.value = true
  CronApi.jobCommand(param, { success: true }).then((result) => {
    formLoading.value = false
    triggerVisible.value = false
    handleRefresh(false, true)
  }).catch(() => {
    loading.value = false
  })
}
const triggerVisible = ref(false)
const handleTrigger = (record: any) => {
  form.value = Object.assign({}, record)
  triggerVisible.value = true
}
</script>

<template>
  <el-card :bordered="false" shadow="never" class="fs-table-search" v-show="searchable">
    <form-search ref="filterRef" :model="filters">
      <form-search-item label="名称" prop="name">
        <el-input v-model="filters.name" clearable />
      </form-search-item>
      <form-search-item label="分组" prop="group">
        <el-input v-model="filters.group" clearable />
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
          <button-add v-permit="'cron:job:add'" @click="handleAdd" />
        <button-delete v-permit="'cron:job:delete'" :disabled="selection === null" @click="handleDelete" />
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
      :row-key="(record: any) => `${record.group}_${record.name}`"
      :border="true"
      v-loading="loading"
      table-layout="auto"
      @selection-change="(newSelection: any) => selection = newSelection"
    >
      <TableRadio v-model="selection" :value="(scope: any) => scope.$index" width="60px" />
      <el-table-column type="expand">
        <template #default="scope">
          <el-descriptions border label-width="60px">
            <el-descriptions-item label="nonConcurrent">{{ scope.row.nonConcurrent }}</el-descriptions-item>
            <el-descriptions-item label="updateData">{{ scope.row.updateData }}</el-descriptions-item>
            <el-descriptions-item label="recover">{{ scope.row.recovery }}</el-descriptions-item>
            <el-descriptions-item label="执行类" :span="2">{{ scope.row.cls }}</el-descriptions-item>
            <el-descriptions-item label="durable">{{ scope.row.durable }}</el-descriptions-item>
            <el-descriptions-item label="描述" :span="3">{{ scope.row.description }}</el-descriptions-item>
          </el-descriptions>
        </template>
        </el-table-column>
      <TableColumn :columns="columns">
        <template #role="scope">
          <el-space><el-tag v-for="item in scope.row.roles" :key="item.id">{{ item.name }}</el-tag></el-space>
        </template>
      </TableColumn>
      <el-table-column label="操作" width="150px">
        <template #default="scope">
          <el-space>
            <el-button link @click="handleShow(scope)" v-permit="'cron:job:'">查看</el-button>
            <el-button link @click="handleEdit(scope)" v-permit="'cron:job:modify'">编辑</el-button>
            <el-dropdown v-permit="'cron:job:'">
              <el-button link>调度</el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item><el-button link @click="handleCommand('pause', scope.row)">暂停调度</el-button></el-dropdown-item>
                  <el-dropdown-item><el-button link @click="handleCommand('resume', scope.row)">恢复调度</el-button></el-dropdown-item>
                  <el-dropdown-item><el-button link @click="handleTrigger(scope.row)">手动触发</el-button></el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </el-space>
        </template>
      </el-table-column>
    </el-table>
    <TablePagination v-model="pagination" @change="handleRefresh(true, true)" />
  </el-card>
  <el-drawer v-model="infoVisible" :title="`信息查看`">
    <el-form :model="form" label-width="auto">
      <el-form-item label="名称">{{ form.name }}</el-form-item>
      <el-form-item label="分组">{{ form.group }}</el-form-item>
      <el-form-item label="执行类">{{ form.cls }}</el-form-item>
      <el-form-item label="描述">{{ form.description }}</el-form-item>
      <el-form-item label="作业参数">
        <el-input type="textarea" v-model="form.arg" :rows="5" disabled />
      </el-form-item>
    </el-form>
  </el-drawer>
  <el-drawer v-model="formVisible" :close-on-click-modal="false" :show-close="false" :destroy-on-close="true">
    <template #header="{ close, titleId, titleClass }">
      <h4 :id="titleId" :class="titleClass">{{ '信息' + (form.id ? ('修改 - ' + form.id) : '添加') }}</h4>
      <el-space>
        <el-button type="primary" @click="handleSubmit" :loading="formLoading">确定</el-button>
        <el-button @click="close">取消</el-button>
      </el-space>
    </template>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="auto">
      <el-form-item label="名称" prop="name">
        <el-input v-model="form.name" />
      </el-form-item>
      <el-form-item label="分组" prop="group">
        <el-input v-model="form.group" />
      </el-form-item>
      <el-form-item label="执行类" prop="cls">
        <el-input type="textarea" v-model="form.cls" />
      </el-form-item>
      <el-form-item label="描述">
        <el-input type="textarea" v-model="form.description" />
      </el-form-item>
      <el-form-item label="作业参数">
        <el-input type="textarea" v-model="form.arg" :rows="5" />
      </el-form-item>
    </el-form>
  </el-drawer>
  <el-dialog v-model="triggerVisible" title="手动触发" draggable>
    <el-form :model="form" label-width="auto">
      <el-form-item label="名称">{{ form.name }}</el-form-item>
      <el-form-item label="分组">{{ form.group }}</el-form-item>
      <el-form-item label="执行类">{{ form.cls }}</el-form-item>
      <el-form-item label="描述">{{ form.description }}</el-form-item>
      <el-form-item label="作业参数">
        <el-input type="textarea" v-model="form.arg" :rows="5" />
      </el-form-item>
    </el-form>
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="triggerVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCommand('trigger', form, form.arg)">确认</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<style lang="scss" scoped>
</style>
