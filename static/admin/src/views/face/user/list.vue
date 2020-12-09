<template>
  <section>
    <a-card :bordered="false">
      <div class="table-page-search-wrapper">
        <a-form-model ref="filters" :model="filters" layout="inline">
          <a-row :gutter="48">
            <a-col :md="6" :sm="24">
              <a-form-model-item label="标识" prop="serial">
                <a-input v-model="filters.serial" placeholder="" :allowClear="true" />
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
            <template v-if="advanced">
              <a-col :md="6" :sm="24">
                <a-form-model-item label="ID" prop="id">
                  <a-input v-model="filters.id" placeholder="" :allowClear="true" />
                </a-form-model-item>
              </a-col>
              <a-col :md="12" :sm="24">
                <a-form-model-item label="分组" prop="groupIds">
                  <a-select
                    v-model="filters.groupIds"
                    showSearch
                    :filterOption="filterOption"
                    mode="multiple"
                    placeholder="请选择"
                    :allowClear="true">
                    <a-select-option v-for="item in config.groups" :key="item.id" :value="item.id">{{ item.name }}</a-select-option>
                  </a-select>
                </a-form-model-item>
              </a-col>
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
            </template>
            <a-col :md="!advanced && 6 || 24" :sm="24">
              <span class="table-page-search-submitButtons" :style="advanced && { float: 'right', overflow: 'hidden' } || {} ">
                <a-button type="primary" @click="search(true, false)" :loading="loading">查询</a-button>
                <a-button style="margin-left: 8px" @click="() => this.$refs.filters.resetFields()">重置</a-button>
                <a @click="toggleAdvanced" style="margin-left: 8px">
                  {{ advanced ? '收起' : '展开' }}
                  <a-icon :type="advanced ? 'up' : 'down'"/>
                </a>
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
          <span slot="groups" slot-scope="text, record">
            <a-tag v-for="item in record.groups" :key="item.id">{{ item.name }}</a-tag>
          </span>
          <span slot="action" slot-scope="text, record">
            <a-button-group>
              <a-button type="link" size="small" v-permit="'face:user:'" @click="show(text, record)">查看</a-button>
              <a-button v-if="record.status != -1" v-permit="'face:user:modify'" type="link" size="small" @click="edit(text, record)">编辑</a-button>
              <a-button v-if="record.status != -1" v-permit="'face:user:group'" type="link" size="small" @click="editTree('group', record.id)">分组</a-button>
              <a-button v-if="record.status != -1" v-permit="'face:photo:'" type="link" size="small" @click="photo(text, record)">人像</a-button>
            </a-button-group>
          </span>
        </a-table>
        <div :class="rows.length > 0 ? 'table-pagination-tools' : 'table-pagination-tools-empty'">
          <a-button icon="minus-circle" type="danger" @click="batchRemove" v-permit="'face:user:delete'" :disabled="selection.selectedRows.length === 0">删除</a-button>
          <a-divider type="vertical" v-permit="'face:user:add'" />
          <a-button icon="plus-circle" type="primary" @click="add" v-permit="'face:user:add'">新增</a-button>
        </div>
      </div>
    </a-card>
    <!--展示界面-->
    <a-modal :title="'信息查看 - ' + form.id" v-model="infoVisible" :footer="null">
      <a-form-model :model="form" :loading="infoLoading" :label-col="{ span: 5 }" :wrapper-col="{ span: 12 }">
        <a-form-model-item label="标识">{{ form.serial }}</a-form-model-item>
        <a-form-model-item label="名称">{{ form.name }}</a-form-model-item>
        <a-form-model-item label="排序">{{ form.sort }}</a-form-model-item>
        <a-form-model-item label="状态">{{ form.statusText }}</a-form-model-item>
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
        <a-form-model-item label="标识" prop="serial">
          <a-input v-model="form.serial" auto-complete="off"></a-input>
        </a-form-model-item>
        <a-form-model-item label="名称" prop="name">
          <a-input v-model="form.name" auto-complete="off"></a-input>
        </a-form-model-item>
        <a-form-model-item label="排序">
          <a-input-number v-model="form.sort" :min="0" :max="200"></a-input-number>
        </a-form-model-item>
        <a-form-model-item label="状态" prop="status">
          <a-select v-model="form.status" placeholder="请选择">
            <a-select-option v-for="(value, key) in config.status" :key="key" :value="key">{{ value }}</a-select-option>
          </a-select>
        </a-form-model-item>
        <a-form-model-item label="描述">
          <a-textarea v-model="form.description"></a-textarea>
        </a-form-model-item>
      </a-form-model>
    </a-modal>
    <!--所属分组-->
    <a-modal :title="tree.title + '[' + tree.id + ']'" v-model="tree.visible" :confirmLoading="tree.loading" :maskClosable="false" @ok="saveTree">
      <a-table
        :columns="tree.columns"
        :rowKey="record => record.id"
        :dataSource="tree.rows"
        :pagination="false"
        :loading="tree.loading"
        :rowSelection="tree.selection"
        :bordered="true"
      >
      </a-table>
    </a-modal>
  </section>
</template>

<script>
import DateUtil from '@/utils/date'
import RouteUtil from '@/utils/route'
import userService from '@/service/face/user'

export default {
  data () {
    return {
      advanced: false,
      filters: {},
      columns: [
        { title: 'ID', dataIndex: 'id' },
        { title: '标识', dataIndex: 'serial' },
        { title: '名称', dataIndex: 'name' },
        { title: '排序', dataIndex: 'sort' },
        { title: '状态', dataIndex: 'statusText' },
        { title: '分组', dataIndex: 'groups', scopedSlots: { customRender: 'groups' } },
        { title: '操作', scopedSlots: { customRender: 'action' } }
      ],
      selection: RouteUtil.selection(),
      pagination: {},
      rows: [],
      loading: false,
      config: {
        ready: false,
        defaultPassword: '',
        status: [],
        groups: []
      },
      infoVisible: false,
      infoLoading: false,
      formVisible: false,
      formLoading: false,
      form: {},
      rules: {
        serial: [{ required: true, message: '请输入人员标识', trigger: 'blur' }],
        name: [{ required: true, message: '请输入人员名称', trigger: 'blur' }],
        status: [{ required: true, message: '请选择状态', trigger: 'change' }]
      },
      tree: {
        id: '',
        type: '',
        title: '',
        visible: false,
        loading: false,
        selection: RouteUtil.selection(),
        rows: [],
        columns: [
          { title: 'ID', dataIndex: 'id' },
          { title: '名称', dataIndex: 'name' },
          { title: '状态', dataIndex: 'statusText' }
        ]
      }
    }
  },
  methods: {
    filterOption: RouteUtil.filterOption,
    batchRemove () {
      this.$confirm(this.selection.confirm(() => {
        this.loading = true
        userService.delete(this.selection.selectedRowKeys, { success: true }).then((result) => {
          if (result.code === 0) {
            this.search(false, true)
          } else {
            this.loading = false
          }
        })
      }))
    },
    dateRender (text, record, index) {
      return DateUtil.format(text)
    },
    tableChange (pagination, filters, sorter) {
      this.pagination = RouteUtil.paginationChange(this.pagination, pagination)
      this.search(true, true)
    },
    dateFormat () {
      return DateUtil.dateMomentFormat()
    },
    showTime (indexRange) {
      return { format: DateUtil.timeMomentFormat(), defaultValue: DateUtil.timeMomentRange()[indexRange] }
    },
    toggleAdvanced () {
      this.advanced = !this.advanced
    },
    search (filter2query, pagination) {
      this.selection.clear()
      Object.assign(this.filters, RouteUtil.paginationData(this.pagination, pagination))
      filter2query && RouteUtil.filter2query(this, this.filters)
      this.loading = true
      userService.list(this.filters).then((result) => {
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
        userService.save(this.form).then(result => {
          if (result.code === 0) {
            this.formVisible = false
            this.search(false, true)
          }
          this.formLoading = false
        })
      })
    },
    add () {
      this.form = { password: this.config.defaultPassword }
      this.formVisible = true
    },
    edit (text, record) {
      this.form = Object.assign({}, record, {
        status: record.status + '',
        lockedTime: DateUtil.format(record.lockedTime)
      })
      this.formVisible = true
    },
    show (text, record) {
      this.form = Object.assign({}, record, {
        lockedTime: DateUtil.format(record.lockedTime),
        description: record.description ? record.description : '暂无'
      })
      this.infoVisible = true
    },
    editTree (type, id) {
      Object.assign(this.tree, {
        id: id,
        type: type,
        title: { group: '所属分组' }[type],
        rows: [],
        visible: true,
        loading: true
      })
      this.tree.selection.clear()
      userService.tree({ id: id, type: type }).then((result) => {
        if (result.code === 0) {
          Object.assign(this.tree, {
            rows: this.config.groups,
            loading: false
          })
          this.tree.selection.selectedRowKeys = result.data.checked
        }
      })
    },
    saveTree () {
      if (this.tree.loading) return false
      this.tree.loading = true
      userService.tree({
        id: this.tree.id,
        type: this.tree.type,
        bids: this.tree.selection.selectedRowKeys }).then(result => {
        if (result.code === 0) {
          this.tree.visible = false
          this.search(false, true)
        }
        this.tree.loading = true
      })
    },
    photo (text, record) {
      this.$router.push({
        path: '/face/photo/list',
        query: {
          [RouteUtil.filterKey]: RouteUtil.encode({ userId: record.id })
        }
      })
    }
  },
  created () {
    this.filters = RouteUtil.query2filter(this, { page: 1, pageSize: 5, groupIds: [] })
    this.pagination = Object.assign({}, RouteUtil.pagination(this.filters), this.pagination)
  },
  mounted () {
    this.search(false, true)
    userService.config().then((result) => {
      this.config.ready = true
      if (result.code === 0) {
        Object.assign(this.config, result.data)
      }
    })
  }
}
</script>
