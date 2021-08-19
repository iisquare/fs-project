<template>
  <a-tabs default-active-key="property" :animated="false">
    <a-tab-pane key="property" tab="节点属性">
      <a-form-model :model="form" labelAlign="left" :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
        <a-form-model-item label="类型">{{ element.type }}</a-form-model-item>
        <a-form-model-item label="标识"><a-input v-model="form.id" auto-complete="on" /></a-form-model-item>
        <a-form-model-item label="名称"><a-input v-model="form.name" auto-complete="on" /></a-form-model-item>
        <a-form-model-item label="描述"><a-textarea v-model="form.documentation" /></a-form-model-item>
        <a-divider v-if="candidacy">任务指派</a-divider>
        <user-task-candidate v-model="form.candidateGroups" :bpmn="bpmn" :element="element" :workflow="value" v-if="candidacy" />
        <a-divider>表单权限</a-divider>
        <user-task-authority v-model="form.authority" :bpmn="bpmn" :element="element" :workflow="value" />
      </a-form-model>
    </a-tab-pane>
    <a-tab-pane key="multiInstance" tab="多方会签" v-if="candidacy">
      <a-form-model :model="form" labelAlign="left" :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
        <a-form-model-item label="启用会签"><a-switch v-model="form.loopCharacteristics" /></a-form-model-item>
        <a-form-model-item label="顺序执行"><a-checkbox v-model="form.isSequential" /></a-form-model-item>
        <a-form-model-item label="循环基数"><a-input v-model="form.loopCardinality" placeholder="Loop Cardinality" auto-complete="on" /></a-form-model-item>
        <a-form-model-item label="集合变量"><a-input v-model="form.collection" placeholder="Collection" auto-complete="on" /></a-form-model-item>
        <a-form-model-item label="元素变量"><a-input v-model="form.elementVariable" placeholder="Element Variable" auto-complete="on" /></a-form-model-item>
        <a-form-model-item label="完成条件">
          <a-tooltip>
            <template slot="title">nrOfInstances - 实例总数<br/>nrOfActiveInstances - 活动中<br/>nrOfCompletedInstances - 已完成</template>
            <a-textarea v-model="form.completionCondition" placeholder="Completion Condition" />
          </a-tooltip>
        </a-form-model-item>
      </a-form-model>
    </a-tab-pane>
  </a-tabs>
</template>

<script>
import UserTaskCandidate from './UserTaskCandidate'
import UserTaskAuthority from './UserTaskAuthority'

export default {
  name: 'UserTaskProperty',
  components: { UserTaskCandidate, UserTaskAuthority },
  props: {
    value: { type: Object, required: true },
    bpmn: { type: Object, required: true },
    element: { type: Object, required: true }
  },
  data () {
    return {
      form: {}
    }
  },
  computed: {
    candidacy () { // 是否可配置候选组
      for (const connection of this.element.incoming) {
        if (connection.type !== 'bpmn:SequenceFlow') continue
        if (connection.source.type === 'bpmn:StartEvent') return false
      }
      return true
    }
  },
  watch: {
    element: {
      handler (obj) {
        this.form = this.formatted(obj)
      },
      immediate: true
    },
    form: {
      handler (obj) {
        this.updateProperties(obj)
      },
      deep: true
    }
  },
  methods: {
    updateProperties (obj) {
      if (!obj.id) return false
      const result = {
        id: obj.id,
        name: obj.name,
        documentation: this.bpmn.createDocumentation(obj.documentation)
      }

      // 权限配置
      let extensionElements = this.element.businessObject.get('extensionElements')
      if (!extensionElements) {
        extensionElements = this.bpmn.moddle.create('bpmn:ExtensionElements')
      }
      extensionElements.authority = this.bpmn.moddle.create('bpmn:FormalExpression', { body: `<![CDATA[${obj.authority}]]>` })
      result.extensionElements = extensionElements

      if (this.candidacy) {
        // 候选组
        result['flowable:candidateGroups'] = obj.candidateGroups

        // 多方会签
        if (obj.loopCharacteristics) {
          let multiInstanceLoopCharacteristics = this.element.businessObject.loopCharacteristics
          if (!multiInstanceLoopCharacteristics) multiInstanceLoopCharacteristics = this.bpmn.moddle.create('bpmn:MultiInstanceLoopCharacteristics')
          multiInstanceLoopCharacteristics.$attrs['isSequential'] = !!obj.isSequential
          if (obj.collection) {
            multiInstanceLoopCharacteristics.$attrs['flowable:collection'] = obj.collection
          } else {
            multiInstanceLoopCharacteristics.$attrs['flowable:collection'] = null
          }
          if (obj.elementVariable) {
            multiInstanceLoopCharacteristics.$attrs['flowable:elementVariable'] = obj.elementVariable
          } else {
            multiInstanceLoopCharacteristics.$attrs['flowable:elementVariable'] = null
          }
          if (obj.loopCardinality) {
            multiInstanceLoopCharacteristics.loopCardinality = this.bpmn.moddle.create('bpmn:Expression', { body: obj.loopCardinality })
          } else {
            multiInstanceLoopCharacteristics.loopCardinality = null
          }
          if (obj.completionCondition) {
            const completionCondition = this.bpmn.moddle.create('bpmn:Expression', { body: obj.completionCondition })
            multiInstanceLoopCharacteristics.completionCondition = completionCondition
          } else {
            multiInstanceLoopCharacteristics.completionCondition = null
          }
          result.loopCharacteristics = multiInstanceLoopCharacteristics
        } else {
          delete this.element.businessObject.loopCharacteristics
        }
      }
      this.bpmn.modeling.updateProperties(this.element, result)
      return true
    },
    formatted (element) {
      const obj = element.businessObject
      const attrs = obj.$attrs
      const result = {
        id: obj.id,
        name: obj.name || '',
        documentation: this.bpmn.parseDocumentation(element),
        authority: this.bpmn.parseCDATA(obj.extensionElements?.authority?.body) // 权限配置
      }

      if (!this.candidacy) return result
      result['candidateGroups'] = attrs['flowable:candidateGroups'] || '' // 候选组

      // 多方会签
      const multiInstanceLoopCharacteristics = obj.loopCharacteristics
      if (multiInstanceLoopCharacteristics) {
        result.loopCharacteristics = true
        const loop = multiInstanceLoopCharacteristics.$attrs
        if (Object.keys(loop).indexOf('isSequential') === -1) {
          Object.assign(result, { isSequential: !!multiInstanceLoopCharacteristics.isSequential })
        } else {
          Object.assign(result, { isSequential: !!loop.isSequential })
        }
        Object.assign(result, {
          collection: loop['flowable:collection'] || '',
          elementVariable: loop['flowable:elementVariable'] || '',
          loopCardinality: multiInstanceLoopCharacteristics.loopCardinality?.body ?? '',
          completionCondition: multiInstanceLoopCharacteristics.completionCondition?.body ?? ''
        })
      } else {
        result.loopCharacteristics = false
      }
      return result
    }
  }
}
</script>

<style lang="less" scoped>

</style>
