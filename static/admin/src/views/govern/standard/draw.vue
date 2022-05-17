<template>
  <div class="fs-layout-box">
    <div class="fs-layout-header">
      <div class="fs-layout-back">
        <a-icon type="left" class="fs-ui-point" @click.native="$router.go(-1)" />
      </div>
      <div class="fs-layout-menu">
        <a-menu v-model="menus" mode="horizontal">
          <a-menu-item key="pick" v-if="mode === 'pick'"><a-icon type="compass" />拾取字段</a-menu-item>
          <a-menu-item key="basic"><a-icon type="edit" />基础信息</a-menu-item>
          <a-menu-item key="column"><a-icon type="profile" />字段属性</a-menu-item>
          <a-menu-item key="assess"><a-icon type="alert" />评估参数</a-menu-item>
        </a-menu>
      </div>
      <div class="fs-layout-action">
        <a-space>
          <a-button type="primary" icon="save" :loading="loading" @click="save">保存</a-button>
        </a-space>
      </div>
    </div>
    <div class="fs-layout-pick" v-show="menus.indexOf('pick') !== -1">
      <div class="table-page-search-wrapper">
        <a-form-model ref="filters" :model="filters" layout="inline">
          <a-row :gutter="48">
            <a-col :md="5" :sm="24">
              <a-form-model-item label="包路径">
                <a-input v-model="filters.catalog" placeholder="" :allowClear="true" />
              </a-form-model-item>
            </a-col>
            <a-col :md="5" :sm="24">
              <a-form-model-item label="模型编码">
                <a-input v-model="filters.model" placeholder="" :allowClear="true" />
              </a-form-model-item>
            </a-col>
            <a-col :md="5" :sm="24">
              <a-form-model-item label="字段编码">
                <a-input v-model="filters.code" placeholder="" :allowClear="true" />
              </a-form-model-item>
            </a-col>
            <a-col :md="5" :sm="24">
              <a-form-model-item label="字段名称">
                <a-input v-model="filters.name" placeholder="" :allowClear="true" />
              </a-form-model-item>
            </a-col>
            <a-col :md="4" :sm="24">
              <a-space>
                <a-button type="primary" @click="search(true, false)" :loading="loading">查询</a-button>
                <a-button @click="() => this.$refs.filters.resetFields()">重置</a-button>
                <a-button @click="pick" :disabled="selection.selectedRows.length === 0">拾取</a-button>
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
        </a-table>
      </div>
    </div>
    <div class="fs-layout-box" v-show="menus.indexOf('basic') !== -1">
      <a-form-model ref="form" :model="form" :rules="rules" :label-col="{ span: 5 }" :wrapper-col="{ span: 18 }">
        <a-form-model-item label="所属目录" prop="catalog">
          <a-input v-model="form.catalog" auto-complete="off" disabled />
        </a-form-model-item>
        <a-form-model-item label="标准编码" prop="code">
          <a-input v-model="form.code" auto-complete="off"></a-input>
        </a-form-model-item>
        <a-form-model-item label="标准名称" prop="name">
          <a-input v-model="form.name" auto-complete="off"></a-input>
        </a-form-model-item>
        <a-form-model-item label="别名编码" prop="another">
          <a-select mode="tags" v-model="form.another" placeholder="输入标准别名编码" />
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
    </div>
    <div class="fs-layout-box" v-show="menus.indexOf('column') !== -1">
      <a-form-model :model="form" :rules="rules" :label-col="{ span: 5 }" :wrapper-col="{ span: 18 }">
        <a-form-model-item label="字段类型" prop="type">
          <a-auto-complete v-model="form.type" optionLabelProp="value" :filterOption="UIUtil.filterOption" :allowClear="true">
            <template slot="dataSource">
              <a-select-option :key="k" :value="v" v-for="(v, k) in config.columnTypes">{{ v }}</a-select-option>
            </template>
          </a-auto-complete>
        </a-form-model-item>
        <a-form-model-item label="字段长度" prop="size">
          <a-input-number v-model="form.size" :min="0"></a-input-number>
        </a-form-model-item>
        <a-form-model-item label="小数位数" prop="digit">
          <a-input-number v-model="form.digit" :min="0"></a-input-number>
        </a-form-model-item>
        <a-form-model-item label="允许为空">
          <a-checkbox v-model="form.nullable">是否允许为空</a-checkbox>
        </a-form-model-item>
      </a-form-model>
    </div>
    <div class="fs-layout-box" v-show="menus.indexOf('assess') !== -1">
      <a-form-model :model="form" :rules="rules" :label-col="{ span: 5 }" :wrapper-col="{ span: 18 }">
        <a-form-model-item label="评估标志" prop="flag">
          <a-select mode="multiple" v-model="form.flag" placeholder="输入选择标志">
            <a-select-option v-for="(value, key) in config.flags" :key="key" :value="key">{{ key }}-{{ value }}</a-select-option>
          </a-select>
        </a-form-model-item>
        <a-form-model-item label="预警等级" prop="level">
          <a-select v-model="form.level" placeholder="请选择">
            <a-select-option v-for="(value, key) in config.levels" :key="key" :value="key">{{ key }}-{{ value }}</a-select-option>
          </a-select>
        </a-form-model-item>
      </a-form-model>
    </div>
  </div>
</template>

<script>
import UIUtil from '@/utils/ui'
import RouteUtil from '@/utils/route'
import standardService from '@/service/govern/standard'
import columnService from '@/service/govern/modelColumn'

export default {
  data () {
    return {
      UIUtil,
      mode: '',
      filters: {},
      columns: [
        { title: '包路径', dataIndex: 'catalog' },
        { title: '模型编码', dataIndex: 'model' },
        { title: '字段编码', dataIndex: 'code' },
        { title: '字段名称', dataIndex: 'name' },
        { title: '字段类型', dataIndex: 'type' },
        { title: '字段长度', dataIndex: 'size' },
        { title: '小数位数', dataIndex: 'digit' },
        { title: '允许为空', dataIndex: 'nullable', customRender: v => v ? 'Y' : 'N' }
      ],
      selection: RouteUtil.selection({ type: 'radio' }),
      pagination: {},
      rows: [],
      loading: false,
      menus: ['basic'],
      form: {},
      rules: {
        code: [{ required: true, message: '请输入编码', trigger: 'blur' }],
        name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
        status: [{ required: true, message: '请选择状态', trigger: 'change' }],
        level: [{ required: true, message: '请选择预警等级', trigger: 'change' }]
      },
      config: {
        ready: false,
        molds: {},
        flags: {},
        levels: {},
        status: {},
        columnTypes: []
      }
    }
  },
  methods: {
    pick () {
      const record = this.rows[this.selection.selectedRowKeys[0]]
      Object.assign(this.form, {
        code: this.form.code ?? record.code,
        name: this.form.name ?? record.name,
        type: record.type,
        size: record.size,
        digit: record.digit,
        nullable: record.nullable === 1
      })
      this.menus = ['basic']
    },
    tableChange (pagination, filters, sorter) {
      this.pagination = RouteUtil.paginationChange(this.pagination, pagination)
      this.search(true, true)
    },
    search (filter2query, pagination) {
      this.selection.clear()
      Object.assign(this.filters, RouteUtil.paginationData(this.pagination, pagination))
      this.loading = true
      columnService.list(this.filters).then((result) => {
        this.pagination = Object.assign({}, this.pagination, RouteUtil.result(result))
        if (result.code === 0) {
          this.rows = result.data.rows
        }
        this.loading = false
      })
    },
    load () {
      this.loading = true
      if (!this.form.code) {
        return (this.loading = false)
      }
      standardService.info(this.form).then(result => {
        if (result.code !== 0) return false
        result.data.status += ''
        this.$set(this, 'form', result.data)
        this.loading = false
      })
    },
    save () {
      this.$refs.form.validate(valid => {
        if (!valid) this.menus = ['basic']
        if (!valid || this.loading) return false
        this.loading = true
        standardService.save(this.form, { success: true }).then(result => {
          if (result.code === 0) {
            result.data.status += ''
            this.$set(this, 'form', result.data)
          }
          this.loading = false
        })
      })
    }
  },
  created () {
    this.form = {
      mold: 'column',
      catalog: this.$route.query.catalog,
      code: this.$route.query.code
    }
    this.mode = this.$route.query.mode
    if (this.mode === 'pick') {
      this.menus = ['pick']
      this.filters = { page: 1, pageSize: 15 }
      this.pagination = Object.assign({}, RouteUtil.pagination(this.filters), this.pagination)
    }
  },
  mounted () {
    this.load()
    if (this.mode === 'pick') this.search(false, true)
    standardService.config().then((result) => {
      this.config.ready = true
      if (result.code === 0) {
        Object.assign(this.config, result.data)
      }
    })
  }
}
</script>

<style lang="less" scoped>
.fs-layout-box {
  width: 100%;
  .fs-layout-header {
    width: 100%;
    height: 48px;
    border-bottom: 1px solid #e8e8e8;
    .fs-layout-back {
      width: 100px;
      height: 100%;
      display: inline-block;
      padding: 0px 15px;
    }
    .fs-layout-menu {
      width: calc(100% - 200px);
      height: 100%;
      display: inline-block;
      text-align: center;
    }
    .fs-layout-action {
      width: 100px;
      height: 100%;
      display: inline-block;
    }
  }
  .fs-layout-box  {
    width: 650px;
    padding: 15px;
    margin: 0 auto;
  }
  .fs-layout-pick {
    width: 100%;
    padding: 15px;
  }
}
</style>
