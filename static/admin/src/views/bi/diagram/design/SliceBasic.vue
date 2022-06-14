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
    <a-form-model-item label="节点">{{ value.id }}</a-form-model-item>
    <a-form-model-item label="类型">{{ value.data.type }}</a-form-model-item>
    <a-form-model-item label="名称"><a-input v-model="value.data.title" auto-complete="on" /></a-form-model-item>
    <a-form-model-item label="备注"><a-textarea v-model="value.data.description" /></a-form-model-item>
    <div class="fs-property-title" v-if="hasAlias || hasPrefix">配置中心</div>
    <a-form-model-item label="别名" v-if="hasAlias"><a-input v-model="value.data.alias" auto-complete="on" :placeholder="'dag_' + config.diagram.id + '_' + value.id" /></a-form-model-item>
    <a-form-model-item label="前缀" v-if="hasPrefix"><a-input v-model="value.data.kvConfigPrefix" auto-complete="on" placeholder="定位配置项，替换{变量}参数" /></a-form-model-item>
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
  computed: {
    hasAlias () {
      return ['Source', 'Transform'].some(suffix => this.value.data.type.endsWith(suffix))
    },
    hasPrefix () {
      return ['Source', 'Transform', 'Sink'].some(suffix => this.value.data.type.endsWith(suffix))
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
    remove () {
      this.flow.graph.removeCell(this.value.id)
      this.$emit('update:activeItem', null)
    },
    formatted (obj) {
      const data = {
        title: obj.data.title || '',
        description: obj.data.description || ''
      }
      if (this.hasAlias) data.alias = obj.alias || ''
      if (this.hasPrefix) data.kvConfigPrefix = obj.kvConfigPrefix || ''
      return this.config.mergeData(obj, data)
    }
  }
}
</script>

<style lang="less" scoped>

</style>
