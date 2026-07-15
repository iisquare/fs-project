<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ApiUtil from '@/utils/ApiUtil'
import ApproveApi from '@/api/oa/ApproveApi'
import ProcessViewer from '@/designer/Workflow/ProcessViewer.vue'
import FlexForm from '@/designer/FlexForm/FlexForm.vue'
import flexFormConfig from '@/designer/FlexForm/config'
import FormAudit from './FormAudit.vue'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const submitting = ref(false)
const formData = ref<any>({})
const frame = ref<any>({ content: { widgets: [], options: {} } })
const info = ref<any>({})
const highlights = ref<Record<string, string>>({})
const audit = ref({ message: '', local: false })

onMounted(() => {
  const taskId = route.query.taskId
  if (!taskId) return
  loading.value = true
  ApproveApi.transact({ taskId }).then(result => {
    const data = ApiUtil.data(result)
    info.value = data
    formData.value = data.formData || {}
    if (data.formConfig) {
      frame.value = data.formConfig
    }
    if (data.highlights) {
      highlights.value = data.highlights
    }
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
})

const handleComplete = (approved: boolean) => {
  submitting.value = true
  ApproveApi.complete({
    taskId: route.query.taskId,
    formData: formData.value,
    audit: audit.value,
    approved,
  }, { success: true }).then(() => {
    router.go(-1)
  }).catch(() => {}).finally(() => {
    submitting.value = false
  })
}
</script>

<template>
  <el-card :bordered="false" shadow="never" v-loading="loading">
    <template #header>
      <span>{{ info.taskName || '任务办理' }} - {{ info.processName }}</span>
    </template>
    <el-row :gutter="20">
      <el-col :span="14">
        <div class="form-section">
          <FlexForm v-if="frame.content.widgets?.length" v-model="formData" :config="flexFormConfig" :frame="frame" :authority="{}" />
          <el-empty v-else description="无表单配置" />
        </div>
        <el-divider />
        <FormAudit v-model="audit" />
        <el-space>
          <el-button type="success" @click="() => handleComplete(true)" :loading="submitting">通过</el-button>
          <el-button type="danger" @click="() => handleComplete(false)" :loading="submitting">驳回</el-button>
          <el-button @click="router.go(-1)">返回</el-button>
        </el-space>
      </el-col>
      <el-col :span="10">
        <div class="diagram-section">
          <ProcessViewer v-if="info.content" :bpmn-x-m-l="info.content" :highlights="highlights" />
          <el-empty v-else description="无流程图" />
        </div>
      </el-col>
    </el-row>
  </el-card>
</template>

<style lang="scss" scoped>
.form-section {
  min-height: 200px;
}
.diagram-section {
  height: 400px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
}
</style>
