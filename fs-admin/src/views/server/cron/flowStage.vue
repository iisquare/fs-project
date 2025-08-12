<script setup lang="ts">
import { onMounted, ref } from 'vue';
import type { TableInstance } from 'element-plus';
import { useRoute, useRouter } from 'vue-router';
import CronApi from '@/api/server/CronApi';
import DateUtil from '@/utils/DateUtil';
import TableUtil from '@/utils/TableUtil';
import Flow from '@/designer/X6/flow'

const route = useRoute()
const router = useRouter()
const tableRef = ref<TableInstance>()
const loading = ref(false)
const columns = ref([
  { prop: 'name', label: '节点' },
  { prop: 'type', label: '类型' },
  { prop: 'id', label: '时间线', slot: 'timeline', width: 300 },
  { prop: 'runTime', label: '开始时间', formatter: DateUtil.render },
  { prop: 'updatedTime', label: '结束时间', formatter: DateUtil.render },
  { prop: 'durationPretty', label: '持续时长' },
  { prop: 'state', label: '运行状态' },
])

const stageVisible = ref(false)
const stageDetail: any = ref({})
const handleDetail = (scope: any) => {
  stageDetail.value = scope.row
  stageVisible.value = true
}

const flowRef = ref()
const options: any = {
  readonly: true,
  grid: true,
}
const form: any = ref({
  stages: [],
})
const handleReload = () => {
  loading.value = true
  const params = { logId: route.query.logId }
  CronApi.flowLogStages(params).then((result: any) => {
    const colors: any = {}
    const time = new Date().getTime()
    result.data.stages.forEach((item: any) => {
      if (item.runTime === 0) {
        Object.assign(item, { min: 0, max: 0 })
      } else {
        Object.assign(item, {
          min: item.runTime - result.data.createdTime,
          max: (['RUNNING', 'WAITING'].indexOf(item.state) === -1 ? item.updatedTime : time) - result.data.createdTime
        })
      }
      Object.assign(item, {
        range: [item.min, item.max],
      })
      colors[item.stageId] = ({
        WAITING: '', // 等待调度
        RUNNING: Flow.HighlightColor.running, // 正在执行
        SKIPPED: Flow.HighlightColor.warning, // 跳过执行
        SUCCEED: Flow.HighlightColor.success, // 执行成功
        FAILED: Flow.HighlightColor.error, // 执行失败
        TERMINATED: Flow.HighlightColor.terminated, // 执行终止
      } as any)[item.state]
    })
    Object.assign(form.value, result.data, {
      min: 0,
      max: (result.data.state === 'RUNNING' ? time : result.data.updatedTime) - result.data.createdTime,
      stages: TableUtil.tree(result.data.stages, '', 'stageId')
    })
    flowRef.value.flow.fromJSON(form.value.content?.cells ?? [])
    flowRef.value.flow.highlight(colors)
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}
onMounted(() => {
  handleReload()
})
</script>

<template>
  <el-card :bordered="false" shadow="never">
    <template #header>
      <div class="flex-between">
        <div>调度明细</div>
        <el-space>
          <el-button type="primary" @click="handleReload" :loading="loading">刷新</el-button>
          <el-button @click="router.go(-1)">返回</el-button>
        </el-space>
      </div>
    </template>
    <el-descriptions border>
      <el-descriptions-item label="日志标识">{{ form.id }}</el-descriptions-item>
      <el-descriptions-item label="流程标识">{{ form.flowId }}</el-descriptions-item>
      <el-descriptions-item label="流程名称">{{ form.flowInfo?.name }}</el-descriptions-item>
      <el-descriptions-item label="并发度">{{ form.concurrent }}</el-descriptions-item>
      <el-descriptions-item label="并发策略">{{ form.concurrency }}</el-descriptions-item>
      <el-descriptions-item label="失败策略">{{ form.failure }}</el-descriptions-item>
      <el-descriptions-item label="运行状态">{{ form.state }}</el-descriptions-item>
      <el-descriptions-item label="开始时间">{{ DateUtil.format(form.createdTime) }}</el-descriptions-item>
      <el-descriptions-item label="结束时间">{{ DateUtil.format(form.updatedTime) }}</el-descriptions-item>
      <el-descriptions-item label="持续时间" :span="3">{{ form.durationPretty }}</el-descriptions-item>
    </el-descriptions>
    <div class="flow"><X6Container ref="flowRef" v-bind="options" /></div>
    <el-table
      ref="tableRef"
      :data="form.stages"
      :row-key="(record: any) => record.stageId"
      :border="true"
      v-loading="loading"
      default-expand-all
      table-layout="auto"
    >
      <TableColumn :columns="columns">
        <template #timeline="scope">
          <el-slider :min="form.min" :max="form.max" range v-model="scope.row.range" />
        </template>
      </TableColumn>
      <el-table-column label="操作" width="80px">
        <template #default="scope">
          <el-space>
            <el-button link @click="handleDetail(scope)" type="primary">详情</el-button>
          </el-space>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
  <el-drawer v-model="stageVisible" :close-on-click-modal="false" :show-close="false" :destroy-on-close="true" size="80%">
    <template #header="{ close, titleId, titleClass }">
      <h4 :id="titleId" :class="titleClass">节点信息</h4>
      <el-space>
        <el-button @click="close">取消</el-button>
      </el-space>
    </template>
    <el-descriptions border label-width="80px">
      <el-descriptions-item label="标识">{{ stageDetail.stageId }}</el-descriptions-item>
      <el-descriptions-item label="类型">{{ stageDetail.type }}</el-descriptions-item>
      <el-descriptions-item label="名称">{{ stageDetail.name }}</el-descriptions-item>
      <el-descriptions-item label="依赖" :span="3">{{ stageDetail.depend }}</el-descriptions-item>
      <el-descriptions-item label="配置" :span="3">
        <el-checkbox v-model="stageDetail.skipped" disabled>跳过执行</el-checkbox>
      </el-descriptions-item>
      <el-descriptions-item label="参数" :span="3">{{ stageDetail.data }}</el-descriptions-item>
      <el-descriptions-item label="输出" :span="3">
        <el-input type="textarea" v-model="stageDetail.content" :rows="20" />
      </el-descriptions-item>
    </el-descriptions>
  </el-drawer>
</template>

<style lang="scss" scoped>
.flow {
  width: 100%;
  height: 500px;
  overflow: hidden;
  margin: 25px 0px;
  border: 1px solid #e8e8e8;
}
:deep(.el-slider) {
  .el-slider__button {
    display: none;
  }
}
</style>
