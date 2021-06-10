<template>
  <section>
    <a-card :bordered="false">
      <template slot="title">
        <a-button @click="toggleRowKeys" :icon="expandedRowKeys.length === 0 ? 'menu-unfold' : 'menu-fold'"></a-button>
        <a-divider type="vertical" v-permit="'dag:node:delete'" />
        <a-button icon="minus-circle" type="danger" @click="batchRemove" v-permit="'dag:node:delete'" :disabled="selection.selectedRows.length === 0">删除</a-button>
        <a-divider type="vertical" v-permit="'dag:node:add'" />
        <a-button icon="plus-circle" type="primary" @click="add(0)" v-permit="'dag:node:add'">新增</a-button>
      </template>
      <div class="table-page-search-wrapper">
        <a-table
          :columns="columns"
          :rowKey="record => record.id"
          :dataSource="rows"
          :pagination="false"
          :loading="loading"
          :rowSelection="selection"
          :bordered="true"
          :expandedRowKeys="expandedRowKeys"
          @expandedRowsChange="(expandedRows) => this.expandedRowKeys = expandedRows"
          :scroll="{ x: true }"
        >
          <span slot="name" slot-scope="text, record">{{ record.name }}/{{ record.parentId }}/{{ record.id }}</span>
          <span slot="permit" slot-scope="text, record">{{ record.module }}/{{ record.controller }}/{{ record.action }}</span>
          <span slot="action" slot-scope="text, record">
            <a-dropdown>
              <a-menu slot="overlay">
                <a-menu-item v-permit="'dag:node:'"><a-button :block="true" type="link" size="small" @click="show(text, record)">查看</a-button></a-menu-item>
                <a-menu-item v-permit="'dag:node:modify'"><a-button :block="true" type="link" size="small" @click="edit(text, record)">编辑</a-button></a-menu-item>
                <a-menu-item v-permit="'dag:node:add'"><a-button :block="true" type="link" size="small" @click="sublevel(text, record)">子级</a-button></a-menu-item>
              </a-menu>
              <a-button type="link" icon="tool"></a-button>
            </a-dropdown>
          </span>
        </a-table>
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
import RouteUtil from '@/utils/route'
import nodeService from '@/service/dag/node'

export default {
  data () {
    return {
      columns: [
        { title: '名称', dataIndex: 'name', scopedSlots: { customRender: 'name' } },
        { title: '全称', dataIndex: 'fullName' },
        { title: '分类', dataIndex: 'type' },
        { title: '插件', dataIndex: 'plugin' },
        { title: '排序', dataIndex: 'sort' },
        { title: '状态', dataIndex: 'statusText' },
        { title: '操作', scopedSlots: { customRender: 'action' } }
      ],
      expandedRowKeys: [],
      selection: RouteUtil.selection(),
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
    toggleRowKeys () {
      if (this.expandedRowKeys.length === 0) {
        this.expandedRowKeys = RouteUtil.expandedRowKeys(this.rows)
      } else {
        this.expandedRowKeys = []
      }
    },
    batchRemove () {
      this.$confirm(this.selection.confirm(() => {
        this.loading = true
        nodeService.delete(this.selection.selectedRowKeys, { success: true }).then((result) => {
          if (result.code === 0) {
            this.search()
          } else {
            this.loading = false
          }
        })
      }))
    },
    search () {
      this.selection.clear()
      this.loading = true
      nodeService.tree().then((result) => {
        if (result.code === 0) {
          this.rows = result.data
          this.expandedRowKeys = RouteUtil.expandedRowKeys(result.data)
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
            this.search()
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
  mounted () {
    this.search()
    nodeService.config().then((result) => {
      this.config.ready = true
      if (result.code === 0) {
        Object.assign(this.config, result.data)
      }
    })
  }
}
</script>
