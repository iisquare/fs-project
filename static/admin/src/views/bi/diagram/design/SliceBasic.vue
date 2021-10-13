<template>
  <section>
    <div class="fs-property-title">基础信息</div>
    <a-form-model-item label="节点">{{ value.id }}</a-form-model-item>
    <a-form-model-item label="类型">{{ value.type }}</a-form-model-item>
    <a-form-model-item label="名称"><a-input v-model="value.name" auto-complete="on" /></a-form-model-item>
    <a-form-model-item label="备注"><a-textarea v-model="value.description" /></a-form-model-item>
    <div class="fs-property-title" v-if="hasAlias || hasPrefix">配置中心</div>
    <a-form-model-item label="别名" v-if="hasAlias"><a-input v-model="value.alias" auto-complete="on" /></a-form-model-item>
    <a-form-model-item label="前缀" v-if="hasPrefix"><a-input v-model="value.kvConfigPrefix" auto-complete="on" placeholder="定位配置项" /></a-form-model-item>
  </section>
</template>

<script>
export default {
  name: 'SliceBasic',
  props: {
    value: { type: Object, required: true },
    config: { type: Object, required: true },
    activeItem: { type: Object, required: true }
  },
  data () {
    return {}
  },
  computed: {
    hasAlias () {
      return ['Source', 'Transform'].some(suffix => this.value.type.endsWith(suffix))
    },
    hasPrefix () {
      return ['Source', 'Transform', 'Sink'].some(suffix => this.value.type.endsWith(suffix))
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
      const data = {
        name: obj.name || '',
        description: obj.description || ''
      }
      if (this.hasAlias) data.alias = obj.alias || ''
      if (this.hasPrefix) data.kvConfigPrefix = obj.kvConfigPrefix || ''
      const result = Object.assign({}, obj, data)
      return result
    }
  }
}
</script>

<style lang="less" scoped>

</style>
