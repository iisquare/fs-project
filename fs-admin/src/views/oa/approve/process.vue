<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import ApiUtil from '@/utils/ApiUtil'
import ApproveApi from '@/api/oa/ApproveApi'
import ProcessViewer from '@/designer/Workflow/ProcessViewer.vue'
import ProcessStatus from './ProcessStatus.vue'

const route = useRoute()
const loading = ref(false)
const info = ref<any>({})
const highlights = ref<Record<string, string>>({})
const tasks = ref<any[]>([])
const auditComments = ref<any[]>([])

onMounted(() => {
  const id = route.query.id
  if (!id) return
  loading.value = true
  ApproveApi.process({ id }).then(result => {
    const data = ApiUtil.data(result)
    info.value = data
    if (data.highlights) {
      highlights.value = data.highlights
    }
    tasks.value = data.tasks || []
    auditComments.value = data.auditComments || []
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
})
</script>

<template>
  <el-card :bordered="false" shadow="never" v-loading="loading">
    <template #header>
      <span>流程详情 - {{ info.name || info.processName || '' }}</span>
    </template>
    <el-row :gutter="20">
      <el-col :span="16">
        <div class="diagram-section">
          <ProcessViewer v-if="info.content" :bpmn-x-m-l="info.content" :highlights="highlights" />
          <el-empty v-else description="无流程图" />
        </div>
      </el-col>
      <el-col :span="8">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="状态">
            <ProcessStatus :status="info.status" />
          </el-descriptions-item>
          <el-descriptions-item label="开始时间">{{ info.startTime }}</el-descriptions-item>
          <el-descriptions-item label="结束时间">{{ info.endTime }}</el-descriptions-item>
          <el-descriptions-item label="发起人">{{ info.startUserId }}</el-descriptions-item>
        </el-descriptions>
      </el-col>
    </el-row>
    <el-divider />
    <h4>任务列表</h4>
    <el-table :data="tasks" border size="small">
      <el-table-column prop="id" label="任务ID" />
      <el-table-column prop="name" label="任务名称" />
      <el-table-column prop="assignee" label="办理人" />
      <el-table-column prop="startTime" label="开始时间" />
      <el-table-column prop="endTime" label="结束时间" />
      <el-table-column label="状态">
        <template #default="scope">
          <ProcessStatus :status="scope.row.status" />
        </template>
      </el-table-column>
    </el-table>
    <el-divider v-if="auditComments.length" />
    <h4 v-if="auditComments.length">审批意见</h4>
    <el-timeline v-if="auditComments.length">
      <el-timeline-item
        v-for="comment in auditComments"
        :key="comment.id || comment.time"
        :timestamp="comment.time"
        placement="top"
      >
        <el-card shadow="hover" size="small">
          <p><strong>{{ comment.userId || comment.user }}</strong></p>
          <p v-if="comment.message">{{ comment.message }}</p>
        </el-card>
      </el-timeline-item>
    </el-timeline>
  </el-card>
</template>

<style lang="scss" scoped>
.diagram-section {
  height: 450px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
}
</style>
