<script setup lang="ts">
import { computed, ref, watch } from 'vue'

const active = ref('property')
const props = defineProps<{
  bpmn: any,
  element: any,
  workflow: any,
}>()

const form = ref<any>({})
let syncing = false

const conditionable = computed(() => {
  const source = props.element?.source
  return ['bpmn:ExclusiveGateway', 'bpmn:InclusiveGateway'].indexOf(source?.type) !== -1
})

const formatted = (element: any) => {
  if (!element) return {}
  const obj = element.businessObject
  return {
    id: obj.id,
    name: obj.name || '',
    documentation: props.bpmn.parseDocumentation(element),
    conditionExpression: conditionable.value ? props.bpmn.parseCDATA(obj.conditionExpression?.body) : ''
  }
}

const updateProperties = (obj: any) => {
  if (!obj.id) return false
  const result: any = {
    id: obj.id,
    name: obj.name,
    documentation: props.bpmn.createDocumentation(obj.documentation)
  }
  if (conditionable.value) {
    result.conditionExpression = props.bpmn.moddle.create('bpmn:FormalExpression', { body: `<![CDATA[${obj.conditionExpression}]]>` })
  }
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
        <template v-if="conditionable">
          <el-divider>表达式</el-divider>
          <el-form-item label="条件">
            <el-input type="textarea" v-model="form.conditionExpression" placeholder="Condition Expression" />
          </el-form-item>
        </template>
      </el-form>
    </el-tab-pane>
  </el-tabs>
</template>

<style lang="scss" scoped>
</style>
