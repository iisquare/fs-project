<script setup lang="ts">
import { ref, watch } from 'vue'

const active = ref('property')
const props = defineProps<{
  bpmn: any,
  element: any,
  workflow: any,
}>()

const form = ref<any>({})
// 由元素同步数据时，避免反向触发模型更新
let syncing = false

const formatted = (element: any) => {
  if (!element) return {}
  const obj = element.businessObject
  return {
    id: obj.id,
    name: obj.name || '',
    documentation: props.bpmn.parseDocumentation(element)
  }
}

const updateProperties = (obj: any) => {
  if (!obj.id) return false
  props.bpmn.modeling.updateProperties(props.element, {
    id: obj.id,
    name: obj.name,
    documentation: props.bpmn.createDocumentation(obj.documentation)
  })
  return true
}

watch(() => props.element, (element) => {
  syncing = true
  form.value = formatted(element)
  syncing = false
}, { immediate: true })

watch(form, (obj) => {
  if (syncing) return
  updateProperties(obj)
}, { deep: true, flush: 'sync' })
</script>

<template>
  <el-tabs v-model="active" class="tab-property">
    <el-tab-pane label="节点属性" name="property">
      <el-form :model="form">
        <el-form-item label="类型">{{ element?.type }}</el-form-item>
        <el-form-item label="标识"><el-input v-model="form.id" autocomplete="off" /></el-form-item>
        <el-form-item label="名称"><el-input v-model="form.name" autocomplete="off" /></el-form-item>
        <el-form-item label="描述"><el-input type="textarea" v-model="form.documentation" /></el-form-item>
      </el-form>
    </el-tab-pane>
  </el-tabs>
</template>

<style lang="scss" scoped>
</style>
