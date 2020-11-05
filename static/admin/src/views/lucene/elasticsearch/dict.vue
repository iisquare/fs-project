<template>
  <section>
    <a-alert :message="tips.synonym" type="info" :closable="true" class="alert-tip" />
    <a-card :bordered="false">
      <div class="table-page-search-wrapper">
        <a-form-model ref="filters" :model="filters" layout="inline">
          <a-row :gutter="48">
            <a-col :md="6" :sm="24">
              <a-form-model-item label="类型" prop="type">
                <a-select v-model="filters.type">
                  <a-select-option v-for="(value, key) in types" :key="key" :value="key">{{ value }}</a-select-option>
                </a-select>
              </a-form-model-item>
            </a-col>
            <a-col :md="6" :sm="24">
              <a-form-model-item label="编号">
                <a-input v-model="filters.dictSerial" placeholder="词典编号" :allowClear="true" />
              </a-form-model-item>
            </a-col>
            <a-col :md="6" :sm="24">
              <a-form-model-item label="标识">
                <a-input v-model="filters.identity" placeholder="标识" :allowClear="true" />
              </a-form-model-item>
            </a-col>
            <a-col :md="6" :sm="24">
              <a-form-model-item label="词条">
                <a-input v-model="filters.text" placeholder="词条" :allowClear="true" />
              </a-form-model-item>
            </a-col>
          </a-row>
          <a-row :gutter="48">
            <a-col :md="18" :sm="24">
              <a-form-model-item label="地址">
                <a-input v-model="elasticsearchURL" placeholder="节点链接地址" disabled />
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
            <a-button type="link" size="small" @click="edit(text, record)">编辑</a-button>
          </span>
        </a-table>
        <div :class="rows.length > 0 ? 'table-pagination-tools' : 'table-pagination-tools-empty'">
          <a-button icon="minus-circle" type="danger" @click="batchRemove" :disabled="selection.selectedRows.length === 0">删除</a-button>
          <a-divider type="vertical" />
          <a-button icon="plus-circle" type="primary" @click="add">新增</a-button>
        </div>
      </div>
    </a-card>
    <!--编辑界面-->
    <a-modal :title="'信息' + (form.id ? ('修改 - ' + form.id) : '添加')" v-model="formVisible" :confirmLoading="formLoading" :maskClosable="false" @ok="submit">
      <a-form-model ref="form" :model="form" :rules="rules" :label-col="{ span: 5 }" :wrapper-col="{ span: 12 }">
        <a-form-model-item label="ID" prop="id">
          <a-input v-model="form.id" auto-complete="off" placeholder="自动生成" disabled></a-input>
        </a-form-model-item>
        <a-form-model-item label="词条" prop="text">
          <a-input v-model="form.text" auto-complete="off"></a-input>
        </a-form-model-item>
        <a-form-model-item label="标识" prop="identity">
          <a-input v-model="form.identity" auto-complete="off"></a-input>
        </a-form-model-item>
      </a-form-model>
    </a-modal>
  </section>
</template>

<script>
import DateUtil from '@/utils/date'
import RouteUtil from '@/utils/route'
import elasticsearchService from '@/service/lucene/elasticsearch'

export default {
  data () {
    return {
      tips: {
        synonym: '提示：基础词条为单个字或连续的词；同义词格式为“词一,词二,词三=>词一,词二,词三”，其中每个词项必须是分词词库中已存在的词条。'
      },
      filters: {},
      elasticsearchURL: '',
      types: {
        word: '分词词典',
        synonym: '同义词词典',
        quantifier: '量词词典',
        stopword: '停用词词典'
      },
      columns: [
        { title: 'ID', dataIndex: 'id' },
        { title: '词条', dataIndex: 'text' },
        { title: '标识', dataIndex: 'identity' },
        { title: '创建时间', dataIndex: 'time_create', customRender: this.dateRender },
        { title: '修改时间', dataIndex: 'time_update', customRender: this.dateRender },
        { title: '操作', width: 80, scopedSlots: { customRender: 'action' } }
      ],
      selection: RouteUtil.selection(),
      pagination: {},
      rows: [],
      loading: false,
      formVisible: false,
      formLoading: false,
      form: {},
      rules: {
        text: [{ required: true, message: '请输入词条内容', trigger: 'blur' }]
      }
    }
  },
  methods: {
    dateRender (text, record, index) {
      return DateUtil.format(text * 1000)
    },
    batchRemove () {
      this.$confirm(this.selection.confirm(() => {
        this.loading = true
        const param = {
          type: this.filters.type,
          dictSerial: this.filters.dictSerial,
          ids: this.selection.selectedRowKeys
        }
        elasticsearchService.dictDelete(param, { success: true }).then((result) => {
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
      elasticsearchService.dictList(this.filters).then(result => {
        this.pagination = Object.assign({}, this.pagination, RouteUtil.result(result))
        if (result.code === 0) {
          this.rows = result.data.rows.map(item => {
            return Object.assign({}, item, { id: item._id })
          })
        }
        this.loading = false
      })
    },
    submit () {
      this.$refs.form.validate(valid => {
        if (!valid || this.formLoading) return false
        this.formLoading = true
        Object.assign(this.form, {
          type: this.filters.type,
          dictSerial: this.filters.dictSerial
        })
        elasticsearchService.dictSave(this.form).then(result => {
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
      this.form = Object.assign({}, record)
      this.formVisible = true
    }
  },
  created () {
    this.elasticsearchURL = elasticsearchService.baseURL()
    this.filters = RouteUtil.query2filter(this, {
      state: false,
      page: 1,
      pageSize: 5,
      type: 'word',
      dictSerial: ''
    })
    this.pagination = Object.assign({}, RouteUtil.pagination(this.filters), this.pagination)
  },
  mounted () {
    if (this.filters.state) {
      this.search(false, true)
    }
  }
}
</script>

<style lang="less" scoped>
.alert-tip {
  margin-bottom: 25px;
}
</style>
