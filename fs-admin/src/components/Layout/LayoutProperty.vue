<script lang="tsx">
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
    modelValue: { type: null }, // 画布配置参数
    activeItem: { type: null, required: true }, // 当前激活组件
    instance: { type: null, required: false }, // 画布操作实例
    config: { type: null, required: false }, // 全局配置参数
    tips: { type: null, required: false }, // 提示消息
    property: { type: null, required: false }, // 属性组件
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
