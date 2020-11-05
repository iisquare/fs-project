<template>
  <section>
    <a-card :bordered="false">
      <div class="table-page-search-wrapper">
        <a-form-model ref="filters" :model="filters" layout="inline">
          <a-row :gutter="48">
            <a-col :md="6" :sm="24">
              <a-form-model-item label="人员" prop="userId">
                <a-input v-model="filters.userId" placeholder="所属人员ID" :allowClear="true" />
              </a-form-model-item>
            </a-col>
            <a-col :md="6" :sm="24">
              <a-form-model-item label="名称" prop="name">
                <a-input v-model="filters.name" placeholder="" :allowClear="true" />
              </a-form-model-item>
            </a-col>
            <a-col :md="6" :sm="24">
              <a-form-model-item label="状态" prop="status">
                <a-select v-model="filters.status" placeholder="请选择" :allowClear="true">
                  <a-select-option v-for="(value, key) in config.status" :key="key" :value="key">{{ value }}</a-select-option>
                </a-select>
              </a-form-model-item>
            </a-col>
            <a-col :md="6" :sm="24">
              <a-button type="primary" @click="search(true, false)" :loading="loading">查询</a-button>
              <a-button style="margin-left: 8px" @click="() => this.$refs.filters.resetFields()">重置</a-button>
              <a-button @click.native="$router.go(-1)" :style="{ marginLeft: '8px' }">返回</a-button>
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
          <span slot="image" slot-scope="text, record">
            <img width="112" height="112" :src="record.face" />
          </span>
          <span slot="user" slot-scope="text, record">
            ID：{{ record.userIdInfo.id }}<br/>
            标识：{{ record.userIdInfo.serial }}<br/>
            名称：{{ record.userIdInfo.name }}<br/>
          </span>
          <span slot="action" slot-scope="text, record">
            <a-button-group>
              <a-button type="link" size="small" v-permit="'face:photo:'" @click="show(text, record)">查看</a-button>
              <a-button v-permit="'face:photo:modify'" type="link" size="small" @click="edit(text, record)">编辑</a-button>
            </a-button-group>
          </span>
        </a-table>
        <div :class="rows.length > 0 ? 'table-pagination-tools' : 'table-pagination-tools-empty'">
          <a-button icon="minus-circle" type="danger" @click="batchRemove" v-permit="'face:photo:delete'" :disabled="selection.selectedRows.length === 0">删除</a-button>
          <a-divider type="vertical" v-permit="'face:photo:add'" />
          <a-button icon="plus-circle" type="primary" @click="add" v-permit="'face:photo:add'">新增</a-button>
        </div>
      </div>
    </a-card>
    <!--展示界面-->
    <a-modal :title="'信息查看 - ' + form.id" v-model="infoVisible" :footer="null">
      <a-form-model :model="form" :loading="infoLoading" :label-col="{ span: 5 }" :wrapper-col="{ span: 12 }">
        <a-form-model-item label="人员ID">{{ form.userIdInfo ? form.userIdInfo.id : '' }}</a-form-model-item>
        <a-form-model-item label="人员标识">{{ form.userIdInfo ? form.userIdInfo.serial : '' }}</a-form-model-item>
        <a-form-model-item label="人员名称">{{ form.userIdInfo ? form.userIdInfo.name : '' }}</a-form-model-item>
        <a-form-model-item label="名称">{{ form.name }}</a-form-model-item>
        <a-form-model-item label="排序">{{ form.sort }}</a-form-model-item>
        <a-form-model-item label="封面">{{ form.cover ? '是' : '否' }}</a-form-model-item>
        <a-form-model-item label="状态">{{ form.statusText }}</a-form-model-item>
        <a-form-model-item label="人像"><a-textarea v-model="form.base64"></a-textarea></a-form-model-item>
        <a-form-model-item label="面部"><a-textarea v-model="form.face"></a-textarea></a-form-model-item>
        <a-form-model-item label="矩形区域"><a-textarea v-model="form.box"></a-textarea></a-form-model-item>
        <a-form-model-item label="方形区域"><a-textarea v-model="form.square"></a-textarea></a-form-model-item>
        <a-form-model-item label="关键点"><a-textarea v-model="form.landmark"></a-textarea></a-form-model-item>
        <a-form-model-item label="特征值"><a-textarea v-model="form.eigenvalue"></a-textarea></a-form-model-item>
        <a-form-model-item label="描述">{{ form.description }}</a-form-model-item>
        <a-form-model-item label="创建者">{{ form.createdUidName }}</a-form-model-item>
        <a-form-model-item label="创建时间">{{ form.createdTime|date }}</a-form-model-item>
        <a-form-model-item label="修改者">{{ form.updatedUidName }}</a-form-model-item>
        <a-form-model-item label="修改时间">{{ form.updatedTime|date }}</a-form-model-item>
      </a-form-model>
    </a-modal>
    <!--编辑界面-->
    <a-modal :title="'信息' + (form.id ? ('修改 - ' + form.id) : '添加')" v-model="formVisible" :confirmLoading="formLoading" :maskClosable="false" @ok="submit">
      <a-form-model ref="form" :model="form" :rules="rules" :label-col="{ span: 5 }" :wrapper-col="{ span: 12 }">
        <a-form-model-item label="人员" prop="userId">
          <a-input v-model="form.userId" auto-complete="off" placeholder="所属人员ID"></a-input>
        </a-form-model-item>
        <a-form-model-item label="名称" prop="name">
          <a-input v-model="form.name" auto-complete="off"></a-input>
        </a-form-model-item>
        <a-form-model-item label="排序">
          <a-input-number v-model="form.sort" :min="0" :max="200"></a-input-number>
        </a-form-model-item>
        <a-form-model-item label="选项">
          <a-checkbox :checked="!!form.cover" @click="() => this.form.cover = !this.form.cover">设置为封面</a-checkbox>
        </a-form-model-item>
        <a-form-model-item label="状态" prop="status">
          <a-select v-model="form.status" placeholder="请选择">
            <a-select-option v-for="(value, key) in config.status" :key="key" :value="key">{{ value }}</a-select-option>
          </a-select>
        </a-form-model-item>
        <a-form-model-item label="人像" prop="base64">
          <a-textarea v-model="form.base64" placeholder="图像Base64编码，可通过选择文件生成"></a-textarea>
        </a-form-model-item>
        <a-form-model-item label="上传">
          <a-upload
            accept="image/*"
            :withCredentials="true"
            :showUploadList="false"
            :before-upload="beforeUpload">
            <a-button icon="picture" type="primary">选择文件</a-button>
          </a-upload>
        </a-form-model-item>
        <a-form-model-item label="描述">
          <a-textarea v-model="form.description"></a-textarea>
        </a-form-model-item>
      </a-form-model>
    </a-modal>
  </section>
</template>

<script>
import RouteUtil from '@/utils/route'
import photoService from '@/service/face/photo'

export default {
  data () {
    return {
      filters: {},
      columns: [
        { title: 'ID', dataIndex: 'id' },
        { title: '图像', scopedSlots: { customRender: 'image' }, width: 145 },
        { title: '名称', dataIndex: 'name' },
        { title: '所属人员', scopedSlots: { customRender: 'user' } },
        { title: '封面', dataIndex: 'cover' },
        { title: '排序', dataIndex: 'sort' },
        { title: '状态', dataIndex: 'statusText' },
        { title: '操作', scopedSlots: { customRender: 'action' } }
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
        userId: [{ required: true, message: '请输入所属人员ID', trigger: 'blur' }],
        name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
        status: [{ required: true, message: '请选择状态', trigger: 'change' }],
        base64: [{ required: true, message: '请选择文件上传人像', trigger: 'blur' }]
      }
    }
  },
  methods: {
    beforeUpload (file) {
      const isJpgOrPng = file.type === 'image/jpeg' || file.type === 'image/png'
      if (!isJpgOrPng) {
        this.$message.error('仅支持JPG和PNG图像')
      }
      const isLt2M = file.size / 1024 / 1024 < 2
      if (!isLt2M) {
        this.$message.error('图片大小不能超过2MB!')
      }
      if (isJpgOrPng && isLt2M) {
        const reader = new FileReader()
        reader.onload = () => this.$set(this.form, 'base64', reader.result)
        reader.onerror = error => this.$message.error('读取图像失败：' + error)
        reader.readAsDataURL(file)
      }
      return false
    },
    batchRemove () {
      this.$confirm(this.selection.confirm(() => {
        this.loading = true
        photoService.delete(this.selection.selectedRowKeys, { success: true }).then((result) => {
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
      photoService.list(this.filters).then((result) => {
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
        photoService.save(this.form).then(result => {
          if (result.code === 0) {
            this.formVisible = false
            this.search(false, true)
          }
          this.formLoading = false
        })
      })
    },
    add () {
      this.form = {
        cover: false,
        userId: this.filters.userId
      }
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
    photoService.config().then((result) => {
      this.config.ready = true
      if (result.code === 0) {
        Object.assign(this.config, result.data)
      }
    })
  }
}
</script>
