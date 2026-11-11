<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import ClusterApi from '@/api/bi/ClusterApi'
import ApiUtil from '@/utils/ApiUtil'
import PropertyPanel from './PropertyPanel.vue'
import PropertySection from '@/components/Layout/PropertySection.vue'

const model: any = defineModel()
const tips: any = defineModel('tips', { type: null })
const props = defineProps<{
  config: any,
  instance: any,
}>()

const clusters: any = ref([])
const clusterId = computed({
  get: () => Number(model.value?.clusterId) > 0 ? Number(model.value.clusterId) : undefined,
  set: (value: any) => {
    model.value.clusterId = value === undefined || value === null || value === ''
      ? 0 : Number(value)
  },
})
const handleClusters = () => {
  const engine = model.value?.engine
  if (!engine) { clusters.value = []; return }
  ClusterApi.options({ engine }).then((result: any) => {
    clusters.value = ApiUtil.data(result) ?? []
  }).catch(() => {})
}
watch(() => model.value?.engine, handleClusters)
onMounted(handleClusters)
</script>

<template>
  <property-panel title="流程属性" tab-title="流程属性">
    <el-form :model="model" label-position="top">
      <property-section title="基础信息" :reset-key="model.id">
        <el-form-item label="流程标识" v-if="model.id">{{ model.id }}</el-form-item>
        <el-form-item label="流程名称">
          <el-input v-model="model.name" />
        </el-form-item>
        <el-form-item label="计算引擎">
          <el-select v-model="model.engine" placeholder="请选择">
            <el-option v-for="(value, key) in config.engines" :key="key" :value="key" :label="value" />
          </el-select>
        </el-form-item>
        <el-form-item label="处理模式">
          <el-select v-model="model.model" placeholder="请选择">
            <el-option v-for="(value, key) in config.models" :key="key" :value="key" :label="value" />
          </el-select>
        </el-form-item>
        <el-form-item label="计算集群">
          <el-select v-model="clusterId" placeholder="本地执行" clearable>
            <el-option v-for="item in clusters" :key="item.id" :value="item.id" :label="item.name" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="model.sort" :controls="false" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="model.status" placeholder="请选择">
            <el-option v-for="(value, key) in config.status" :key="key" :value="key" :label="value" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input type="textarea" v-model="model.description" />
        </el-form-item>
      </property-section>

      <property-section title="定时调度" :reset-key="model.id">
        <el-form-item label="启用调度">
          <el-switch v-model="model.schedule" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="表达式">
          <form-cron v-model="model.expression" />
        </el-form-item>
        <el-form-item label="下次调度">
          <span>{{ model.nextTime ? new Date(model.nextTime).toLocaleString() : '暂无' }}</span>
        </el-form-item>
      </property-section>

      <property-section title="容错恢复" :reset-key="model.id">
        <el-form-item label="自动恢复">
          <el-switch v-model="model.autoRecover" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="最大重试">
          <el-input-number v-model="model.maxRetry" :min="0" :controls="false" />
        </el-form-item>
      </property-section>

      <property-section title="消息通知" :reset-key="model.id">
        <el-form-item label="失败通知" v-if="model.notify">
          <el-input type="textarea" v-model="model.notify.failure" :rows="4" placeholder="采用英文逗号分隔" />
        </el-form-item>
      </property-section>
    </el-form>
  </property-panel>
</template>

<style lang="scss" scoped>
</style>
