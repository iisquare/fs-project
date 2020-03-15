<template>
  <a-card :bordered="false">
    <div class="table-page-search-wrapper">
      <a-form layout="inline">
        <a-row :gutter="48">
          <a-col :md="6" :sm="24">
            <a-form-item label="帐号">
              <a-input v-model="filters.serial" placeholder=""/>
            </a-form-item>
          </a-col>
          <a-col :md="6" :sm="24">
            <a-form-item label="名称">
              <a-input v-model="filters.name" placeholder=""/>
            </a-form-item>
          </a-col>
          <a-col :md="6" :sm="24">
            <a-form-item label="状态">
              <a-select v-model="filters.status" placeholder="请选择" :allowClear="true">
                <a-select-option v-for="(value, key) in config.status" :key="key" :value="key">{{ value }}</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <template v-if="advanced">
            <a-col :md="6" :sm="24">
              <a-form-item label="ID">
                <a-input v-model="filters.id" placeholder=""/>
              </a-form-item>
            </a-col>
            <a-col :md="12" :sm="24">
              <a-form-item label="角色">
                <a-select v-model="filters.roleIds" mode="multiple" placeholder="请选择" :allowClear="true">
                  <a-select-option v-for="item in config.roles" :key="item.id" :value="item.id">{{ item.name }}</a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :md="6" :sm="24">
              <a-form-item label="注册IP">
                <a-input v-model="filters.createdIp" placeholder=""/>
              </a-form-item>
            </a-col>
            <a-col :md="6" :sm="24">
              <a-form-item label="登录IP">
                <a-input v-model="filters.loginedIp" placeholder=""/>
              </a-form-item>
            </a-col>
            <a-col :md="12" :sm="24">
              <a-form-item label="创建时间">
                <s-date-picker
                  v-model="filters.createdTimeStart"
                  :showTime="showTime(0)"
                  :format="dateFormat()"
                  placeholder="开始时间"
                />
                <span> ~ </span>
                <s-date-picker
                  v-model="filters.createdTimeEnd"
                  :showTime="showTime(1)"
                  :format="dateFormat()"
                  placeholder="结束时间"
                />
              </a-form-item>
            </a-col>
            <a-col :md="12" :sm="24">
              <a-form-item label="修改时间">
                <s-date-picker
                  v-model="filters.updatedTimeStart"
                  :showTime="showTime(0)"
                  :format="dateFormat()"
                  placeholder="开始时间"
                />
                <span> ~ </span>
                <s-date-picker
                  v-model="filters.updatedTimeEnd"
                  :showTime="showTime(1)"
                  :format="dateFormat()"
                  placeholder="结束时间"
                />
              </a-form-item>
            </a-col>
            <a-col :md="12" :sm="24">
              <a-form-item label="登录时间">
                <s-date-picker
                  v-model="filters.loginedTimeStart"
                  :showTime="showTime(0)"
                  :format="dateFormat()"
                  placeholder="开始时间"
                />
                <span> ~ </span>
                <s-date-picker
                  v-model="filters.loginedTimeEnd"
                  :showTime="showTime(1)"
                  :format="dateFormat()"
                  placeholder="结束时间"
                />
              </a-form-item>
            </a-col>
            <a-col :md="12" :sm="24">
              <a-form-item label="锁定时间">
                <s-date-picker
                  v-model="filters.lockedTimeStart"
                  :showTime="showTime(0)"
                  :format="dateFormat()"
                  placeholder="开始时间"
                />
                <span> ~ </span>
                <s-date-picker
                  v-model="filters.lockedTimeEnd"
                  :showTime="showTime(1)"
                  :format="dateFormat()"
                  placeholder="结束时间"
                />
              </a-form-item>
            </a-col>
          </template>
          <a-col :md="!advanced && 6 || 24" :sm="24">
            <span class="table-page-search-submitButtons" :style="advanced && { float: 'right', overflow: 'hidden' } || {} ">
              <a-button type="primary" @click="search" :loading="loading">查询</a-button>
              <a-button style="margin-left: 8px" @click="reset('filters')">重置</a-button>
              <a @click="toggleAdvanced" style="margin-left: 8px">
                {{ advanced ? '收起' : '展开' }}
                <a-icon :type="advanced ? 'up' : 'down'"/>
              </a>
            </span>
          </a-col>
        </a-row>
      </a-form>
      <a-table
        :columns="columns"
        :rowKey="record => record.id"
        :dataSource="rows"
        :pagination="pagination"
        :loading="loading"
        :rowSelection="rowSelection"
      >
      </a-table>
    </div>
  </a-card>
</template>

<script>
import DateUtil from '@/utils/date'
import RouteUtil from '@/utils/route'
import userService from '@/service/member/user'
import SDatePicker from '@/components/DatePicker'

export default {
  components: { SDatePicker },
  data () {
    return {
      advanced: false,
      filters: {},
      columns: [
        { title: 'ID', dataIndex: 'id' },
        { title: '账号', dataIndex: 'serial' },
        { title: '名称', dataIndex: 'name' },
        { title: '排序', dataIndex: 'sort' },
        { title: '状态', dataIndex: 'statusText' },
        { title: '登录IP', dataIndex: 'loginedIp' },
        { title: '登录时间', dataIndex: 'loginedTime' },
        { title: '锁定时间', dataIndex: 'lockedTime' }
      ],
      rowSelection: {
        change (page, pageSize) {
          debugger
          this.search()
        },
        showSizeChange (current, size) {
          debugger
          this.search()
        }
      },
      pagination: {},
      rows: [],
      loading: false,
      config: {
        ready: false,
        defaultPassword: '',
        status: [],
        roles: []
      }
    }
  },
  methods: {
    dateFormat () {
      return DateUtil.dateMomentFormat()
    },
    showTime (indexRange) {
      return { format: DateUtil.timeMomentFormat(), defaultValue: DateUtil.timeMomentRange()[indexRange] }
    },
    toggleAdvanced () {
      this.advanced = !this.advanced
    },
    defaultFilters () {
      return RouteUtil.query2filter(this, {
        page: 1,
        pageSize: 15
      })
    },
    reset (form) {
      switch (form) {
        case 'filters' :
          this.filters = this.defaultFilters()
          break
      }
    },
    search (filter2query = true) {
      if (filter2query) {
        this.filters.page = this.pagination.current
        this.filters.pageSize = this.pagination.pageSize
        RouteUtil.filter2query(this, this.filters, this.pagination)
      }
      this.loading = true
      userService.list(this.filters).then((result) => {
        RouteUtil.result(result, this.pagination)
        if (result.code === 0) {
          this.rows = result.data.rows
        }
        this.loading = false
      })
    }
  },
  created () {
    this.filters = this.defaultFilters()
    RouteUtil.pagination(this.pagination, this.filters, this.search)
  },
  mounted () {
    this.search(false)
    userService.config().then((result) => {
      this.config.ready = true
      if (result.code === 0) {
        Object.assign(this.config, result.data)
      }
    })
  }
}
</script>
