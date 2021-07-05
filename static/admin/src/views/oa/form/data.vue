<template>
  <section>
    <a-card :bordered="false">
      <div class="table-page-search-wrapper">
        <a-form-model ref="filters" :model="filters" layout="inline">
          <a-row :gutter="48">
            <a-col :md="6" :sm="24">
              <a-form-model-item label="所属表单" prop="frameId">
                <a-input v-model="filters.frameId" placeholder="" :allowClear="true" />
              </a-form-model-item>
            </a-col>
            <a-col :md="6" :sm="24">
              <a-form-model-item label="流程模型" prop="bpmWorkflowId">
                <a-input v-model="filters.bpmWorkflowId" placeholder="" :allowClear="true" />
              </a-form-model-item>
            </a-col>
            <a-col :md="6" :sm="24">
              <a-form-model-item label="流程实例" prop="bpmInstanceId">
                <a-input v-model="filters.bpmInstanceId" placeholder="" :allowClear="true" />
              </a-form-model-item>
            </a-col>
            <a-col :md="6" :sm="24">
              <a-form-model-item label="发起人" prop="bpmStartUserId">
                <a-input v-model="filters.bpmStartUserId" placeholder="" :allowClear="true" />
              </a-form-model-item>
            </a-col>
          </a-row>
          <a-row :gutter="48">
            <a-col :md="12" :sm="24">
              <a-form-model-item label="创建开始时间" prop="createdTimeBegin">
                <s-date-picker
                  v-model="filters.createdTimeBegin"
                  :showTime="showTime(0)"
                  :format="dateFormat()"
                  placeholder="开始时间"
                />
              </a-form-model-item>
            </a-col>
            <a-col :md="12" :sm="24">
              <a-form-model-item label="创建结束时间" prop="createdTimeEnd">
                <s-date-picker
                  v-model="filters.createdTimeEnd"
                  :showTime="showTime(1)"
                  :format="dateFormat()"
                  placeholder="结束时间"
                />
              </a-form-model-item>
            </a-col>
            <a-col :md="12" :sm="24">
              <a-form-model-item label="修改开始时间" prop="updatedTimeBegin">
                <s-date-picker
                  v-model="filters.updatedTimeBegin"
                  :showTime="showTime(0)"
                  :format="dateFormat()"
                  placeholder="开始时间"
                />
              </a-form-model-item>
            </a-col>
            <a-col :md="12" :sm="24">
              <a-form-model-item label="修改结束时间" prop="updatedTimeEnd">
                <s-date-picker
                  v-model="filters.updatedTimeEnd"
                  :showTime="showTime(1)"
                  :format="dateFormat()"
                  placeholder="结束时间"
                />
              </a-form-model-item>
            </a-col>
          </a-row>
          <a-row :gutter="48">
            <a-col :md="6" :sm="24">
              <a-form-model-item label="创建用户" prop="createdUid">
                <a-input v-model="filters.createdUid" placeholder="" :allowClear="true" />
              </a-form-model-item>
            </a-col>
            <a-col :md="6" :sm="24">
              <a-form-model-item label="修改用户" prop="updatedUid">
                <a-input v-model="filters.updatedUid" placeholder="" :allowClear="true" />
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
              <span class="table-page-search-submitButtons" :style="advanced && { float: 'right', overflow: 'hidden' } || {} ">
                <a-button type="primary" @click="search(true, false)" :loading="loading">查询</a-button>
                <a-button style="margin-left: 8px" @click="() => this.$refs.filters.resetFields()">重置</a-button>
              </span>
            </a-col>
          </a-row>
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
          <span slot="frame" slot-scope="text, record">
            <router-link :to="'/oa/form/list?id=' + record.frameId" target="_blank">{{ record.frameId }} - {{ record.frameIdName }}</router-link>
          </span>
          <span slot="action" slot-scope="text, record">
            <a-button-group>
              <a-button type="link" size="small" v-permit="'oa:formData:'" @click="show(text, record)">查看</a-button>
              <a-button v-permit="'oa:formData:modify'" type="link" size="small" @click="edit(text, record)">编辑</a-button>
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
    <a-modal :title="'信息查看 - ' + form._id" v-model="infoVisible" :footer="null">
      <a-form-model :model="form" :loading="infoLoading" :label-col="{ span: 5 }" :wrapper-col="{ span: 15 }">
        <a-form-model-item label="所属表单">{{ form.frameId }} - {{ form.frameIdName }}</a-form-model-item>
        <a-form-model-item label="流程模型">{{ form.bpmWorkflowId }}</a-form-model-item>
        <a-form-model-item label="流程实例">{{ form.bpmInstanceId }}</a-form-model-item>
        <a-form-model-item label="发起人">{{ form.bpmStartUserId }}</a-form-model-item>
        <a-form-model-item label="数据内容"><a-textarea v-model="form.content" /></a-form-model-item>
        <a-form-model-item label="创建者">{{ form.createdUidName }}</a-form-model-item>
        <a-form-model-item label="创建时间">{{ form.createdTime|date }}</a-form-model-item>
        <a-form-model-item label="修改者">{{ form.updatedUidName }}</a-form-model-item>
        <a-form-model-item label="修改时间">{{ form.updatedTime|date }}</a-form-model-item>
      </a-form-model>
    </a-modal>
    <!--编辑界面-->
    <a-modal :title="'信息' + (form._id ? ('修改 - ' + form._id) : '添加')" v-model="formVisible" :confirmLoading="formLoading" :maskClosable="false" @ok="submit">
      <a-form-model ref="form" :model="form" :rules="rules" :label-col="{ span: 5 }" :wrapper-col="{ span: 18 }">
        <a-form-model-item label="" prop="" :wrapper-col="{ span: 24 }">
          <a-alert message="内容中的预留字段，以表单输入为准！" type="info" show-icon />
        </a-form-model-item>
        <a-form-model-item label="ID" prop="_id">
          <a-input v-model="form._id" auto-complete="off"></a-input>
        </a-form-model-item>
        <a-form-model-item label="所属表单" prop="frameId">
          <a-input v-model="form.frameId" auto-complete="off"></a-input>
        </a-form-model-item>
        <a-form-model-item label="流程模型" prop="bpmWorkflowId">
          <a-input v-model="form.bpmWorkflowId" auto-complete="on"></a-input>
        </a-form-model-item>
        <a-form-model-item label="流程实例" prop="bpmInstanceId">
          <a-input v-model="form.bpmInstanceId" auto-complete="on"></a-input>
        </a-form-model-item>
        <a-form-model-item label="发起人" prop="bpmStartUserId">
          <a-input v-model="form.bpmStartUserId" auto-complete="on"></a-input>
        </a-form-model-item>
        <a-form-model-item label="内容">
          <a-textarea v-model="form.content" />
        </a-form-model-item>
      </a-form-model>
    </a-modal>
  </section>
</template>

<script>
import DateUtil from '@/utils/date'
import RouteUtil from '@/utils/route'
import formDataService from '@/service/oa/formData'

export default {
  data () {
    return {
      advanced: false,
      filters: {},
      columns: [
        { title: 'ID', dataIndex: '_id' },
        { title: '所属表单', scopedSlots: { customRender: 'frame' } },
        { title: '流程模型', dataIndex: 'bpmWorkflowId' },
        { title: '流程实例', dataIndex: 'bpmInstanceId' },
        { title: '发起人', dataIndex: 'bpmStartUserId' },
        { title: '操作', scopedSlots: { customRender: 'action' }, width: 120 }
      ],
      selection: RouteUtil.selection(),
      pagination: {},
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
        frameId: [{ required: true, message: '所属表单不能为空', trigger: 'blur' }]
      }
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
        formDataService.delete(this.selection.selectedRowKeys, { success: true }).then((result) => {
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
      formDataService.list(this.filters).then((result) => {
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
        let data = {}
        try {
          data = Object.assign({}, this.form, {
            content: JSON.parse(this.form.content)
          })
        } catch (e) {
          this.$error({ title: '数据格式异常', content: e.message })
          return false
        }
        this.formLoading = true
        formDataService.save(data).then(result => {
          if (result.code === 0) {
            this.formVisible = false
            this.search(false, true)
          }
          this.formLoading = false
        })
      })
    },
    add () {
      this.form = { content: '{}' }
      this.formVisible = true
    },
    edit (text, record) {
      this.form = Object.assign({}, record, {
        content: record.content ? JSON.stringify(record.content, null, 2) : ''
      })
      this.formVisible = true
    },
    show (text, record) {
      this.form = Object.assign({}, record, {
        content: record.content ? JSON.stringify(record.content, null, 2) : '暂无'
      })
      this.infoVisible = true
    }
  },
  created () {
    this.filters = RouteUtil.query2filter(this, { page: 1, pageSize: 5 })
    this.pagination = Object.assign({}, RouteUtil.pagination(this.filters), this.pagination)
  },
  mounted () {
    this.search(false, true)
    formDataService.config().then((result) => {
      this.config.ready = true
      if (result.code === 0) {
        Object.assign(this.config, result.data)
      }
    })
  }
}
</script>
