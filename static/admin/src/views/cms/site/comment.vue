<template>
  <section>
    <a-card :bordered="false">
      <div class="table-page-search-wrapper">
        <a-form-model ref="filters" :model="filters" layout="inline">
          <a-row :gutter="48">
            <a-col :md="6" :sm="24">
              <a-form-model-item label="标识" prop="id">
                <a-input v-model="filters.id" placeholder="" :allowClear="true" />
              </a-form-model-item>
            </a-col>
            <a-col :md="6" :sm="24">
              <a-form-model-item label="父级" prop="parentId">
                <a-input v-model="filters.parentId" placeholder="" :allowClear="true" />
              </a-form-model-item>
            </a-col>
            <a-col :md="6" :sm="24">
              <a-form-model-item label="层级" prop="levelId">
                <a-input v-model="filters.levelId" placeholder="" :allowClear="true" />
              </a-form-model-item>
            </a-col>
            <a-col :md="6" :sm="24">
              <a-button type="primary" @click="search(true, false)" :loading="loading">查询</a-button>
              <a-button style="margin-left: 8px" @click="() => this.$refs.filters.resetFields()">重置</a-button>
            </a-col>
          </a-row>
          <a-row :gutter="48">
            <a-col :md="6" :sm="24">
              <a-form-model-item label="引用类型" prop="referType">
                <a-input v-model="filters.referType" placeholder="" :allowClear="true" />
              </a-form-model-item>
            </a-col>
            <a-col :md="6" :sm="24">
              <a-form-model-item label="引用标识" prop="referId">
                <a-input v-model="filters.referId" placeholder="" :allowClear="true" />
              </a-form-model-item>
            </a-col>
            <a-col :md="6" :sm="24">
              <a-form-model-item label="发布开始时间" prop="publishTimeBegin">
                <s-date-picker
                  v-model="filters.publishTimeBegin"
                  :showTime="DateUtil.showTime(0)"
                  :format="DateUtil.dateFormat()"
                  placeholder="开始时间"
                />
              </a-form-model-item>
            </a-col>
            <a-col :md="6" :sm="24">
              <a-form-model-item label="发布结束时间" prop="publishTimeEnd">
                <s-date-picker
                  v-model="filters.publishTimeEnd"
                  :showTime="DateUtil.showTime(1)"
                  :format="DateUtil.dateFormat()"
                  placeholder="结束时间"
                />
              </a-form-model-item>
            </a-col>
          </a-row>
          <a-row :gutter="48">
            <a-col :md="6" :sm="24">
              <a-form-model-item label="页面地址" prop="url">
                <a-input v-model="filters.url" placeholder="" :allowClear="true" />
              </a-form-model-item>
            </a-col>
            <a-col :md="6" :sm="24">
              <a-form-model-item label="浏览器标识" prop="ua">
                <a-input v-model="filters.ua" placeholder="" :allowClear="true" />
              </a-form-model-item>
            </a-col>
            <a-col :md="6" :sm="24">
              <a-form-model-item label="客户端IP" prop="ip">
                <a-input v-model="filters.ip" placeholder="" :allowClear="true" />
              </a-form-model-item>
            </a-col>
            <a-col :md="6" :sm="24">
              <a-form-model-item label="状态" prop="status">
                <a-select v-model="filters.status" placeholder="请选择" :allowClear="true">
                  <a-select-option v-for="(value, key) in config.status" :key="key" :value="key">{{ value }}</a-select-option>
                </a-select>
              </a-form-model-item>
            </a-col>
          </a-row>
          <a-row :gutter="48">
            <a-col :md="6" :sm="24">
              <a-form-model-item label="发布者" prop="publishUid">
                <a-input v-model="filters.publishUid" placeholder="" :allowClear="true" />
              </a-form-model-item>
            </a-col>
            <a-col :md="6" :sm="24">
              <a-form-model-item label="审核者" prop="auditUid">
                <a-input v-model="filters.ua" placeholder="" :allowClear="true" />
              </a-form-model-item>
            </a-col>
            <a-col :md="6" :sm="24">
              <a-form-model-item label="审核标签" prop="auditTag">
                <a-select v-model="filters.auditTag" mode="tags" placeholder="请选择" :allowClear="true">
                  <a-select-option v-for="(item, index) in config.auditTag" :key="index" :value="item.value">{{ item.label }}</a-select-option>
                </a-select>
              </a-form-model-item>
            </a-col>
            <a-col :md="6" :sm="24">
              <a-form-model-item label="排序" prop="sort">
                <a-input v-model="filters.sort" placeholder="" :allowClear="true" />
              </a-form-model-item>
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
          <span slot="refer" slot-scope="text, record">
            <p>类型：{{ record.referType }}</p>
            <p>标识：{{ record.referId }}</p>
            <p>摘要：{{ record.referIdDigest }}</p>
          </span>
          <span slot="action" slot-scope="text, record">
            <a-button-group>
              <a-button type="link" size="small" v-permit="'cms:comment:'" @click="show(text, record)">查看</a-button>
              <a-button v-permit="'cms:comment:audit'" type="link" size="small" @click="audit(text, record)">审核</a-button>
            </a-button-group>
          </span>
        </a-table>
        <div :class="rows.length > 0 ? 'table-pagination-tools' : 'table-pagination-tools-empty'">
          <a-button icon="minus-circle" type="danger" @click="batchRemove" v-permit="'cms:comment:delete'" :disabled="selection.selectedRows.length === 0">删除</a-button>
        </div>
      </div>
    </a-card>
    <!--展示界面-->
    <a-modal :title="'信息查看 - ' + form.id" v-model="infoVisible" :footer="null">
      <a-form-model :model="form" :loading="infoLoading" :label-col="{ span: 5 }" :wrapper-col="{ span: 18 }">
        <a-form-model-item label="标识">{{ form.id }}</a-form-model-item>
        <a-form-model-item label="父级">{{ form.parentId }}</a-form-model-item>
        <a-form-model-item label="层级">{{ form.levelId }}</a-form-model-item>
        <a-form-model-item label="来源类型">{{ form.referType }}</a-form-model-item>
        <a-form-model-item label="来源标识">{{ form.referId }}</a-form-model-item>
        <a-form-model-item label="来源摘要">{{ form.referIdDigest }}</a-form-model-item>
        <a-form-model-item label="评论内容">{{ form.content }}</a-form-model-item>
        <a-form-model-item label="页面地址">{{ form.url }}</a-form-model-item>
        <a-form-model-item label="浏览器标识">{{ form.ua }}</a-form-model-item>
        <a-form-model-item label="客户端IP">{{ form.ip }}</a-form-model-item>
        <a-form-model-item label="赞成数量">{{ form.countApprove }}</a-form-model-item>
        <a-form-model-item label="反对数量">{{ form.countOppose }}</a-form-model-item>
        <a-form-model-item label="状态">{{ form.statusText }}</a-form-model-item>
        <a-form-model-item label="审核标签">{{ form.auditTag }}</a-form-model-item>
        <a-form-model-item label="审核意见">{{ form.auditReason }}</a-form-model-item>
        <a-form-model-item label="发布者">{{ form.publishUidName }}</a-form-model-item>
        <a-form-model-item label="发布时间">{{ form.publishTime|date }}</a-form-model-item>
        <a-form-model-item label="审核者">{{ form.auditUidName }}</a-form-model-item>
        <a-form-model-item label="审核时间">{{ form.auditTime|date }}</a-form-model-item>
      </a-form-model>
    </a-modal>
    <!--编辑界面-->
    <a-modal title="内容审核" v-model="formVisible" :confirmLoading="formLoading" :maskClosable="false" @ok="submit">
      <a-form-model ref="form" :model="form" :rules="rules" :label-col="{ span: 5 }" :wrapper-col="{ span: 18 }">
        <a-form-model-item label="标识">{{ form.id }}</a-form-model-item>
        <a-form-model-item label="引用类型">{{ form.referType }}</a-form-model-item>
        <a-form-model-item label="引用标识">{{ form.referId }}</a-form-model-item>
        <a-form-model-item label="引用摘要">{{ form.referIdDigest }}</a-form-model-item>
        <a-form-model-item label="评论内容">{{ form.content }}</a-form-model-item>
        <a-form-model-item label="评论状态" prop="status">
          <a-select v-model="form.status" placeholder="请选择">
            <a-select-option v-for="(value, key) in config.status" :key="key" :value="key">{{ value }}</a-select-option>
          </a-select>
        </a-form-model-item>
        <a-form-model-item label="审核标识" prop="auditTag">
          <a-select v-model="form.auditTag" mode="tags" placeholder="请选择" :allowClear="true">
            <a-select-option v-for="(item, index) in config.auditTag" :key="index" :value="item.value">{{ item.label }}</a-select-option>
          </a-select>
        </a-form-model-item>
        <a-form-model-item label="审核意见" prop="auditReason">
          <a-textarea v-model="form.auditReason" />
        </a-form-model-item>
      </a-form-model>
    </a-modal>
  </section>
</template>

<script>
import DateUtil from '@/utils/date'
import RouteUtil from '@/utils/route'
import commentService from '@/service/cms/comment'

export default {
  data () {
    return {
      DateUtil,
      filters: {},
      columns: [
        { title: 'ID', dataIndex: 'id' },
        { title: '父级', dataIndex: 'parentId' },
        { title: '层级', dataIndex: 'levelId' },
        { title: '引用', dataIndex: 'refer', scopedSlots: { customRender: 'refer' } },
        { title: '评论内容', dataIndex: 'content' },
        { title: '赞成数', dataIndex: 'countApprove' },
        { title: '反对数', dataIndex: 'countOppose' },
        { title: '状态', dataIndex: 'statusText' },
        { title: '发布时间', dataIndex: 'auditTime', customRender: DateUtil.dateRender, width: 170 },
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
        auditTag: [{ required: true, message: '请选择审核标签', trigger: 'change' }],
        auditReason: [{ required: true, message: '请输入审核意见', trigger: 'blur' }]
      }
    }
  },
  methods: {
    batchRemove () {
      this.$confirm(this.selection.confirm(() => {
        this.loading = true
        commentService.delete(this.selection.selectedRowKeys, { success: true }).then((result) => {
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
      commentService.list(this.filters).then((result) => {
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
        commentService.audit(this.form).then(result => {
          if (result.code === 0) {
            this.formVisible = false
            this.search(false, true)
          }
          this.formLoading = false
        })
      })
    },
    audit (text, record) {
      this.form = Object.assign({}, record, {
        status: record.status + '',
        auditTag: record.auditTag ? record.auditTag.split(',') : []
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
  created () {
    this.filters = RouteUtil.query2filter(this, { page: 1, pageSize: 5 })
    this.pagination = Object.assign({}, RouteUtil.pagination(this.filters), this.pagination)
  },
  mounted () {
    this.search(false, true)
    commentService.config().then((result) => {
      this.config.ready = true
      if (result.code === 0) {
        Object.assign(this.config, result.data)
      }
    })
  }
}
</script>
