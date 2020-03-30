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
              <a-form-model-item label="状态" prop="status">
                <a-select v-model="filters.status" placeholder="请选择" :allowClear="true">
                  <a-select-option v-for="(value, key) in config.status" :key="key" :value="key">{{ value }}</a-select-option>
                </a-select>
              </a-form-model-item>
            </a-col>
            <a-col :md="12" :sm="24">
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
              <a-button type="link" size="small" v-permit="'member:role:'" @click="show(text, record)">查看</a-button>
              <a-button v-permit="'member:role:modify'" type="link" size="small" @click="edit(text, record)">编辑</a-button>
              <a-button type="link" size="small" v-permit="'member:role:resource'" @click="editTree('resource', record.id)">资源</a-button>
              <a-button type="link" size="small" v-permit="'member:role:menu'" @click="editTree('menu', record.id)">菜单</a-button>
            </a-button-group>
          </span>
        </a-table>
        <div class="table-pagination-tools">
          <a-button icon="minus-circle" type="danger" @click="batchRemove" v-permit="'member:role:delete'" :disabled="selection.selectedRows.length === 0">删除</a-button>
          <a-divider type="vertical" v-permit="'member:role:add'" />
          <a-button icon="plus-circle" type="primary" @click="add" v-permit="'member:role:add'">新增</a-button>
        </div>
      </div>
    </a-card>
    <!--展示界面-->
    <a-modal :title="'信息查看 - ' + form.id" v-model="infoVisible" :footer="null">
      <a-form-model :model="form" :loading="infoLoading" :label-col="{ span: 5 }" :wrapper-col="{ span: 12 }">
        <a-form-model-item label="名称">{{ form.name }}</a-form-model-item>
        <a-form-model-item label="排序">{{ form.sort }}</a-form-model-item>
        <a-form-model-item label="状态">{{ form.statusText }}</a-form-model-item>
        <a-form-model-item label="描述">{{ form.description }}</a-form-model-item>
        <a-form-model-item label="创建者">{{ form.createdUidName }}</a-form-model-item>
        <a-form-model-item label="创建时间">{{ form.createdTime|date }}</a-form-model-item>
        <a-form-model-item label="修改者">{{ form.updatedUidName }}</a-form-model-item>
        <a-form-model-item label="修改时间">{{ form.updatedTime|date }}</a-form-model-item>
      </a-form-model>
    </a-modal>
    <!--编辑界面-->
    <a-modal :title="'信息' + (form.id ? ('修改 - ' + form.id) : '添加')" v-model="formVisible" :confirmLoading="formLoading" :maskClosable="false" @ok="submit">
      <a-form-model ref="form" :model="form" :rules="rules" :label-col="{ span: 5 }" :wrapper-col="{ span: 12 }">
        <a-form-model-item label="名称" prop="name">
          <a-input v-model="form.name" auto-complete="off"></a-input>
        </a-form-model-item>
        <a-form-model-item label="排序">
          <a-input-number v-model="form.sort" :min="0" :max="200"></a-input-number>
        </a-form-model-item>
        <a-form-model-item label="状态" prop="status">
          <a-select v-model="form.status" placeholder="请选择">
            <a-select-option v-for="(value, key) in config.status" :key="key" :value="key">{{ value }}</a-select-option>
          </a-select>
        </a-form-model-item>
        <a-form-model-item label="描述">
          <a-textarea v-model="form.description"></a-textarea>
        </a-form-model-item>
      </a-form-model>
    </a-modal>
    <!--资源菜单-->
    <a-modal
      :title="tree.title + '[' + tree.id + ']'"
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
import roleService from '@/service/member/role'

export default {
  data () {
    return {
      filters: {},
      columns: [
        { title: 'ID', dataIndex: 'id' },
        { title: '名称', dataIndex: 'name' },
        { title: '排序', dataIndex: 'sort' },
        { title: '状态', dataIndex: 'statusText' },
        { title: '操作', scopedSlots: { customRender: 'action' } }
      ],
      selection: RouteUtil.selection(),
      pagination: {},
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
        name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
        status: [{ required: true, message: '请选择状态', trigger: 'change' }]
      },
      tree: {
        id: '',
        type: '',
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
        roleService.delete(this.selection.selectedRowKeys, { success: true }).then((result) => {
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
      roleService.list(this.filters).then((result) => {
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
        roleService.save(this.form).then(result => {
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
    },
    show (text, record) {
      this.form = Object.assign({}, record, {
        description: record.description ? record.description : '暂无'
      })
      this.infoVisible = true
    },
    editTree (type, id) {
      Object.assign(this.tree, {
        id: id,
        type: type,
        title: { resource: '资源分配', menu: '菜单分配' }[type],
        rows: [],
        visible: true,
        loading: true
      })
      this.tree.selection.clear()
      roleService.tree({ id: id, type: type }).then((result) => {
        if (result.code === 0) {
          Object.assign(this.tree, {
            loading: false,
            rows: result.data.tree,
            expandedRowKeys: RouteUtil.expandedRowKeys(result.data.tree)
          })
          this.tree.selection.selectedRowKeys = result.data.checked
        }
      })
    },
    saveTree () {
      if (this.tree.loading) return false
      this.tree.loading = true
      roleService.tree({
        id: this.tree.id,
        type: this.tree.type,
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
    this.filters = RouteUtil.query2filter(this, { page: 1, pageSize: 5 })
    this.pagination = Object.assign({}, RouteUtil.pagination(this.filters), this.pagination)
  },
  mounted () {
    this.search(false, true)
    roleService.config().then((result) => {
      this.config.ready = true
      if (result.code === 0) {
        Object.assign(this.config, result.data)
      }
    })
  }
}
</script>
