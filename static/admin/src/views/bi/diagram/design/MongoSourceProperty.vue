<template>
  <a-tabs default-active-key="property" :animated="false">
    <a-tab-pane key="property" tab="属性">
      <a-form-model :model="value" labelAlign="left" :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
        <slice-basic
          :value="value"
          @input="value => $emit('input', value)"
          :flow="flow"
          :config="config"
          :diagram="diagram"
          :activeItem="activeItem"
          @update:activeItem="val => $emit('update:activeItem', val)"
          :tips="tips"
          @update:tips="val => $emit('update:tips', val)" />
        <div class="fs-property-title">参数配置</div>
        <a-form-model-item label="主机列表"><a-textarea v-model="value.data.options.hosts" placeholder="host1,host2,host3:27017" /></a-form-model-item>
        <a-form-model-item label="认证用户"><a-input v-model="value.data.options.username" placeholder="为空时忽略安全认证" /></a-form-model-item>
        <a-form-model-item label="认证密码"><a-input v-model="value.data.options.password" placeholder="仅在配置认证用户时有效" /></a-form-model-item>
        <a-form-model-item label="数据库"><a-input v-model="value.data.options.database" placeholder="database name to write data" /></a-form-model-item>
        <a-form-model-item label="集合名称"><a-input v-model="value.data.options.collection" placeholder="collection name to write data" /></a-form-model-item>
        <a-form-model-item label="聚合管道"><a-textarea v-model="value.data.options.pipeline" /></a-form-model-item>
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
    flow: { type: Object, required: true },
    config: { type: Object, required: true },
    diagram: { type: Object, required: true },
    activeItem: { type: Object, default: null },
    tips: { type: String, default: '' }
  },
  data () {
    return {
      operationTypes: [
        { label: 'insert - 插入数据', value: 'insert' },
        { label: 'replace - 替换数据', value: 'replace' },
        { label: 'update - 更新数据', value: 'update' }
      ]
    }
  },
  computed: {
    defaults () {
      return this.config.widgetDefaults(this.value.data.type)
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
        hosts: obj.data.options.hosts || this.defaults.hosts,
        database: obj.data.options.database || this.defaults.database,
        username: obj.data.options.username || this.defaults.username,
        password: obj.data.options.password || this.defaults.password,
        collection: obj.data.options.collection || this.defaults.collection,
        pipeline: obj.data.options.pipeline || this.defaults.pipeline
      }
      return this.config.mergeOptions(obj, options)
    }
  }
}
</script>

<style lang="less" scoped>

</style>
