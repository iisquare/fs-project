<template>
  <section>
    <a-card :bordered="false">
      <div class="table-page-search-wrapper">
        <a-form-model ref="filters" :model="filters" layout="inline">
          <a-form-model-item :label="frame.name">
            <a-space>
              <a-popover placement="bottomLeft" trigger="click">
                <section slot="content"><list-filter v-model="filters.condition" :fields="fields.filter" :config="config" /></section>
                <a-button icon="filter">筛选条件</a-button>
              </a-popover>
              <a-popover placement="bottomLeft" trigger="click">
                <section slot="content"><list-sorter v-model="filters.sort" :fields="fields.sorter" :config="config" /></section>
                <a-button icon="sort-descending">排序字段</a-button>
              </a-popover>
              <a-popover placement="bottomLeft" trigger="click">
                <section slot="content"><list-viewer v-model="filters.column" :fields="fields.viewer" :config="config" /></section>
                <a-button icon="eye">展示字段</a-button>
              </a-popover>
              <a-button icon="search" type="primary" @click="search(true, false)" :loading="loading">查询</a-button>
            </a-space>
          </a-form-model-item>
        </a-form-model>
        <a-table
          :columns="columns"
          :rowKey="record => record._id"
          :dataSource="rows"
          :pagination="pagination"
          :loading="loading"
          :rowSelection="selection"
          @change="tableChange"
          :bordered="true"
        >
          <span slot="action" slot-scope="text, record, index">
            <a-button-group>
              <a-button type="link" size="small" v-permit="'oa:formData:'" @click="show(text, record, index)">查看</a-button>
              <a-button v-permit="'oa:formData:modify'" type="link" size="small" @click="edit(text, record, index)">编辑</a-button>
            </a-button-group>
          </span>
        </a-table>
        <div :class="rows.length > 0 ? 'table-pagination-tools' : 'table-pagination-tools-empty'">
          <a-button icon="minus-circle" type="danger" @click="batchRemove" v-permit="'oa:formData:delete'" :disabled="selection.selectedRows.length === 0">删除</a-button>
          <a-divider type="vertical" v-permit="'oa:formData:add'" />
          <a-button icon="plus-circle" type="primary" @click="add" v-permit="'oa:formData:add'">新增</a-button>
        </div>
      </div>
    </a-card>
    <!--展示界面-->
    <a-modal :title="'信息查看 - ' + form._id" v-model="infoVisible" :destroyOnClose="true" :footer="null">
      <fs-view v-model="form" :frame="frame" :config="config" />
    </a-modal>
    <!--编辑界面-->
    <a-modal
      :title="'信息' + (form._id ? ('修改 - ' + form._id) : '添加')"
      v-model="formVisible"
      :confirmLoading="formLoading"
      :maskClosable="false"
      @ok="submit"
      :destroyOnClose="true"
      width="818px">
      <fs-form v-model="form" ref="form" :frame="frame" :config="config" />
      <template slot="footer">
        <a-button key="back" @click="() => formVisible = false">取消</a-button>
        <a-button key="submit" type="primary" :loading="formLoading" @click="submit(false)">确定</a-button>
        <a-button key="force" type="danger" :loading="formLoading" @click="submit(true)">强制提交</a-button>
      </template>
    </a-modal>
  </section>
</template>

<script>
import DateUtil from '@/utils/date'
import RouteUtil from '@/utils/route'
import formService from '@/service/oa/form'
import ListFilter from './design/ListFilter'
import ListSorter from './design/ListSorter'
import ListViewer from './design/ListViewer'
import config from './design/config'
import FsForm from './design/FsForm'
import FsView from './design/FsView'

export default {
  components: { ListFilter, ListSorter, ListViewer, FsForm, FsView },
  data () {
    return {
      config,
      advanced: false,
      filters: { condition: [], sort: [], column: [] },
      columns: [],
      selection: RouteUtil.selection(),
      pagination: {},
      formRows: [],
      loading: false,
      infoVisible: false,
      infoLoading: false,
      formVisible: false,
      formLoading: false,
      form: {},
      frame: {
        id: 0,
        name: '未就绪'
      },
      fields: { filter: [], sorter: [], viewer: [] }
    }
  },
  computed: {
    rows () {
      if (!this.frame.widgets) return []
      const widgets = [this.config.idField].concat(this.frame.widgets, this.config.reservedFields)
      return this.config.validator.pretty(widgets, this.formRows)
    }
  },
  methods: {
    dateRender (text, record, index) {
      return DateUtil.format(text)
    },
    dateFormat () {
      return DateUtil.dateMomentFormat()
    },
    showTime (indexRange) {
      return { format: DateUtil.timeMomentFormat(), defaultValue: DateUtil.timeMomentRange()[indexRange] }
    },
    batchRemove () {
      this.$confirm(this.selection.confirm(() => {
        this.loading = true
        const param = {
          frameId: this.frame.id,
          ids: this.selection.selectedRowKeys
        }
        formService.delete(param, { success: true }).then((result) => {
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
      this.$set(this, 'columns', this.config.exhibition.tableColumns(this.filters.column).concat([
        { title: '操作', width: 120, scopedSlots: { customRender: 'action' } }
      ]))
      this.selection.clear()
      Object.assign(this.filters, RouteUtil.paginationData(this.pagination, pagination))
      filter2query && RouteUtil.filter2query(this, this.filters)
      this.loading = true
      formService.list(Object.assign({}, this.filters, { frameId: this.frame.id })).then((result) => {
        this.pagination = Object.assign({}, this.pagination, RouteUtil.result(result))
        if (result.code === 0) {
          this.formRows = result.data.rows
        }
        this.loading = false
      })
    },
    submit (modeForce) {
      this.$refs.form.validate(valid => {
        if (!valid) {
          this.$message.warning('数据校验不通过')
          if (!modeForce) return
        }
        if (this.formLoading) return false
        const data = { frameId: this.frame.id, form: this.form }
        this.formLoading = true
        formService.save(data).then(result => {
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
    edit (text, record, index) {
      this.form = this.formRows[index]
      this.formVisible = true
    },
    show (text, record, index) {
      this.form = record
      this.infoVisible = true
    },
    load () {
      this.loading = true
      formService.frame({ id: this.$route.query.id }).then(result => {
        this.loading = false
        if (result.code !== 0) return false
        this.$set(this, 'frame', result.data)
        let pageSize = 5
        if (result.data.options && result.data.options.pageSize > 0) {
          pageSize = result.data.options.pageSize
        }
        Object.assign(this.filters, RouteUtil.query2filter(this, { page: 1, pageSize }))
        this.pagination = Object.assign({}, RouteUtil.pagination(this.filters), this.pagination)
        this.fields.filter = this.config.exhibition.operateFields(this.frame.widgets, 'filterable', false)
            .concat(this.config.exhibition.operateFields(this.config.reservedFields, 'filterable', false))
        this.fields.sorter = this.config.exhibition.operateFields(this.frame.widgets, 'sortable', false)
            .concat(this.config.exhibition.operateFields(this.config.reservedFields, 'sortable', false))
        this.fields.viewer = this.config.exhibition.operateFields(this.frame.widgets, 'viewable', true)
            .concat(this.config.exhibition.operateFields(this.config.reservedFields, 'viewable', true))
        if (RouteUtil.hasFilter(this)) {
          this.filters.column = this.config.exhibition.mergeColumnItem(this.fields.viewer, this.filters.column)
        } else { // 采用默认配置
          this.filters.sort = this.config.exhibition.parseSortor(this.frame.options.sort)
          this.filters.column = this.config.exhibition.mergeColumnItem(
            this.fields.viewer, this.config.exhibition.parseColumnSorted(this.frame.options.column))
        }
        this.search(false, true)
      })
    }
  },
  mounted () {
    this.load()
  }
}
</script>
