<script lang="tsx">
/**
 * 属性面板渲染器 - 根据当前选中的 bpmn 元素类型渲染对应的属性组件
 *
 * @prop {*}      modelValue - 流程信息对象（Workflow）
 * @prop {Object} bpmn       - BpmnModeler 封装实例
 * @prop {Object} element    - 当前选中的 bpmn 元素，为空时展示流程属性
 * @prop {Object} config     - 流程设计器配置
 */
import { defineAsyncComponent, defineComponent, h } from 'vue'

// 元素类型对应的属性组件是固定引用，缓存异步组件定义，避免重复创建导致重复挂载
const cache = new WeakMap<object, any>()
const resolve = (property: any) => {
  if (!cache.has(property)) cache.set(property, defineAsyncComponent(property))
  return cache.get(property)
}

const render = (props: any) => {
  const config = props.config
  const item = props.element
    ? (config.elements[props.element.type] || config.elements['bpmn:Task'] || config.canvas)
    : config.canvas
  return h(resolve(item.property), {
    workflow: props.modelValue,
    bpmn: props.bpmn,
    element: props.element,
  })
}

export default defineComponent({
  props: {
    modelValue: { type: null, required: false },
    bpmn: { type: null, required: true },
    element: { type: null, required: false },
    config: { type: null, required: true },
  },
  setup(props) {
    return () => render(props)
  }
})
</script>

<style lang="scss" scoped>
</style>
