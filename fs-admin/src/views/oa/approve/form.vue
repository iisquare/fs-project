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
const audit = ref({ message: '', local: false })

onMounted(() => {
  const deploymentId = route.query.deploymentId
  if (!deploymentId) return
  loading.value = true
  ApproveApi.form({ deploymentId }).then(result => {
    const data = ApiUtil.data(result)
    info.value = data
    formData.value = data.formData || {}
    if (data.formConfig) {
      frame.value = data.formConfig
    }
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
})

const handleSubmit = () => {
  submitting.value = true
  ApproveApi.submit({
    deploymentId: route.query.deploymentId,
    formData: formData.value,
    audit: audit.value,
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
      <span>{{ info.name || '流程表单' }}</span>
    </template>
    <el-row :gutter="20">
      <el-col :span="14">
        <div class="form-section">
          <FlexForm v-if="frame.content.widgets?.length" v-model="formData" :config="flexFormConfig" :frame="frame" :authority="{}" />
          <el-empty v-else description="无表单配置" />
        </div>
        <el-divider />
        <FormAudit v-model="audit" />
        <el-button type="primary" @click="handleSubmit" :loading="submitting">提交</el-button>
        <el-button @click="router.go(-1)">返回</el-button>
      </el-col>
      <el-col :span="10">
        <div class="diagram-section">
          <ProcessViewer v-if="info.content" :bpmn-x-m-l="info.content" />
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
