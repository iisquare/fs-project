<template>
  <a-tabs default-active-key="property" :animated="false">
    <a-tab-pane key="property" tab="属性">
      <a-form-model :model="value" labelAlign="left" :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
        <slice-basic :value="value" @input="value => $emit('input', value)" :config="config" :activeItem="activeItem" />
        <div class="fs-property-title">参数配置</div>
        <a-form-model-item label="驱动">
          <a-select v-model="value.options.driver" placeholder="请选择连接驱动" :allowClear="true">
            <a-select-option :value="item.value" v-for="item in drivers" :key="item.value">{{ item.label }}</a-select-option>
          </a-select>
        </a-form-model-item>
        <a-form-model-item label="链接"><a-textarea v-model="value.options.url" placeholder="url" /></a-form-model-item>
        <a-form-model-item label="用户"><a-input v-model="value.options.username" placeholder="username" /></a-form-model-item>
        <a-form-model-item label="密码"><a-input v-model="value.options.password" placeholder="password" /></a-form-model-item>
        <a-form-model-item label="循环实例"><a-checkbox v-model="value.options.iterable">循环配置，读取多个数据库实例</a-checkbox></a-form-model-item>
        <div class="fs-property-title">优化参数</div>
        <a-form-model-item label="分区字段"><a-input v-model="value.options.partitionColumn" placeholder="numeric, date, timestamp" /></a-form-model-item>
        <a-form-model-item label="分区下限"><a-input v-model="value.options.lowerBound" placeholder="数值类型，跨度起始值" /></a-form-model-item>
        <a-form-model-item label="分区上限"><a-input v-model="value.options.upperBound" placeholder="数值类型，跨度结束值" /></a-form-model-item>
        <a-form-model-item label="分区数量"><a-input-number v-model="value.options.numPartitions" /></a-form-model-item>
        <a-form-model-item label="分批大小"><a-input-number v-model="value.options.fetchSize" placeholder="fetch size per round trip" /></a-form-model-item>
        <div class="fs-property-title">SQL查询</div>
        <code-editor ref="sql" v-model="value.options.sql" mode="sql" :height="230" />
      </a-form-model>
    </a-tab-pane>
  </a-tabs>
</template>

<script>
export default {
  name: 'JDBCSourceProperty',
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
    return {
      drivers: [
        { label: 'MySQL', value: 'com.mysql.jdbc.Driver' }
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
        this.$refs.sql && this.$refs.sql.setContent(this.value.options.sql)
      },
      immediate: true
    }
  },
  methods: {
    formatted (obj) {
      const options = {
        driver: obj.options.driver || this.defaults.driver,
        url: obj.options.url || this.defaults.url,
        username: obj.options.username || this.defaults.username,
        password: obj.options.password || this.defaults.password,
        iterable: !!obj.options.iterable,
        partitionColumn: obj.options.partitionColumn || this.defaults.partitionColumn,
        lowerBound: obj.options.lowerBound || this.defaults.lowerBound,
        upperBound: obj.options.upperBound || this.defaults.upperBound,
        numPartitions: Number.isInteger(obj.options.numPartitions) ? obj.options.numPartitions : this.defaults.numPartitions,
        fetchSize: Number.isInteger(obj.options.fetchSize) ? obj.options.fetchSize : this.defaults.fetchSize,
        sql: obj.options.sql || this.defaults.sql
      }
      const result = Object.assign({}, obj, { options: Object.assign({}, obj.options, options) })
      return result
    }
  }
}
</script>

<style lang="less" scoped>

</style>
