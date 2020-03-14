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
              <a-form-item label="角色">
                <a-select v-model="filters.roleIds" placeholder="请选择" :allowClear="true">
                  <a-select-option v-for="item in config.roles" :key="item.id" :value="item.id">{{ item.name }}</a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :md="12" :sm="24">
              <a-form-item label="注册IP">
                <a-input v-model="filters.createdIp" placeholder=""/>
              </a-form-item>
            </a-col>
            <a-col :md="12" :sm="24">
              <a-form-item label="登录IP">
                <a-input v-model="filters.loginedIp" placeholder=""/>
              </a-form-item>
            </a-col>
            <a-col :md="12" :sm="24">
              <a-form-item label="创建时间">
                <a-range-picker
                  v-model="filters.createdTime"
                  :showTime="showTime()"
                  :format="dateFormat()"
                  :placeholder="['开始时间', '结束时间']"
                  @ok="dateChange('createdTime')"
                />
              </a-form-item>
            </a-col>
            <a-col :md="12" :sm="24">
              <a-form-item label="修改时间">
                <a-range-picker
                  v-model="filters.updatedTime"
                  :showTime="showTime()"
                  :format="dateFormat()"
                  :placeholder="['开始时间', '结束时间']"
                  @ok="dateChange('updatedTime')"
                />
              </a-form-item>
            </a-col>
            <a-col :md="12" :sm="24">
              <a-form-item label="登录时间">
                <a-range-picker
                  v-model="filters.loginedTime"
                  :showTime="showTime()"
                  :format="dateFormat()"
                  :placeholder="['开始时间', '结束时间']"
                  @ok="dateChange('loginedTime')"
                />
              </a-form-item>
            </a-col>
            <a-col :md="12" :sm="24">
              <a-form-item label="锁定时间">
                <a-range-picker
                  v-model="filters.lockedTime"
                  :showTime="showTime()"
                  :format="dateFormat()"
                  :placeholder="['开始时间', '结束时间']"
                  @ok="dateChange('lockedTime')"
                />
              </a-form-item>
            </a-col>
          </template>
          <a-col :md="!advanced && 6 || 24" :sm="24">
            <span class="table-page-search-submitButtons" :style="advanced && { float: 'right', overflow: 'hidden' } || {} ">
              <a-button type="primary" @click="search">查询</a-button>
              <a-button style="margin-left: 8px" @click="reset('filters')">重置</a-button>
              <a @click="toggleAdvanced" style="margin-left: 8px">
                {{ advanced ? '收起' : '展开' }}
                <a-icon :type="advanced ? 'up' : 'down'"/>
              </a>
            </span>
          </a-col>
        </a-row>
      </a-form>
    </div>
  </a-card>
</template>

<script>
import DateUtil from '@/utils/date'
import userService from '@/service/member/user'

export default {
  data () {
    return {
      advanced: false,
      filters: this.defaultFilters(),
      rows: [],
      total: 0,
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
    showTime () {
      return { format: DateUtil.timeMomentFormat(), defaultValue: DateUtil.timeMomentRange() }
    },
    toggleAdvanced () {
      this.advanced = !this.advanced
    },
    defaultFilters () {
      return {
        page: 1,
        pageSize: 15,
        createdTime: [],
        updatedTime: [],
        loginedTime: [],
        lockedTime: []
      }
    },
    dateChange (field) {
      if (this.filters[field]) {
        this.filters[field + 'Start'] = DateUtil.format(new Date(this.filters[field][0]).getTime())
        this.filters[field + 'End'] = DateUtil.format(new Date(this.filters[field][1]).getTime())
      } else {
        this.filters[field + 'Start'] = this.filters[field + 'End'] = undefined
      }
    },
    reset (form) {
      this.filters = this.defaultFilters()
      switch (form) {
        case 'filters' :
          ['createdTime', 'updatedTime', 'loginedTime', 'lockedTime'].forEach((value) => {
            this.dateChange(value)
          })
          break
      }
    },
    search () {
      this.loading = true
      userService.list(this.filters).then((result) => {
        if (result.code === 0) {
          this.total = result.data.total
          this.rows = result.data.rows
        }
        this.loading = false
      })
    }
  },
  mounted () {
    this.search()
    userService.config().then((result) => {
      this.config.ready = true
      if (result.code === 0) {
        Object.assign(this.config, result.data)
      }
    })
  }
}
</script>
