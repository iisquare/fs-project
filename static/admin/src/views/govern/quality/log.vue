<template>
  <section>
    <a-card :bordered="false">
      <div class="table-page-search-wrapper">
        <a-form-model ref="filters" :model="filters" layout="inline">
          <a-row :gutter="48">
            <a-col :md="6" :sm="24">
              <a-form-model-item label="所属分类" prop="logicId">
                <a-tree-select
                  v-model="filters.logicId"
                  :tree-data="logicTree"
                  placeholder="请选择所属分类"
                  tree-default-expand-all
                  :allowClear="true"
                >
                </a-tree-select>
              </a-form-model-item>
            </a-col>
            <a-col :md="6" :sm="24">
              <a-form-model-item label="所属规则" prop="ruleId">
                <a-input v-model="filters.ruleId" placeholder="规则ID" :allowClear="true"></a-input>
              </a-form-model-item>
            </a-col>
            <a-col :md="6" :sm="24">
              <a-form-model-item label="所属方案" prop="planId">
                <a-input v-model="filters.planId" placeholder="方案ID" :allowClear="true"></a-input>
              </a-form-model-item>
            </a-col>
            <a-col :md="6" :sm="24">
              <a-form-model-item label="检查状态" prop="state">
                <a-input v-model="filters.state" placeholder="状态码" :allowClear="true"></a-input>
              </a-form-model-item>
            </a-col>
          </a-row>
          <a-row :gutter="48">
            <a-col :md="6" :sm="24">
              <a-form-model-item label="数据源编码" prop="source">
                <a-input v-model="filters.source" auto-complete="off" :allowClear="true"></a-input>
              </a-form-model-item>
            </a-col>
            <a-col :md="6" :sm="24">
              <a-form-model-item label="检查表名" prop="checkTable">
                <a-input v-model="filters.checkTable" auto-complete="off" :allowClear="true"></a-input>
              </a-form-model-item>
            </a-col>
            <a-col :md="6" :sm="24">
              <a-form-model-item label="检查字段" prop="checkColumn">
                <a-input v-model="filters.checkColumn" auto-complete="off" :allowClear="true"></a-input>
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
              <a-button type="link" size="small" @click="show(text, record)">查看</a-button>
            </a-button-group>
          </span>
        </a-table>
        <div :class="rows.length > 0 ? 'table-pagination-tools' : 'table-pagination-tools-empty'">
          <a-button icon="minus-circle" type="danger" @click="batchRemove" v-permit="'govern:qualityPlan:clear'" :disabled="selection.selectedRows.length === 0">删除</a-button>
        </div>
      </div>
    </a-card>
    <!--编辑界面-->
    <a-drawer
      title="质检报告详细日志"
      :visible="formVisible"
      @close="formVisible = false"
      :maskClosable="false"
      width="80%">
      <a-form-model ref="form" :model="form" :rules="rules" v-bind="UIUtil.formLayoutFlex()">
        <a-row class="form-row">
          <a-col :md="8" :sm="24">
            <a-form-model-item label="日志标识">{{ form.id }}</a-form-model-item>
          </a-col>
          <a-col :md="8" :sm="24">
            <a-form-model-item label="检查状态">{{ form.state }}</a-form-model-item>
          </a-col>
          <a-col :md="8" :sm="24">
            <a-form-model-item label="检查时间">{{ DateUtil.format(form.createdTime) }}</a-form-model-item>
          </a-col>
        </a-row>
        <a-row class="form-row">
          <a-col :md="8" :sm="24">
            <a-form-model-item label="所属方案">{{ form.planId }} - {{ form.planIdName }}</a-form-model-item>
          </a-col>
          <a-col :md="8" :sm="24">
            <a-form-model-item label="所属规则">{{ form.ruleId }} - {{ form.ruleIdName }}</a-form-model-item>
          </a-col>
          <a-col :md="8" :sm="24">
            <a-form-model-item label="所属分类">{{ form.logicId }} - {{ form.logicIdName }}</a-form-model-item>
          </a-col>
        </a-row>
        <a-row class="form-row">
          <a-col :md="8" :sm="24">
            <a-form-model-item label="数据源编码">{{ form.source }}</a-form-model-item>
          </a-col>
          <a-col :md="8" :sm="24">
            <a-form-model-item label="检查表名">{{ form.checkTable }}</a-form-model-item>
          </a-col>
          <a-col :md="8" :sm="24">
            <a-form-model-item label="检查字段">{{ form.checkColumn }}</a-form-model-item>
          </a-col>
        </a-row>
        <a-row class="form-row">
          <a-col :md="8" :sm="24">
            <a-form-model-item label="检查记录">{{ form.checkCount }}</a-form-model-item>
          </a-col>
          <a-col :md="8" :sm="24">
            <a-form-model-item label="命中记录">{{ form.hitCount }}</a-form-model-item>
          </a-col>
        </a-row>
        <a-row class="form-row">
          <a-col :md="24" :sm="24">
            <a-form-model-item label="整改原因">{{ form.reason }}</a-form-model-item>
          </a-col>
        </a-row>
        <a-row class="form-row">
          <a-col :md="24" :sm="24">
            <a-form-model-item label="检查逻辑">{{ form.expression }}</a-form-model-item>
          </a-col>
        </a-row>
      </a-form-model>
    </a-drawer>
  </section>
</template>

<script>
import RouteUtil from '@/utils/route'
import planService from '@/service/govern/qualityPlan'
import logicService from '@/service/govern/qualityLogic'
import UIUtil from '@/utils/ui'
import DateUtil from '@/utils/date'
import config from './design/config'

export default {
  data () {
    return {
      UIUtil,
      DateUtil,
      filters: {},
      columns: [
        { title: 'ID', dataIndex: 'id' },
        { title: '数据源', dataIndex: 'source' },
        { title: '所属分类', dataIndex: 'logicIdName' },
        { title: '所属规则', dataIndex: 'ruleIdName' },
        { title: '所属方案', dataIndex: 'planIdName' },
        { title: '检查表名', dataIndex: 'checkTable' },
        { title: '检查字段', dataIndex: 'checkColumn' },
        { title: '检查记录', dataIndex: 'checkCount' },
        { title: '命中记录', dataIndex: 'hitCount' },
        { title: '检查状态', dataIndex: 'state' },
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
      rules: {},
      logicTree: []
    }
  },
  methods: {
    batchRemove () {
      this.$confirm(this.selection.confirm(() => {
        this.loading = true
        planService.clear(this.selection.selectedRowKeys, { success: true }).then((result) => {
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
      planService.log(this.filters).then((result) => {
        this.pagination = Object.assign({}, this.pagination, RouteUtil.result(result))
        if (result.code === 0) {
          this.rows = result.data.rows
        }
        this.loading = false
      })
    },
    show (text, record) {
      this.form = Object.assign({}, record)
      this.formVisible = true
    }
  },
  created () {
    this.filters = RouteUtil.query2filter(this, { page: 1, pageSize: 5 })
    this.pagination = Object.assign({}, RouteUtil.pagination(this.filters), this.pagination)
  },
  mounted () {
    this.search(false, true)
    logicService.tree().then((result) => {
      if (result.code === 0) {
        this.logicTree = UIUtil.treeSelect(result.data).tree
      }
    })
  }
}
</script>
