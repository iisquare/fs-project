<template>
  <a-tabs default-active-key="property" :animated="false">
    <a-tab-pane key="property" tab="属性">
      <a-form-model :model="value" labelAlign="left" :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
        <div class="fs-property-title">基础信息</div>
        <a-form-model-item label="主键">{{ value.id }}</a-form-model-item>
        <a-form-model-item label="名称">{{ value.name }}</a-form-model-item>
        <div class="fs-property-title">画布设置</div>
        <a-form-model-item label="宽度">
          <a-space>
            <a-input-number v-model="value.options.width" :min="10" />
            <span>px</span>
          </a-space>
        </a-form-model-item>
        <a-form-model-item label="高度">
          <a-space>
            <a-input-number v-model="value.options.height" :min="10" />
            <span>px</span>
          </a-space>
        </a-form-model-item>
        <a-form-model-item label="背景颜色">
          <a-input v-model="value.options.color" placeholder="为空时无背景颜色" />
        </a-form-model-item>
        <a-form-model-item label="背景图片">
          <a-textarea v-model="value.options.url" placeholder="为空时无背景图片" />
        </a-form-model-item>
      </a-form-model>
    </a-tab-pane>
  </a-tabs>
</template>

<script>
export default {
  name: 'CanvasProperty',
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
      return this.config.canvas.options()
    }
  },
  methods: {
    formatted (obj) {
      const options = {
        width: Number.isInteger(obj.options.width) ? obj.options.width : this.defaults.width,
        height: Number.isInteger(obj.options.height) ? obj.options.height : this.defaults.height,
        color: (typeof obj.options.color === 'undefined') ? this.defaults.color : obj.options.color,
        url: (typeof obj.options.url === 'undefined') ? this.defaults.url : obj.options.url
      }
      const result = Object.assign({}, obj, { options })
      return result
    }
  },
  mounted () {
    this.$emit('input', this.formatted(this.value))
  }
}
</script>

<style lang="less" scoped>

</style>
