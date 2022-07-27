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
              <a-form-model-item label="运行状态" prop="state">
                <a-input v-model="filters.state" placeholder="" :allowClear="true" />
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
        <a-card style="margin-bottom: 25px;"><div ref="statistics" class="fs-ui-chart"></div></a-card>
        <a-table
          :columns="columns"
          :rowKey="(record, index) => index"
          :dataSource="rows"
          :pagination="pagination"
          :loading="loading"
          @change="tableChange"
          :bordered="true"
        >
          <span slot="action" slot-scope="text, record">
            <a-button-group>
              <a-button v-permit="'cron:flow:'" type="link" size="small" @click="e => stage(e, record)">明细</a-button>
            </a-button-group>
          </span>
          <a-descriptions slot="expandedRowRender" slot-scope="record">
            <a-descriptions-item label="并发度">{{ record.concurrent }}</a-descriptions-item>
            <a-descriptions-item label="并发策略">{{ record.concurrency }}</a-descriptions-item>
            <a-descriptions-item label="失败策略">{{ record.failure }}</a-descriptions-item>
          </a-descriptions>
        </a-table>
      </div>
    </a-card>
  </section>
</template>

<script>
import DateUtil from '@/utils/date'
import RouteUtil from '@/utils/route'
import cronService from '@/service/server/cron'
import * as echarts from 'echarts'

export default {
  data () {
    return {
      DateUtil,
      advanced: false,
      filters: {},
      columns: [
        { title: 'ID', dataIndex: 'id' },
        { title: '流程名称', dataIndex: 'name' },
        { title: '项目名称', dataIndex: 'project' },
        { title: '开始时间', dataIndex: 'createdTime', customRender: DateUtil.dateRender },
        { title: '结束时间', dataIndex: 'updatedTime', customRender: DateUtil.dateRender },
        { title: '持续时长', dataIndex: 'durationPretty' },
        { title: '运行状态', dataIndex: 'state' },
        { title: '操作', scopedSlots: { customRender: 'action' }, width: 80 }
      ],
      pagination: {},
      rows: [],
      loading: false
    }
  },
  methods: {
    tableChange (pagination, filters, sorter) {
      this.pagination = RouteUtil.paginationChange(this.pagination, pagination)
      this.search(true, true)
    },
    search (filter2query, pagination) {
      Object.assign(this.filters, RouteUtil.paginationData(this.pagination, pagination))
      filter2query && RouteUtil.filter2query(this, this.filters)
      this.loading = true
      cronService.flowLogList(this.filters).then((result) => {
        this.pagination = Object.assign({}, this.pagination, RouteUtil.result(result))
        if (result.code === 0) {
          this.rows = result.data.rows
          this.statistics()
        }
        this.loading = false
      })
    },
    stage (event, record = {}) {
      RouteUtil.forward(this, event, {
        path: '/server/cron/flowStage', query: { logId: record.id }
      })
    },
    statistics () {
      const data = this.rows.map(item => {
        return [new Date(item.createdTime), item.duration, item.durationPretty]
      })
      echarts.init(this.$refs.statistics).setOption({
        tooltip: {
          trigger: 'item',
          formatter (params) {
            return [
              '开始时间: ',
              DateUtil.format(params.data[0]),
              '<br/>',
              params.marker,
              ' ',
              params.data[2]
            ].join('')
          },
          axisPointer: {
            type: 'shadow'
          }
        },
        xAxis: { type: 'time' },
        yAxis: {},
        series: [{ ymbolSize: 20, data, type: 'scatter' }]
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
<style lang="less" scoped>
.fs-ui-chart {
  width: 100%;
  height: 350px;
}
</style>
