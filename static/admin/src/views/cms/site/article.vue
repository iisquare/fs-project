<template>
  <section>
    <a-card class="fs-layout">
      <template slot="title">
        <a-button icon="minus-circle" type="danger" @click="batchRemove" v-permit="'cms:article:delete'" :disabled="selection.selectedRows.length === 0">删除</a-button>
        <a-divider type="vertical" v-permit="'cms:article:add'" />
        <a-button icon="plus-circle" type="primary" @click="editor" v-permit="'cms:article:add'">发布</a-button>
      </template>
      <aside class="fs-layout-left">
        <div class="head"><h3>栏目列表</h3></div>
        <div class="body">
          <a-tree
            v-model="filters.catalog"
            class="draggable-tree"
            checkable
            :checkStrictly="true"
            :selectable="false"
            :expandedKeys.sync="expandedKeys"
            :tree-data="config.catalog" />
        </div>
      </aside>
      <section class="fs-layout-body">
        <div class="table-page-search-wrapper">
          <a-form-model ref="filters" :model="filters" layout="inline">
            <a-row :gutter="48">
              <a-col :md="6" :sm="24">
                <a-form-model-item label="标题" prop="title">
                  <a-input v-model="filters.title" placeholder="" :allowClear="true" />
                </a-form-model-item>
              </a-col>
              <a-col :md="6" :sm="24">
                <a-form-model-item label="排序" prop="sort">
                  <a-tooltip>
                    <template slot="title">id - 主键<br/>sort - 排序<br/>publishTime - 发布时间<br/>createdTime - 创建时间<br/>updatedTime - 修改时间<br/>countView - 浏览量<br/>countApprove - 赞成数<br/>countOppose - 反对数<br/>countComment - 评论数</template>
                    <a-input v-model="filters.sort" placeholder="sort.desc,publishTime.desc" :allowClear="true" />
                  </a-tooltip>
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
              </a-col>
            </a-row>
            <a-row :gutter="48">
              <a-col :md="12" :sm="24">
                <a-form-model-item label="系统标签" prop="label">
                  <a-select v-model="filters.label" mode="multiple" placeholder="请选择" :allowClear="true">
                    <a-select-option v-for="(value, key) in config.label" :key="key" :value="key">{{ value }}</a-select-option>
                  </a-select>
                </a-form-model-item>
              </a-col>
              <a-col :md="12" :sm="24">
                <a-form-model-item label="内容标签" prop="tag">
                  <a-select v-model="filters.tag" mode="tags" placeholder="请输入" :allowClear="true">
                  </a-select>
                </a-form-model-item>
              </a-col>
            </a-row>
            <a-row :gutter="48">
              <a-col :md="6" :sm="24">
                <a-form-model-item label="引用名称" prop="citeName">
                  <a-input v-model="filters.citeName" placeholder="" :allowClear="true" />
                </a-form-model-item>
              </a-col>
              <a-col :md="6" :sm="24">
                <a-form-model-item label="引用作者" prop="citeAuthor">
                  <a-input v-model="filters.citeAuthor" placeholder="" :allowClear="true" />
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
                <a-button type="link" size="small" v-permit="'cms:article:'" @click="detail(text, record)">查看</a-button>
                <a-button v-permit="'cms:article:modify'" type="link" size="small" @click="editor(text, record)">编辑</a-button>
              </a-button-group>
            </span>
          </a-table>
        </div>
      </section>
    </a-card>
  </section>
</template>

<script>
import UIUtil from '@/utils/ui'
import DateUtil from '@/utils/date'
import RouteUtil from '@/utils/route'
import articleService from '@/service/cms/article'

export default {
  data () {
    return {
      DateUtil,
      filters: {},
      columns: [
        { title: 'ID', dataIndex: 'id' },
        { title: '标题', dataIndex: 'title' },
        { title: '栏目', dataIndex: 'catalogIdName' },
        { title: '浏览量', dataIndex: 'countView' },
        { title: '赞成数', dataIndex: 'countApprove' },
        { title: '反对数', dataIndex: 'countOppose' },
        { title: '评论数', dataIndex: 'countComment' },
        { title: '排序', dataIndex: 'sort' },
        { title: '状态', dataIndex: 'statusText' },
        { title: '操作', scopedSlots: { customRender: 'action' }, width: 120 }
      ],
      selection: RouteUtil.selection(),
      pagination: {},
      rows: [],
      loading: false,
      expandedKeys: [],
      config: { ready: false, status: {}, label: {}, format: {}, catalog: [] }
    }
  },
  methods: {
    editor (text, record) {
      this.$router.push({
        path: '/cms/site/editor', query: record ? { id: record.id } : null
      })
    },
    detail (text, record) {
      const url = this.$router.resolve({
        path: '/cms/article/detail', query: { id: record.id }
      })
      window.open(url.href)
    },
    batchRemove () {
      this.$confirm(this.selection.confirm(() => {
        this.loading = true
        articleService.delete(this.selection.selectedRowKeys, { success: true }).then((result) => {
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
      const param = Object.assign({}, this.filters, { catalogId: this.filters.catalog.checked })
      articleService.list(param).then((result) => {
        this.pagination = Object.assign({}, this.pagination, RouteUtil.result(result))
        if (result.code === 0) {
          this.rows = result.data.rows
        }
        this.loading = false
      })
    }
  },
  created () {
    this.filters = RouteUtil.query2filter(this, { page: 1, pageSize: 5, catalog: {} })
    this.pagination = Object.assign({}, RouteUtil.pagination(this.filters), this.pagination)
  },
  mounted () {
    this.search(false, true)
    articleService.config().then((result) => {
      this.config.ready = true
      if (result.code === 0) {
        const catalog = UIUtil.treeData(result.data.catalog)
        Object.assign(this.config, result.data, { catalog: catalog.tree })
        this.expandedKeys = catalog.expandedKeys
      }
    })
  }
}
</script>
<style lang="less" scoped>
.fs-layout {
  min-height: 300px;
  height: calc(100vh - 112px);
  overflow: hidden;
  & /deep/ .ant-card-body {
    padding: 0px;
    height: calc(100% - 64px);
  }
  .fs-layout-left {
    width: 260px;
    height: 100%;
    display: inline-block;
    border-right: 1px solid #e8e8e8;
    .head {
      height: 44px;
      line-height: 44px;
      padding: 0px 15px;
      border-bottom: solid 1px #e8e8e8;
    }
    .body {
      height: calc(100% - 44px);
      padding: 15px;
      overflow: auto;
    }
  }
  .fs-layout-body {
    height: 100%;
    width: calc(100% - 260px);
    display: inline-block;
    vertical-align: top;
    overflow-y: auto;
    overflow-x: hidden;
    padding: 15px;
  }
}
</style>
