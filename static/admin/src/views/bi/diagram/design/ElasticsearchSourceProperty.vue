<template>
  <a-tabs default-active-key="property" :animated="false">
    <a-tab-pane key="property" tab="属性">
      <a-form-model :model="value" labelAlign="left" :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
        <slice-basic :value="value" @input="value => $emit('input', value)" :config="config" :activeItem="activeItem" />
        <div class="fs-property-title">参数配置</div>
        <a-form-model-item label="集群名称"><a-input v-model="value.options.cluster" placeholder="cluster.name" /></a-form-model-item>
        <a-form-model-item label="节点列表"><a-input v-model="value.options.servers" placeholder="多个节点以英文逗号分隔" /></a-form-model-item>
        <a-form-model-item label="认证用户"><a-input v-model="value.options.username" placeholder="为空时忽略安全认证" /></a-form-model-item>
        <a-form-model-item label="认证密码"><a-input v-model="value.options.password" placeholder="仅在配置认证用户时有效" /></a-form-model-item>
        <a-form-model-item label="索引名称"><a-input v-model="value.options.collection" placeholder="index name" /></a-form-model-item>
        <div class="fs-property-title">查询条件</div>
        <code-editor v-model="value.options.query" mode="javascript" :height="230" />
      </a-form-model>
    </a-tab-pane>
  </a-tabs>
</template>

<script>
export default {
  name: 'ElasticsearchSourceProperty',
  components: {
    SliceBasic: () => import('./SliceBasic'),
    CodeEditor: () => import('@/components/Editor/CodeEditor')
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
        cluster: obj.options.cluster || this.defaults.cluster,
        servers: obj.options.servers || this.defaults.servers,
        username: obj.options.username || this.defaults.username,
        password: obj.options.password || this.defaults.password,
        collection: obj.options.collection || this.defaults.collection,
        query: obj.options.query || this.defaults.query
      }
      const result = Object.assign({}, obj, { options: Object.assign({}, obj.options, options) })
      return result
    }
  }
}
</script>

<style lang="less" scoped>

</style>
