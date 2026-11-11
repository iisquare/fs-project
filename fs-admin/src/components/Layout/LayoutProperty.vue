<script lang="tsx">
/**
 * 属性面板动态渲染器 - 根据传入的 property 组件异步加载并渲染属性编辑面板，自动在 modelValue 和 activeItem 之间选择绑定目标。
 *
 * @prop {*}          modelValue  - 画布全局配置参数（当 activeItem 为空时作为 v-model）
 * @prop {*}          activeItem  - 当前激活组件的数据（非空时作为 v-model）
 * @prop {*}          instance    - 画布操作实例，传递给属性组件
 * @prop {*}          config      - 全局配置参数，传递给属性组件
 * @prop {*}          context     - 当前画布数据，传递给属性组件
 * @prop {*}          tips        - 提示消息，传递给属性组件
 * @prop {Component}  property    - 要渲染的属性组件（异步组件）
 *
 * @emits update:modelValue  - 全局配置变更
 * @emits update:activeItem  - 当前激活组件变更
 * @emits update:tips        - 提示消息变更
 *
 * @example
 * <layout-property v-model="canvasConfig" :activeItem="selectedNode" :property="NodeProperty" />
 */
import DataUtil from '@/utils/DataUtil'
import { defineAsyncComponent, defineComponent, h } from 'vue'

/**
 * 属性组件缓存：按 loader 函数缓存异步组件定义。
 *
 * defineAsyncComponent 每次调用都会生成新的组件对象，写在 render 里会让 Vue 认为组件类型发生了变化，
 * 于是每次重渲染都整块卸载重建：编辑器实例重建、异步加载多闪一帧、面板内折叠状态归零、挂载时发起的请求重复。
 * loader 来自配置、引用稳定，按引用缓存后，切换节点只是一次普通的 props 更新。
 */
const propertyComponents = new WeakMap<any, any>()
const propertyComponent = (property: any) => {
  if (!property) return null
  // 非函数视为已经定义好的组件，直接使用
  if ('function' !== typeof property) return property
  if (!propertyComponents.has(property)) {
    propertyComponents.set(property, defineAsyncComponent(property))
  }
  return propertyComponents.get(property)
}

const render = (props: any) => {
  const component = propertyComponent(props.property)
  if (!component) return null
  return h(component, DataUtil.empty(props.activeItem) ? {
    modelValue: props.modelValue,
    instance: props.instance,
    config: props.config,
    context: props.context,
    tips: props.tips,
  } : {
    modelValue: props.activeItem,
    instance: props.instance,
    config: props.config,
    context: props.context,
    tips: props.tips,
  })
}

export default defineComponent({
  props: {
    modelValue: { type: null },
    activeItem: { type: null, required: true },
    instance: { type: null, required: false },
    config: { type: null, required: false },
    context: { type: null, required: false },
    tips: { type: null, required: false },
    property: { type: null, required: false },
  },
  emits: {
    'update:modelValue': (val?: any) => true,
    'update:activeItem': (val?: any) => true,
    'update:tips': (val?: any) => true,
  },

  setup(props) {
    return () => render(props)
  }
})
</script>

<style lang="scss" scoped>
</style>
