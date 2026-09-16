<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ApiUtil from '@/utils/ApiUtil'
import DateUtil from '@/utils/DateUtil'
import ApproveApi from '@/api/oa/ApproveApi'
import ProcessViewer from '@/designer/Workflow/ProcessViewer.vue'
import FlexForm from '@/designer/FlexForm/FlexForm.vue'
import flexFormConfig from '@/designer/FlexForm/config'
import workflowConfig from '@/designer/Workflow/config'
import { useUserStore } from '@/stores/user'
import FormAudit from '@/views/oa/components/FormAudit.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const submitting = ref(false)
const info = ref<any>({})
const frame = ref<any>({ id: 0, name: '', widgets: [], options: {} })
const formRef = ref<any>()
const form = ref<any>({})
const audit = ref({ message: '', local: false })
const tasks = ref<any[]>([])

const rejectable = computed(() => {
  const startUserId = info.value?.processInstanceInfo?.startUserId
  return Number.parseInt(startUserId) !== (userStore as any)?.data?.info?.id
})

onMounted(() => {
  const taskId = route.query.taskId
  if (!taskId) return
  loading.value = true
  ApproveApi.transact({ taskId, withForm: true }).then((result: any) => {
    const data = ApiUtil.data(result)
    info.value = data
    frame.value = data.formInfo || frame.value
    form.value = data.form || {}
    tasks.value = (data.historicTaskInstances || []).map((task: any) => {
      return Object.assign({}, task, workflowConfig.audit(data.comments, task.id))
    })
    Object.assign(audit.value, workflowConfig.audit(data.comments, data.taskId).audit)
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
})

const handleComplete = (modeForce: boolean, modeComplete: boolean, modeReject: boolean) => {
  formRef.value?.validate((valid: boolean) => {
    if (!valid && !modeForce) return
    if (submitting.value) return
    submitting.value = true
    ApproveApi.complete({
      taskId: info.value.taskId,
      form: form.value,
      audit: audit.value,
      modeForce,
      modeComplete,
      modeReject,
    }, { success: true }).then(() => {
      router.push({ path: '/oa/approve/assignee' })
    }).catch(() => {}).finally(() => {
      submitting.value = false
    })
  })
}
</script>

<template>
  <el-card :bordered="false" shadow="never" v-loading="loading">
    <template #header>
      <span>{{ (info.processInstanceInfo && info.processInstanceInfo.name) || info.name || '任务办理' }}</span>
    </template>
    <div class="page-content">
      <el-descriptions :column="2" border size="small">
        <el-descriptions-item label="单据名称">{{ info.name }}</el-descriptions-item>
        <el-descriptions-item label="单据描述">{{ info.description || '暂无' }}</el-descriptions-item>
      </el-descriptions>
      <el-divider content-position="left">流程图</el-divider>
      <div class="diagram-section">
        <ProcessViewer v-if="info.content" :bpmn-x-m-l="info.content" :activities="info.historicActivityInstances" />
        <el-empty v-else description="无流程图" />
      </div>
      <el-divider content-position="left">表单数据</el-divider>
      <div class="form-section">
        <FlexForm ref="formRef" v-model="form" :config="flexFormConfig" :frame="frame" :authority="info.authority || {}" v-if="frame.widgets?.length" />
        <el-empty v-else description="无表单配置" />
      </div>
      <el-divider content-position="left">备注信息</el-divider>
      <FormAudit v-model="audit" />
      <div class="footer-action">
        <el-space>
          <el-button type="success" @click="() => handleComplete(false, true, false)" :loading="submitting">通过</el-button>
          <el-button type="warning" @click="() => handleComplete(false, false, false)" :loading="submitting">保存</el-button>
          <el-button type="danger" @click="() => handleComplete(false, false, true)" :loading="submitting" v-if="rejectable">驳回</el-button>
          <el-button type="danger" plain @click="() => handleComplete(true, true, false)" :loading="submitting">强制提交</el-button>
          <el-button @click="router.go(-1)">返回</el-button>
        </el-space>
      </div>
      <template v-if="tasks.length">
        <el-divider content-position="left">流转记录</el-divider>
        <el-table :data="tasks" border size="small">
          <el-table-column prop="id" label="ID" />
          <el-table-column prop="name" label="节点名称" />
          <el-table-column prop="assigneeName" label="处理人" />
          <el-table-column label="审批备注">
            <template #default="scope">{{ scope.row.audit?.message }}</template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间" :formatter="DateUtil.render" width="170" />
          <el-table-column prop="claimTime" label="签收时间" :formatter="DateUtil.render" width="170" />
          <el-table-column prop="endTime" label="结束时间" :formatter="DateUtil.render" width="170" />
          <el-table-column label="工作时间">
            <template #default="scope">{{ workflowConfig.duration(scope.row.workTimeInMillis) }}</template>
          </el-table-column>
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
.footer-action {
  padding-top: 15px;
  text-align: center;
}
</style>
