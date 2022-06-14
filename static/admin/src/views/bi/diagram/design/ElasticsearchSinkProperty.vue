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
        <a-form-model-item label="节点列表"><a-input v-model="value.data.options.servers" placeholder="多个节点以英文逗号分隔" /></a-form-model-item>
        <a-form-model-item label="认证用户"><a-input v-model="value.data.options.username" placeholder="为空时忽略安全认证" /></a-form-model-item>
        <a-form-model-item label="认证密码"><a-input v-model="value.data.options.password" placeholder="仅在配置认证用户时有效" /></a-form-model-item>
        <a-form-model-item label="索引名称"><a-input v-model="value.data.options.collection" placeholder="index name" /></a-form-model-item>
        <a-form-model-item label="分批大小"><a-input-number v-model="value.data.options.batchSize" placeholder="-1禁用" /></a-form-model-item>
        <a-form-model-item label="刷新间隔" v-if="config.diagram.engine === config.ENGINE_FLINK">
          <a-space><a-input-number v-model="value.data.options.flushInterval" placeholder="-1禁用" /><span>ms</span></a-space>
        </a-form-model-item>
        <a-form-model-item label="主键字段"><a-input v-model="value.data.options.idField" placeholder="主键字段名称，留空自动生成" /></a-form-model-item>
        <a-form-model-item label="索引字段" v-if="config.diagram.engine === config.ENGINE_FLINK">
          <a-input v-model="value.data.options.tableField" placeholder="索引字段名称，用于索引拆分" />
        </a-form-model-item>
        <a-form-model-item label="数据格式">
          <a-select v-model="value.data.options.format" placeholder="请选择数据格式" :allowClear="true">
            <a-select-option :value="item.value" v-for="item in formats" :key="item.value">{{ item.label }}</a-select-option>
          </a-select>
        </a-form-model-item>
        <a-form-model-item label="输出模式">
          <a-select v-model="value.data.options.mode" placeholder="请选择输出模式" :allowClear="true">
            <a-select-option :value="item.value" v-for="item in modes" :key="item.value">{{ item.label }}</a-select-option>
          </a-select>
        </a-form-model-item>
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
    flow: { type: Object, required: true },
    config: { type: Object, required: true },
    diagram: { type: Object, required: true },
    activeItem: { type: Object, default: null },
    tips: { type: String, default: '' }
  },
  data () {
    return {
      modes: [
        { label: 'index - 添新替旧', value: 'index' },
        { label: 'create - 添新，若存在抛异常', value: 'create' },
        { label: 'update - 更旧，若不存在抛异常', value: 'update' },
        { label: 'upsert - 添新合旧', value: 'upsert' }
      ],
      formats: [
        { label: '基础格式', value: '' },
        { label: 'JSON格式', value: 'json' }
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
        servers: obj.data.options.servers || this.defaults.servers,
        username: obj.data.options.username || this.defaults.username,
        password: obj.data.options.password || this.defaults.password,
        collection: obj.data.options.collection || this.defaults.collection,
        batchSize: Number.isInteger(obj.data.options.batchSize) ? obj.data.options.batchSize : this.defaults.batchSize,
        flushInterval: Number.isInteger(obj.data.options.flushInterval) ? obj.data.options.flushInterval : this.defaults.flushInterval,
        idField: typeof obj.data.options.idField === 'undefined' ? this.defaults.idField : obj.data.options.idField,
        tableField: obj.data.options.tableField || this.defaults.tableField,
        mode: obj.data.options.mode || this.defaults.mode,
        format: obj.data.options.format || this.defaults.format
      }
      return this.config.mergeOptions(obj, options)
    }
  }
}
</script>

<style lang="less" scoped>

</style>
