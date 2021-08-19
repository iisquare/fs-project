<template>
  <section>
    <a-card :bordered="false">
      <template slot="title">
        <a-button @click="toggleRowKeys" :icon="expandedRowKeys.length === 0 ? 'menu-unfold' : 'menu-fold'"></a-button>
        <a-divider type="vertical" v-permit="'cms:catalog:delete'" />
        <a-button icon="minus-circle" type="danger" @click="batchRemove" v-permit="'cms:catalog:delete'" :disabled="selection.selectedRows.length === 0">删除</a-button>
        <a-divider type="vertical" v-permit="'cms:catalog:add'" />
        <a-button icon="plus-circle" type="primary" @click="add(0)" v-permit="'cms:catalog:add'">新增</a-button>
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
          <span slot="action" slot-scope="text, record">
            <a-button v-permit="'cms:catalog:'" type="link" size="small" @click="show(text, record)">查看</a-button>
            <a-button v-permit="'cms:catalog:modify'" type="link" size="small" @click="edit(text, record)">编辑</a-button>
            <a-button v-permit="'cms:catalog:add'" type="link" size="small" @click="sublevel(text, record)">子级</a-button>
          </span>
        </a-table>
      </div>
    </a-card>
    <!--展示界面-->
    <a-modal :title="'信息查看 - ' + form.id" v-model="infoVisible" :footer="null">
      <a-form-model :model="form" :loading="infoLoading" :label-col="{ span: 5 }" :wrapper-col="{ span: 18 }">
        <a-form-model-item label="父级">{{ form.parentId }}</a-form-model-item>
        <a-form-model-item label="名称">{{ form.name }}</a-form-model-item>
        <a-form-model-item label="封面">{{ form.cover }}</a-form-model-item>
        <a-form-model-item label="标题">{{ form.title }}</a-form-model-item>
        <a-form-model-item label="关键词">{{ form.keyword }}</a-form-model-item>
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
        <a-form-model-item label="封面" prop="cover">
          <fs-input-image v-model="form.cover" bucket="cms" />
        </a-form-model-item>
        <a-form-model-item label="标题" prop="title">
          <a-input v-model="form.title" auto-complete="on" placeholder="默认为栏目名称"></a-input>
        </a-form-model-item>
        <a-form-model-item label="关键词" prop="keyword">
          <a-input v-model="form.keyword" auto-complete="on"></a-input>
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
import catalogService from '@/service/cms/catalog'

export default {
  components: { FsInputImage: () => import('@/components/Upload/InputImage.vue') },
  data () {
    return {
      columns: [
        { title: '名称', dataIndex: 'name' },
        { title: '标识', dataIndex: 'id' },
        { title: '父级', dataIndex: 'parentId' },
        { title: '标题', dataIndex: 'title' },
        { title: '排序', dataIndex: 'sort' },
        { title: '状态', dataIndex: 'statusText' },
        { title: '操作', scopedSlots: { customRender: 'action' }, width: 180 }
      ],
      expandedRowKeys: [],
      selection: RouteUtil.selection({ type: 'radio' }),
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
        catalogService.delete(this.selection.selectedRowKeys[0], { success: true }).then((result) => {
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
      catalogService.tree().then((result) => {
        if (result.code === 0) {
          this.rows = result.data
          this.expandedRowKeys = RouteUtil.expandedRowKeys(result.data, 1)
        }
        this.loading = false
      })
    },
    submit () {
      this.$refs.form.validate(valid => {
        if (!valid || this.formLoading) return false
        this.formLoading = true
        catalogService.save(this.form).then(result => {
          if (result.code === 0) {
            this.formVisible = false
            this.search()
          }
          this.formLoading = false
        })
      })
    },
    sublevel (text, record) {
      this.form = { parentId: record.id }
      this.formVisible = true
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
    catalogService.config().then((result) => {
      this.config.ready = true
      if (result.code === 0) {
        Object.assign(this.config, result.data)
      }
    })
  }
}
</script>
<style lang="less" scoped>
</style>
