<script setup lang="ts">
import { ref, watch } from 'vue'
import UserTaskAuthority from './UserTaskAuthority.vue'

const active = ref('property')
const props = defineProps<{
  bpmn: any,
  element: any,
  workflow: any,
}>()

const form = ref<any>({})
let syncing = false

const formatted = (element: any) => {
  if (!element) return {}
  const obj = element.businessObject
  return {
    id: obj.id,
    name: obj.name || '',
    documentation: props.bpmn.parseDocumentation(element),
    authority: props.bpmn.parseCDATA(obj.extensionElements?.authority?.body)
  }
}

const updateProperties = (obj: any) => {
  if (!obj.id) return false
  const result: any = {
    id: obj.id,
    name: obj.name,
    documentation: props.bpmn.createDocumentation(obj.documentation)
  }
  let extensionElements = props.element.businessObject.get('extensionElements')
  if (!extensionElements) {
    extensionElements = props.bpmn.moddle.create('bpmn:ExtensionElements')
  }
  extensionElements.authority = props.bpmn.moddle.create('bpmn:FormalExpression', { body: `<![CDATA[${obj.authority}]]>` })
  result.extensionElements = extensionElements
  props.bpmn.modeling.updateProperties(props.element, result)
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
        <el-divider>表单权限</el-divider>
        <UserTaskAuthority v-model="form.authority" :bpmn="bpmn" :element="element" :workflow="workflow" />
      </el-form>
    </el-tab-pane>
  </el-tabs>
</template>

<style lang="scss" scoped>
</style>
