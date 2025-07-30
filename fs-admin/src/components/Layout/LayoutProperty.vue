<script lang="tsx">
import { defineAsyncComponent, defineComponent, h } from 'vue'

const render = (props: any, context: any) => {
  const ac = defineAsyncComponent(props.property)
  return h(ac, {
    modelValue: props.modelValue,
    instance: props.instance,
    config: props.config,
    tips: props.tips,
  })
}

export default defineComponent({
  props: {
    modelValue: { type: null },
    property: { type: null, required: false }, // 用于传递属性组件
    instance: { type: null, required: false }, // 用于传递操作实例
    config: { type: null, required: false }, // 用于传递配置参数
    tips: { type: null, required: false }, // 用于传递提示消息
  },
  emits: {
    'update:modelValue': (val?: any) => true,
    'update:tips': (val?: any) => true,
  },
  setup(props, context) {
    return () => render(props, context)
  }
})
</script>

<style lang="scss" scoped>
</style>
