<template>
  <section>
    <a-card :bordered="false">
      <div class="table-page-search-wrapper">
        <a-form-model ref="filters" :model="filters" layout="inline">
          <a-row :gutter="48">
            <a-col :md="6" :sm="24">
              <a-form-model-item label="触发器名称" prop="name">
                <a-input v-model="filters.name" placeholder="" :allowClear="true" />
              </a-form-model-item>
            </a-col>
            <a-col :md="6" :sm="24">
              <a-form-model-item label="触发器分组" prop="group">
                <a-input v-model="filters.group" placeholder="" :allowClear="true" />
              </a-form-model-item>
            </a-col>
          </a-row>
          <a-row :gutter="48">
            <a-col :md="6" :sm="24">
              <a-form-model-item label="作业名称" prop="jobName">
                <a-input v-model="filters.jobName" placeholder="" :allowClear="true" />
              </a-form-model-item>
            </a-col>
            <a-col :md="6" :sm="24">
              <a-form-model-item label="作业分组" prop="jobGroup">
                <a-input v-model="filters.jobGroup" placeholder="" :allowClear="true" />
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
            <a-space>
              <a-button v-permit="'cron:job:'" type="link" size="small" @click="edit(text, record)">编辑</a-button>
              <a-button v-permit="'cron:job:'" type="link" size="small" @click="command('pause', record)" v-if="record.state !== 'PAUSED'">暂停</a-button>
              <a-button v-permit="'cron:job:'" type="link" size="small" @click="command('resume', record)" v-if="record.state === 'PAUSED'">恢复</a-button>
            </a-space>
          </span>
          <a-descriptions slot="expandedRowRender" slot-scope="record">
            <a-descriptions-item label="Cron表达式" :span="3" v-if="record.type === 'CRON'">{{ record.expression }}</a-descriptions-item>
            <a-descriptions-item label="开始时间">{{ DateUtil.format(record.startTime) }}</a-descriptions-item>
            <a-descriptions-item label="结束时间">{{ DateUtil.format(record.endTime) }}</a-descriptions-item>
            <a-descriptions-item label="排期日历">{{ record.calendar }}</a-descriptions-item>
            <a-descriptions-item label="上次触发">{{ DateUtil.format(record.previousFireTime) }}</a-descriptions-item>
            <a-descriptions-item label="下次触发">{{ DateUtil.format(record.nextFireTime) }}</a-descriptions-item>
            <a-descriptions-item label="最终触发">{{ DateUtil.format(record.finalFireTime) }}</a-descriptions-item>
            <a-descriptions-item label="misfire">{{ record.misfire }}</a-descriptions-item>
            <a-descriptions-item label="data" :span="2">{{ record.data }}</a-descriptions-item>
            <a-descriptions-item label="描述" :span="3">{{ record.description }}</a-descriptions-item>
          </a-descriptions>
        </a-table>
        <div :class="rows.length > 0 ? 'table-pagination-tools' : 'table-pagination-tools-empty'">
          <a-button icon="minus-circle" type="danger" @click="batchRemove" v-permit="'cron:job:delete'" :disabled="selection.selectedRows.length === 0">删除</a-button>
          <a-divider type="vertical" v-permit="'cron:job:add'" />
          <a-button icon="plus-circle" type="primary" @click="add" v-permit="'cron:job:add'">新增</a-button>
        </div>
      </div>
    </a-card>
    <a-modal
      :title="'信息' + (form.id ? ('修改 - ' + form.id) : '添加')"
      v-model="formVisible"
      :confirmLoading="formLoading"
      :maskClosable="false"
      @ok="submit"
      :width="650">
      <a-tabs default-active-key="basic" :animated="false" tabPosition="left">
        <a-tab-pane key="basic" tab="基础信息">
          <a-form-model ref="form" :model="form" :rules="rules" :label-col="{ span: 5 }" :wrapper-col="{ span: 18 }">
            <a-form-model-item label="触发器名称" prop="name">
              <a-input v-model="form.name" auto-complete="off"></a-input>
            </a-form-model-item>
            <a-form-model-item label="触发器分组" prop="group">
              <a-input v-model="form.group" auto-complete="off"></a-input>
            </a-form-model-item>
            <a-form-model-item label="作业名称" prop="jobName">
              <a-input v-model="form.jobName" auto-complete="off"></a-input>
            </a-form-model-item>
            <a-form-model-item label="作业分组" prop="jobGroup">
              <a-input v-model="form.jobGroup" auto-complete="off"></a-input>
            </a-form-model-item>
            <a-form-model-item label="优先级">
              <a-input-number v-model="form.priority" :allowClear="true"></a-input-number>
            </a-form-model-item>
            <a-form-model-item label="Cron表达式" prop="expression">
              <a-input v-model="form.expression" auto-complete="off"></a-input>
            </a-form-model-item>
            <a-form-model-item label="描述">
              <a-textarea v-model="form.description" />
            </a-form-model-item>
          </a-form-model>
        </a-tab-pane>
        <a-tab-pane key="arg" tab="触发参数">
          <code-editor ref="arg" v-model="form.arg" mode="javascript" :height="260" />
        </a-tab-pane>
      </a-tabs>
    </a-modal>
  </section>
</template>

<script>
import DateUtil from '@/utils/date'
import RouteUtil from '@/utils/route'
import cronService from '@/service/server/cron'

export default {
  components: {
    CodeEditor: () => import('@/components/Editor/CodeEditor')
  },
  data () {
    return {
      DateUtil,
      advanced: false,
      filters: {},
      columns: [
        { title: '触发器名称', dataIndex: 'name' },
        { title: '触发器分组', dataIndex: 'group' },
        { title: '作业名称', dataIndex: 'jobName' },
        { title: '作业分组', dataIndex: 'jobGroup' },
        { title: '调度器', dataIndex: 'schedule' },
        { title: '优先级', dataIndex: 'priority' },
        { title: '状态', dataIndex: 'state' },
        { title: '类型', dataIndex: 'type' },
        { title: '操作', scopedSlots: { customRender: 'action' }, width: 120 }
      ],
      selection: RouteUtil.selection({ type: 'radio' }),
      pagination: {},
      rows: [],
      loading: false,
      config: {
        ready: false,
        status: []
      },
      formVisible: false,
      formLoading: false,
      form: {},
      rules: {
        name: [{ required: true, message: '请输入触发器名称', trigger: 'blur' }],
        group: [{ required: true, message: '请输入触发器分组', trigger: 'blur' }],
        jobName: [{ required: true, message: '请输入作业名称', trigger: 'blur' }],
        jobGroup: [{ required: true, message: '请输入作业分组', trigger: 'blur' }],
        expression: [{ required: true, message: '请输入Cron表达', trigger: 'blur' }]
      }
    }
  },
  methods: {
    command (command, record) {
      const param = {
        command,
        name: record.name,
        group: record.group
      }
      this.loading = true
      cronService.triggerCommand(param, { success: true }).then((result) => {
        if (result.code === 0) {
          this.search(false, true)
        } else {
          this.loading = false
        }
      })
    },
    batchRemove () {
      this.$confirm(this.selection.confirm(() => {
        const record = this.rows[this.selection.selectedRowKeys[0]]
        this.command('delete', record)
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
      cronService.triggerList(this.filters).then((result) => {
        this.pagination = Object.assign({}, this.pagination, RouteUtil.result(result))
        if (result.code === 0) {
          this.rows = result.data.rows
        }
        this.loading = false
      })
    },
    submit () {
      if (this.$refs.arg) {
        this.form.arg = this.$refs.arg.getContent()
      }
      this.$refs.form.validate(valid => {
        if (!valid || this.formLoading) return false
        this.formLoading = true
        cronService.triggerSave(this.form).then(result => {
          if (result.code === 0) {
            this.formVisible = false
            this.search(false, true)
          }
          this.formLoading = false
        })
      })
    },
    add () {
      this.form = {}
      this.formVisible = true
    },
    edit (text, record) {
      this.form = Object.assign({}, record)
      this.formVisible = true
      this.$refs.arg && this.$refs.arg.setContent(this.form.arg)
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
