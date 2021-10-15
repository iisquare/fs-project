<template>
  <a-tabs default-active-key="property" :animated="false">
    <a-tab-pane key="property" tab="属性">
      <a-form-model :model="value" labelAlign="left" :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
        <slice-basic :value="value" @input="value => $emit('input', value)" :config="config" :activeItem="activeItem" />
        <div class="fs-property-title">参数配置</div>
        <a-form-model-item label="节点列表"><a-input v-model="value.options.bootstrap" placeholder="bootstrap" /></a-form-model-item>
        <a-form-model-item label="ZK连接"><a-input v-model="value.options.zookeeper" placeholder="zookeeper" /></a-form-model-item>
        <a-form-model-item label="初始偏移">
          <a-select v-model="value.options.offset" placeholder="请选择初始偏移" :allowClear="true">
            <a-select-option :value="item.value" v-for="item in offsets" :key="item.value">{{ item.label }}</a-select-option>
          </a-select>
        </a-form-model-item>
        <a-form-model-item label="消费分组"><a-input v-model="value.options.group" placeholder="group" /></a-form-model-item>
        <a-form-model-item label="主题名称"><a-input v-model="value.options.topic" placeholder="topic" /></a-form-model-item>
        <a-form-model-item label="提交间隔">
          <a-space><a-input-number v-model="value.options.commitInterval" /><span>ms</span></a-space>
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
    config: { type: Object, required: true },
    activeItem: { type: Object, required: true }
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
        bootstrap: obj.options.bootstrap || this.defaults.bootstrap,
        zookeeper: obj.options.zookeeper || this.defaults.zookeeper,
        offset: obj.options.offset || this.defaults.offset,
        group: obj.options.group || this.defaults.group,
        topic: obj.options.topic || this.defaults.topic,
        commitInterval: Number.isInteger(obj.options.commitInterval) ? obj.options.commitInterval : this.defaults.commitInterval
      }
      const result = Object.assign({}, obj, { options: Object.assign({}, obj.options, options) })
      return result
    }
  }
}
</script>

<style lang="less" scoped>

</style>
