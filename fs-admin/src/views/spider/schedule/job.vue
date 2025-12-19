<script setup lang="ts">
import { onMounted, ref } from 'vue';
import type { FormInstance, TableInstance } from 'element-plus';
import NodeApi from '@/api/spider/NodeApi';
import DateUtil from '@/utils/DateUtil';
import SpiderPanParams from '@/designer/Spider/SpiderPanParams.vue';
import ElementUtil from '@/utils/ElementUtil';

const tableRef = ref<TableInstance>()
const loading = ref(false)
const columns = ref([
  { prop: 'id', label: 'ID' },
  { prop: 'template.name', label: '模板名称' },
  { prop: 'template.typeText', label: '采集方式' },
  { prop: 'template.maxThreads', label: '最大线程' },
  { prop: 'template.rateInfo.name', label: '请求频率' },
  { prop: 'template.rateInfo.parallel', label: '并行度' },
  { prop: 'template.rateInfo.concurrent', label: '并发数' },
  { prop: 'template.rateInfo.interval', label: '并发间隔' },
  { prop: 'template.priority', label: '优先级' },
  { prop: 'template.minHalt', label: '最小停顿间隔', hide: true },
  { prop: 'template.maxHalt', label: '最大停顿间隔', hide: true },
  { prop: 'statusText', label: '状态' },
  { prop: 'channel.size', label: '任务积压' },
  { prop: 'channel.top.score', label: '下次调度时间', formatter: DateUtil.render },
  { prop: 'createdTime', label: '创建时间', formatter: DateUtil.render, hide: true },
  { prop: 'updatedTime', label: '修改时间', formatter: DateUtil.render, hide: true },
  { prop: 'finishedTime', label: '完成时间', formatter: DateUtil.render, hide: true },
])

const rows = ref([])
const handleRefresh = (filter2query: boolean, keepPage: boolean) => {
  loading.value = true
  NodeApi.job({}).then((result: any) => {
    rows.value = Object.values(result.data)
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}
onMounted(() => {
  handleRefresh(false, true)
})

const formVisible = ref(false)
const formLoading = ref(false)
const form: any = ref({})
const handleExecute = (scope: any) => {
  form.value = Object.assign({}, scope.row.template)
  formVisible.value = true
}
const handleStart = (scope: any) => {
  ElementUtil.confirm(`确定启动'${scope.row.template.name}'作业？`).then(() => {
    NodeApi.startJob({ id: scope.row.id }, { success: true }).then(() => {
      handleRefresh(false, true)
    }).catch(() => {})
  }).catch(() => {})
}
const handlePause = (scope: any) => {
  ElementUtil.confirm(`确定暂停'${scope.row.template.name}'作业？`).then(() => {
    NodeApi.pauseJob({ id: scope.row.id }, { success: true }).then(() => {
      handleRefresh(false, true)
    }).catch(() => {})
  }).catch(() => {})
}
const handleStop = (scope: any) => {
  ElementUtil.confirm(`确定停止'${scope.row.template.name}'作业？`).then(() => {
    NodeApi.stopJob({ id: scope.row.id }, { success: true }).then(() => {
      handleRefresh(false, true)
    }).catch(() => {})
  }).catch(() => {})
}
const handleClear = (scope: any) => {
  ElementUtil.confirm(`确定清空'${scope.row.template.name}'作业？`).then(() => {
    NodeApi.clearJob({ id: scope.row.id }, { success: true }).then(() => {
      handleRefresh(false, true)
    }).catch(() => {})
  }).catch(() => {})
}

const executed = ref([])
const handleSubmit = () => {
  if (formLoading.value) return
  formLoading.value = true
  NodeApi.execute(form.value, { success: true }).then((result: any) => {
    executed.value = result.data
    handleRefresh(false, true)
  }).catch(() => {}).finally(() => {
    formLoading.value = false
  })
}
</script>

<template>
  <el-card :bordered="false" shadow="never" class="fs-table-card pb-20">
    <div class="fs-table-toolbar flex-between">
      <el-space>
        <h5>作业管理</h5>
      </el-space>
      <el-space>
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
    >
      <el-table-column type="expand">
        <template #default="scope">
          <el-descriptions border>
            <el-descriptions-item label="强制重采">{{ scope.row.channel.top.force }}</el-descriptions-item>
            <el-descriptions-item label="分派时间">{{ DateUtil.format(scope.row.channel.top.dispatchTime) }}</el-descriptions-item>
            <el-descriptions-item label="放置时间">{{ DateUtil.format(scope.row.channel.top.lastTime) }}</el-descriptions-item>
            <el-descriptions-item label="链接地址" :span="3">{{ scope.row.channel.top.url }}</el-descriptions-item>
          </el-descriptions>
          <el-input type="textarea" :model-value="JSON.stringify(scope.row.template, null, 4)" :rows="12" />
        </template>
      </el-table-column>
      <TableColumn :columns="columns">
      </TableColumn>
      <el-table-column label="操作" width="250px">
        <template #default="scope">
          <el-button link @click="handleExecute(scope)">执行</el-button>
          <el-button link @click="handleStart(scope)" v-if="scope.row.status !== 'RUNNING'">启动</el-button>
          <el-button link @click="handlePause(scope)" v-if="scope.row.status !== 'PAUSE'">暂停</el-button>
          <el-button link @click="handleStop(scope)" v-if="scope.row.status !== 'STOP'">停止</el-button>
          <el-button link @click="handleClear(scope)">清空</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
  <el-dialog v-model="formVisible" :title="`执行作业 - ${form.name}`" :close-on-click-modal="false" draggable>
    <SpiderPanParams v-model="form.params" />
    <el-table
      :data="executed"
      :border="true"
      v-loading="formLoading"
      table-layout="auto"
    >
      <el-table-column label="序号" type="index" width="60" />
      <el-table-column label="链接" prop="url" />
      <el-table-column label="结果" prop="result" />
    </el-table>
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">执行</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<style lang="scss" scoped>
</style>
