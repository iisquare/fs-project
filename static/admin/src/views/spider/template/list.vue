<template>
  <section>
    <a-card :bordered="false">
      <div class="table-page-search-wrapper">
        <a-form-model ref="filters" :model="filters" layout="inline">
          <a-row :gutter="48">
            <a-col :md="6" :sm="24">
              <a-form-model-item label="">
                <a-input v-model="crawlerURL" placeholder="节点链接地址" :allowClear="true" />
              </a-form-model-item>
            </a-col>
            <a-col :md="5" :sm="24">
              <a-form-model-item>
                <a-input v-model="filters.name" placeholder="名称" :allowClear="true" />
              </a-form-model-item>
            </a-col>
            <a-col :md="5" :sm="24">
              <a-form-model-item label="">
                <a-input v-model="filters.type" placeholder="分组" :allowClear="true" />
              </a-form-model-item>
            </a-col>
            <a-col :md="8" :sm="24">
              <a-button type="primary" @click="search(true, false)" :loading="loading">查询</a-button>
              <a-button style="margin-left: 8px" @click="() => this.$refs.filters.resetFields()">重置</a-button>
              <a-button type="primary" style="margin-left: 8px" @click="connect">{{ filters.state ? '断开' : '连接' }}</a-button>
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
          <span slot="action" slot-scope="text, record">
            <a-dropdown :disabled="!filters.state">
              <a-menu slot="overlay">
                <a-menu-item><a-button type="link" size="small" @click="scheduleSubmit(text, record)">同步</a-button></a-menu-item>
                <a-menu-item><a-button type="link" size="small" @click="scheduleRun(text, record)">运行</a-button></a-menu-item>
                <a-menu-item v-if="record.history.scheduleId"><a-button type="link" size="small" @click="scheduleRemove(text, record)">删除</a-button></a-menu-item>
              </a-menu>
              <a-button type="link">作业</a-button>
            </a-dropdown>
            <a-dropdown :disabled="!filters.state || !record.history.scheduleId">
              <a-menu slot="overlay">
                <a-menu-item v-if="record.history.status === 'PAUSE'"><a-button type="link" size="small" @click="historyChange(text, record.history, 'start')">唤起</a-button></a-menu-item>
                <a-menu-item v-if="record.history.status === 'RUNNING'"><a-button type="link" size="small" @click="historyChange(text, record.history, 'pause')">暂停</a-button></a-menu-item>
                <a-menu-item v-if="record.history.status === 'PAUSE' || record.history.status === 'RUNNING'"><a-button type="link" size="small" @click="historyChange(text, record.history, 'stop')">停止</a-button></a-menu-item>
                <a-menu-item><a-button type="link" size="small" @click="historyClear(text, record.history)">清空</a-button></a-menu-item>
                <a-menu-item v-if="record.history.status === 'STOP' || record.history.status === 'FINISHED'"><a-button type="link" size="small" @click="historyRemove(text, record.history)">移除</a-button></a-menu-item>
              </a-menu>
              <a-button type="link">调度</a-button>
            </a-dropdown>
            <a-dropdown>
              <a-menu slot="overlay">
                <a-menu-item v-permit="'spider:template:'"><a-button type="link" size="small" @click="show(text, record)">查看</a-button></a-menu-item>
                <a-menu-item v-permit="'spider:template:modify'"><a-button type="link" size="small" @click="edit(text, record)">编辑</a-button></a-menu-item>
                <a-menu-item v-permit="'spider:template:modify'"><a-button type="link" size="small" @click="model(text, record)">模型</a-button></a-menu-item>
              </a-menu>
              <a-button type="link">模板</a-button>
            </a-dropdown>
          </span>
        </a-table>
        <div :class="rows.length > 0 ? 'table-pagination-tools' : 'table-pagination-tools-empty'">
          <a-button icon="minus-circle" type="danger" @click="batchRemove" v-permit="'spider:template:delete'" :disabled="selection.selectedRows.length === 0">删除</a-button>
          <a-divider type="vertical" v-permit="'spider:template:add'" />
          <a-button icon="plus-circle" type="primary" @click="add" v-permit="'spider:template:add'">新增</a-button>
        </div>
      </div>
    </a-card>
    <!--展示界面-->
    <a-modal :title="'信息查看 - ' + form.id" v-model="infoVisible" :footer="null">
      <a-form-model :model="form" :loading="infoLoading" :label-col="{ span: 5 }" :wrapper-col="{ span: 18 }">
        <a-form-model-item label="名称">{{ form.name }}</a-form-model-item>
        <a-form-model-item label="分组">{{ form.type }}</a-form-model-item>
        <a-form-model-item label="排序">{{ form.sort }}</a-form-model-item>
        <a-form-model-item label="内容"><a-textarea v-model="form.content" /></a-form-model-item>
        <a-form-model-item label="描述">{{ form.description }}</a-form-model-item>
        <a-form-model-item label="创建者">{{ form.createdUidName }}</a-form-model-item>
        <a-form-model-item label="创建时间">{{ form.createdTime|date }}</a-form-model-item>
        <a-form-model-item label="修改者">{{ form.updatedUidName }}</a-form-model-item>
        <a-form-model-item label="修改时间">{{ form.updatedTime|date }}</a-form-model-item>
      </a-form-model>
    </a-modal>
    <!--编辑界面-->
    <a-modal :title="'信息' + (form.id ? ('修改 - ' + form.id) : '添加')" v-model="formVisible" :confirmLoading="formLoading" :maskClosable="false" @ok="submit">
      <a-form-model ref="form" :model="form" :rules="rules" :label-col="{ span: 5 }" :wrapper-col="{ span: 18 }">
        <a-form-model-item label="名称" prop="name">
          <a-input v-model="form.name" auto-complete="off"></a-input>
        </a-form-model-item>
        <a-form-model-item label="分组" prop="type">
          <a-input v-model="form.type" auto-complete="on"></a-input>
        </a-form-model-item>
        <a-form-model-item label="排序">
          <a-input-number v-model="form.sort" :min="0" :max="200"></a-input-number>
        </a-form-model-item>
        <a-form-model-item label="内容">
          <a-textarea v-model="form.content" />
        </a-form-model-item>
        <a-form-model-item label="描述">
          <a-textarea v-model="form.description" />
        </a-form-model-item>
      </a-form-model>
    </a-modal>
  </section>
</template>

<script>
import DateUtil from '@/utils/date'
import RouteUtil from '@/utils/route'
import crawlerService from '@/service/spider/crawler'
import templateService from '@/service/spider/template'

export default {
  data () {
    return {
      filters: {},
      crawlerURL: '',
      columns: [
        { title: 'ID', dataIndex: 'id' },
        { title: '名称', dataIndex: 'name' },
        { title: '分组', dataIndex: 'type' },
        { title: '状态', dataIndex: 'history.status' },
        { title: '积压', dataIndex: 'history.channel' },
        { title: '触发时间', dataIndex: 'history.dispatch', customRender: DateUtil.dateRender },
        { title: '状态更新', dataIndex: 'history.version', customRender: DateUtil.dateRender },
        { title: '操作', width: 213, scopedSlots: { customRender: 'action' } }
      ],
      selection: RouteUtil.selection(),
      pagination: {},
      rows: [],
      loading: false,
      infoVisible: false,
      infoLoading: false,
      formVisible: false,
      formLoading: false,
      form: {},
      rules: {
        name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
        status: [{ required: true, message: '请选择状态', trigger: 'change' }]
      }
    }
  },
  methods: {
    connect () {
      this.filters.state = !this.filters.state
      if (this.filters.state) {
        this.crawlerURL = crawlerService.saveURL(this.crawlerURL)
      }
      this.search(true, true)
    },
    historyClear (id, row) {
      this.loading = true
      crawlerService.scheduleClear({ id: row.scheduleId }, { success: true }).then((result) => {
        this.search(false, true)
      })
    },
    historyChange (id, row, cmd) {
      this.loading = true
      crawlerService.scheduleChange({ status: cmd, id: row.scheduleId }, { success: true }).then((result) => {
        this.search(false, true)
      })
    },
    historyRemove (id, row) {
      this.loading = true
      crawlerService.scheduleRemove({ type: 'history', id: row.scheduleId }, { success: true }).then((result) => {
        this.search(false, true)
      })
    },
    scheduleRemove (id, row) {
      this.loading = true
      crawlerService.scheduleRemove({ type: 'schedule', id: row.scheduleId }, { success: true }).then((result) => {
        this.search(false, true)
      })
    },
    scheduleSubmit (id, row) {
      this.loading = true
      templateService.plain(row.id).then((content) => {
        content = JSON.stringify(content)
        crawlerService.scheduleSave({ type: 'schedule', content: content }, { success: true })
          .then((result) => { this.loading = false })
      })
    },
    scheduleRun (id, row) {
      this.loading = true
      crawlerService.scheduleSubmit({ id: row.id }, { success: true }).then((result) => {
        this.loading = false
      })
    },
    model (id, row) {
      this.$router.push('/spider/template/model?id=' + row.id)
    },
    batchRemove () {
      this.$confirm(this.selection.confirm(() => {
        this.loading = true
        templateService.delete(this.selection.selectedRowKeys, { success: true }).then((result) => {
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
      Promise.all([
        templateService.list(this.filters),
        this.filters.state ? crawlerService.scheduleHistory() : Promise.resolve({ code: 0, data: [] })
      ]).then(([result, histories]) => {
        this.pagination = Object.assign({}, this.pagination, RouteUtil.result(result))
        if (histories.code === 0) {
          histories = histories.data.reduce((accumulator, item) => {
            accumulator[item.scheduleId] = item
            return accumulator
          }, {})
        } else {
          histories = {}
        }
        if (result.code === 0) {
          this.rows = result.data.rows.map(item => {
            item.history = histories[item.id] ? histories[item.id] : {}
            return item
          })
        }
        this.loading = false
      })
    },
    submit () {
      this.$refs.form.validate(valid => {
        if (!valid || this.formLoading) return false
        this.formLoading = true
        templateService.save(this.form).then(result => {
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
    },
    show (text, record) {
      this.form = Object.assign({}, record, {
        description: record.description ? record.description : '暂无'
      })
      this.infoVisible = true
    }
  },
  created () {
    this.crawlerURL = crawlerService.baseURL()
    this.filters = RouteUtil.query2filter(this, { state: false, page: 1, pageSize: 5 })
    this.pagination = Object.assign({}, RouteUtil.pagination(this.filters), this.pagination)
  },
  mounted () {
    this.search(false, true)
  }
}
</script>
