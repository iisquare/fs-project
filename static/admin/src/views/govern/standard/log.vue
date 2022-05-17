<template>
  <section>
    <a-card :bordered="false">
      <div class="table-page-search-wrapper">
        <a-form-model ref="filters" :model="filters" layout="inline">
          <a-row :gutter="48">
            <a-col :md="6" :sm="24">
              <a-form-model-item label="落地方案" prop="assess">
                <a-input v-model="filters.assess" placeholder="" :allowClear="true" />
              </a-form-model-item>
            </a-col>
            <a-col :md="6" :sm="24">
              <a-form-model-item label="评估标准" prop="standard">
                <a-input v-model="filters.standard" placeholder="" :allowClear="true" />
              </a-form-model-item>
            </a-col>
            <a-col :md="6" :sm="24">
              <a-form-model-item label="预警等级" prop="level">
                <a-input v-model="filters.level" placeholder="" :allowClear="true" />
              </a-form-model-item>
            </a-col>
          </a-row>
          <a-row :gutter="48">
            <a-col :md="6" :sm="24">
              <a-form-model-item label="数据源" prop="source">
                <a-input v-model="filters.source" placeholder="" :allowClear="true" />
              </a-form-model-item>
            </a-col>
            <a-col :md="6" :sm="24">
              <a-form-model-item label="表名称" prop="model">
                <a-input v-model="filters.model" placeholder="" :allowClear="true" />
              </a-form-model-item>
            </a-col>
            <a-col :md="6" :sm="24">
              <a-form-model-item label="字段名称" prop="code">
                <a-input v-model="filters.code" placeholder="" :allowClear="true" />
              </a-form-model-item>
            </a-col>
            <a-col :md="6" :sm="24">
              <span class="table-page-search-submitButtons" :style="advanced && { float: 'right', oversource: 'hidden' } || {} ">
                <a-button type="primary" @click="search(true, false)" :loading="loading">查询</a-button>
                <a-button style="margin-left: 8px" @click="() => this.$refs.filters.resetFields()">重置</a-button>
              </span>
            </a-col>
          </a-row>
        </a-form-model>
        <a-table
          :columns="columns"
          :rowKey="record => record.id"
          :dataSource="rows"
          :pagination="pagination"
          :loading="loading"
          :rowSelection="selection"
          @change="tableChange"
          :bordered="true"
        >
          <a-descriptions slot="expandedRowRender" slot-scope="record">
            <a-descriptions-item label="检测时间" :span="1">{{ DateUtil.format(record.checkTime) }}</a-descriptions-item>
            <a-descriptions-item label="标准路径" :span="2">{{ record.standard }}</a-descriptions-item>
            <a-descriptions-item label="standardName">{{ record.detail.standardName }}</a-descriptions-item>
            <a-descriptions-item label="sourceName">{{ record.detail.sourceName }}</a-descriptions-item>
            <a-descriptions-item label="resultName">{{ record.name }}</a-descriptions-item>
            <a-descriptions-item label="standardType">{{ record.detail.standardType }}</a-descriptions-item>
            <a-descriptions-item label="sourceType">{{ record.detail.sourceType }}</a-descriptions-item>
            <a-descriptions-item label="resultType">{{ record.type }}</a-descriptions-item>
            <a-descriptions-item label="standardSize">{{ record.detail.standardSize }}</a-descriptions-item>
            <a-descriptions-item label="sourceSize">{{ record.detail.sourceSize }}</a-descriptions-item>
            <a-descriptions-item label="resultSize">{{ record.size }}</a-descriptions-item>
            <a-descriptions-item label="standardDigit">{{ record.detail.standardDigit }}</a-descriptions-item>
            <a-descriptions-item label="sourceDigit">{{ record.detail.sourceDigit }}</a-descriptions-item>
            <a-descriptions-item label="resultDigit">{{ record.digit }}</a-descriptions-item>
            <a-descriptions-item label="standardNullable">{{ record.detail.standardNullable }}</a-descriptions-item>
            <a-descriptions-item label="sourceNullable">{{ record.detail.sourceNullable }}</a-descriptions-item>
            <a-descriptions-item label="resultNullable">{{ record.nullable }}</a-descriptions-item>
          </a-descriptions>
        </a-table>
        <div :class="rows.length > 0 ? 'table-pagination-tools' : 'table-pagination-tools-empty'">
          <a-space>
            <a-button icon="minus-circle" type="danger" @click="batchRemove" v-permit="'govern:assess:clear'" :disabled="selection.selectedRows.length === 0">删除</a-button>
          </a-space>
        </div>
      </div>
    </a-card>
  </section>
</template>

<script>
import DateUtil from '@/utils/date'
import RouteUtil from '@/utils/route'
import assessService from '@/service/govern/assess'

export default {
  data () {
    return {
      DateUtil,
      advanced: false,
      filters: {},
      columns: [
        { title: 'ID', dataIndex: 'id' },
        { title: '规则', dataIndex: 'assess' },
        { title: '数据源', dataIndex: 'source' },
        { title: '数据表', dataIndex: 'model' },
        { title: '字段名', dataIndex: 'code' },
        { title: '等级', dataIndex: 'level' },
        { title: '编码', dataIndex: 'name' },
        { title: '类型', dataIndex: 'type' },
        { title: '长度', dataIndex: 'size' },
        { title: '精度', dataIndex: 'digit' },
        { title: '为空', dataIndex: 'nullable' }
      ],
      selection: RouteUtil.selection(),
      pagination: {},
      rows: [],
      loading: false
    }
  },
  methods: {
    batchRemove () {
      this.$confirm(this.selection.confirm(() => {
        this.loading = true
        assessService.clear(this.selection.selectedRowKeys, { success: true }).then((result) => {
          if (result.code === 0) {
            this.search(false, true)
          } else {
            this.loading = false
          }
        })
      }))
    },
    tableChange (pagination, filters, sorter) {
      this.pagination = RouteUtil.paginationChange(this.pagination, pagination)
      this.search(true, true)
    },
    search (filter2query, pagination) {
      this.selection.clear()
      Object.assign(this.filters, RouteUtil.paginationData(this.pagination, pagination))
      filter2query && RouteUtil.filter2query(this, this.filters)
      this.loading = true
      assessService.log(this.filters).then((result) => {
        this.pagination = Object.assign({}, this.pagination, RouteUtil.result(result))
        if (result.code === 0) {
          this.rows = result.data.rows.map(item => {
            return Object.assign(item, { detail: JSON.parse(item.detail) })
          })
        }
        this.loading = false
      })
    }
  },
  created () {
    this.filters = RouteUtil.query2filter(this, { page: 1, pageSize: 5 })
    this.pagination = Object.assign({}, RouteUtil.pagination(this.filters), this.pagination)
  },
  mounted () {
    this.search(false, true)
  }
}
</script>
