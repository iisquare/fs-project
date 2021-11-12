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
              <a-form-model-item label="类型" prop="type">
                <a-select v-model="filters.type" placeholder="请选择" :allowClear="true">
                  <a-select-option v-for="(value, key) in config.types" :key="key" :value="key">{{ value }}</a-select-option>
                </a-select>
              </a-form-model-item>
            </a-col>
            <a-col :md="6" :sm="24">
              <span class="table-page-search-submitButtons" :style="advanced && { float: 'right', oversource: 'hidden' } || {} ">
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
          :rowSelection="selection"
          @change="tableChange"
          :bordered="true"
        >
          <span slot="action" slot-scope="text, record">
            <a-button-group>
              <a-button v-permit="'bi:source:modify'" type="link" size="small" @click="edit(text, record)">编辑</a-button>
            </a-button-group>
          </span>
        </a-table>
        <div :class="rows.length > 0 ? 'table-pagination-tools' : 'table-pagination-tools-empty'">
          <a-button icon="minus-circle" type="danger" @click="batchRemove" v-permit="'bi:source:delete'" :disabled="selection.selectedRows.length === 0">删除</a-button>
          <a-divider type="vertical" v-permit="'bi:source:add'" />
          <a-button icon="plus-circle" type="primary" @click="choice" v-permit="'bi:source:add'">新增</a-button>
        </div>
      </div>
    </a-card>
    <!--选择界面-->
    <a-modal title="数据源类型" v-model="choiceVisible" :footer="null" :bodyStyle="{ padding: '0px' }">
      <a-tabs default-active-key="server" :animated="false" tabPosition="left">
        <a-tab-pane :tab="catalog.name" :key="catalog.key" v-for="catalog in designConfig.widgets">
          <ul class="fs-ui-source">
            <li v-for="widget in catalog.children" :key="widget.type" @click="add(widget)">{{ widget.label }}</li>
          </ul>
        </a-tab-pane>
      </a-tabs>
    </a-modal>
    <!--编辑界面-->
    <a-modal
      :title="form.type + '数据源'"
      v-model="formVisible"
      :confirmLoading="formLoading"
      :maskClosable="false"
      @ok="submit"
      :destroyOnClose="true"
      :bodyStyle="{ padding: '0px' }">
      <a-form-model
        ref="form"
        :model="form"
        :rules="rules"
        labelAlign="left"
        :label-col="{ span: 5 }"
        :wrapper-col="{ span: 18 }">
        <property v-model="options" :config="designConfig" :form="form">
          <a-form-model-item></a-form-model-item>
          <a-form-model-item label="ID" prop="id">
            <a-input v-model="form.id" auto-complete="off"></a-input>
          </a-form-model-item>
          <a-form-model-item label="名称" prop="name">
            <a-input v-model="form.name" auto-complete="off"></a-input>
          </a-form-model-item>
          <a-form-model-item label="类型" prop="type">{{ form.type }}</a-form-model-item>
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
        </property>
      </a-form-model>
    </a-modal>
  </section>
</template>

<script>
import RouteUtil from '@/utils/route'
import designConfig from './design/config'
import Property from './design/Property'
import sourceService from '@/service/bi/source'

export default {
  components: { Property },
  data () {
    return {
      designConfig,
      advanced: false,
      filters: {},
      columns: [
        { title: 'ID', dataIndex: 'id' },
        { title: '名称', dataIndex: 'name' },
        { title: '类型', dataIndex: 'typeText' },
        { title: '排序', dataIndex: 'sort' },
        { title: '状态', dataIndex: 'statusText' },
        { title: '操作', scopedSlots: { customRender: 'action' }, width: 80 }
      ],
      selection: RouteUtil.selection(),
      pagination: {},
      rows: [],
      loading: false,
      config: {
        ready: false,
        status: [],
        types: []
      },
      choiceVisible: false,
      infoVisible: false,
      infoLoading: false,
      formVisible: false,
      formLoading: false,
      form: {},
      options: {},
      rules: {}
    }
  },
  methods: {
    batchRemove () {
      this.$confirm(this.selection.confirm(() => {
        this.loading = true
        sourceService.delete(this.selection.selectedRowKeys, { success: true }).then((result) => {
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
      sourceService.list(this.filters).then((result) => {
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
        sourceService.save(Object.assign({}, this.form, { content: JSON.stringify(this.options) })).then(result => {
          if (result.code === 0) {
            this.formVisible = false
            this.options = {}
            this.search(false, true)
          }
          this.formLoading = false
        })
      })
    },
    choice () {
      this.choiceVisible = true
    },
    add (widget) {
      this.choiceVisible = false
      this.form = { type: widget.type }
      this.options = {}
      this.formVisible = true
    },
    edit (text, record) {
      this.form = Object.assign({}, record, {
        status: record.status + ''
      })
      this.options = {}
      try {
        if (this.form.content) {
          this.options = JSON.parse(this.form.content)
        }
      } catch (e) {
        this.$error({ title: '数据解析异常', content: e.message })
      }
      this.formVisible = true
    }
  },
  created () {
    this.filters = RouteUtil.query2filter(this, { page: 1, pageSize: 5 })
    this.pagination = Object.assign({}, RouteUtil.pagination(this.filters), this.pagination)
  },
  mounted () {
    this.search(false, true)
    sourceService.config().then((result) => {
      this.config.ready = true
      if (result.code === 0) {
        Object.assign(this.config, result.data)
      }
    })
  }
}
</script>
<style lang="less" scoped>
.fs-ui-source {
  padding: 0;
  margin: 0;
  li {
    font-size: 12px;
    display: block;
    width: 28%;
    line-height: 26px;
    position: relative;
    float: left;
    left: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    margin: 1%;
    padding: 3px 10px;
    cursor: pointer;
    &:hover {
      background: #f4f6fc;
    }
  }
}
</style>
