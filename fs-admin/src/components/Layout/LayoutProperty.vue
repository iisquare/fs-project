<script lang="tsx">
/**
 * 属性面板动态渲染器 - 根据传入的 property 组件异步加载并渲染属性编辑面板，自动在 modelValue 和 activeItem 之间选择绑定目标。
 *
 * @prop {*}          modelValue  - 画布全局配置参数（当 activeItem 为空时作为 v-model）
 * @prop {*}          activeItem  - 当前激活组件的数据（非空时作为 v-model）
 * @prop {*}          instance    - 画布操作实例，传递给属性组件
 * @prop {*}          config      - 全局配置参数，传递给属性组件
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

const render = (props: any, context: any) => {
  const ac = defineAsyncComponent(props.property)
  return h(ac, DataUtil.empty(props.activeItem) ? {
    modelValue: props.modelValue,
    instance: props.instance,
    config: props.config,
    tips: props.tips,
  } : {
    modelValue: props.activeItem,
    instance: props.instance,
    config: props.config,
    tips: props.tips,
  })
}

export default defineComponent({
  props: {
    modelValue: { type: null },
    activeItem: { type: null, required: true },
    instance: { type: null, required: false },
    config: { type: null, required: false },
    tips: { type: null, required: false },
    property: { type: null, required: false },
  },
  emits: {
    'update:modelValue': (val?: any) => true,
    'update:activeItem': (val?: any) => true,
    'update:tips': (val?: any) => true,
  },

  setup(props, context) {
    return () => render(props, context)
  }
})
</script>

<style lang="scss" scoped>
</style>
