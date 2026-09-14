<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import ApiUtil from '@/utils/ApiUtil'
import DateUtil from '@/utils/DateUtil'
import DataUtil from '@/utils/DataUtil'
import ApproveApi from '@/api/oa/ApproveApi'
import WorkflowApi from '@/api/oa/WorkflowApi'
import ProcessViewer from '@/designer/Workflow/ProcessViewer.vue'
import FlexForm from '@/designer/FlexForm/FlexForm.vue'
import flexFormConfig from '@/designer/FlexForm/config'
import workflowConfig from '@/designer/Workflow/config'

const route = useRoute()
const loading = ref(false)
const info = ref<any>({})
const frame = ref<any>({ id: 0, name: '', widgets: [], options: {} })
const form = ref<any>({})
const tasks = ref<any[]>([])

// 流程查看页只读展示：仅保留 viewable，节点上配置的可编辑权限在此不生效
const formAuthority = computed(() => {
  return DataUtil.filtration(info.value?.authority || {}, { viewable: 'viewable' }, null)
})

const handleLoad = () => {
  const param = {
    processInstanceId: route.query.processInstanceId,
    taskId: route.query.taskId,
  }
  if (!param.processInstanceId) return
  // 未指定taskId时为流程管理入口，指定时（含空串）为审批入口
  const service: any = Object.keys(route.query).indexOf('taskId') === -1 ? WorkflowApi : ApproveApi
  loading.value = true
  service.process(param).then((result: any) => {
    const data = ApiUtil.data(result)
    info.value = data
    frame.value = data.formInfo || frame.value
    form.value = data.form || {}
    tasks.value = (data.historicTaskInstances || []).map((task: any) => {
      return Object.assign({}, task, workflowConfig.audit(data.comments, task.id))
    })
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}

onMounted(() => {
  handleLoad()
})
</script>

<template>
  <el-card :bordered="false" shadow="never" v-loading="loading">
    <template #header>
      <span>流程详情 - {{ info.name || '' }}</span>
    </template>
    <div class="page-content">
      <el-descriptions :column="3" border size="small">
        <el-descriptions-item label="流程名称">{{ info.name }}</el-descriptions-item>
        <el-descriptions-item label="单据描述">{{ info.description || '暂无' }}</el-descriptions-item>
        <el-descriptions-item label="业务编号">
          {{ info.processInstanceInfo?.businessKey || info.historicProcessInstanceInfo?.businessKey }}
        </el-descriptions-item>
        <el-descriptions-item label="发起人">
          {{ info.historicProcessInstanceInfo?.startUserName || info.historicProcessInstanceInfo?.startUserId }}
        </el-descriptions-item>
        <el-descriptions-item label="开始时间">
          {{ DateUtil.format(info.processInstanceInfo?.startTime || info.historicProcessInstanceInfo?.startTime) }}
        </el-descriptions-item>
        <el-descriptions-item label="结束时间">
          {{ DateUtil.format(info.historicProcessInstanceInfo?.endTime) }}
        </el-descriptions-item>
      </el-descriptions>
      <el-divider content-position="left">流程图</el-divider>
      <div class="diagram-section">
        <ProcessViewer v-if="info.content" :bpmn-x-m-l="info.content" :activities="info.historicActivityInstances" />
        <el-empty v-else description="无流程图" />
      </div>
      <el-divider content-position="left">表单数据</el-divider>
      <div class="form-section">
        <FlexForm v-model="form" :config="flexFormConfig" :frame="frame" :authority="formAuthority" v-if="frame.widgets?.length" />
        <el-empty v-else description="无表单配置" />
      </div>
      <template v-if="tasks.length">
        <el-divider content-position="left">流转记录</el-divider>
        <el-table :data="tasks" border size="small">
          <el-table-column prop="id" label="任务ID" />
          <el-table-column prop="name" label="节点名称" />
          <el-table-column prop="assigneeName" label="处理人" />
          <el-table-column label="审批备注">
            <template #default="scope">{{ scope.row.audit?.message }}</template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间" :formatter="DateUtil.render" width="170" />
          <el-table-column prop="claimTime" label="签收时间" :formatter="DateUtil.render" width="170" />
          <el-table-column prop="endTime" label="结束时间" :formatter="DateUtil.render" width="170" />
          <el-table-column label="持续时间">
            <template #default="scope">{{ workflowConfig.duration(scope.row.durationInMillis) }}</template>
          </el-table-column>
        </el-table>
      </template>
    </div>
  </el-card>
</template>

<style lang="scss" scoped>
// 内容区限宽并自动居中，避免宽屏下表单被拉满整个页面
.page-content {
  max-width: 1080px;
  width: 100%;
  margin: 0 auto;
}
.form-section {
  min-height: 200px;
}
.diagram-section {
  height: 400px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
}
</style>
