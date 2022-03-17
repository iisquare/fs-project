<template>
  <a-tabs default-active-key="property" :animated="false">
    <a-tab-pane key="property" tab="属性">
      <a-form-model :model="value" labelAlign="left" :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
        <div class="fs-property-title">画布设置</div>
        <a-form-model-item label="宽度"><a-input-number v-model="value.options.width" :min="100" /></a-form-model-item>
        <a-form-model-item label="高度"><a-input-number v-model="value.options.height" :min="100" /></a-form-model-item>
        <div class="fs-property-title">流程配置</div>
        <a-form-model-item label="并发度"><a-input-number v-model="value.concurrency" :min="0" /></a-form-model-item>
        <a-form-model-item label="并发策略">
          <a-select v-model="value.concurrent" placeholder="请选择并发策略">
            <a-select-option :value="item.value" v-for="item in config.concurrents" :key="item.value">{{ item.label }}</a-select-option>
          </a-select>
        </a-form-model-item>
        <a-form-model-item label="失败策略">
          <a-select v-model="value.failure" placeholder="请选择失败策略">
            <a-select-option :value="item.value" v-for="item in config.failures" :key="item.value">{{ item.label }}</a-select-option>
          </a-select>
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
        height: Number.isInteger(obj.options.height) ? obj.options.height : this.defaults.height
      }
      const result = Object.assign({
        concurrency: Number.isInteger(obj.concurrency) ? obj.concurrency : 1,
        concurrent: obj.concurrent || 'SkipExecution',
        failure: obj.concurrent || 'FinishCurrentRunning'
      }, obj, { options })
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
