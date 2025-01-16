<template>
  <section>
    <a-card :bordered="false">
      <div class="table-page-search-wrapper">
        <a-form-model ref="filters" :model="filters" layout="inline">
          <a-row :gutter="48">
            <a-col :md="6" :sm="24">
              <a-form-model-item label="帐号" prop="serial">
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
                <a-form-model-item label="角色" prop="roleIds">
                  <a-select
                    v-model="filters.roleIds"
                    showSearch
                    :filterOption="filterOption"
                    mode="multiple"
                    placeholder="请选择"
                    :allowClear="true">
                    <a-select-option v-for="item in config.roles" :key="item.id" :value="item.id">{{ item.name }}</a-select-option>
                  </a-select>
                </a-form-model-item>
              </a-col>
              <a-col :md="6" :sm="24">
                <a-form-model-item label="注册IP" prop="createdIp">
                  <a-input v-model="filters.createdIp" placeholder="" :allowClear="true" />
                </a-form-model-item>
              </a-col>
              <a-col :md="6" :sm="24">
                <a-form-model-item label="登录IP" prop="loginedIp">
                  <a-input v-model="filters.loginedIp" placeholder="" :allowClear="true" />
                </a-form-model-item>
              </a-col>
              <a-col :md="12" :sm="24">
                <a-form-model-item label="创建开始时间" prop="createdTimeBegin">
                  <s-date-picker
                    v-model="filters.createdTimeBegin"
                    :showTime="DateUtil.showTime(0)"
                    :format="DateUtil.dateFormat()"
                    placeholder="开始时间"
                  />
                </a-form-model-item>
              </a-col>
              <a-col :md="12" :sm="24">
                <a-form-model-item label="创建结束时间" prop="createdTimeEnd">
                  <s-date-picker
                    v-model="filters.createdTimeEnd"
                    :showTime="DateUtil.showTime(1)"
                    :format="DateUtil.dateFormat()"
                    placeholder="结束时间"
                  />
                </a-form-model-item>
              </a-col>
              <a-col :md="12" :sm="24">
                <a-form-model-item label="修改开始时间" prop="updatedTimeBegin">
                  <s-date-picker
                    v-model="filters.updatedTimeBegin"
                    :showTime="DateUtil.showTime(0)"
                    :format="DateUtil.dateFormat()"
                    placeholder="开始时间"
                  />
                </a-form-model-item>
              </a-col>
              <a-col :md="12" :sm="24">
                <a-form-model-item label="修改结束时间" prop="updatedTimeEnd">
                  <s-date-picker
                    v-model="filters.updatedTimeEnd"
                    :showTime="DateUtil.showTime(1)"
                    :format="DateUtil.dateFormat()"
                    placeholder="结束时间"
                  />
                </a-form-model-item>
              </a-col>
              <a-col :md="12" :sm="24">
                <a-form-model-item label="登录开始时间" prop="loginedTimeBegin">
                  <s-date-picker
                    v-model="filters.loginedTimeBegin"
                    :showTime="DateUtil.showTime(0)"
                    :format="DateUtil.dateFormat()"
                    placeholder="开始时间"
                  />
                </a-form-model-item>
              </a-col>
              <a-col :md="12" :sm="24">
                <a-form-model-item label="登录结束时间" prop="loginedTimeEnd">
                  <s-date-picker
                    v-model="filters.loginedTimeEnd"
                    :showTime="DateUtil.showTime(1)"
                    :format="DateUtil.dateFormat()"
                    placeholder="结束时间"
                  />
                </a-form-model-item>
              </a-col>
              <a-col :md="12" :sm="24">
                <a-form-model-item label="锁定开始时间" prop="lockedTimeBegin">
                  <s-date-picker
                    v-model="filters.lockedTimeBegin"
                    :showTime="DateUtil.showTime(0)"
                    :format="DateUtil.dateFormat()"
                    placeholder="开始时间"
                  />
                </a-form-model-item>
              </a-col>
              <a-col :md="12" :sm="24">
                <a-form-model-item label="锁定结束时间" prop="lockedTimeEnd">
                  <s-date-picker
                    v-model="filters.lockedTimeEnd"
                    :showTime="DateUtil.showTime(1)"
                    :format="DateUtil.dateFormat()"
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
          <span slot="roles" slot-scope="text, record">
            <a-tag v-for="item in record.roles" :key="item.id">{{ item.name }}</a-tag>
          </span>
          <span slot="action" slot-scope="text, record">
            <a-button-group>
              <a-button type="link" size="small" v-permit="'member:user:'" @click="show(text, record)">查看</a-button>
              <a-button v-if="record.deletedTime == 0" v-permit="'member:user:modify'" type="link" size="small" @click="edit(text, record)">编辑</a-button>
              <a-button v-if="record.deletedTime == 0" v-permit="'member:user:role'" type="link" size="small" @click="editTree('role', record.id)">角色</a-button>
            </a-button-group>
          </span>
        </a-table>
        <div :class="rows.length > 0 ? 'table-pagination-tools' : 'table-pagination-tools-empty'">
          <a-button icon="minus-circle" type="danger" @click="batchRemove" v-permit="'member:user:delete'" :disabled="selection.selectedRows.length === 0">删除</a-button>
          <a-divider type="vertical" v-permit="'member:user:add'" />
          <a-button icon="plus-circle" type="primary" @click="add" v-permit="'member:user:add'">新增</a-button>
        </div>
      </div>
    </a-card>
    <!--展示界面-->
    <a-modal :title="'信息查看 - ' + form.id" v-model="infoVisible" :footer="null">
      <a-form-model :model="form" :loading="infoLoading" :label-col="{ span: 5 }" :wrapper-col="{ span: 18 }">
        <a-form-model-item label="账号">{{ form.serial }}</a-form-model-item>
        <a-form-model-item label="名称">{{ form.name }}</a-form-model-item>
        <a-form-model-item label="排序">{{ form.sort }}</a-form-model-item>
        <a-form-model-item label="状态">{{ form.statusText }}</a-form-model-item>
        <a-form-model-item label="描述">{{ form.description }}</a-form-model-item>
        <a-form-model-item label="创建者">{{ form.createdUserInfo?.name }}</a-form-model-item>
        <a-form-model-item label="创建时间">{{ form.createdTime|date }}</a-form-model-item>
        <a-form-model-item label="注册IP">{{ form.createdIp }}</a-form-model-item>
        <a-form-model-item label="修改者">{{ form.updatedUserInfo?.name }}</a-form-model-item>
        <a-form-model-item label="修改时间">{{ form.updatedTime|date }}</a-form-model-item>
        <a-form-model-item label="删除者">{{ form.deletedUserInfo?.name }}</a-form-model-item>
        <a-form-model-item label="删除时间">{{ form.deletedTime|date }}</a-form-model-item>
        <a-form-model-item label="登录IP">{{ form.loginedIp }}</a-form-model-item>
        <a-form-model-item label="登录时间">{{ form.loginedTime|date }}</a-form-model-item>
        <a-form-model-item label="锁定时间">{{ form.lockedTime|date }}</a-form-model-item>
      </a-form-model>
    </a-modal>
    <!--编辑界面-->
    <a-modal :title="'信息' + (form.id ? ('修改 - ' + form.id) : '添加')" v-model="formVisible" :confirmLoading="formLoading" :maskClosable="false" @ok="submit">
      <a-form-model ref="form" :model="form" :rules="rules" :label-col="{ span: 5 }" :wrapper-col="{ span: 18 }">
        <a-form-model-item label="账号" prop="serial">
          <a-input v-model="form.serial" auto-complete="off" :disabled="form.id ? true : false"></a-input>
        </a-form-model-item>
        <a-form-model-item label="名称" prop="name">
          <a-input v-model="form.name" auto-complete="off"></a-input>
        </a-form-model-item>
        <a-form-model-item label="密码">
          <a-input v-model="form.password" auto-complete="off" placeholder="留空时不对密码做任何处理"></a-input>
        </a-form-model-item>
        <a-form-model-item label="排序">
          <a-input-number v-model="form.sort" :min="0" :max="200"></a-input-number>
        </a-form-model-item>
        <a-form-model-item label="状态" prop="status">
          <a-select v-model="form.status" placeholder="请选择">
            <a-select-option v-for="(value, key) in config.status" :key="key" :value="key">{{ value }}</a-select-option>
          </a-select>
        </a-form-model-item>
        <a-form-model-item label="锁定">
          <s-date-picker v-model="form.lockedTime" :format="DateUtil.dateFormat()" placeholder="选择日期时间"></s-date-picker>
        </a-form-model-item>
        <a-form-model-item label="描述">
          <a-textarea v-model="form.description" />
        </a-form-model-item>
      </a-form-model>
    </a-modal>
    <!--角色分配-->
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
import userService from '@/service/member/user'

export default {
  data () {
    return {
      DateUtil,
      advanced: false,
      filters: {},
      columns: [
        { title: 'ID', dataIndex: 'id' },
        { title: '账号', dataIndex: 'serial' },
        { title: '名称', dataIndex: 'name' },
        { title: '排序', dataIndex: 'sort' },
        { title: '状态', dataIndex: 'statusText' },
        { title: '角色', dataIndex: 'roles', scopedSlots: { customRender: 'roles' } },
        { title: '锁定时间', dataIndex: 'lockedTime', customRender: DateUtil.dateRender },
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
        roles: []
      },
      infoVisible: false,
      infoLoading: false,
      formVisible: false,
      formLoading: false,
      form: {},
      rules: {
        serial: [{ required: true, message: '请输入账号', trigger: 'blur' }],
        name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
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
        title: { role: '角色分配' }[type],
        rows: [],
        visible: true,
        loading: true
      })
      this.tree.selection.clear()
      userService.tree({ id: id, type: type }).then((result) => {
        if (result.code === 0) {
          Object.assign(this.tree, {
            rows: this.config.roles,
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
    }
  },
  created () {
    this.filters = RouteUtil.query2filter(this, { page: 1, pageSize: 5, roleIds: [] })
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
