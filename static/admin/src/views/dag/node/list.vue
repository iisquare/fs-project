<template>
  <section>
    <a-card :bordered="false">
      <div class="table-page-search-wrapper">
        <a-form-model ref="filters" :model="filters" layout="inline">
          <a-row :gutter="48">
            <a-col :md="6" :sm="24">
              <a-form-model-item label="父级ID" prop="parentId">
                <a-input v-model="filters.parentId" placeholder="" :allowClear="true" />
              </a-form-model-item>
            </a-col>
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
            <template v-if="advanced">
              <a-col :md="6" :sm="24">
                <a-form-model-item label="类型" prop="type">
                  <a-input v-model="filters.type" placeholder="" :allowClear="true" />
                </a-form-model-item>
              </a-col>
              <a-col :md="6" :sm="24">
                <a-form-model-item label="插件" prop="plugin">
                  <a-input v-model="filters.plugin" placeholder="" :allowClear="true" />
                </a-form-model-item>
              </a-col>
              <a-col :md="12" :sm="24">
                <a-form-model-item label="类名" prop="classname">
                  <a-input v-model="filters.classname" placeholder="" :allowClear="true" />
                </a-form-model-item>
              </a-col>
            </template>
            <a-col :md="6" :sm="24">
              <span class="table-page-search-submitButtons" :style="advanced && { float: 'right', overflow: 'hidden' } || {} ">
                <a-button type="primary" @click="search(true, false)" :loading="loading">查询</a-button>
                <a-button style="margin-left: 8px" @click="() => this.$refs.filters.resetFields()">重置</a-button>
                <a @click="toggleAdvanced" style="margin-left: 8px">
                  {{ advanced ? '收起' : '展开' }}
                  <a-icon :type="advanced ? 'up' : 'down'"/>
                </a>
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
          :rowSelection="selection"
          @change="tableChange"
          :bordered="true"
        >
          <span slot="parentId" slot-scope="text, record">[{{ record.parentId }}]{{ record.parentId > 0 ? record.parentIdName : '根节点' }}</span>
          <span slot="action" slot-scope="text, record">
            <a-button-group>
              <a-button type="link" size="small" v-permit="'dag:node:'" @click="show(text, record)">查看</a-button>
              <a-button v-permit="'dag:node:modify'" type="link" size="small" @click="edit(text, record)">编辑</a-button>
              <a-button v-permit="'dag:node:modify'" type="link" size="small" @click="sublevel(text, record)">子级</a-button>
            </a-button-group>
          </span>
        </a-table>
        <div :class="rows.length > 0 ? 'table-pagination-tools' : 'table-pagination-tools-empty'">
          <a-button icon="minus-circle" type="danger" @click="batchRemove" v-permit="'dag:node:delete'" :disabled="selection.selectedRows.length === 0">删除</a-button>
          <a-divider type="vertical" v-permit="'dag:node:add'" />
          <a-button icon="plus-circle" type="primary" @click="add(0)" v-permit="'dag:node:add'">新增</a-button>
        </div>
      </div>
    </a-card>
    <!--展示界面-->
    <a-modal :title="'信息查看 - ' + form.id" v-model="infoVisible" :footer="null">
      <a-form-model :model="form" :loading="infoLoading" :label-col="{ span: 5 }" :wrapper-col="{ span: 15 }">
        <a-form-model-item label="父级">[{{ form.parentId }}]{{ form.parentId > 0 ? form.parentIdName : '根节点' }}</a-form-model-item>
        <a-form-model-item label="名称">{{ form.name }}</a-form-model-item>
        <a-form-model-item label="全称">{{ form.fullName }}</a-form-model-item>
        <a-form-model-item label="类型">{{ form.type }}</a-form-model-item>
        <a-form-model-item label="插件">{{ form.plugin }}</a-form-model-item>
        <a-form-model-item label="图标"><a-icon v-if="form.icon" :type="form.icon" />{{ form.icon }}</a-form-model-item>
        <a-form-model-item label="展开">{{ form.state }}</a-form-model-item>
        <a-form-model-item label="类名">{{ form.classname }}</a-form-model-item>
        <a-form-model-item label="拖拽">{{ form.draggable ? '是' : '否' }}</a-form-model-item>
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
      <a-form-model ref="form" :model="form" :rules="rules" :label-col="{ span: 5 }" :wrapper-col="{ span: 18 }">
        <a-form-model-item label="父级" prop="parentId">
          <a-input v-model="form.parentId" auto-complete="off"></a-input>
        </a-form-model-item>
        <a-form-model-item label="ID" prop="id">
          <a-input v-model="form.id" auto-complete="off"></a-input>
        </a-form-model-item>
        <a-form-model-item label="名称" prop="name">
          <a-input v-model="form.name" auto-complete="off"></a-input>
        </a-form-model-item>
        <a-form-model-item label="类型" prop="type">
          <a-input v-model="form.type" auto-complete="off"></a-input>
        </a-form-model-item>
        <a-form-model-item label="插件" prop="plugin">
          <a-input v-model="form.plugin" auto-complete="off"></a-input>
        </a-form-model-item>
        <a-form-model-item label="图标" prop="icon">
          <a-input v-model="form.icon" auto-complete="off"></a-input>
        </a-form-model-item>
        <a-form-model-item label="展开状态" prop="state">
          <a-input v-model="form.state" auto-complete="off"></a-input>
        </a-form-model-item>
        <a-form-model-item label="类名" prop="classname">
          <a-input v-model="form.classname" auto-complete="off"></a-input>
        </a-form-model-item>
        <a-form-model-item label="拖拽" prop="draggable">
          <a-checkbox v-model="form.draggable">允许拖拽</a-checkbox>
        </a-form-model-item>
        <a-form-model-item label="属性" prop="property">
          <a-textarea v-model="form.property"></a-textarea>
        </a-form-model-item>
        <a-form-model-item label="返回值" prop="returns">
          <a-textarea v-model="form.returns"></a-textarea>
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
  </section>
</template>

<script>
import DateUtil from '@/utils/date'
import RouteUtil from '@/utils/route'
import nodeService from '@/service/dag/node'

export default {
  data () {
    return {
      advanced: false,
      filters: {},
      columns: [
        { title: 'ID', dataIndex: 'id' },
        { title: '名称', dataIndex: 'name' },
        { title: '全称', dataIndex: 'fullName' },
        { title: '父级', dataIndex: 'parentId', scopedSlots: { customRender: 'parentId' } },
        { title: '分类', dataIndex: 'type' },
        { title: '插件', dataIndex: 'plugin' },
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
      }
    }
  },
  methods: {
    dateRender (text, record, index) {
      return DateUtil.format(text)
    },
    batchRemove () {
      this.$confirm(this.selection.confirm(() => {
        this.loading = true
        nodeService.delete(this.selection.selectedRowKeys, { success: true }).then((result) => {
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
    toggleAdvanced () {
      this.advanced = !this.advanced
    },
    search (filter2query, pagination) {
      this.selection.clear()
      Object.assign(this.filters, RouteUtil.paginationData(this.pagination, pagination))
      filter2query && RouteUtil.filter2query(this, this.filters)
      this.loading = true
      nodeService.list(this.filters).then((result) => {
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
        nodeService.save(this.form).then(result => {
          if (result.code === 0) {
            this.formVisible = false
            this.search(false, true)
          }
          this.formLoading = false
        })
      })
    },
    sublevel (text, record) {
      this.add(record.id)
    },
    add (parentId = 0) {
      this.form = { parentId }
      this.formVisible = true
    },
    edit (text, record) {
      this.form = Object.assign({}, record, {
        status: record.status + '',
        draggable: record.draggable === 1
      })
      this.formVisible = true
    },
    show (text, record) {
      this.form = Object.assign({}, record, {
        draggable: record.draggable === 1,
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
    nodeService.config().then((result) => {
      this.config.ready = true
      if (result.code === 0) {
        Object.assign(this.config, result.data)
      }
    })
  }
}
</script>
