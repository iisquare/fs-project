<template>
  <section>
    <a-card :bordered="false">
      <template slot="title">
        <a-button @click="toggleRowKeys" :icon="expandedRowKeys.length === 0 ? 'menu-unfold' : 'menu-fold'"></a-button>
        <a-divider type="vertical" v-permit="'member:dictionary:delete'" />
        <a-button icon="minus-circle" type="danger" @click="batchRemove" v-permit="'member:dictionary:delete'" :disabled="selection.selectedRows.length === 0">删除</a-button>
        <a-divider type="vertical" v-permit="'member:dictionary:add'" />
        <a-button icon="plus-circle" type="primary" @click="add(0)" v-permit="'member:dictionary:add'">新增</a-button>
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
          <span slot="url" slot-scope="text, record">
            <a-icon :type="record.icon" v-if="record.icon" />
            <a :href="record.url" :title="record.url" target="_blank">{{ record.target ? record.target : (record.url ? '_self' : '无链接') }}</a>
          </span>
          <span slot="action" slot-scope="text, record">
            <a-dropdown>
              <a-menu slot="overlay">
                <a-menu-item v-permit="'member:dictionary:'"><a-button :block="true" type="link" size="small" @click="show(text, record)">查看</a-button></a-menu-item>
                <a-menu-item v-permit="'member:dictionary:modify'"><a-button :block="true" type="link" size="small" @click="edit(text, record)">编辑</a-button></a-menu-item>
                <a-menu-item v-permit="'member:dictionary:add'"><a-button :block="true" type="link" size="small" @click="sublevel(text, record)">子级</a-button></a-menu-item>
              </a-menu>
              <a-button type="link" icon="tool"></a-button>
            </a-dropdown>
          </span>
        </a-table>
      </div>
    </a-card>
    <!--展示界面-->
    <a-modal :title="'信息查看 - ' + form.id" v-model="infoVisible" :footer="null">
      <a-form-model :model="form" :loading="infoLoading" :label-col="{ span: 5 }" :wrapper-col="{ span: 18 }">
        <a-form-model-item label="父级">[{{ form.parentId }}]{{ form.parentId > 0 ? form.parentIdName : '根节点' }}</a-form-model-item>
        <a-form-model-item label="名称">{{ form.name }}</a-form-model-item>
        <a-form-model-item label="全称">{{ form.fullName }}</a-form-model-item>
        <a-form-model-item label="内容">{{ form.content }}</a-form-model-item>
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
        <a-form-model-item label="内容" prop="content">
          <a-input v-model="form.content" auto-complete="on"></a-input>
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
          <a-textarea v-model="form.description" />
        </a-form-model-item>
      </a-form-model>
    </a-modal>
  </section>
</template>

<script>
import RouteUtil from '@/utils/route'
import dictionaryService from '@/service/member/dictionary'

export default {
  data () {
    return {
      columns: [
        { title: '名称', dataIndex: 'name', scopedSlots: { customRender: 'name' } },
        { title: '全称', dataIndex: 'fullName' },
        { title: '内容', dataIndex: 'content' },
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
        dictionaryService.delete(this.selection.selectedRowKeys, { success: true }).then((result) => {
          if (result.code === 0) {
            this.search(false, true)
          } else {
            this.loading = false
          }
        })
      }))
    },
    search () {
      this.selection.clear()
      this.loading = true
      dictionaryService.tree().then((result) => {
        if (result.code === 0) {
          this.rows = result.data
          if (this.expandedRowKeys.length === 0) {
            this.expandedRowKeys = RouteUtil.expandedRowKeys(result.data)
          }
        }
        this.loading = false
      })
    },
    submit () {
      this.$refs.form.validate(valid => {
        if (!valid || this.formLoading) return false
        this.formLoading = true
        dictionaryService.save(this.form).then(result => {
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
        status: record.status + ''
      })
      this.formVisible = true
    },
    show (text, record) {
      this.form = Object.assign({}, record, {
        description: record.description ? record.description : '暂无'
      })
      this.infoVisible = true
    }
  },
  mounted () {
    this.search()
    dictionaryService.config().then((result) => {
      this.config.ready = true
      if (result.code === 0) {
        Object.assign(this.config, result.data)
      }
    })
  }
}
</script>
