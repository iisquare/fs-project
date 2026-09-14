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
const info = ref<any>({})
const frame = ref<any>({ id: 0, name: '', widgets: [], options: {} })
const formRef = ref<any>()
const form = ref<any>({})
const audit = ref({ message: '', local: false })

onMounted(() => {
  const id = route.query.id || route.query.workflowId
  if (!id) return
  loading.value = true
  ApproveApi.form({ workflowId: id, withForm: true }).then((result: any) => {
    const data = ApiUtil.data(result)
    info.value = data
    frame.value = data.formInfo || frame.value
    form.value = data.form || {}
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
})

const handleSubmit = (modeForce: boolean, modeComplete: boolean) => {
  formRef.value?.validate((valid: boolean) => {
    if (!valid && !modeForce) return
    if (submitting.value) return
    submitting.value = true
    ApproveApi.submit({
      workflowId: info.value.id,
      form: form.value,
      audit: audit.value,
      modeForce,
      modeComplete,
    }, { success: true }).then(() => {
      router.push({ path: '/oa/approve/history' })
    }).catch(() => {}).finally(() => {
      submitting.value = false
    })
  })
}
</script>

<template>
  <el-card :bordered="false" shadow="never" v-loading="loading">
    <template #header>
      <span>{{ info.name || '单据填报' }}</span>
    </template>
    <div class="page-content">
      <el-descriptions :column="2" border size="small">
        <el-descriptions-item label="单据名称">{{ info.name }}</el-descriptions-item>
        <el-descriptions-item label="单据描述">{{ info.description || '暂无' }}</el-descriptions-item>
      </el-descriptions>
      <el-divider content-position="left">流程图</el-divider>
      <div class="diagram-section">
        <ProcessViewer v-if="info.content" :bpmn-x-m-l="info.content" />
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
          <el-button type="primary" @click="() => handleSubmit(false, true)" :loading="submitting">提交</el-button>
          <el-button type="danger" @click="() => handleSubmit(true, true)" :loading="submitting">强制提交</el-button>
          <el-button @click="() => handleSubmit(false, false)" :loading="submitting">保存</el-button>
          <el-button @click="router.go(-1)">返回</el-button>
        </el-space>
      </div>
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
