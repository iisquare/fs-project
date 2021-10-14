<template>
  <a-tabs default-active-key="property" :animated="false">
    <a-tab-pane key="property" tab="属性">
      <a-form-model :model="value" labelAlign="left" :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
        <slice-basic :value="value" @input="value => $emit('input', value)" :config="config" :activeItem="activeItem" />
        <div class="fs-property-title">参数配置</div>
        <a-form-model-item label="主机列表"><a-textarea v-model="value.options.hosts" placeholder="host1,host2,host3:27017" /></a-form-model-item>
        <a-form-model-item label="认证用户"><a-input v-model="value.options.username" placeholder="为空时忽略安全认证" /></a-form-model-item>
        <a-form-model-item label="认证密码"><a-input v-model="value.options.password" placeholder="仅在配置认证用户时有效" /></a-form-model-item>
        <a-form-model-item label="数据库"><a-input v-model="value.options.database" placeholder="database name to write data" /></a-form-model-item>
        <a-form-model-item label="集合名称"><a-input v-model="value.options.collection" placeholder="collection name to write data" /></a-form-model-item>
        <a-form-model-item label="分批大小"><a-input-number v-model="value.options.batchSize" placeholder="maximum batch size for bulk operations" /></a-form-model-item>
        <a-form-model-item label="全量替换"><a-checkbox v-model="value.options.replaceDocument">整体替换文档或替换部分字段</a-checkbox></a-form-model-item>
        <a-form-model-item label="强制插入"><a-checkbox v-model="value.options.forceInsert">存在_id字段依然采用插入方式</a-checkbox></a-form-model-item>
      </a-form-model>
    </a-tab-pane>
  </a-tabs>
</template>

<script>
export default {
  name: 'MongoSinkProperty',
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
        hosts: obj.options.hosts || this.defaults.hosts,
        database: obj.options.database || this.defaults.database,
        username: obj.options.username || this.defaults.username,
        password: obj.options.password || this.defaults.password,
        collection: obj.options.collection || this.defaults.collection,
        batchSize: Number.Integer(obj.options.batchSize) ? obj.options.batchSize : this.defaults.batchSize,
        replaceDocument: !!obj.options.replaceDocument,
        forceInsert: !!obj.options.forceInsert
      }
      const result = Object.assign({}, obj, { options: Object.assign({}, obj.options, options) })
      return result
    }
  }
}
</script>

<style lang="less" scoped>

</style>
