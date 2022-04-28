<template>
  <section>
    <a-card title="基础信息">
      <a-button type="link" @click.native="$router.go(-1)" slot="extra">返回</a-button>
      <a-skeleton v-if="loading" />
      <a-descriptions v-else>
        <a-descriptions-item label="所属包" :span="3">{{ form.catalog }}</a-descriptions-item>
        <a-descriptions-item label="编码">{{ form.code }}</a-descriptions-item>
        <a-descriptions-item label="名称">{{ form.name }}</a-descriptions-item>
        <a-descriptions-item label="类型">{{ form.type }}</a-descriptions-item>
        <a-descriptions-item label="描述" :span="3">{{ form.description }}</a-descriptions-item>
      </a-descriptions>
    </a-card>
    <a-card class="fs-ui-column" v-if="form.type && form.type !== 'catalog'">
      <a-table
        :columns="columns"
        :loading="loading"
        :dataSource="form.columns"
        :pagination="false"
        :rowKey="(record, index) => index"
        :bordered="false"
      >
        <a-descriptions slot="expandedRowRender" slot-scope="record">
          <a-descriptions-item label="描述信息" :span="3">{{ record.description }}</a-descriptions-item>
        </a-descriptions>
        <span slot="code" slot-scope="text, record">
          <span :class="[record.code === $route.query.column && 'fs-ui-match']">{{ record.code }}</span>
        </span>
      </a-table>
    </a-card>
  </section>
</template>

<script>
import modelService from '@/service/govern/model'

export default {
  data () {
    return {
      loading: false,
      form: {},
      columns: [
        { title: '字段编码', scopedSlots: { customRender: 'code' } },
        { title: '字段名称', dataIndex: 'name' },
        { title: '字段类型', dataIndex: 'type' },
        { title: '字段长度', dataIndex: 'size' },
        { title: '小数位数', dataIndex: 'digit' },
        { title: '允许为空', dataIndex: 'nullable', customRender: v => v ? 'Y' : 'N' },
        { title: '模型主键', dataIndex: 'code', customRender: this.pk }
      ]
    }
  },
  methods: {
    pk (code) {
      const index = this.form.pk.indexOf(code)
      return index === -1 ? '' : ('PK-' + index)
    },
    load () {
      this.loading = true
      if (!this.form.code) {
        return (this.loading = false)
      }
      modelService.info(this.form).then(result => {
        if (result.code !== 0) return false
        this.$set(this, 'form', result.data)
        this.loading = false
      })
    }
  },
  created () {
    this.form = {
      columns: [],
      catalog: this.$route.query.catalog,
      code: this.$route.query.code
    }
  },
  mounted () {
    this.load()
  }
}
</script>

<style lang="less" scoped>
.fs-ui-column {
  margin-top: 25px;
  & /deep/ .ant-card-body {
    padding: 0px;
  }
}
.fs-ui-match {
  color: red;
}
</style>
