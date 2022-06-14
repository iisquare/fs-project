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
        <a-form-model-item label="ZK连接"><a-input v-model="value.data.options.zookeeper" placeholder="zookeeper" /></a-form-model-item>
        <a-form-model-item label="初始偏移">
          <a-select v-model="value.data.options.offset" placeholder="请选择初始偏移" :allowClear="true">
            <a-select-option :value="item.value" v-for="item in offsets" :key="item.value">{{ item.label }}</a-select-option>
          </a-select>
        </a-form-model-item>
        <a-form-model-item label="消费分组"><a-input v-model="value.data.options.group" placeholder="group" /></a-form-model-item>
        <a-form-model-item label="主题名称"><a-input v-model="value.data.options.topic" placeholder="topic" /></a-form-model-item>
        <a-form-model-item label="提交间隔">
          <a-space><a-input-number v-model="value.data.options.commitInterval" /><span>ms</span></a-space>
        </a-form-model-item>
      </a-form-model>
    </a-tab-pane>
  </a-tabs>
</template>

<script>
export default {
  name: 'KafkaSourceProperty',
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
      offsets: [
        { label: 'smallest', value: 'smallest' },
        { label: 'largest', value: 'largest' },
        { label: 'earliest', value: 'earliest' },
        { label: 'latest', value: 'latest' }
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
        zookeeper: obj.data.options.zookeeper || this.defaults.zookeeper,
        offset: obj.data.options.offset || this.defaults.offset,
        group: obj.data.options.group || this.defaults.group,
        topic: obj.data.options.topic || this.defaults.topic,
        commitInterval: Number.isInteger(obj.data.options.commitInterval) ? obj.data.options.commitInterval : this.defaults.commitInterval
      }
      return this.config.mergeOptions(obj, options)
    }
  }
}
</script>

<style lang="less" scoped>

</style>
