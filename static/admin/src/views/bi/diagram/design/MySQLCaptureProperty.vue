<template>
  <a-tabs default-active-key="property" :animated="false">
    <a-tab-pane key="property" tab="属性">
      <a-form-model :model="value" labelAlign="left" :label-col="{ span: 6 }" :wrapper-col="{ span: 18 }">
        <slice-basic :value="value" @input="value => $emit('input', value)" :config="config" :activeItem="activeItem" />
        <div class="fs-property-title">参数配置</div>
        <a-form-model-item label="主机"><a-input v-model="value.options.hostname" placeholder="hostname" /></a-form-model-item>
        <a-form-model-item label="端口"><a-input v-model="value.options.port" placeholder="port default 3306" /></a-form-model-item>
        <a-form-model-item label="用户"><a-input v-model="value.options.username" placeholder="username" /></a-form-model-item>
        <a-form-model-item label="密码"><a-input v-model="value.options.password" placeholder="password" /></a-form-model-item>
        <a-form-model-item label="库列表"><a-textarea v-model="value.options.database" placeholder="regular expressions like .* or db1,db2,db3" /></a-form-model-item>
        <a-form-model-item label="表列表"><a-textarea v-model="value.options.table" placeholder="regular expressions like <databaseName>.<tableName>" /></a-form-model-item>
        <a-form-model-item label="启动模式">
          <a-auto-complete
            v-model="value.options.startup"
            placeholder="请选择和配置启动模式"
            :allowClear="true"
            @select="startupSelect"
            :filterOption="UIUtil.filterOption">
            <template slot="dataSource">
              <a-select-option :key="k" :value="v" v-for="(v, k) in startups">{{ v }}</a-select-option>
            </template>
          </a-auto-complete>
        </a-form-model-item>
        <div class="fs-property-title">拓展配置</div>
        <a-form-model-item label="时区"><a-input v-model="value.options.timeZone" placeholder="session timezone Asia/Shanghai" /></a-form-model-item>
        <a-form-model-item label="poolSize"><a-input v-model="value.options.poolSize" placeholder="default 20" /></a-form-model-item>
        <a-form-model-item label="fetchSize"><a-input v-model="value.options.fetchSize" placeholder="default 1024" /></a-form-model-item>
        <a-form-model-item label="JDBC属性"><a-textarea v-model="value.options.jdbcProperties" placeholder="k1=v1&k2=v2&k3=v3" /></a-form-model-item>
        <a-form-model-item label="Debezium"><a-textarea v-model="value.options.debeziumProperties" placeholder="k1=v1&k2=v2&k3=v3" /></a-form-model-item>
      </a-form-model>
    </a-tab-pane>
  </a-tabs>
</template>

<script>
import UIUtil from '@/utils/ui'

export default {
  name: 'MySQLCaptureProperty',
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
      UIUtil,
      startups: [
        'initial',
        'earliest',
        'latest',
        'timestamp:milliseconds',
        'datetime:yyyy-MM-dd HH:mm:ss.S',
        'specific:OffsetFile,OffsetPos'
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
    startupSelect (value) {
      const options = Object.assign({}, this.value.options, { startup: value })
      const result = Object.assign({}, this.value, { options })
      this.$nextTick(() => {
        this.$emit('input', Object.assign({}, this.value, result))
      })
    },
    formatted (obj) {
      const options = {
        hostname: obj.options.hostname || this.defaults.hostname,
        username: obj.options.username || this.defaults.username,
        password: obj.options.password || this.defaults.password,
        latest: obj.options.latest || this.defaults.latest
      }
      const result = Object.assign({}, obj, { options: Object.assign({}, obj.options, options) })
      return result
    }
  }
}
</script>

<style lang="less" scoped>

</style>
