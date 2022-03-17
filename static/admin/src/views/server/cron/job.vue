<template>
  <section>
    <a-card :bordered="false">
      <div class="table-page-search-wrapper">
        <a-form-model ref="filters" :model="filters" layout="inline">
          <a-row :gutter="48">
            <a-col :md="6" :sm="24">
              <a-form-model-item label="名称" prop="name">
                <a-input v-model="filters.name" placeholder="" :allowClear="true" />
              </a-form-model-item>
            </a-col>
            <a-col :md="6" :sm="24">
              <a-form-model-item label="分组" prop="group">
                <a-input v-model="filters.group" placeholder="" :allowClear="true" />
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
              <a-button v-permit="'cron:job:modify'" type="link" size="small" @click="edit(text, record)">编辑</a-button>
              <a-dropdown>
                <a-menu slot="overlay">
                  <a-menu-item v-permit="'cron:job:'"><a-button :block="true" type="link" size="small" @click="command('pause', record)">暂停调度</a-button></a-menu-item>
                  <a-menu-item v-permit="'cron:job:'"><a-button :block="true" type="link" size="small" @click="command('resume', record)">恢复调度</a-button></a-menu-item>
                  <a-menu-item v-permit="'cron:job:'"><a-button :block="true" type="link" size="small" @click="trigger(text, record)">手动触发</a-button></a-menu-item>
                </a-menu>
                <a-button type="link" icon="tool"></a-button>
              </a-dropdown>
            </a-space>
          </span>
          <a-descriptions slot="expandedRowRender" slot-scope="record">
            <a-descriptions-item label="nonConcurrent">{{ record.nonConcurrent }}</a-descriptions-item>
            <a-descriptions-item label="updateData">{{ record.updateData }}</a-descriptions-item>
            <a-descriptions-item label="recovery">{{ record.recovery }}</a-descriptions-item>
            <a-descriptions-item label="执行类" :span="2">{{ record.cls }}</a-descriptions-item>
            <a-descriptions-item label="durable">{{ record.durable }}</a-descriptions-item>
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
    <a-modal :title="'信息' + (form.id ? ('修改 - ' + form.id) : '添加')" v-model="formVisible" :confirmLoading="formLoading" :maskClosable="false" @ok="submit">
      <a-tabs default-active-key="basic" :animated="false" tabPosition="left">
        <a-tab-pane key="basic" tab="基础信息">
          <a-form-model ref="form" :model="form" :rules="rules" :label-col="{ span: 5 }" :wrapper-col="{ span: 18 }">
            <a-form-model-item label="名称" prop="name">
              <a-input v-model="form.name" auto-complete="off"></a-input>
            </a-form-model-item>
            <a-form-model-item label="分组" prop="group">
              <a-input v-model="form.group" auto-complete="off"></a-input>
            </a-form-model-item>
            <a-form-model-item label="执行类" prop="cls">
              <a-input v-model="form.cls" auto-complete="off"></a-input>
            </a-form-model-item>
            <a-form-model-item label="描述">
              <a-textarea v-model="form.description" />
            </a-form-model-item>
          </a-form-model>
        </a-tab-pane>
        <a-tab-pane key="arg" tab="作业参数">
          <code-editor ref="arg" v-model="form.arg" mode="javascript" :height="260" />
        </a-tab-pane>
      </a-tabs>
    </a-modal>
    <a-modal title="手动触发" v-model="triggerVisible" :confirmLoading="formLoading" :maskClosable="false" @ok="command('trigger', form, form.arg)">
      <a-form-model :model="form" :label-col="{ span: 5 }" :wrapper-col="{ span: 18 }">
        <a-form-model-item label="名称" prop="name">{{ form.name }}</a-form-model-item>
        <a-form-model-item label="分组" prop="group">{{ form.group }}</a-form-model-item>
        <a-form-model-item label="执行类" prop="cls">{{ form.cls }}</a-form-model-item>
        <a-form-model-item label="触发参数">
          <code-editor ref="trigger" v-model="form.arg" mode="javascript" :height="260" />
        </a-form-model-item>
      </a-form-model>
    </a-modal>
  </section>
</template>

<script>
import RouteUtil from '@/utils/route'
import cronService from '@/service/server/cron'

export default {
  components: {
    CodeEditor: () => import('@/components/Editor/CodeEditor')
  },
  data () {
    return {
      advanced: false,
      filters: {},
      columns: [
        { title: '名称', dataIndex: 'name' },
        { title: '分组', dataIndex: 'group' },
        { title: '调度器', dataIndex: 'schedule' },
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
      triggerVisible: false,
      formVisible: false,
      formLoading: false,
      form: {},
      rules: {
        name: [{ required: true, message: '请输入作业名称', trigger: 'blur' }],
        group: [{ required: true, message: '请输入作业分组', trigger: 'blur' }],
        cls: [{ required: true, message: '请输入执行器类名称', trigger: 'blur' }]
      }
    }
  },
  methods: {
    command (command, record, arg = null) {
      const param = {
        arg,
        command,
        name: record.name,
        group: record.group
      }
      this.loading = true
      this.formLoading = true
      cronService.jobCommand(param, { success: true }).then((result) => {
        if (result.code === 0) {
          this.formLoading = false
          this.triggerVisible = false
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
      cronService.jobList(this.filters).then((result) => {
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
        cronService.jobSave(this.form).then(result => {
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
    },
    trigger (text, record) {
      this.form = Object.assign({}, record)
      this.triggerVisible = true
      this.$refs.trigger && this.$refs.trigger.setContent(this.form.arg)
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
