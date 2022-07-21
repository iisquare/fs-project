<template>
  <section>
    <div class="fs-property-title">
      <span>基础信息</span>
      <a-space class="fs-property-action">
        <a-popconfirm title="确认删除该元素?" placement="bottomRight" @confirm="remove">
          <a-icon type="delete" class="fs-action-delete" />
        </a-popconfirm>
      </a-space>
    </div>
    <a-form-model-item label="类型">{{ value.data.type }}</a-form-model-item>
    <a-form-model-item label="名称"><a-input v-model="value.data.name" auto-complete="on" :placeholder="'node_' + value.data.index" /></a-form-model-item>
    <a-form-model-item label="标题"><a-input v-model="value.data.title" auto-complete="on" /></a-form-model-item>
    <a-form-model-item label="备注"><a-textarea v-model="value.data.description" /></a-form-model-item>
    <div class="fs-property-title">流程配置</div>
    <a-form-model-item label="跳过执行"><a-switch v-model="value.data.skiped" /></a-form-model-item>
  </section>
</template>

<script>
export default {
  name: 'SliceBasic',
  props: {
    value: { type: Object, required: true },
    flow: { type: Object, required: true },
    config: { type: Object, required: true },
    diagram: { type: Object, required: true },
    activeItem: { type: Object, default: null },
    tips: { type: String, default: '' }
  },
  data () {
    return {}
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
    remove () {
      this.flow.graph.removeCell(this.value.id)
      this.$emit('update:activeItem', null)
    },
    formatted (obj) {
      const data = {
        name: obj.data.name || '',
        title: obj.data.title || '',
        description: obj.data.description || '',
        skiped: !!obj.data.skiped
      }
      return this.config.mergeData(obj, data)
    }
  }
}
</script>

<style lang="less" scoped>

</style>
