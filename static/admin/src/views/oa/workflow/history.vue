<template>
  <section>
    <a-card :bordered="false">
      <div class="table-page-search-wrapper">
        <a-form-model ref="filters" :model="filters" layout="inline">
          <a-row :gutter="48">
            <a-col :md="6" :sm="24">
              <a-form-model-item label="流程名称" prop="deploymentId">
                <service-auto-complete :search="workflowService.list" v-model="filters.deploymentId" :label.sync="filters.deploymentName" fieldValue="deploymentId" />
              </a-form-model-item>
            </a-col>
            <a-col :md="6" :sm="24">
              <a-form-model-item label="发起人" prop="submitter">
                <service-auto-complete :search="userService.list" v-model="filters.submitter" :label.sync="filters.submitterName" />
              </a-form-model-item>
            </a-col>
            <a-col :md="6" :sm="24">
              <a-form-model-item label="参与人" prop="involvedUserId">
                <service-auto-complete :search="userService.list" v-model="filters.involvedUserId" :label.sync="filters.involvedUserName" />
              </a-form-model-item>
            </a-col>
          </a-row>
          <a-row :gutter="48">
            <a-col :md="6" :sm="24">
              <a-form-model-item label="完成状态" prop="finishStatus">
                <a-select v-model="filters.finishStatus" placeholder="请选择" :allowClear="true">
                  <a-select-option v-for="(value, key) in config.finishStatus" :key="key" :value="key">{{ value }}</a-select-option>
                </a-select>
              </a-form-model-item>
            </a-col>
            <a-col :md="6" :sm="24">
              <a-form-model-item label="删除状态" prop="deleteStatus">
                <a-select v-model="filters.deleteStatus" placeholder="请选择" :allowClear="true">
                  <a-select-option v-for="(value, key) in config.deleteStatus" :key="key" :value="key">{{ value }}</a-select-option>
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
              <a-button type="link" size="small" v-permit="'oa:workflow:process'">
                <router-link :to="'/oa/approve/process?processInstanceId=' + record.id">查看</router-link>
              </a-button>
            </a-button-group>
            <a-dropdown>
              <a-menu slot="overlay">
                <a-menu-item v-permit="'oa:workflow:reject'" v-if="!record.endTime">
                  <a-button :block="true" type="link" size="small" @click="reason(text, record, 'reject')">驳回</a-button>
                </a-menu-item>
                <a-menu-item v-permit="'oa:workflow:suspendProcessInstance'" v-if="record.processInstanceInfo.id && !record.processInstanceInfo.isSuspended">
                  <a-button :block="true" type="link" size="small" @click="toggle(text, record, false)">挂起</a-button>
                </a-menu-item>
                <a-menu-item v-permit="'oa:workflow:activateProcessInstance'" v-if="record.processInstanceInfo.id && record.processInstanceInfo.isSuspended">
                  <a-button :block="true" type="link" size="small" @click="toggle(text, record, true)">激活</a-button>
                </a-menu-item>
                <a-menu-item v-permit="'oa:workflow:deleteProcessInstance'" v-if="!record.endTime">
                  <a-button :block="true" type="link" size="small" @click="reason(text, record, 'deleteProcessInstance')">撤销</a-button>
                </a-menu-item>
                <a-menu-item v-permit="'oa:workflow:deleteHistoricProcessInstance'" v-if="record.endTime">
                  <a-button :block="true" type="link" size="small" @click="remove(text, record)">删除</a-button>
                </a-menu-item>
              </a-menu>
              <a-button type="link" icon="tool"></a-button>
            </a-dropdown>
          </span>
          <template slot="status" slot-scope="record">
            <approve-process-status :historic="record" :runtime="record.processInstanceInfo" />
          </template>
          <template slot="expandedRowRender" slot-scope="record" style="margin: 0">
            <p>{{ record.description || '暂无描述' }}</p>
          </template>
        </a-table>
      </div>
    </a-card>
    <!--撤销界面-->
    <a-modal :title="formTitle" v-model="formVisible" :confirmLoading="formLoading" :maskClosable="false" @ok="reasonSubmit">
      <a-form-model ref="form" :model="form" :rules="formRules" :label-col="{ span: 5 }" :wrapper-col="{ span: 18 }">
        <a-form-model-item label="流程名称">{{ form.deploymentInfo.name }}</a-form-model-item>
        <a-form-model-item label="业务编号">{{ form.businessKey }}</a-form-model-item>
        <a-form-model-item label="创建时间">{{ dateRender(form.startTime, form) }}</a-form-model-item>
        <a-form-model-item label="发起人">{{ form.startUserIdName }}</a-form-model-item>
        <a-form-model-item label="驳回选项" v-if="reasonAction === 'reject'">
          <a-checkbox v-model="form.local">仅局部节点可见</a-checkbox>
        </a-form-model-item>
        <a-form-model-item :label="reasonName" prop="reason">
          <a-textarea v-model="form.reason" />
        </a-form-model-item>
      </a-form-model>
    </a-modal>
  </section>
</template>

<script>
import DateUtil from '@/utils/date'
import RouteUtil from '@/utils/route'
import userService from '@/service/member/user'
import workflowService from '@/service/oa/workflow'

export default {
  components: {
    ApproveProcessStatus: () => import('../approve/ProcessStatus'),
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
        { title: '状态', scopedSlots: { customRender: 'status' }, width: 65, align: 'center' },
        { title: '创建时间', dataIndex: 'startTime', customRender: this.dateRender, width: 170 },
        { title: '结束时间', dataIndex: 'endTime', customRender: this.dateRender, width: 170 },
        { title: '操作', scopedSlots: { customRender: 'action' }, width: 110 }
      ],
      pagination: {},
      rows: [],
      loading: false,
      config: {
        ready: false,
        finishStatus: [],
        deleteStatus: []
      },
      formVisible: false,
      formLoading: false,
      form: { deploymentInfo: {}, local: false, reason: '' },
      reasonAction: null
    }
  },
  computed: {
    formTitle () {
      switch (this.reasonAction) {
        case 'reject': return '驳回单据'
        case 'deleteProcessInstance': return '撤销单据'
        default: return ''
      }
    },
    formRules () {
      switch (this.reasonAction) {
        case 'reject': return { reason: [{ required: true, message: '请输入撤销原因', trigger: 'blur' }] }
        case 'deleteProcessInstance': return { reason: [{ required: true, message: '请输入驳回原因', trigger: 'blur' }] }
        default: return {}
      }
    },
    reasonName () {
      switch (this.reasonAction) {
        case 'reject': return '驳回原因'
        case 'deleteProcessInstance': return '撤销原因'
        default: return ''
      }
    }
  },
  methods: {
    toggle (text, record, active) {
      const param = { processInstanceId: record.id }
      this.$confirm({
        title: '操作提示',
        content: `确认${active ? '激活' : '挂起'}流程吗？`,
        onOk: () => {
          this.loading = true
          const toggleProcessInstance = active ? workflowService.activateProcessInstance : workflowService.suspendProcessInstance
          toggleProcessInstance(param, { success: true }).then(result => {
            if (result.code === 0) {
              this.search(false, true)
            } else {
              this.loading = false
            }
          })
        }
      })
    },
    reasonSubmit () {
      this.$refs.form.validate(valid => {
        if (!valid || this.formLoading) return false
        this.formLoading = true
        const data = { processInstanceId: this.form.id, local: this.form.local, reason: this.form.reason }
        workflowService[this.reasonAction](data, { success: true }).then(result => {
          this.formLoading = false
          if (result.code !== 0) return result
          this.formVisible = false
          this.search()
        })
      })
    },
    reason (text, record, action) {
      this.reasonAction = action
      this.form = Object.assign({}, record, {
        reason: ''
      })
      this.formVisible = true
    },
    remove (text, record) {
      const param = { processInstanceId: record.id }
      this.$confirm({
        title: '操作提示',
        content: '确认删除[ID=' + record.id + ',流程名称=' + record.deploymentInfo.name + ',业务编号=' + record.businessKey + ']的流程吗？',
        onOk: () => {
          this.loading = true
          workflowService.deleteHistoricProcessInstance(param, { success: true }).then(result => {
            if (result.code === 0) {
              this.search(false, true)
            } else {
              this.loading = false
            }
          })
        }
      })
    },
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
      workflowService.searchHistory(this.filters).then((result) => {
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
