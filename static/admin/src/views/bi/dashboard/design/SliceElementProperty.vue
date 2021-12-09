<template>
  <a-form-model :model="value" labelAlign="left" :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
    <a-form-model-item label="标识">{{ value.id }}</a-form-model-item>
    <a-form-model-item label="类型">{{ value.type }}</a-form-model-item>
    <a-form-model-item label="名称"><a-input v-model="value.name" placeholder="元素名称" /></a-form-model-item>
    <a-form-model-item label="锁定"><a-checkbox v-model="value.locked">固定元素，不可选中</a-checkbox></a-form-model-item>
    <a-form-model-item label="隐藏"><a-checkbox v-model="value.hidden">隐藏元素，在画布中不可见</a-checkbox></a-form-model-item>
    <a-form-model-item label="层级">
      <a-space>
        <a-input-number v-model="value.level" />
        <span>z-index</span>
      </a-space>
    </a-form-model-item>
    <a-form-model-item label="顶部">
      <a-space>
        <a-input-number v-model="value.top" />
        <span>px</span>
      </a-space>
    </a-form-model-item>
    <a-form-model-item label="左部">
      <a-space>
        <a-input-number v-model="value.left" />
        <span>px</span>
      </a-space>
    </a-form-model-item>
    <a-form-model-item label="宽度">
      <a-space>
        <a-input-number v-model="value.width" :min="10" />
        <span>px</span>
      </a-space>
    </a-form-model-item>
    <a-form-model-item label="高度">
      <a-space>
        <a-input-number v-model="value.height" :min="10" />
        <span>px</span>
      </a-space>
    </a-form-model-item>
    <a-form-model-item label="旋转">
      <a-space>
        <a-input-number v-model="value.rotate" />
        <span>度</span>
      </a-space>
    </a-form-model-item>
    <a-form-model-item label="不透明度">
      <a-space>
        <a-input-number v-model="value.opacity" :min="0" :max="100" />
        <span>%</span>
      </a-space>
    </a-form-model-item>
  </a-form-model>
</template>

<script>
export default {
  name: 'SliceElementProperty',
  props: {
    value: { type: Object, required: true },
    config: { type: Object, required: true },
    activeItem: { type: Object, default: null }
  },
  data () {
    return {}
  },
  computed: {
    defaults () {
      return this.config.widgetDefaults(this.value.type)
    }
  },
  watch: {
    'activeItem.id': {
      handler () {
        this.$emit('input', this.formatted(this.value))
      },
      immediate: true
    }
  },
  methods: {
    formatted (obj) {
      const options = {
        top: Number.isInteger(obj.top) ? obj.top : this.defaults.top,
        left: Number.isInteger(obj.left) ? obj.left : this.defaults.left,
        width: Number.isInteger(obj.width) ? obj.width : this.defaults.width,
        height: Number.isInteger(obj.height) ? obj.height : this.defaults.height,
        rotate: Number.isInteger(obj.rotate) ? obj.rotate : this.defaults.rotate,
        opacity: Number.isInteger(obj.opacity) ? obj.opacity : this.defaults.opacity
      }
      const result = Object.assign({}, obj, options)
      return result
    }
  }
}
</script>

<style lang="less" scoped>

</style>
