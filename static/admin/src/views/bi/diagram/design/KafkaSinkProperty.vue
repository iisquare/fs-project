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
        <a-form-model-item label="节点列表"><a-input v-model="value.data.options.bootstrap" placeholder="bootstrap" /></a-form-model-item>
        <a-form-model-item label="主题名称"><a-input v-model="value.data.options.topic" placeholder="topic" /></a-form-model-item>
        <a-form-model-item label="消息语义">
          <a-select v-model="value.data.options.semantic" placeholder="请选择消息语义" :allowClear="true">
            <a-select-option :value="item.value" v-for="item in semantics" :key="item.value">{{ item.label }}</a-select-option>
          </a-select>
        </a-form-model-item>
        <a-form-model-item label="主题字段"><a-input v-model="value.data.options.topicKey" placeholder="单条消息投送的主题字段名称" /></a-form-model-item>
        <a-form-model-item label="分区字段"><a-input v-model="value.data.options.partitionKey" placeholder="单挑消息投送的分区字段名称" /></a-form-model-item>
      </a-form-model>
    </a-tab-pane>
  </a-tabs>
</template>

<script>
export default {
  name: 'KafkaSinkProperty',
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
      semantics: [
        { label: 'NONE - 无保证', value: 'NONE' },
        { label: 'AT_LEAST_ONCE - 至少送达一次 ', value: 'AT_LEAST_ONCE' },
        { label: 'EXACTLY_ONCE - 精确送达一次', value: 'EXACTLY_ONCE' }
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
        bootstrap: obj.data.options.bootstrap || this.defaults.bootstrap,
        topic: obj.data.options.topic || this.defaults.topic,
        semantic: obj.data.options.semantic || this.defaults.semantic,
        topicKey: obj.data.options.topicKey || this.defaults.topicKey,
        partitionKey: obj.data.options.partitionKey || this.defaults.partitionKey
      }
      return this.config.mergeOptions(obj, options)
    }
  }
}
</script>

<style lang="less" scoped>

</style>
