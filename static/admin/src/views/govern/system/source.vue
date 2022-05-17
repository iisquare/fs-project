<template>
  <section>
    <a-card :bordered="false">
      <div class="table-page-search-wrapper">
        <a-form-model ref="filters" :model="filters" layout="inline">
          <a-row :gutter="48">
            <a-col :md="6" :sm="24">
              <a-form-model-item label="编码" prop="code">
                <a-input v-model="filters.code" placeholder="" :allowClear="true" />
              </a-form-model-item>
            </a-col>
            <a-col :md="6" :sm="24">
              <a-form-model-item label="名称" prop="name">
                <a-input v-model="filters.name" placeholder="" :allowClear="true" />
              </a-form-model-item>
            </a-col>
            <a-col :md="6" :sm="24">
              <a-form-model-item label="类型" prop="status">
                <a-select v-model="filters.type" placeholder="请选择" :allowClear="true">
                  <a-select-option v-for="item in config.sourceTypes" :key="item.type" :value="item.type">{{ item.title }}</a-select-option>
                </a-select>
              </a-form-model-item>
            </a-col>
            <a-col :md="6" :sm="24">
              <a-space>
                <a-button type="primary" @click="search(true, false)" :loading="loading">查询</a-button>
                <a-button @click="() => this.$refs.filters.resetFields()">重置</a-button>
              </a-space>
            </a-col>
          </a-row>
        </a-form-model>
        <a-table
          :columns="columns"
          :rowKey="record => record.code"
          :dataSource="rows"
          :pagination="pagination"
          :loading="loading"
          :rowSelection="selection"
          @change="tableChange"
          :bordered="true"
        >
          <span slot="action" slot-scope="text, record">
            <a-button-group>
              <a-button v-permit="'govern:source:modify'" type="link" size="small" @click="e => edit(e, record)">编辑</a-button>
            </a-button-group>
          </span>
          <a-descriptions slot="expandedRowRender" slot-scope="record">
            <a-descriptions-item label="创建者">{{ record.createdUidName }}</a-descriptions-item>
            <a-descriptions-item label="创建时间" :span="2">{{ DateUtil.format(record.createdTime) }}</a-descriptions-item>
            <a-descriptions-item label="修改者">{{ record.updatedUidName }}</a-descriptions-item>
            <a-descriptions-item label="修改时间" :span="2">{{ DateUtil.format(record.updatedTime) }}</a-descriptions-item>
            <a-descriptions-item label="描述信息" :span="3">{{ record.description }}</a-descriptions-item>
          </a-descriptions>
        </a-table>
        <div :class="rows.length > 0 ? 'table-pagination-tools' : 'table-pagination-tools-empty'">
          <a-space>
            <a-button icon="minus-circle" type="danger" @click="batchRemove" v-permit="'govern:source:delete'" :disabled="selection.selectedRows.length === 0">删除</a-button>
            <a-button icon="plus-circle" type="primary" @click="e => edit(e)" v-permit="'govern:source:add'">新增</a-button>
          </a-space>
        </div>
      </div>
    </a-card>
  </section>
</template>

<script>
import config from './design/config'
import DateUtil from '@/utils/date'
import RouteUtil from '@/utils/route'
import sourceService from '@/service/govern/source'

export default {
  data () {
    return {
      DateUtil,
      advanced: false,
      filters: {},
      columns: [
        { title: '编码', dataIndex: 'code' },
        { title: '名称', dataIndex: 'name' },
        { title: '类型', dataIndex: 'type' },
        { title: '版本', dataIndex: 'version' },
        { title: '排序', dataIndex: 'sort' },
        { title: '状态', dataIndex: 'statusText' },
        { title: '操作', scopedSlots: { customRender: 'action' }, width: 80 }
      ],
      selection: RouteUtil.selection({ type: 'radio' }),
      pagination: {},
      rows: [],
      loading: false,
      config: Object.assign(config, {
        ready: false,
        status: {}
      })
    }
  },
  methods: {
    batchRemove () {
      this.$confirm(this.selection.confirm(() => {
        this.loading = true
        const param = { code: this.selection.selectedRowKeys[0] }
        sourceService.delete(param, { success: true }).then((result) => {
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
      sourceService.list(this.filters).then((result) => {
        this.pagination = Object.assign({}, this.pagination, RouteUtil.result(result))
        if (result.code === 0) {
          this.rows = result.data.rows
        }
        this.loading = false
      })
    },
    edit (e, record = {}) {
      RouteUtil.forward(this, e, { path: '/govern/system/sourceModel', query: { code: record.code } })
    }
  },
  created () {
    this.filters = RouteUtil.query2filter(this, { page: 1, pageSize: 100 })
    this.pagination = Object.assign({}, RouteUtil.pagination(this.filters), this.pagination)
  },
  mounted () {
    this.search(false, true)
    sourceService.config().then((result) => {
      this.config.ready = true
      if (result.code === 0) {
        Object.assign(this.config, result.data)
      }
    })
  }
}
</script>
