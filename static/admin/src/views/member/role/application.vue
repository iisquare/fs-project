<template>
  <section>
    <a-card :bordered="false">
      <template slot="title">角色：[{{ info.id }}]{{ info.name }}</template>
      <template slot="extra">
        <a-button @click.native="$router.go(-1)">返回</a-button>
      </template>
      <div class="table-page-search-wrapper">
        <a-form-model ref="filters" :model="filters" layout="inline">
          <a-row :gutter="48">
            <a-col :md="6" :sm="24">
              <a-form-model-item label="标识" prop="serial">
                <a-input v-model="filters.serial" placeholder="" :allowClear="true" />
              </a-form-model-item>
            </a-col>
            <a-col :md="6" :sm="24">
              <a-form-model-item label="名称" prop="name">
                <a-input v-model="filters.name" placeholder="" :allowClear="true" />
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
          <span slot="url" slot-scope="text, record">
            <a-icon :type="record.icon" v-if="record.icon" />
            <a :href="record.url" :title="record.url" target="_blank">{{ record.target ? record.target : (record.url ? '_self' : '无链接') }}</a>
          </span>
          <span slot="action" slot-scope="text, record">
            <a-button-group>
              <a-button type="link" size="small" v-permit="'member:role:resource'" @click="editTree('resource', record)">资源</a-button>
              <a-button type="link" size="small" v-permit="'member:role:menu'" @click="editTree('menu', record)">菜单</a-button>
            </a-button-group>
          </span>
        </a-table>
        <div :class="rows.length > 0 ? 'table-pagination-tools' : 'table-pagination-tools-empty'">
          <a-button icon="minus-circle" @click="revoke" :disabled="selection.selectedRows.length === 0" :loading="loading">解除</a-button>
          <a-divider type="vertical" />
          <a-button icon="plus-circle" @click="grant" :disabled="selection.selectedRows.length === 0" :loading="loading">授权</a-button>
        </div>
      </div>
    </a-card>
    <!--资源菜单-->
    <a-modal
      :title="tree.title + ' - 角色：' + info.name + ' - 应用：' + tree.application.name"
      v-model="tree.visible"
      :confirmLoading="tree.loading"
      :maskClosable="false"
      :width="650"
      @ok="saveTree">
      <a-table
        :columns="tree.columns"
        :rowKey="record => record.id"
        :dataSource="tree.rows"
        :pagination="false"
        :loading="tree.loading"
        :rowSelection="tree.selection"
        :bordered="true"
        size="small"
        :expandedRowKeys="tree.expandedRowKeys"
        @expandedRowsChange="(expandedRows) => this.tree.expandedRowKeys = expandedRows"
      >
      </a-table>
    </a-modal>
  </section>
</template>

<script>
import RouteUtil from '@/utils/route'
import applicationService from '@/service/member/application'
import ApiUtil from '@/utils/api'
import roleService from '@/service/member/role'

export default {
  data () {
    return {
      filters: {},
      columns: [
        { title: 'ID', dataIndex: 'id' },
        { title: '标识', dataIndex: 'serial' },
        { title: '名称', dataIndex: 'name' },
        { title: '授权状态', dataIndex: 'permit' },
        { title: '应用状态', dataIndex: 'statusText' },
        { title: '操作', scopedSlots: { customRender: 'action' } }
      ],
      selection: RouteUtil.selection(),
      pagination: {},
      info: {},
      permitted: [],
      rows: [],
      loading: false,
      config: {
        ready: false,
        status: []
      },
      infoVisible: false,
      infoLoading: false,
      formVisible: false,
      formLoading: false,
      form: {},
      rules: {
        serial: [{ required: true, message: '请输入标识', trigger: 'blur' }],
        name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
        status: [{ required: true, message: '请选择状态', trigger: 'change' }]
      },
      tree: {
        id: '',
        type: '',
        application: {},
        title: '',
        visible: false,
        loading: false,
        selection: RouteUtil.selection(),
        rows: [],
        expandedRowKeys: [],
        columns: [
          { title: '名称', dataIndex: 'name' },
          { title: '全称', dataIndex: 'fullName' }
        ]
      }
    }
  },
  methods: {
    batchRemove () {
      this.$confirm(this.selection.confirm(() => {
        this.loading = true
        applicationService.delete(this.selection.selectedRowKeys, { success: true }).then((result) => {
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
      const params = {
        id: this.$route.query.id,
        type: 'application'
      }
      Promise.all([roleService.permit(params), applicationService.list(this.filters)]).then((results) => {
        this.pagination = Object.assign({}, this.pagination, RouteUtil.result(results[1]))
        if (ApiUtil.succeed(results[0]) && ApiUtil.succeed(results[1])) {
          this.permitted = results[0].data.checked
          this.rows = results[1].data.rows.map(item => {
            item.permit = this.permitted.indexOf(item.id) === -1 ? 'N' : 'Y'
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
        applicationService.save(this.form).then(result => {
          if (result.code === 0) {
            this.formVisible = false
            this.search(false, true)
          }
          this.formLoading = false
        })
      })
    },
    grant () {
      const bids = new Set(this.permitted)
      this.selection.selectedRowKeys.forEach(item => {
        bids.add(item)
      })
      this.permit(Array.from(bids))
    },
    revoke () {
      const bids = new Set(this.permitted)
      this.selection.selectedRowKeys.forEach(item => {
        bids.delete(item)
      })
      this.permit(Array.from(bids))
    },
    permit (bids) {
      if (this.loading) return false
      this.loading = true
      const params = { id: this.$route.query.id, type: 'application', bids }
      console.log(bids)
      roleService.permit(params).then(result => {
        if (result.code === 0) {
          this.search(false, true)
        }
      })
    },
    editTree (type, application) {
      Object.assign(this.tree, {
        id: this.info.id,
        type: type,
        application: application,
        title: { resource: '资源分配', menu: '菜单分配' }[type],
        rows: [],
        visible: true,
        loading: true
      })
      this.tree.selection.clear()
      roleService.permit({ id: this.tree.id, type, applicationId: application.id }).then((result) => {
        if (result.code === 0) {
          Object.assign(this.tree, {
            loading: false,
            rows: result.data.tree,
            expandedRowKeys: RouteUtil.expandedRowKeys(result.data.tree, 2)
          })
          this.tree.selection.selectedRowKeys = result.data.checked
        }
      })
    },
    saveTree () {
      if (this.tree.loading) return false
      this.tree.loading = true
      roleService.permit({
        id: this.tree.id,
        type: this.tree.type,
        applicationId: this.tree.application.id,
        bids: this.tree.selection.selectedRowKeys }).then(result => {
        if (result.code === 0) {
          this.tree.visible = false
          this.search(false, true)
        }
        this.tree.loading = true
      })
    }
  },
  created () {
    this.filters = RouteUtil.query2filter(this, { page: 1, pageSize: 100 })
    this.pagination = Object.assign({}, RouteUtil.pagination(this.filters), this.pagination)
  },
  mounted () {
    const id = this.$route.query.id
    roleService.info(id).then(result => {
      if (ApiUtil.succeed(result)) {
        this.info = result.data
      }
    })
    this.search(false, true)
  }
}
</script>
