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
              <a-form-model-item label="名称" prop="name">
                <a-input v-model="filters.name" placeholder="" :allowClear="true" />
              </a-form-model-item>
            </a-col>
            <a-col :md="6" :sm="24">
              <a-form-model-item label="归类" prop="bucket">
                <a-input v-model="filters.bucket" placeholder="" :allowClear="true" />
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
              <a-form-model-item label="创建开始时间" prop="createdTimeBegin">
                <s-date-picker
                  v-model="filters.createdTimeBegin"
                  :showTime="DateUtil.showTime(0)"
                  :format="DateUtil.dateFormat()"
                  placeholder="开始时间"
                />
              </a-form-model-item>
            </a-col>
            <a-col :md="6" :sm="24">
              <a-form-model-item label="创建结束时间" prop="createdTimeEnd">
                <s-date-picker
                  v-model="filters.createdTimeEnd"
                  :showTime="DateUtil.showTime(1)"
                  :format="DateUtil.dateFormat()"
                  placeholder="结束时间"
                />
              </a-form-model-item>
            </a-col>
            <a-col :md="6" :sm="24">
              <a-form-model-item label="修改开始时间" prop="updatedTimeBegin">
                <s-date-picker
                  v-model="filters.updatedTimeBegin"
                  :showTime="DateUtil.showTime(0)"
                  :format="DateUtil.dateFormat()"
                  placeholder="开始时间"
                />
              </a-form-model-item>
            </a-col>
            <a-col :md="6" :sm="24">
              <a-form-model-item label="修改结束时间" prop="updatedTimeEnd">
                <s-date-picker
                  v-model="filters.updatedTimeEnd"
                  :showTime="DateUtil.showTime(1)"
                  :format="DateUtil.dateFormat()"
                  placeholder="结束时间"
                />
              </a-form-model-item>
            </a-col>
          </a-row>
          <a-row :gutter="48">
            <a-col :md="6" :sm="24">
              <a-form-model-item label="类型" prop="type">
                <a-input v-model="filters.type" placeholder="" :allowClear="true" />
              </a-form-model-item>
            </a-col>
            <a-col :md="4" :sm="24">
              <a-form-model-item label="后缀" prop="suffix">
                <a-input v-model="filters.suffix" placeholder="" :allowClear="true" />
              </a-form-model-item>
            </a-col>
            <a-col :md="4" :sm="24">
              <a-form-model-item label="最小文件大小" prop="sizeBegin">
                <a-input-number v-model="filters.sizeBegin" placeholder="" />
              </a-form-model-item>
            </a-col>
            <a-col :md="4" :sm="24">
              <a-form-model-item label="最大文件大小" prop="sizeEnd">
                <a-input-number v-model="filters.sizeEnd" placeholder="" />
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
              <a-button type="link" size="small" v-permit="'file:archive:'" @click="show(text, record)">查看</a-button>
              <a-button v-permit="'file:archive:save'" type="link" size="small" @click="edit(text, record)">编辑</a-button>
              <a-button type="link" size="small">
                <a v-permit="'file:archive:download'" :href="proxyService.blank('File', '/archive/download', { id: record.id })" target="_blank">下载</a>
              </a-button>
            </a-button-group>
          </span>
        </a-table>
        <div :class="rows.length > 0 ? 'table-pagination-tools' : 'table-pagination-tools-empty'">
          <a-button icon="minus-circle" type="danger" @click="batchRemove" v-permit="'file:archive:delete'" :disabled="selection.selectedRows.length === 0">删除</a-button>
          <a-divider type="vertical" v-permit="'file:archive:upload'" />
          <a-upload
            v-permit="'file:archive:upload'"
            :action="proxyService.uploadAction()"
            :data="file => proxyService.uploadData('File', '/archive/upload', { bucket: filters.bucket || '' })"
            :withCredentials="true"
            :showUploadList="false"
            @change="uploadChange">
            <a-button icon="cloud-upload" type="primary" :loading="uploading">上传</a-button>
          </a-upload>
        </div>
      </div>
    </a-card>
    <!--展示界面-->
    <a-modal :title="'信息查看 - ' + form.id" v-model="infoVisible" :footer="null">
      <a-form-model :model="form" :loading="infoLoading" :label-col="{ span: 5 }" :wrapper-col="{ span: 15 }">
        <a-form-model-item label="文件标识">{{ form.id }}</a-form-model-item>
        <a-form-model-item label="文件名称">{{ form.name }}</a-form-model-item>
        <a-form-model-item label="存储分类">{{ form.bucket }}</a-form-model-item>
        <a-form-model-item label="存储目录">{{ form.dir }}</a-form-model-item>
        <a-form-model-item label="文件后缀">{{ form.suffix }}</a-form-model-item>
        <a-form-model-item label="文件类型">{{ form.type }}</a-form-model-item>
        <a-form-model-item label="文件大小">{{ form.size }}</a-form-model-item>
        <a-form-model-item label="头部摘要">{{ form.digest }}</a-form-model-item>
        <a-form-model-item label="内容校验">{{ form.hash }}</a-form-model-item>
        <a-form-model-item label="状态">{{ form.statusText }}</a-form-model-item>
        <a-form-model-item label="创建者">{{ form.createdUidName }}</a-form-model-item>
        <a-form-model-item label="创建时间">{{ form.createdTime|date }}</a-form-model-item>
        <a-form-model-item label="修改者">{{ form.updatedUidName }}</a-form-model-item>
        <a-form-model-item label="修改时间">{{ form.updatedTime|date }}</a-form-model-item>
      </a-form-model>
    </a-modal>
    <!--编辑界面-->
    <a-modal :title="'信息' + (form.id ? ('修改 - ' + form.id) : '添加')" v-model="formVisible" :confirmLoading="formLoading" :maskClosable="false" @ok="submit">
      <a-form-model ref="form" :model="form" :rules="rules" :label-col="{ span: 5 }" :wrapper-col="{ span: 18 }">
        <a-form-model-item label="ID" prop="id">{{ form.id }}</a-form-model-item>
        <a-form-model-item label="名称" prop="name">{{ form.name }}</a-form-model-item>
        <a-form-model-item label="状态" prop="status">
          <a-select v-model="form.status" placeholder="请选择">
            <a-select-option v-for="(value, key) in config.status" :key="key" :value="key">{{ value }}</a-select-option>
          </a-select>
        </a-form-model-item>
      </a-form-model>
    </a-modal>
  </section>
</template>

<script>
import UIUtil from '@/utils/ui'
import DateUtil from '@/utils/date'
import RouteUtil from '@/utils/route'
import archiveService from '@/service/file/archive'
import proxyService from '@/service/admin/proxy'

export default {
  data () {
    return {
      DateUtil,
      proxyService,
      advanced: false,
      filters: {},
      columns: [
        { title: 'ID', dataIndex: 'id', width: 280 },
        { title: '名称', dataIndex: 'name' },
        { title: '归类', dataIndex: 'bucket' },
        { title: '类型', dataIndex: 'type' },
        { title: '大小', dataIndex: 'size', customRender: UIUtil.prettyFileSize },
        { title: '状态', dataIndex: 'statusText', width: 70 },
        { title: '修改时间', dataIndex: 'updatedTime', customRender: DateUtil.dateRender, width: 170 },
        { title: '操作', scopedSlots: { customRender: 'action' }, width: 165 }
      ],
      selection: RouteUtil.selection({ type: 'radio' }),
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
        status: [{ required: true, message: '请选择状态', trigger: 'change' }]
      },
      uploading: false
    }
  },
  methods: {
    uploadChange (uploader) {
      proxyService.uploadChange(this, uploader, result => {
        this.search(false, true)
        this.show(uploader.file, result.data)
      })
    },
    batchRemove () {
      this.$confirm(this.selection.confirm(() => {
        this.loading = true
        archiveService.delete(this.selection.selectedRowKeys[0], { success: true }).then((result) => {
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
      archiveService.list(this.filters).then((result) => {
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
        archiveService.save(this.form).then(result => {
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
      this.form = Object.assign({}, record, {
        status: record.status + ''
      })
      this.formVisible = true
    },
    show (text, record) {
      this.form = Object.assign({}, record, {
        size: UIUtil.prettyFileSize(record.size)
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
    archiveService.config().then((result) => {
      this.config.ready = true
      if (result.code === 0) {
        Object.assign(this.config, result.data)
      }
    })
  }
}
</script>
