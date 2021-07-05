<template>
  <section>
    <a-card :bordered="false">
      <div class="table-page-search-wrapper">
        <a-form-model ref="filters" :model="filters" layout="inline">
          <a-row :gutter="48">
            <a-col :md="6" :sm="24">
              <a-form-model-item label="发起人" prop="submitter">
                <service-auto-complete :search="userService.list" v-model="filters.submitter" :label.sync="filters.submitterName" />
              </a-form-model-item>
            </a-col>
            <a-col :md="6" :sm="24">
              <a-form-model-item label="流程名称" prop="deploymentId">
                <service-auto-complete :search="workflowService.list" v-model="filters.deploymentId" :label.sync="filters.deploymentName" fieldValue="deploymentId" />
              </a-form-model-item>
            </a-col>
            <a-col :md="6" :sm="24">
              <a-form-model-item label="完成状态" prop="finishStatus">
                <a-select v-model="filters.finishStatus" placeholder="请选择" :allowClear="true">
                  <a-select-option v-for="(value, key) in config.finishStatus" :key="key" :value="key">{{ value }}</a-select-option>
                </a-select>
              </a-form-model-item>
            </a-col>
            <a-col :md="6" :sm="24">
              <span class="table-page-search-submitButtons">
                <a-button type="primary" @click="search(true, false)" :loading="loading">查询</a-button>
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
          :bordered="true"
          @change="tableChange"
        >
          <span slot="action" slot-scope="text, record">
            <a-button-group>
              <a-button type="link" size="small">
                <router-link :to="'/oa/approve/process?processInstanceId=' + record.id + '&taskId='">查看</router-link>
              </a-button>
            </a-button-group>
          </span>
          <template slot="expandedRowRender" slot-scope="record" style="margin: 0">
            <p>{{ record.description || '暂无描述' }}</p>
          </template>
        </a-table>
      </div>
    </a-card>
  </section>
</template>

<script>
import DateUtil from '@/utils/date'
import RouteUtil from '@/utils/route'
import approveService from '@/service/oa/approve'
import userService from '@/service/member/user'
import workflowService from '@/service/oa/workflow'

export default {
  components: {
    ServiceAutoComplete: () => import('@/components/Service/AutoComplete')
  },
  data () {
    return {
      userService,
      workflowService,
      advanced: false,
      filters: {},
      columns: [
        { title: 'ID', dataIndex: 'id' },
        { title: '流程名称', dataIndex: 'deploymentInfo.name' },
        { title: '发起人', dataIndex: 'startUserIdName' },
        { title: '业务编号', dataIndex: 'businessKey' },
        { title: '创建时间', dataIndex: 'startTime', customRender: this.dateRender, width: 170 },
        { title: '结束时间', dataIndex: 'endTime', customRender: this.dateRender, width: 170 },
        { title: '操作', scopedSlots: { customRender: 'action' }, width: 80 }
      ],
      pagination: {},
      rows: [],
      loading: false,
      config: {
        ready: false,
        finishStatus: []
      }
    }
  },
  methods: {
    dateRender (text, record, index) {
      return DateUtil.format(text)
    },
    tableChange (pagination, filters, sorter) {
      this.pagination = RouteUtil.paginationChange(this.pagination, pagination)
      this.search(true, true)
    },
    search (filter2query, pagination) {
      Object.assign(this.filters, RouteUtil.paginationData(this.pagination, pagination))
      filter2query && RouteUtil.filter2query(this, this.filters)
      this.loading = true
      approveService.searchHistory(this.filters).then((result) => {
        this.pagination = Object.assign({}, this.pagination, RouteUtil.result(result))
        if (result.code === 0) {
          this.rows = result.data.rows
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
    workflowService.config().then((result) => {
      this.config.ready = true
      if (result.code === 0) {
        Object.assign(this.config, result.data)
      }
    })
  }
}
</script>
