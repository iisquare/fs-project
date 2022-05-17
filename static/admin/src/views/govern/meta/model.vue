<template>
  <section>
    <a-card :bordered="false">
      <a-breadcrumb style="margin-bottom: 20px;">
        <a-breadcrumb-item>
          <a-icon type="home" class="fs-ui-point" @click="forward()" />
        </a-breadcrumb-item>
        <a-breadcrumb-item :key="index" v-for="(item, index) in breadcrumb">
          <span class="fs-ui-point" @click="forward(item)">{{ item.code }}</span>
        </a-breadcrumb-item>
      </a-breadcrumb>
      <div class="table-page-search-wrapper">
        <a-form-model ref="filters" :model="filters" layout="inline">
          <a-row :gutter="48">
            <a-col :md="6" :sm="24">
              <a-form-model-item label="编码" prop="code">
                <a-input v-model="filters.code" placeholder="" :allowClear="true" />
              </a-form-model-item>
            </a-col>
            <a-col :md="6" :sm="24">
              <a-form-model-item label="名称" prop="name">
                <a-input v-model="filters.name" placeholder="" :allowClear="true" />
              </a-form-model-item>
            </a-col>
            <a-col :md="6" :sm="24">
              <a-form-model-item label="类型" prop="type">
                <a-select v-model="filters.type" placeholder="请选择" :allowClear="true">
                  <a-select-option v-for="(value, key) in config.types" :key="key" :value="key">{{ key }} - {{ value }}</a-select-option>
                </a-select>
              </a-form-model-item>
            </a-col>
            <a-col :md="6" :sm="24">
              <a-space>
                <a-button type="primary" @click="search(true, false)" :loading="loading">查询</a-button>
                <a-button @click="() => this.$refs.filters.resetFields()">重置</a-button>
                <a-button @click="compare(filters)" v-permit="'govern:model:add'">导入</a-button>
              </a-space>
            </a-col>
          </a-row>
        </a-form-model>
        <a-table
          :columns="columns"
          :rowKey="(record, index) => index"
          :dataSource="rows"
          :pagination="pagination"
          :loading="loading"
          :rowSelection="selection"
          @change="tableChange"
          :bordered="true"
        >
          <span slot="action" slot-scope="text, record">
            <a-button-group>
              <a-button v-if="record.type === 'catalog'" type="link" size="small" @click="forward(record)">进入</a-button>
              <a-button v-if="record.type === 'catalog'" v-permit="'govern:model:modify'" type="link" size="small" @click="catalog(record)">编辑</a-button>
              <a-button v-if="record.type !== 'catalog'" v-permit="'govern:model:modify'" type="link" size="small" @click="draw(record)">编辑</a-button>
              <a-button v-if="record.type !== 'catalog'" v-permit="'govern:model:modify'" type="link" size="small" @click="compare(record)">对比</a-button>
            </a-button-group>
          </span>
          <a-descriptions slot="expandedRowRender" slot-scope="record">
            <a-descriptions-item label="创建者">{{ record.createdUidName }}</a-descriptions-item>
            <a-descriptions-item label="创建时间" :span="2">{{ DateUtil.format(record.createdTime) }}</a-descriptions-item>
            <a-descriptions-item label="修改者">{{ record.updatedUidName }}</a-descriptions-item>
            <a-descriptions-item label="修改时间" :span="2">{{ DateUtil.format(record.updatedTime) }}</a-descriptions-item>
            <a-descriptions-item label="描述信息" :span="3">{{ record.description }}</a-descriptions-item>
          </a-descriptions>
        </a-table>
        <div :class="rows.length > 0 ? 'table-pagination-tools' : 'table-pagination-tools-empty'">
          <a-space>
            <a-button icon="minus-circle" type="danger" @click="batchRemove" v-permit="'govern:model:delete'" :disabled="selection.selectedRows.length === 0">删除</a-button>
            <a-button icon="plus-circle" type="primary" @click="catalog" v-permit="'govern:model:add'">新增包</a-button>
            <a-button icon="plus-circle" type="primary" @click="draw" v-permit="'govern:model:add'">新增类</a-button>
          </a-space>
        </div>
      </div>
    </a-card>
    <!--编辑界面-->
    <a-modal :title="(form.id ? ('修改 - ' + form.id) : '新增') + '包'" v-model="formVisible" :confirmLoading="formLoading" :maskClosable="false" @ok="submit">
      <a-form-model ref="form" :model="form" :rules="rules" :label-col="{ span: 5 }" :wrapper-col="{ span: 18 }">
        <a-form-model-item label="所属包" prop="catalog">
          <a-input v-model="form.catalog" auto-complete="off" disabled />
        </a-form-model-item>
        <a-form-model-item label="编码" prop="code">
          <a-input v-model="form.code" auto-complete="off"></a-input>
        </a-form-model-item>
        <a-form-model-item label="名称" prop="name">
          <a-input v-model="form.name" auto-complete="off"></a-input>
        </a-form-model-item>
        <a-form-model-item label="排序">
          <a-input-number v-model="form.sort" :min="0"></a-input-number>
        </a-form-model-item>
        <a-form-model-item label="描述">
          <a-textarea v-model="form.description" />
        </a-form-model-item>
      </a-form-model>
    </a-modal>
  </section>
</template>

<script>
import DateUtil from '@/utils/date'
import RouteUtil from '@/utils/route'
import modelService from '@/service/govern/model'

export default {
  data () {
    return {
      DateUtil,
      advanced: false,
      filters: {},
      columns: [
        { title: '编码', dataIndex: 'code' },
        { title: '名称', dataIndex: 'name' },
        { title: '类型', dataIndex: 'type' },
        { title: '排序', dataIndex: 'sort' },
        { title: '操作', scopedSlots: { customRender: 'action' }, width: 80 }
      ],
      selection: RouteUtil.selection({ type: 'radio' }),
      pagination: {},
      rows: [],
      loading: false,
      config: {
        ready: false,
        types: {}
      },
      formVisible: false,
      formLoading: false,
      form: {},
      rules: {
        code: [{ required: true, message: '请输入编码', trigger: 'blur' }],
        name: [{ required: true, message: '请输入名称', trigger: 'blur' }]
      }
    }
  },
  computed: {
    breadcrumb () {
      const catalog = this.filters.catalog || ''
      const catalogs = catalog.split('/')
      const result = []
      for (let i = 1; i < catalogs.length - 1; i++) {
        result.push({
          code: catalogs[i],
          catalog: catalogs.slice(0, i).join('/') + '/'
        })
      }
      return result
    }
  },
  methods: {
    batchRemove () {
      this.$confirm(this.selection.confirm(() => {
        this.loading = true
        const record = this.rows[this.selection.selectedRowKeys[0]]
        const param = { catalog: record.catalog, code: record.code }
        modelService.delete(param, { success: true }).then((result) => {
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
      if (!this.filters.catalog) this.filters.catalog = '/'
      filter2query && RouteUtil.filter2query(this, this.filters)
      this.loading = true
      modelService.list(this.filters).then((result) => {
        this.pagination = Object.assign({}, this.pagination, RouteUtil.result(result))
        if (result.code === 0) {
          this.rows = result.data.rows
        }
        this.loading = false
      })
    },
    forward (record = {}) {
      this.$set(this, 'filters', {
        pageSize: this.filters.pageSize,
        catalog: record.code ? record.catalog + record.code + '/' : '/'
      })
      this.search(true, false)
    },
    draw (record = {}) {
      const catalog = record.code ? record.catalog : this.filters.catalog
      this.$router.push({
        path: '/govern/meta/draw', query: { catalog, code: record.code }
      })
    },
    compare (record = {}) {
      this.$router.push({
        path: '/govern/meta/modelCompare', query: { catalog: record.catalog, code: record.code }
      })
    },
    catalog (record = {}) {
      this.form = Object.assign({ catalog: this.filters.catalog, type: 'catalog' }, record)
      this.formVisible = true
    },
    submit () {
      this.$refs.form.validate(valid => {
        if (!valid || this.formLoading) return false
        this.formLoading = true
        modelService.save(this.form).then(result => {
          if (result.code === 0) {
            this.formVisible = false
            this.search(false, true)
          }
          this.formLoading = false
        })
      })
    }
  },
  created () {
    this.filters = RouteUtil.query2filter(this, { page: 1, pageSize: 100 })
    this.pagination = Object.assign({}, RouteUtil.pagination(this.filters), this.pagination)
  },
  mounted () {
    this.search(false, true)
    modelService.config().then((result) => {
      this.config.ready = true
      if (result.code === 0) {
        Object.assign(this.config, result.data)
      }
    })
  }
}
</script>
