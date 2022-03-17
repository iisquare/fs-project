<template>
  <section>
    <a-card :bordered="false">
      <div class="table-page-search-wrapper">
        <a-form-model ref="filters" :model="filters" layout="inline">
          <a-row :gutter="48">
            <a-col :md="6" :sm="24">
              <a-form-model-item label="流程名称" prop="name">
                <a-input v-model="filters.name" placeholder="" :allowClear="true" />
              </a-form-model-item>
            </a-col>
            <a-col :md="6" :sm="24">
              <a-form-model-item label="项目名称" prop="project">
                <a-input v-model="filters.project" placeholder="" :allowClear="true" />
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
          :rowKey="(record, index) => index"
          :dataSource="rows"
          :pagination="pagination"
          :loading="loading"
          :rowSelection="selection"
          @change="tableChange"
          :bordered="true"
        >
          <span slot="action" slot-scope="text, record">
            <a-button-group>
              <a-button v-permit="'cron:flow:modify'" type="link" size="small" @click="model(record)">编辑</a-button>
            </a-button-group>
          </span>
          <a-descriptions slot="expandedRowRender" slot-scope="record">
            <a-descriptions-item label="创建者">{{ record.createdUidName }}</a-descriptions-item>
            <a-descriptions-item label="创建时间">{{ DateUtil.format(record.createdTime) }}</a-descriptions-item>
            <a-descriptions-item label="开始时间">{{ DateUtil.format(record.startTime) }}</a-descriptions-item>
            <a-descriptions-item label="修改者">{{ record.updatedUidName }}</a-descriptions-item>
            <a-descriptions-item label="修改时间">{{ DateUtil.format(record.updatedTime) }}</a-descriptions-item>
            <a-descriptions-item label="结束时间">{{ DateUtil.format(record.endTime) }}</a-descriptions-item>
            <a-descriptions-item label="上次触发">{{ DateUtil.format(record.previousFireTime) }}</a-descriptions-item>
            <a-descriptions-item label="下次触发">{{ DateUtil.format(record.nextFireTime) }}</a-descriptions-item>
            <a-descriptions-item label="最终触发">{{ DateUtil.format(record.finalFireTime) }}</a-descriptions-item>
            <a-descriptions-item label="描述信息" :span="3">{{ record.description }}</a-descriptions-item>
          </a-descriptions>
        </a-table>
        <div :class="rows.length > 0 ? 'table-pagination-tools' : 'table-pagination-tools-empty'">
          <a-button icon="minus-circle" type="danger" @click="batchRemove" v-permit="'cron:flow:delete'" :disabled="selection.selectedRows.length === 0">删除</a-button>
          <a-divider type="vertical" v-permit="'cron:flow:add'" />
          <a-button icon="plus-circle" type="primary" @click="model()" v-permit="'cron:flow:add'">新增</a-button>
        </div>
      </div>
    </a-card>
  </section>
</template>

<script>
import DateUtil from '@/utils/date'
import RouteUtil from '@/utils/route'
import cronService from '@/service/server/cron'

export default {
  data () {
    return {
      DateUtil,
      advanced: false,
      filters: {},
      columns: [
        { title: '流程名称', dataIndex: 'name' },
        { title: '项目名称', dataIndex: 'project' },
        { title: 'Cron表达式', dataIndex: 'expression' },
        { title: '排序', dataIndex: 'sort' },
        { title: '状态', dataIndex: 'state' },
        { title: '操作', scopedSlots: { customRender: 'action' }, width: 80 }
      ],
      selection: RouteUtil.selection({ type: 'radio' }),
      pagination: {},
      rows: [],
      loading: false
    }
  },
  methods: {
    batchRemove () {
      this.$confirm(this.selection.confirm(() => {
        this.loading = true
        const record = this.rows[this.selection.selectedRowKeys]
        const param = { name: record.name, project: record.project }
        cronService.flowDelete(param, { success: true }).then((result) => {
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
      cronService.flowList(this.filters).then((result) => {
        this.pagination = Object.assign({}, this.pagination, RouteUtil.result(result))
        if (result.code === 0) {
          this.rows = result.data.rows
        }
        this.loading = false
      })
    },
    model (record = {}) {
      this.$router.push({
        path: '/server/cron/model', query: { name: record.name, project: record.project }
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
