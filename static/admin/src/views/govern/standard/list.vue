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
              <a-form-model-item label="别名" prop="another">
                <a-input v-model="filters.another" placeholder="" :allowClear="true" />
              </a-form-model-item>
            </a-col>
            <a-col :md="6" :sm="24">
              <a-space>
                <a-button type="primary" @click="search(true, false)" :loading="loading">查询</a-button>
                <a-button @click="() => this.$refs.filters.resetFields()">重置</a-button>
                <a-button @click="draw('pick')" v-permit="'govern:standard:add'">拾取</a-button>
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
              <a-button v-if="record.mold === 'catalog'" type="link" size="small" @click="forward(record)">进入</a-button>
              <a-button v-if="record.mold === 'catalog'" v-permit="'govern:standard:modify'" type="link" size="small" @click="catalog(record)">编辑</a-button>
              <a-button v-if="record.mold !== 'catalog'" v-permit="'govern:standard:modify'" type="link" size="small" @click="draw('', record)">编辑</a-button>
            </a-button-group>
          </span>
          <a-descriptions slot="expandedRowRender" slot-scope="record">
            <a-descriptions-item label="全路径" :span="3">{{ record.path }}</a-descriptions-item>
            <a-descriptions-item label="编码别名" :span="3" v-if="record.mold !== 'catalog'">{{ record.another }}</a-descriptions-item>
            <a-descriptions-item label="记录标志" :span="3" v-if="record.mold !== 'catalog'">{{ record.flag }}</a-descriptions-item>
            <a-descriptions-item label="字段类型" v-if="record.mold !== 'catalog'">{{ record.type }}</a-descriptions-item>
            <a-descriptions-item label="预警等级" v-if="record.mold !== 'catalog'">{{ record.level }}</a-descriptions-item>
            <a-descriptions-item label="记录类型" v-if="record.mold !== 'catalog'">{{ record.mold }}</a-descriptions-item>
            <a-descriptions-item label="字段长度" v-if="record.mold !== 'catalog'">{{ record.size }}</a-descriptions-item>
            <a-descriptions-item label="小数位数" v-if="record.mold !== 'catalog'">{{ record.digit }}</a-descriptions-item>
            <a-descriptions-item label="允许为空" v-if="record.mold !== 'catalog'">{{ record.nullable ? 'Y' : 'N' }}</a-descriptions-item>
            <a-descriptions-item label="创建者">{{ record.createdUidName }}</a-descriptions-item>
            <a-descriptions-item label="创建时间" :span="2">{{ DateUtil.format(record.createdTime) }}</a-descriptions-item>
            <a-descriptions-item label="修改者">{{ record.updatedUidName }}</a-descriptions-item>
            <a-descriptions-item label="修改时间" :span="2">{{ DateUtil.format(record.updatedTime) }}</a-descriptions-item>
            <a-descriptions-item label="描述信息" :span="3">{{ record.description }}</a-descriptions-item>
          </a-descriptions>
        </a-table>
        <div :class="rows.length > 0 ? 'table-pagination-tools' : 'table-pagination-tools-empty'">
          <a-space>
            <a-button icon="minus-circle" type="danger" @click="batchRemove" v-permit="'govern:standard:delete'" :disabled="selection.selectedRows.length === 0">删除</a-button>
            <a-button icon="plus-circle" type="primary" @click="catalog" v-permit="'govern:standard:add'">新增目录</a-button>
            <a-button icon="plus-circle" type="primary" @click="draw('')" v-permit="'govern:standard:add'">新增标准</a-button>
          </a-space>
        </div>
      </div>
    </a-card>
    <!--编辑界面-->
    <a-modal :title="(form.id ? ('修改 - ' + form.id) : '新增') + '目录'" v-model="formVisible" :confirmLoading="formLoading" :maskClosable="false" @ok="submit">
      <a-form-model ref="form" :model="form" :rules="rules" :label-col="{ span: 5 }" :wrapper-col="{ span: 18 }">
        <a-form-model-item label="所属目录" prop="catalog">
          <a-input v-model="form.catalog" auto-complete="off" disabled />
        </a-form-model-item>
        <a-form-model-item label="标准编码" prop="code">
          <a-input v-model="form.code" auto-complete="off"></a-input>
        </a-form-model-item>
        <a-form-model-item label="名称" prop="name">
          <a-input v-model="form.name" auto-complete="off"></a-input>
        </a-form-model-item>
        <a-form-model-item label="状态" prop="status">
          <a-select v-model="form.status" placeholder="请选择">
            <a-select-option v-for="(value, key) in config.status" :key="key" :value="key">{{ value }}</a-select-option>
          </a-select>
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
import standardService from '@/service/govern/standard'

export default {
  data () {
    return {
      DateUtil,
      advanced: false,
      filters: {},
      columns: [
        { title: '编码', dataIndex: 'code' },
        { title: '名称', dataIndex: 'name' },
        { title: '类型', dataIndex: 'mold' },
        { title: '状态', dataIndex: 'statusText' },
        { title: '排序', dataIndex: 'sort' },
        { title: '操作', scopedSlots: { customRender: 'action' }, width: 80 }
      ],
      selection: RouteUtil.selection({ type: 'radio' }),
      pagination: {},
      rows: [],
      loading: false,
      config: {
        ready: false,
        molds: {},
        flags: {},
        levels: {},
        status: {},
        columnTypes: []
      },
      formVisible: false,
      formLoading: false,
      form: {},
      rules: {
        code: [{ required: true, message: '请输入编码', trigger: 'blur' }],
        name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
        status: [{ required: true, message: '请选择状态', trigger: 'change' }]
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
        standardService.delete(param, { success: true }).then((result) => {
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
      standardService.list(this.filters).then((result) => {
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
    draw (mode, record = {}) {
      const catalog = record.code ? record.catalog : this.filters.catalog
      this.$router.push({
        path: '/govern/standard/draw', query: { catalog, code: record.code, mode }
      })
    },
    catalog (record = {}) {
      this.form = Object.assign({ catalog: this.filters.catalog, mold: 'catalog' }, record)
      if (this.form.status) this.form.status += ''
      this.formVisible = true
    },
    submit () {
      this.$refs.form.validate(valid => {
        if (!valid || this.formLoading) return false
        this.formLoading = true
        standardService.save(this.form).then(result => {
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
    standardService.config().then((result) => {
      this.config.ready = true
      if (result.code === 0) {
        Object.assign(this.config, result.data)
      }
    })
  }
}
</script>
