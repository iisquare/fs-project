<template>
  <section>
    <a-card :bordered="false">
      <div class="table-page-search-wrapper">
        <a-form-model ref="filters" :model="filters" layout="inline">
          <a-row :gutter="48">
            <a-col :md="6" :sm="24">
              <a-form-model-item label="标识" prop="key">
                <a-input v-model="filters.key" placeholder="" :allowClear="true" />
              </a-form-model-item>
            </a-col>
            <a-col :md="6" :sm="24">
              <a-form-model-item label="名称" prop="name">
                <a-input v-model="filters.name" placeholder="" :allowClear="true" />
              </a-form-model-item>
            </a-col>
            <a-col :md="6" :sm="24">
              <span class="table-page-search-submitButtons" :style="advanced && { float: 'right', overflow: 'hidden' } || {} ">
                <a-button type="primary" @click="search(true, false)" :loading="loading">查询</a-button>
                <a-button style="margin-left: 8px" @click="() => this.$refs.filters.resetFields()">重置</a-button>
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
          @change="tableChange"
          :bordered="true"
        >
          <span slot="action" slot-scope="text, record">
            <a-button-group>
              <a-button type="link" size="small" @click="show(text, record)">查看</a-button>
              <template v-permit="'oa:workflow:deleteDeployment'">
                <a-button type="link" size="small" @click="remove(text, record, false)">删除</a-button>
                <a-button type="link" size="small" @click="remove(text, record, true)">级联删除</a-button>
              </template>
            </a-button-group>
          </span>
        </a-table>
      </div>
    </a-card>
    <!--展示界面-->
    <a-modal :title="'信息查看 - ' + form.id" v-model="infoVisible" :footer="null">
      <a-form-model :model="form" :loading="infoLoading" :label-col="{ span: 5 }" :wrapper-col="{ span: 15 }">
        <a-form-model-item label="父级">{{ form.parentDeploymentId }}</a-form-model-item>
        <a-form-model-item label="主键">{{ form.id }}</a-form-model-item>
        <a-form-model-item label="标识">{{ form.key }}</a-form-model-item>
        <a-form-model-item label="名称">{{ form.name }}</a-form-model-item>
        <a-form-model-item label="租户">{{ form.tenantId }}</a-form-model-item>
        <a-form-model-item label="目录">{{ form.category }}</a-form-model-item>
        <a-form-model-item label="源自">{{ form.derivedFrom }}</a-form-model-item>
        <a-form-model-item label="源自根">{{ form.derivedFromRoot }}</a-form-model-item>
        <a-form-model-item label="版本">{{ form.engineVersion }}</a-form-model-item>
        <a-form-model-item label="部署时间">{{ form.deploymentTime|date }}</a-form-model-item>
      </a-form-model>
    </a-modal>
  </section>
</template>

<script>
import DateUtil from '@/utils/date'
import RouteUtil from '@/utils/route'
import workflowService from '@/service/oa/workflow'

export default {
  data () {
    return {
      advanced: false,
      filters: {},
      columns: [
        { title: 'ID', dataIndex: 'id' },
        { title: '标识', dataIndex: 'key' },
        { title: '名称', dataIndex: 'name' },
        { title: '部署时间', dataIndex: 'deploymentTime', customRender: this.dateRender, width: 170 },
        { title: '操作', scopedSlots: { customRender: 'action' }, width: 200 }
      ],
      pagination: {},
      rows: [],
      form: {},
      loading: false,
      infoVisible: false,
      infoLoading: false
    }
  },
  methods: {
    dateRender (text, record, index) {
      return DateUtil.format(text)
    },
    remove (text, record, cascade) {
      const _this = this
      this.$confirm({
        title: '操作提示',
        content: '确认删除所选记录吗？',
        onOk () {
          _this.loading = true
          workflowService.deleteDeployment({ id: record.id, cascade }, { success: true }).then((result) => {
            if (result.code === 0) {
              _this.search(false, true)
            } else {
              _this.loading = false
            }
          })
        }
      })
    },
    tableChange (pagination, filters, sorter) {
      this.pagination = RouteUtil.paginationChange(this.pagination, pagination)
      this.search(true, true)
    },
    toggleAdvanced () {
      this.advanced = !this.advanced
    },
    search (filter2query, pagination) {
      Object.assign(this.filters, RouteUtil.paginationData(this.pagination, pagination))
      filter2query && RouteUtil.filter2query(this, this.filters)
      this.loading = true
      workflowService.searchDeployment(this.filters).then((result) => {
        this.pagination = Object.assign({}, this.pagination, RouteUtil.result(result))
        if (result.code === 0) {
          this.rows = result.data.rows
        }
        this.loading = false
      })
    },
    show (text, record) {
      this.form = Object.assign({}, record, {
        description: record.description ? record.description : '暂无'
      })
      this.infoVisible = true
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
