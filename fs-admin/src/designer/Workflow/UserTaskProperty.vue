<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import UserTaskAuthority from './UserTaskAuthority.vue'
import UserTaskCandidate from './UserTaskCandidate.vue'

const active = ref('property')
const props = defineProps<{
  bpmn: any,
  element: any,
  workflow: any,
}>()

const form = ref<any>({})
let syncing = false

// 是否可配置候选组：直接由开始事件连线而来的用户任务为申报节点
const candidacy = computed(() => {
  for (const connection of props.element?.incoming || []) {
    if (connection.type !== 'bpmn:SequenceFlow') continue
    if (connection.source.type === 'bpmn:StartEvent') return false
  }
  return true
})

const formatted = (element: any) => {
  if (!element) return {}
  const obj = element.businessObject
  const attrs = obj.$attrs || {}
  const result: any = {
    id: obj.id,
    name: obj.name || '',
    documentation: props.bpmn.parseDocumentation(element),
    authority: props.bpmn.parseCDATA(obj.extensionElements?.authority?.body)
  }
  if (!candidacy.value) return result
  result.candidateGroups = attrs['flowable:candidateGroups'] || ''
  // 多方会签
  const loopCharacteristics = obj.loopCharacteristics
  if (loopCharacteristics) {
    const loop = loopCharacteristics.$attrs || {}
    result.loopCharacteristics = true
    if (Object.keys(loop).indexOf('isSequential') === -1) {
      result.isSequential = !!loopCharacteristics.isSequential
    } else {
      result.isSequential = !!loop.isSequential
    }
    result.collection = loop['flowable:collection'] || ''
    result.elementVariable = loop['flowable:elementVariable'] || ''
    result.loopCardinality = loopCharacteristics.loopCardinality?.body ?? ''
    result.completionCondition = loopCharacteristics.completionCondition?.body ?? ''
  } else {
    result.loopCharacteristics = false
  }
  return result
}

const updateProperties = (obj: any) => {
  if (!obj.id) return false
  const result: any = {
    id: obj.id,
    name: obj.name,
    documentation: props.bpmn.createDocumentation(obj.documentation)
  }

  // 权限配置
  let extensionElements = props.element.businessObject.get('extensionElements')
  if (!extensionElements) {
    extensionElements = props.bpmn.moddle.create('bpmn:ExtensionElements')
  }
  extensionElements.authority = props.bpmn.moddle.create('bpmn:FormalExpression', { body: `<![CDATA[${obj.authority}]]>` })
  result.extensionElements = extensionElements

  if (candidacy.value) {
    // 候选组
    result['flowable:candidateGroups'] = obj.candidateGroups

    // 多方会签
    if (obj.loopCharacteristics) {
      let loopCharacteristics = props.element.businessObject.loopCharacteristics
      if (!loopCharacteristics) loopCharacteristics = props.bpmn.moddle.create('bpmn:MultiInstanceLoopCharacteristics')
      loopCharacteristics.$attrs['isSequential'] = !!obj.isSequential
      loopCharacteristics.$attrs['flowable:collection'] = obj.collection ? obj.collection : null
      loopCharacteristics.$attrs['flowable:elementVariable'] = obj.elementVariable ? obj.elementVariable : null
      if (obj.loopCardinality) {
        loopCharacteristics.loopCardinality = props.bpmn.moddle.create('bpmn:Expression', { body: obj.loopCardinality })
      } else {
        loopCharacteristics.loopCardinality = null
      }
      if (obj.completionCondition) {
        loopCharacteristics.completionCondition = props.bpmn.moddle.create('bpmn:Expression', { body: obj.completionCondition })
      } else {
        loopCharacteristics.completionCondition = null
      }
      result.loopCharacteristics = loopCharacteristics
    } else {
      delete props.element.businessObject.loopCharacteristics
    }
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
        <template v-if="candidacy">
          <el-divider>任务指派</el-divider>
          <UserTaskCandidate v-model="form.candidateGroups" :bpmn="bpmn" :element="element" :workflow="workflow" />
        </template>
        <el-divider>表单权限</el-divider>
        <UserTaskAuthority v-model="form.authority" :bpmn="bpmn" :element="element" :workflow="workflow" />
      </el-form>
    </el-tab-pane>
    <el-tab-pane label="多方会签" name="multiInstance" v-if="candidacy">
      <el-form :model="form">
        <el-form-item label="启用会签"><el-switch v-model="form.loopCharacteristics" /></el-form-item>
        <template v-if="form.loopCharacteristics">
          <el-form-item label="顺序执行"><el-checkbox v-model="form.isSequential" /></el-form-item>
          <el-form-item label="循环基数"><el-input v-model="form.loopCardinality" placeholder="Loop Cardinality" /></el-form-item>
          <el-form-item label="集合变量"><el-input v-model="form.collection" placeholder="Collection" /></el-form-item>
          <el-form-item label="元素变量"><el-input v-model="form.elementVariable" placeholder="Element Variable" /></el-form-item>
          <el-form-item label="完成条件">
            <el-tooltip placement="top">
              <template #content>
                nrOfInstances - 实例总数<br />nrOfActiveInstances - 活动中<br />nrOfCompletedInstances - 已完成
              </template>
              <el-input type="textarea" v-model="form.completionCondition" placeholder="Completion Condition" />
            </el-tooltip>
          </el-form-item>
        </template>
      </el-form>
    </el-tab-pane>
  </el-tabs>
</template>

<style lang="scss" scoped>
</style>
