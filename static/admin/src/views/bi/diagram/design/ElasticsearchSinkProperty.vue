<template>
  <a-tabs default-active-key="property" :animated="false">
    <a-tab-pane key="property" tab="属性">
      <a-form-model :model="value" labelAlign="left" :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
        <slice-basic :value="value" @input="value => $emit('input', value)" :config="config" :activeItem="activeItem" />
        <div class="fs-property-title">参数配置</div>
        <a-form-model-item label="节点列表"><a-input v-model="value.options.servers" placeholder="多个节点以英文逗号分隔" /></a-form-model-item>
        <a-form-model-item label="认证用户"><a-input v-model="value.options.username" placeholder="为空时忽略安全认证" /></a-form-model-item>
        <a-form-model-item label="认证密码"><a-input v-model="value.options.password" placeholder="仅在配置认证用户时有效" /></a-form-model-item>
        <a-form-model-item label="索引名称"><a-input v-model="value.options.collection" placeholder="index name" /></a-form-model-item>
        <a-form-model-item label="分批大小"><a-input-number v-model="value.options.batchSize" placeholder="-1禁用" /></a-form-model-item>
        <a-form-model-item label="刷新间隔">
          <a-space><a-input-number v-model="value.options.flushInterval" placeholder="-1禁用" /><span>ms</span></a-space>
        </a-form-model-item>
        <a-form-model-item label="主键字段"><a-input v-model="value.options.idField" placeholder="主键字段名称" /></a-form-model-item>
        <a-form-model-item label="索引字段"><a-input v-model="value.options.tableField" placeholder="索引字段名称，用于索引拆分" /></a-form-model-item>
      </a-form-model>
    </a-tab-pane>
  </a-tabs>
</template>

<script>
export default {
  name: 'ElasticsearchSinkProperty',
  components: {
    SliceBasic: () => import('./SliceBasic')
  },
  props: {
    value: { type: Object, required: true },
    config: { type: Object, required: true },
    activeItem: { type: Object, required: true }
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
        servers: obj.options.servers || this.defaults.servers,
        username: obj.options.username || this.defaults.username,
        password: obj.options.password || this.defaults.password,
        collection: obj.options.collection || this.defaults.collection,
        batchSize: Number.isInteger(obj.options.batchSize) ? obj.options.batchSize : this.defaults.batchSize,
        flushInterval: Number.isInteger(obj.options.flushInterval) ? obj.options.flushInterval : this.defaults.flushInterval,
        idField: obj.options.idField || this.defaults.idField,
        tableField: obj.options.tableField || this.defaults.tableField
      }
      const result = Object.assign({}, obj, { options: Object.assign({}, obj.options, options) })
      return result
    }
  }
}
</script>

<style lang="less" scoped>

</style>
