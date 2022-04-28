<template>
  <section>
    <a-card :bordered="false">
      <div class="table-page-search-wrapper">
        <a-form-model ref="filters" :model="filters" layout="inline">
          <a-row :gutter="48">
            <a-col :md="6" :sm="24">
              <a-form-model-item label="所属包" prop="catalog">
                <a-input v-model="filters.catalog" placeholder="" :allowClear="true" />
              </a-form-model-item>
            </a-col>
            <a-col :md="6" :sm="24">
              <a-form-model-item label="模型编码" prop="model">
                <a-input v-model="filters.model" placeholder="" :allowClear="true" />
              </a-form-model-item>
            </a-col>
            <a-col :md="6" :sm="24">
              <a-form-model-item label="字段编码" prop="column">
                <a-input v-model="filters.column" placeholder="" :allowClear="true" />
              </a-form-model-item>
            </a-col>
            <a-col :md="6" :sm="24">
              <a-form-model-item label="关联关系" prop="relation">
                <a-select v-model="filters.relation" placeholder="请选择" :allowClear="true">
                  <a-select-option v-for="(value, key) in config.relations" :key="key" :value="key">{{ key }} - {{ value.name }}</a-select-option>
                </a-select>
              </a-form-model-item>
            </a-col>
          </a-row>
          <a-row :gutter="48">
            <a-col :md="6" :sm="24">
              <a-form-model-item label="来源包" prop="sourceCatalog">
                <a-input v-model="filters.sourceCatalog" placeholder="" :allowClear="true" />
              </a-form-model-item>
            </a-col>
            <a-col :md="6" :sm="24">
              <a-form-model-item label="来源模型" prop="sourceModel">
                <a-input v-model="filters.sourceModel" placeholder="" :allowClear="true" />
              </a-form-model-item>
            </a-col>
            <a-col :md="6" :sm="24">
              <a-form-model-item label="来源字段" prop="sourceColumn">
                <a-input v-model="filters.sourceColumn" placeholder="" :allowClear="true" />
              </a-form-model-item>
            </a-col>
            <a-col :md="6" :sm="24">
              <a-button @click="exchange">交换</a-button>
            </a-col>
          </a-row>
          <a-row :gutter="48">
            <a-col :md="6" :sm="24">
              <a-form-model-item label="目标包" prop="targetCatalog">
                <a-input v-model="filters.targetCatalog" placeholder="" :allowClear="true" />
              </a-form-model-item>
            </a-col>
            <a-col :md="6" :sm="24">
              <a-form-model-item label="目标模型" prop="targetModel">
                <a-input v-model="filters.targetModel" placeholder="" :allowClear="true" />
              </a-form-model-item>
            </a-col>
            <a-col :md="6" :sm="24">
              <a-form-model-item label="目标字段" prop="targetColumn">
                <a-input v-model="filters.targetColumn" placeholder="" :allowClear="true" />
              </a-form-model-item>
            </a-col>
            <a-col :md="6" :sm="24">
              <a-space>
                <a-button type="primary" @click="search(true, false)" :loading="loading">查询</a-button>
                <a-button @click="() => this.$refs.filters.resetFields()">重置</a-button>
                <a-button @click.native="$router.go(-1)">返回</a-button>
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
          <span slot="action" slot-scope="text, record">
            <a-button-group>
              <a-button v-permit="'govern:modelRelation:modify'" type="link" size="small" @click="edit(text, record)">编辑</a-button>
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
            <a-button icon="minus-circle" type="danger" @click="batchRemove" v-permit="'govern:modelRelation:delete'" :disabled="selection.selectedRows.length === 0">删除</a-button>
            <a-button icon="plus-circle" type="primary" @click="add" v-permit="'govern:modelRelation:add'">新增</a-button>
          </a-space>
        </div>
      </div>
    </a-card>
    <!--编辑界面-->
    <a-modal :title="(form.id ? ('修改 - ' + form.id) : '新增') + '关系'" v-model="formVisible" :confirmLoading="formLoading" :maskClosable="false" @ok="submit">
      <a-form-model ref="form" :model="form" :rules="rules" :label-col="{ span: 5 }" :wrapper-col="{ span: 18 }">
        <a-form-model-item label="主键" prop="id">
          <a-input v-model="form.id" auto-complete="off" />
        </a-form-model-item>
        <a-form-model-item label="来源包" prop="sourceCatalog">
          <a-input v-model="form.sourceCatalog" auto-complete="off" />
        </a-form-model-item>
        <a-form-model-item label="来源模型" prop="sourceModel">
          <a-input v-model="form.sourceModel" auto-complete="off"></a-input>
        </a-form-model-item>
        <a-form-model-item label="来源字段" prop="sourceColumn">
          <a-input v-model="form.sourceColumn" auto-complete="off"></a-input>
        </a-form-model-item>
        <a-form-model-item label="关联关系" prop="relation">
          <a-select v-model="form.relation" placeholder="请选择" :allowClear="true">
            <a-select-option v-for="(value, key) in config.relations" :key="key" :value="key">{{ key }} - {{ value.name }}</a-select-option>
          </a-select>
        </a-form-model-item>
        <a-form-model-item label="目标包" prop="targetCatalog">
          <a-input v-model="form.targetCatalog" auto-complete="off" />
        </a-form-model-item>
        <a-form-model-item label="目标模型" prop="targetModel">
          <a-input v-model="form.targetModel" auto-complete="off"></a-input>
        </a-form-model-item>
        <a-form-model-item label="目标字段" prop="targetColumn">
          <a-input v-model="form.targetColumn" auto-complete="off"></a-input>
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
import modelRelationService from '@/service/govern/modelRelation'

export default {
  data () {
    return {
      DateUtil,
      advanced: false,
      filters: {},
      columns: [
        { title: 'ID', dataIndex: 'id' },
        { title: '来源包', dataIndex: 'sourceCatalog' },
        { title: '来源模型', dataIndex: 'sourceModel' },
        { title: '来源字段', dataIndex: 'sourceColumn' },
        { title: '关联关系', dataIndex: 'relation' },
        { title: '目标包', dataIndex: 'targetCatalog' },
        { title: '目标模型', dataIndex: 'targetModel' },
        { title: '目标字段', dataIndex: 'targetColumn' },
        { title: '操作', scopedSlots: { customRender: 'action' }, width: 80 }
      ],
      selection: RouteUtil.selection(),
      pagination: {},
      rows: [],
      loading: false,
      config: {
        ready: false,
        relations: {}
      },
      formVisible: false,
      formLoading: false,
      form: {},
      rules: {
        sourceCatalog: [{ required: true, message: '请输入来源包', trigger: 'blur' }],
        sourceModel: [{ required: true, message: '请输入来源模型', trigger: 'blur' }],
        relation: [{ required: true, message: '请选择关联关系', trigger: 'change' }],
        targetCatalog: [{ required: true, message: '请输入目标包', trigger: 'blur' }],
        targetModel: [{ required: true, message: '请输入目标模型', trigger: 'blur' }]
      }
    }
  },
  computed: {
    breadcrumb () {
      const catalog = this.filters.catalog || ''
      const catalogs = catalog.split('/')
      const result = []
      for (let i = 1; i < catalogs.length; i++) {
        result.push({
          code: catalogs[i],
          catalog: catalogs.slice(0, i).join('/')
        })
      }
      return result
    }
  },
  methods: {
    batchRemove () {
      this.$confirm(this.selection.confirm(() => {
        this.loading = true
        modelRelationService.delete(this.selection.selectedRowKeys, { success: true }).then((result) => {
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
      modelRelationService.list(this.filters).then((result) => {
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
        modelRelationService.save(this.form).then(result => {
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
    },
    exchange () {
      Object.assign(this.filters, {
        sourceCatalog: this.filters.targetCatalog,
        sourceModel: this.filters.targetModel,
        sourceColumn: this.filters.targetColumn,
        targetCatalog: this.filters.sourceCatalog,
        targetModel: this.filters.sourceModel,
        targetColumn: this.filters.sourceColumn
      })
    }
  },
  created () {
    this.filters = RouteUtil.query2filter(this, { page: 1, pageSize: 5 })
    this.pagination = Object.assign({}, RouteUtil.pagination(this.filters), this.pagination)
  },
  mounted () {
    this.search(false, true)
    modelRelationService.config().then((result) => {
      this.config.ready = true
      if (result.code === 0) {
        Object.assign(this.config, result.data)
      }
    })
  }
}
</script>
