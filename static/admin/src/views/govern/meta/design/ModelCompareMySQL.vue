<template>
  <div class="fs-layout-box">
    <a-form-model ref="jdbc" :model="jdbc" :rules="rules">
      <a-form-model-item label="链接" prop="url"><a-textarea v-model="jdbc.url" placeholder="url" /></a-form-model-item>
      <a-form-model-item label="用户"><a-input v-model="jdbc.username" placeholder="username" /></a-form-model-item>
      <a-form-model-item label="密码"><a-input v-model="jdbc.password" placeholder="password" /></a-form-model-item>
      <a-form-model-item label="">
        <a-space>
          <a-button @click="load" :loading="loading">载入</a-button>
          <a-button
            @click="compare"
            :loading="loading"
            type="primary"
            :disabled="selection.selectedRows.length === 0"
          >对比</a-button>
          <a-checkbox v-model="useRemarkAsName">将字段备注作为名称</a-checkbox>
        </a-space>
      </a-form-model-item>
    </a-form-model>
    <a-table
      :columns="columns"
      :rowKey="(record, index) => index"
      :dataSource="rows"
      :pagination="false"
      :loading="loading"
      :rowSelection="selection"
      :bordered="true"
    />
  </div>
</template>

<script>
import APIUtil from '@/utils/api'
import UIUtil from '@/utils/ui'
import RouteUtil from '@/utils/route'
import modelCompareService from '@/service/govern/modelCompare'

export default {
  name: 'ModelCompareMySQL',
  props: {
    value: { type: Array, required: true },
    form: { type: Object, required: true },
    type: { type: String, required: true },
    config: { type: Object, required: true },
    menus: { type: Array, required: true }
  },
  data () {
    return {
      loading: false,
      jdbc: {},
      rows: [],
      rules: {
        url: [{ required: true, message: '请输入连接字符串', trigger: 'blur' }]
      },
      selection: RouteUtil.selection({ type: 'radio' }),
      columns: [
        { title: '名称', dataIndex: 'name' },
        { title: '类型', dataIndex: 'type' }
      ],
      useRemarkAsName: false
    }
  },
  methods: {
    load () {
      this.$refs.jdbc.validate(valid => {
        if (!valid || this.loading) return false
        this.loading = true
        modelCompareService.jdbc(this.jdbc).then(result => {
          if (APIUtil.succeed(result)) {
            this.rows = Object.values(result.data)
            UIUtil.kv(this, this.jdbc)
          }
          this.loading = false
        })
      })
    },
    compare () {
      const record = this.rows[this.selection.selectedRowKeys[0]]
      this.$emit('input', Object.values(record.columns).map(item => {
        return {
          code: item.name,
          name: this.useRemarkAsName ? item.remark : '',
          type: item.type,
          size: item.size,
          digit: item.digit,
          nullable: item.nullable,
          description: item.remark
        }
      }))
      this.$emit('update:menus', ['column'])
    }
  },
  mounted () {
    this.jdbc = UIUtil.kv(this) || {}
    this.$emit('input', [])
  }
}
</script>

<style lang="less" scoped>
</style>
