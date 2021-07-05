<template>
  <section>
    <a-alert :message="tips.synonym" type="info" show-icon :closable="true" class="alert-tip" />
    <a-card :bordered="false">
      <div class="table-page-search-wrapper">
        <a-form-model ref="filters" :model="filters" layout="inline">
          <a-row :gutter="24">
            <a-col :md="6" :sm="24">
              <a-form-model-item label="目录" prop="catalogue">
                <a-input v-model="filters.catalogue" placeholder="" :allowClear="true" />
              </a-form-model-item>
            </a-col>
            <a-col :md="6" :sm="24">
              <a-form-model-item label="类型" prop="type">
                <a-select v-model="filters.type" placeholder="请选择" :allowClear="true">
                  <a-select-option v-for="(value, key) in config.type" :key="key" :value="key">{{ value }}</a-select-option>
                </a-select>
              </a-form-model-item>
            </a-col>
            <a-col :md="6" :sm="24">
              <a-form-model-item label="来源" prop="source">
                <a-input v-model="filters.source" placeholder="英文逗号分割" :allowClear="true" />
              </a-form-model-item>
            </a-col>
            <a-col :md="6" :sm="24">
              <a-space>
                <a-button type="primary" @click="search(true, false)" :loading="loading">查询</a-button>
                <a-button @click="() => this.$refs.filters.resetFields()">重置</a-button>
                <a-button @click="download">下载</a-button>
              </a-space>
            </a-col>
          </a-row>
          <a-row :gutter="24">
            <a-col :md="12" :sm="24">
              <a-form-model-item label="词条" prop="content">
                <a-input v-model="filters.content" placeholder="" :allowClear="true" />
              </a-form-model-item>
            </a-col>
            <a-col :md="6" :sm="24">
              <a-form-model-item label="排序" prop="sort">
                <a-select v-model="filters.sort" placeholder="请选择" :allowClear="true">
                  <a-select-option v-for="(value, key) in config.sort" :key="key" :value="key">{{ value }}</a-select-option>
                </a-select>
              </a-form-model-item>
            </a-col>
            <a-col :md="6" :sm="24">
              <a-space>
                <a-button type="danger" @click="unique" :loading="loading" v-permit="'lucene:dictionary:delete'">去重</a-button>
                <a-upload
                  v-permit="'lucene:dictionary:add'"
                  :action="upload.action"
                  accept=".scel"
                  :withCredentials="true"
                  :showUploadList="false"
                  @change="uploadChange">
                  <a-button icon="cloud-upload" :loading="upload.loading">细胞词库</a-button>
                </a-upload>
              </a-space>
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
          <span slot="url" slot-scope="text, record">
            <a-icon v-if="record.icon" :type="record.icon" />
            <a :href="record.url" :title="record.url" target="_blank">{{ record.target ? record.target : (record.url ? '_self' : '无链接') }}</a>
          </span>
          <span slot="action" slot-scope="text, record">
            <a-button-group>
              <a-button type="link" size="small" v-permit="'lucene:dictionary:'" @click="show(text, record)">查看</a-button>
              <a-button v-permit="'lucene:dictionary:modify'" type="link" size="small" @click="edit(text, record)">编辑</a-button>
            </a-button-group>
          </span>
        </a-table>
        <div :class="rows.length > 0 ? 'table-pagination-tools' : 'table-pagination-tools-empty'">
          <a-button icon="minus-circle" type="danger" @click="batchRemove" v-permit="'lucene:dictionary:delete'" :disabled="selection.selectedRows.length === 0">删除</a-button>
          <a-divider type="vertical" v-permit="'lucene:dictionary:add'" />
          <a-button icon="plus-circle" type="primary" @click="add(0)" v-permit="'lucene:dictionary:add'">新增</a-button>
        </div>
      </div>
    </a-card>
    <!--展示界面-->
    <a-modal :title="'信息查看 - ' + form.id" v-model="infoVisible" :footer="null">
      <a-form-model :model="form" :loading="infoLoading" :label-col="{ span: 5 }" :wrapper-col="{ span: 18 }">
        <a-form-model-item label="目录">{{ form.catalogue }}</a-form-model-item>
        <a-form-model-item label="类型">{{ form.typeText }}</a-form-model-item>
        <a-form-model-item label="来源">{{ form.source }}</a-form-model-item>
        <a-form-model-item label="词条">{{ form.content }}</a-form-model-item>
        <a-form-model-item label="创建者">{{ form.createdUidName }}</a-form-model-item>
        <a-form-model-item label="创建时间">{{ form.createdTime|date }}</a-form-model-item>
        <a-form-model-item label="修改者">{{ form.updatedUidName }}</a-form-model-item>
        <a-form-model-item label="修改时间">{{ form.updatedTime|date }}</a-form-model-item>
      </a-form-model>
    </a-modal>
    <!--编辑界面-->
    <a-modal :title="'信息' + (form.id ? ('修改 - ' + form.id) : '添加')" v-model="formVisible" :confirmLoading="formLoading" :maskClosable="false" @ok="submit">
      <a-form-model ref="form" :model="form" :rules="rules" :label-col="{ span: 5 }" :wrapper-col="{ span: 18 }">
        <a-form-model-item label="ID" prop="id">
          <a-input v-model="form.id" auto-complete="on"></a-input>
        </a-form-model-item>
        <a-form-model-item label="目录" prop="catalogue">
          <a-input v-model="form.catalogue" auto-complete="on"></a-input>
        </a-form-model-item>
        <a-form-model-item label="类型" prop="type">
          <a-select v-model="form.type" placeholder="请选择">
            <a-select-option v-for="(value, key) in config.type" :key="key" :value="key">{{ value }}</a-select-option>
          </a-select>
        </a-form-model-item>
        <a-form-model-item label="来源" prop="source">
          <a-input v-model="form.source" auto-complete="on"></a-input>
        </a-form-model-item>
        <a-form-model-item label="词条">
          <a-textarea v-model="form.content" placeholder="批量添加采用换行符分割" />
        </a-form-model-item>
      </a-form-model>
    </a-modal>
  </section>
</template>

<script>
import RouteUtil from '@/utils/route'
import dictionaryService from '@/service/lucene/dictionary'

export default {
  data () {
    return {
      tips: {
        synonym: '提示：基础词条为单个字或连续的词；同义词格式为“词一,词二,词三=>词一,词二,词三”，其中每个词项必须是分词词库中已存在的词条。'
      },
      filters: {},
      columns: [
        { title: 'ID', dataIndex: 'id' },
        { title: '目录', dataIndex: 'catalogue' },
        { title: '类型', dataIndex: 'typeText' },
        { title: '来源', dataIndex: 'source' },
        { title: '词条', dataIndex: 'content' },
        { title: '操作', scopedSlots: { customRender: 'action' } }
      ],
      selection: RouteUtil.selection(),
      pagination: {},
      rows: [],
      loading: false,
      config: {
        ready: false,
        type: [],
        sort: []
      },
      infoVisible: false,
      infoLoading: false,
      formVisible: false,
      formLoading: false,
      form: {},
      rules: {
        catalogue: [{ required: true, message: '请输入词库目录', trigger: 'blur' }],
        type: [{ required: true, message: '请选择词库类型', trigger: 'change' }],
        source: [{ required: true, message: '请输入词条来源', trigger: 'blur' }],
        content: [{ required: true, message: '请输入词条内容', trigger: 'blur' }]
      },
      upload: {
        action: process.env.VUE_APP_API_BASE_URL + '/proxy/upload?app=lucene&uri=/dictionary/scel',
        loading: false
      }
    }
  },
  methods: {
    download () {
      const param = Object.keys(this.filters).map(key => key + '=' + encodeURIComponent(this.filters[key]))
      const url = process.env.VUE_APP_API_BASE_URL + '/lucene/plain?' + param.join('&')
      window.open(url)
    },
    uploadChange ({ file, fileList, event }) {
      switch (file.status) {
        case 'uploading':
          this.upload.loading = true
          break
        case 'done':
          this.upload.loading = false
          const result = file.response
          if (result.code === 0) {
            this.$notification.success({ message: '状态：' + result.code, description: '消息:' + result.message })
            this.form = Object.assign({}, this.filters, {
              source: file.name,
              content: Object.keys(result.data).join('\n')
            })
            this.formVisible = true
          } else {
            this.$notification.warning({ message: '状态：' + result.code, description: '消息:' + result.message })
          }
          break
        case 'error':
          this.upload.loading = false
          this.$notification.error({ message: '请求异常', description: `${file.name} file upload failed.` })
          break
      }
    },
    unique () {
      this.$confirm({
        title: '操作提示',
        content: '确认清理[词库目录=' + this.filters.catalogue + ',词库类型=' + this.filters.type + ']下的重复词条吗？',
        onOk: () => {
          this.loading = true
          dictionaryService.unique(this.filters).then((result) => {
            if (result.code === 0) {
              this.$message.success('清理重复记录' + result.data + '条')
              this.search(false, true)
            } else {
              this.loading = false
            }
          })
        }
      })
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
    tableChange (pagination, filters, sorter) {
      this.pagination = RouteUtil.paginationChange(this.pagination, pagination)
      this.search(true, true)
    },
    search (filter2query, pagination) {
      this.selection.clear()
      Object.assign(this.filters, RouteUtil.paginationData(this.pagination, pagination))
      filter2query && RouteUtil.filter2query(this, this.filters)
      this.loading = true
      dictionaryService.list(this.filters).then((result) => {
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
        dictionaryService.save(this.form).then(result => {
          if (result.code === 0) {
            this.formVisible = false
            this.search(false, true)
          }
          this.formLoading = false
        })
      })
    },
    add () {
      this.form = Object.assign({}, this.filters, { source: '' })
      this.formVisible = true
    },
    edit (text, record) {
      this.form = Object.assign({}, record)
      this.formVisible = true
    },
    show (text, record) {
      this.form = Object.assign({}, record)
      this.infoVisible = true
    }
  },
  created () {
    this.filters = RouteUtil.query2filter(this, { page: 1, pageSize: 5 })
    this.pagination = Object.assign({}, RouteUtil.pagination(this.filters), this.pagination)
  },
  mounted () {
    this.search(false, true)
    dictionaryService.config().then((result) => {
      this.config.ready = true
      if (result.code === 0) {
        Object.assign(this.config, result.data)
      }
    })
  }
}
</script>

<style lang="less" scoped>
.alert-tip {
  margin-bottom: 25px;
}
</style>
