<template>
  <section>
    <a-card :bordered="false">
      <div class="table-page-search-wrapper">
        <a-form-model ref="filters" :model="filters" layout="inline">
          <a-row :gutter="48">
            <a-col :md="6" :sm="24">
              <a-form-model-item label="方案名称" prop="type">
                <a-input v-model="filters.name" auto-complete="off"></a-input>
              </a-form-model-item>
            </a-col>
            <a-col :md="6" :sm="24">
              <a-form-model-item label="数据源编码" prop="source">
                <a-input v-model="filters.source" auto-complete="off"></a-input>
              </a-form-model-item>
            </a-col>
            <a-col :md="6" :sm="24">
              <a-form-model-item label="状态" prop="status">
                <a-select v-model="filters.status" placeholder="请选择" :allowClear="true">
                  <a-select-option v-for="(value, key) in config.status" :key="key" :value="key">{{ value }}</a-select-option>
                </a-select>
              </a-form-model-item>
            </a-col>
            <a-col :md="6" :sm="24">
              <a-button type="primary" @click="search(true, false)" :loading="loading">查询</a-button>
              <a-button style="margin-left: 8px" @click="() => this.$refs.filters.resetFields()">重置</a-button>
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
            <a-button-group>
              <a-button v-permit="'govern:qualityPlan:modify'" type="link" size="small" @click="edit(text, record)">编辑</a-button>
            </a-button-group>
          </span>
        </a-table>
        <div :class="rows.length > 0 ? 'table-pagination-tools' : 'table-pagination-tools-empty'">
          <a-button icon="minus-circle" type="danger" @click="batchRemove" v-permit="'govern:qualityPlan:delete'" :disabled="selection.selectedRows.length === 0">删除</a-button>
          <a-divider type="vertical" v-permit="'govern:qualityPlan:add'" />
          <a-button icon="plus-circle" type="primary" @click="add(0)" v-permit="'govern:qualityPlan:add'">新增</a-button>
        </div>
      </div>
    </a-card>
    <!--编辑界面-->
    <a-drawer
      :title="'信息' + (form.id ? ('修改 - ' + form.id) : '添加')"
      :visible="formVisible"
      @close="formVisible = false"
      :maskClosable="false"
      width="80%">
      <a-form-model ref="form" :model="form" :rules="rules" v-bind="UIUtil.formLayoutFlex()">
        <a-row class="form-row">
          <a-col :md="8" :sm="24">
            <a-form-model-item label="主键">
              <a-input type="text" v-model="form.id" auto-complete="off" placeholder="新增时自动生成"></a-input>
            </a-form-model-item>
          </a-col>
          <a-col :md="8" :sm="24">
            <a-form-model-item label="方案名称" prop="name">
              <a-input v-model="form.name" auto-complete="off"></a-input>
            </a-form-model-item>
          </a-col>
          <a-col :md="8" :sm="24">
            <a-form-model-item label="数据源编码" prop="source">
              <a-input v-model="form.source" auto-complete="off"></a-input>
            </a-form-model-item>
          </a-col>
        </a-row>
        <a-row class="form-row">
          <a-col :md="8" :sm="24">
            <a-form-model-item label="排序">
              <a-input-number v-model="form.sort" :min="0" :max="200"></a-input-number>
            </a-form-model-item>
          </a-col>
          <a-col :md="8" :sm="24">
            <a-form-model-item label="状态" prop="status">
              <a-select v-model="form.status" placeholder="请选择">
                <a-select-option v-for="(value, key) in config.status" :key="key" :value="key">{{ value }}</a-select-option>
              </a-select>
            </a-form-model-item>
          </a-col>
        </a-row>
        <a-row class="form-row">
          <a-col :md="24" :sm="24">
            <a-form-model-item label="描述">
              <a-textarea v-model="form.description" />
            </a-form-model-item>
          </a-col>
        </a-row>
        <a-row class="form-row">
          <a-col :md="24" :sm="24">
            <a-form-model-item label="规则集合">
              <a-textarea v-model="form.content" placeholder="质检规则主键集合，以英文逗号分割" />
            </a-form-model-item>
          </a-col>
        </a-row>
        <a-row class="form-row">
          <a-col :md="24" :sm="24" style="text-align: center;">
            <a-space>
              <a-button type="primary" @click="submit" :loading="formLoading">保存</a-button>
              <a-button @click="formVisible = false">取消</a-button>
            </a-space>
          </a-col>
        </a-row>
      </a-form-model>
    </a-drawer>
  </section>
</template>

<script>
import RouteUtil from '@/utils/route'
import planService from '@/service/govern/qualityPlan'
import UIUtil from '@/utils/ui'
import config from './design/config'

export default {
  data () {
    return {
      UIUtil,
      filters: {},
      columns: [
        { title: 'ID', dataIndex: 'id' },
        { title: '名称', dataIndex: 'name' },
        { title: '数据源', dataIndex: 'source' },
        { title: '排序', dataIndex: 'sort' },
        { title: '状态', dataIndex: 'statusText' },
        { title: '操作', scopedSlots: { customRender: 'action' } }
      ],
      selection: RouteUtil.selection(),
      pagination: {},
      rows: [],
      loading: false,
      config: Object.assign(config, {
        ready: false,
        status: []
      }),
      infoVisible: false,
      infoLoading: false,
      formVisible: false,
      formLoading: false,
      form: {},
      rules: {
        name: [{ required: true, message: '请输入规则名称', trigger: 'blur' }],
        source: [{ required: true, message: '请输入数据源编码', trigger: 'blur' }],
        status: [{ required: true, message: '请选择状态', trigger: 'change' }]
      }
    }
  },
  methods: {
    batchRemove () {
      this.$confirm(this.selection.confirm(() => {
        this.loading = true
        planService.delete(this.selection.selectedRowKeys, { success: true }).then((result) => {
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
      planService.list(this.filters).then((result) => {
        this.pagination = Object.assign({}, this.pagination, RouteUtil.result(result))
        if (result.code === 0) {
          this.rows = result.data.rows
        }
        this.loading = false
      })
    },
    submit () {
      this.$refs.form.validate(valid => {
        if (!valid || this.formLoading) return false
        this.formLoading = true
        planService.save(this.form).then(result => {
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
      this.form = Object.assign({}, record, {
        status: record.status + ''
      })
      this.formVisible = true
    }
  },
  created () {
    this.filters = RouteUtil.query2filter(this, { page: 1, pageSize: 5 })
    this.pagination = Object.assign({}, RouteUtil.pagination(this.filters), this.pagination)
  },
  mounted () {
    this.search(false, true)
    planService.config().then((result) => {
      this.config.ready = true
      if (result.code === 0) {
        Object.assign(this.config, result.data)
      }
    })
  }
}
</script>
