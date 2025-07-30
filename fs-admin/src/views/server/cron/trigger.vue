<script setup lang="ts">
import { onMounted, ref } from 'vue';
import type { FormInstance, TableInstance } from 'element-plus';
import RouteUtil from '@/utils/RouteUtil'
import { useRoute, useRouter } from 'vue-router';
import TableUtil from '@/utils/TableUtil';
import CronApi from '@/api/server/CronApi';
import TableRadio from '@/components/Table/TableRadio.vue';
import DateUtil from '@/utils/DateUtil';

const route = useRoute()
const router = useRouter()
const tableRef = ref<TableInstance>()
const loading = ref(false)
const searchable = ref(true)
const columns = ref([
  { prop: 'name', label: '触发器名称' },
  { prop: 'group', label: '触发器分组' },
  { prop: 'jobName', label: '作业名称' },
  { prop: 'jobGroup', label: '作业分组' },
  { prop: 'schedule', label: '调度器' },
  { prop: 'priority', label: '优先级' },
  { prop: 'state', label: '状态' },
  { prop: 'type', label: '类型' },
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
  CronApi.triggerList(filters.value).then((result: any) => {
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
  name: [{ required: true, message: '请输入触发器名称', trigger: 'blur' }],
  group: [{ required: true, message: '请输入触发器分组', trigger: 'blur' }],
  jobName: [{ required: true, message: '请输入作业名称', trigger: 'blur' }],
  jobGroup: [{ required: true, message: '请输入作业分组', trigger: 'blur' }],
  expression: [{ required: true, message: '请输入Cron表达', trigger: 'blur' }]
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
    CronApi.triggerSave(form.value, { success: true }).then(result => {
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
const handleCommand = (command: String, record: any) => {
  const param = {
    command,
    name: record.name,
    group: record.group
  }
  loading.value = true
  CronApi.triggerCommand(param, { success: true }).then((result) => {
    handleRefresh(false, true)
  }).catch(() => {
    loading.value = false
  })
}
</script>

<template>
  <el-card :bordered="false" shadow="never" class="fs-table-search" v-show="searchable">
    <form-search ref="filterRef" :model="filters">
      <form-search-item label="触发器名称" prop="name">
        <el-input v-model="filters.name" clearable />
      </form-search-item>
      <form-search-item label="作业名称" prop="jobName">
        <el-input v-model="filters.name" clearable />
      </form-search-item>
      <form-search-item label="作业分组" prop="jobGroup">
        <el-input v-model="filters.group" clearable />
      </form-search-item>
      <form-search-item>
        <el-button type="primary" @click="handleRefresh(true, false)">查询</el-button>
        <el-button @click="filterRef?.resetFields()">重置</el-button>
      </form-search-item>
      <form-search-item label="触发器分组" prop="group">
        <el-input v-model="filters.group" clearable />
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
          <el-descriptions border label-width="130px">
            <el-descriptions-item label="Cron表达式" :span="3" v-if="scope.row.type === 'CRON'">{{ scope.row.expression }}</el-descriptions-item>
            <el-descriptions-item label="开始时间">{{ DateUtil.format(scope.row.startTime) }}</el-descriptions-item>
            <el-descriptions-item label="结束时间">{{ DateUtil.format(scope.row.endTime) }}</el-descriptions-item>
            <el-descriptions-item label="排期日历">{{ scope.row.calendar }}</el-descriptions-item>
            <el-descriptions-item label="上次触发">{{ DateUtil.format(scope.row.previousFireTime) }}</el-descriptions-item>
            <el-descriptions-item label="下次触发">{{ DateUtil.format(scope.row.nextFireTime) }}</el-descriptions-item>
            <el-descriptions-item label="最终触发">{{ DateUtil.format(scope.row.finalFireTime) }}</el-descriptions-item>
            <el-descriptions-item label="misfire">{{ scope.row.misfire }}</el-descriptions-item>
            <el-descriptions-item label="data" :span="2">{{ scope.row.data }}</el-descriptions-item>
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
            <el-button link @click="handleEdit(scope)" v-permit="'cron:job:'">编辑</el-button>
            <el-button link @click="handleCommand('pause', scope.row)" v-permit="'cron:job:'" v-if="scope.row.state !== 'PAUSED'">暂停</el-button>
            <el-button link @click="handleCommand('resume', scope.row)" v-permit="'cron:job:'" v-if="scope.row.state === 'PAUSED'">恢复</el-button>
          </el-space>
        </template>
      </el-table-column>
    </el-table>
    <TablePagination v-model="pagination" @change="handleRefresh(true, true)" />
  </el-card>
  <el-drawer v-model="infoVisible" :title="`信息查看`">
    <el-form :model="form" label-width="auto">
      <el-form-item label="触发器名称">{{ form.name }}</el-form-item>
      <el-form-item label="触发器分组">{{ form.group }}</el-form-item>
      <el-form-item label="作业名称">{{ form.jobName }}</el-form-item>
      <el-form-item label="作业分组">{{ form.jobGroup }}</el-form-item>
      <el-form-item label="优先级">{{ form.priority }}</el-form-item>
      <el-form-item label="Cron表达式">{{ form.expression }}</el-form-item>
      <el-form-item label="描述">{{ form.description }}</el-form-item>
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
      <el-form-item label="触发器名称" prop="name">
        <el-input v-model="form.name" />
      </el-form-item>
      <el-form-item label="触发器分组" prop="group">
        <el-input v-model="form.group" />
      </el-form-item>
      <el-form-item label="作业名称" prop="jobName">
        <el-input v-model="form.jobName" />
      </el-form-item>
      <el-form-item label="作业分组" prop="jobGroup">
        <el-input v-model="form.jobGroup" />
      </el-form-item>
      <el-form-item label="优先级" prop="priority">
        <el-input-number v-model="form.priority" />
      </el-form-item>
      <el-form-item label="Cron表达式" prop="expression">
        <el-input v-model="form.expression" />
      </el-form-item>
      <el-form-item label="描述">
        <el-input type="textarea" v-model="form.description" />
      </el-form-item>
    </el-form>
  </el-drawer>
</template>

<style lang="scss" scoped>
</style>
