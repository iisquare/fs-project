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
        <a-form-model-item label="驱动">
          <a-select v-model="value.data.options.driver" placeholder="请选择连接驱动" :allowClear="true">
            <a-select-option :value="item.value" v-for="item in dagConfig.jdbcDrivers" :key="item.value">{{ item.label }}</a-select-option>
          </a-select>
        </a-form-model-item>
        <a-form-model-item label="链接"><a-textarea v-model="value.data.options.url" placeholder="url" /></a-form-model-item>
        <a-form-model-item label="用户"><a-input v-model="value.data.options.username" placeholder="username" /></a-form-model-item>
        <a-form-model-item label="密码"><a-input v-model="value.data.options.password" placeholder="password" /></a-form-model-item>
        <a-form-model-item label="循环实例"><a-checkbox v-model="value.data.options.iterable">循环配置，读取多个数据库实例</a-checkbox></a-form-model-item>
        <div class="fs-property-title">优化参数</div>
        <a-form-model-item label="分区字段"><a-input v-model="value.data.options.partitionColumn" placeholder="numeric, date, timestamp" /></a-form-model-item>
        <a-form-model-item label="分区下限"><a-input v-model="value.data.options.lowerBound" placeholder="数值类型，跨度起始值" /></a-form-model-item>
        <a-form-model-item label="分区上限"><a-input v-model="value.data.options.upperBound" placeholder="数值类型，跨度结束值" /></a-form-model-item>
        <a-form-model-item label="分区数量"><a-input-number v-model="value.data.options.numPartitions" /></a-form-model-item>
        <a-form-model-item label="分批大小"><a-input-number v-model="value.data.options.fetchSize" placeholder="fetch size per round trip" /></a-form-model-item>
        <div class="fs-property-title">SQL查询</div>
        <code-editor ref="sql" v-model="value.data.options.sql" mode="sql" :height="230" />
      </a-form-model>
    </a-tab-pane>
  </a-tabs>
</template>

<script>
import UIUtil from '@/utils/ui'
import dagService from '@/service/bi/dag'

export default {
  name: 'JDBCSourceProperty',
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
    return {
      dagConfig: { jdbcDrivers: [] }
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
        this.$refs.sql && this.$refs.sql.setContent(this.value.data.options.sql)
      },
      immediate: true
    }
  },
  methods: {
    formatted (obj) {
      const options = {
        driver: obj.data.options.driver || this.defaults.driver,
        url: obj.data.options.url || this.defaults.url,
        username: obj.data.options.username || this.defaults.username,
        password: obj.data.options.password || this.defaults.password,
        iterable: !!obj.data.options.iterable,
        partitionColumn: obj.data.options.partitionColumn || this.defaults.partitionColumn,
        lowerBound: obj.data.options.lowerBound || this.defaults.lowerBound,
        upperBound: obj.data.options.upperBound || this.defaults.upperBound,
        numPartitions: Number.isInteger(obj.data.options.numPartitions) ? obj.data.options.numPartitions : this.defaults.numPartitions,
        fetchSize: Number.isInteger(obj.data.options.fetchSize) ? obj.data.options.fetchSize : this.defaults.fetchSize,
        sql: obj.data.options.sql || this.defaults.sql
      }
      return this.config.mergeOptions(obj, options)
    },
    loadDAGConfig () {
      UIUtil.cache(null, () => dagService.config(), 0).then(result => {
        if (result.code === 0) {
          this.dagConfig = result.data
        }
      })
    }
  },
  mounted () {
    this.loadDAGConfig()
  }
}
</script>

<style lang="less" scoped>

</style>
