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
        <a-form-model-item label="集群名称"><a-input v-model="value.data.options.cluster" placeholder="cluster.name" /></a-form-model-item>
        <a-form-model-item label="节点列表"><a-input v-model="value.data.options.servers" placeholder="多个节点以英文逗号分隔" /></a-form-model-item>
        <a-form-model-item label="认证用户"><a-input v-model="value.data.options.username" placeholder="为空时忽略安全认证" /></a-form-model-item>
        <a-form-model-item label="认证密码"><a-input v-model="value.data.options.password" placeholder="仅在配置认证用户时有效" /></a-form-model-item>
        <a-form-model-item label="索引名称"><a-input v-model="value.data.options.collection" placeholder="index name" /></a-form-model-item>
        <div class="fs-property-title">查询条件</div>
        <code-editor ref="query" v-model="value.data.options.query" mode="javascript" :height="230" />
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
    defaults () {
      return this.config.widgetDefaults(this.value.data.type)
    }
  },
  watch: {
    'activeItem.id': {
      handler () {
        this.$emit('input', this.formatted(this.value))
        this.$refs.query && this.$refs.query.setContent(this.value.data.options.query)
      },
      immediate: true
    }
  },
  methods: {
    formatted (obj) {
      const options = {
        cluster: obj.data.options.cluster || this.defaults.cluster,
        servers: obj.data.options.servers || this.defaults.servers,
        username: obj.data.options.username || this.defaults.username,
        password: obj.data.options.password || this.defaults.password,
        collection: obj.data.options.collection || this.defaults.collection,
        query: obj.data.options.query || this.defaults.query
      }
      return this.config.mergeOptions(obj, options)
    }
  }
}
</script>

<style lang="less" scoped>

</style>
