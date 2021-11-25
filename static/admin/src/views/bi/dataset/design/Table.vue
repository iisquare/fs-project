<template>
  <div class="fs-relation-layout">
    <div class="fs-relation-left">
      <a-card title="源字段">
        <template slot="extra">
          <a-input-search v-model="treeSearch" placeholder="搜索字段" />
        </template>
        <a-tree
          draggable
          :treeData="treeData"
          :expandedKeys.sync="treeExpandedKeys"
          @rightClick="handleContextMenu"
          @dragstart="({ event, node }) => treeDragged = node.dataRef"
          @dragend="treeDragged = null">
          <template slot="title" slot-scope="node">
            <a-icon class="icon" :component="icons[node.icon]" />
            <span>{{ node.title }}</span>
          </template>
        </a-tree>
      </a-card>
    </div>
    <div class="fs-relation-content" @dragover="tableDragOver" @drop="tableDrop">
      <a-card>
        <template slot="title">
          <a-space>
            <a-popover placement="bottomLeft" trigger="click">
              <section slot="content"><query-filter v-model="filter" :fields="fields" /></section>
              <a-button icon="filter">筛选</a-button>
            </a-popover>
            <a-popover placement="bottomLeft" trigger="click">
              <section slot="content"><query-sorter v-model="sorter" :fields="fields" /></section>
              <a-button icon="bars">排序</a-button>
            </a-popover>
            <a-button icon="search" type="primary" @click="search(false)" :loading="loading">搜索</a-button>
          </a-space>
        </template>
        <template slot="extra">
          <a-space>
            <a-button icon="edit" @click="edit">编辑</a-button>
            <a-button icon="arrow-up" @click="sortTable.up()">上移</a-button>
            <a-button icon="arrow-down" @click="sortTable.down()">下移</a-button>
            <a-button icon="delete" @click="sortTable.remove()">删除</a-button>
            <a-button icon="history" type="danger" @click="sortTable.reset([])">清空</a-button>
          </a-space>
        </template>
        <div class="fs-ui-left">
          <div class="fs-ui-main">
            <a-table
              v-if="dataColumns.length > 0"
              :columns="dataColumns"
              :rowKey="(record, index) => index"
              :dataSource="dataRows"
              :pagination="false"
              :loading="loading"
              :bordered="true"
              :scroll="{ y: 'calc(100vh - 370px)' }"
            />
          </div>
          <div class="fs-ui-bottom">
            <a-pagination v-bind="pagination" @change="tableChange" @showSizeChange="tableChange" />
          </div>
        </div>
        <div class="fs-ui-right">
          <a-table
            :columns="columns"
            :dataSource="sortTable.rows"
            :rowSelection="sortTable.selection"
            :pagination="false"
            :scroll="{ x: true, y: 'calc(100vh - 308px)' }"
            :rowKey="(record, index) => index">
            <template slot="icon" slot-scope="text, record">
              <a-icon class="icon" :component="icons['bi' + record.format]" />
              <span>{{ record.title }}</span>
            </template>
            <template slot="state" slot-scope="text, record">
              <a-checkbox v-model="record.enabled" />
              <a-divider type="vertical" />
              <a-checkbox v-model="record.viewable" />
            </template>
          </a-table>
        </div>
      </a-card>
    </div>
    <a-modal title="编辑字段" v-model="formVisible" :maskClosable="false" :footer="null">
      <a-form-model ref="form" :model="form" :rules="rules" :label-col="{ span: 5 }" :wrapper-col="{ span: 18 }">
        <a-form-model-item label="目标字段" prop="name">
          <a-input v-model="form.name" auto-complete="on"></a-input>
        </a-form-model-item>
        <a-form-model-item label="目标名称" prop="title">
          <a-input v-model="form.title" auto-complete="on"></a-input>
        </a-form-model-item>
        <a-form-model-item label="来源表名" prop="table">
          <a-input v-model="form.table" auto-complete="on"></a-input>
        </a-form-model-item>
        <a-form-model-item label="来源字段" prop="column">
          <a-input v-model="form.column" auto-complete="on"></a-input>
        </a-form-model-item>
        <a-form-model-item label="处理方式" prop="transform">{{ form.transform || '默认值' }} from {{ form.type }} to {{ form.format }}</a-form-model-item>
        <column-calculate v-model="form.options" :column="form" :relation="relation" v-if="form.transform === 'calculate'" />
        <column-date v-model="form.options" :column="form" :relation="relation" v-if="form.transform === 'cast2date'" />
        <column-location v-model="form.options" :column="form" :relation="relation" v-if="form.transform === 'location'" />
      </a-form-model>
    </a-modal>
  </div>
</template>

<script>
import icons from '@/assets/icons'
import RouteUtil from '@/utils/route'
import MenuUtil from '@/utils/menu'
import SortTable from '@/utils/helper/SortTable'
import datasetService from '@/service/bi/dataset'

export default {
  name: 'Table',
  components: {
    QueryFilter: () => import('@/components/Service/QueryFilter'),
    QuerySorter: () => import('@/components/Service/QuerySorter'),
    ColumnCalculate: () => import('./ColumnCalculate'),
    ColumnDate: () => import('./ColumnDate'),
    ColumnLocation: () => import('./ColumnLocation')
  },
  props: {
    value: { type: Array, required: true },
    relation: { type: Object, required: true }
  },
  data () {
    return {
      icons,
      treeSearch: '',
      treeDragged: null,
      treeExpandedKeys: [],
      loading: false,
      sortTable: new SortTable([], { columnWidth: 25 }),
      columns: [
        { title: '目标字段', scopedSlots: { customRender: 'icon' }, ellipsis: true },
        { title: '状态|预览', scopedSlots: { customRender: 'state' }, width: 100, align: 'center' }
      ],
      form: {},
      formVisible: false,
      rules: {},
      filter: [],
      sorter: [],
      pagination: {},
      dataColumns: [],
      dataRows: []
    }
  },
  computed: {
    fields () {
      const result = []
      this.sortTable.rows.forEach(item => {
        result.push({ label: item.title, value: '`' + item.name + '`' })
      })
      return result
    },
    treeData () {
      const result = []
      if (!this.relation.items) return result
      this.relation.items.forEach((item, tableIndex) => {
        const node = {
          key: tableIndex,
          title: item.name,
          icon: 'biTable',
          type: 'table',
          name: item.table,
          data: item,
          children: []
        }
        for (const columnIndex in item.columns) {
          const column = item.columns[columnIndex]
          if (column.name.toUpperCase().indexOf(this.treeSearch.toUpperCase()) === -1) continue
          node.children.push({
            key: tableIndex + '_' + columnIndex,
            title: column.name,
            icon: 'bi' + column.format,
            type: 'column',
            name: column.name,
            data: item
          })
        }
        result.push(node)
      })
      return result
    }
  },
  watch: {
    treeData: {
      handler () {
        this.treeExpandedKeys = []
        this.treeData.forEach(node => this.treeExpandedKeys.push(node.key))
      },
      immediate: true
    }
  },
  methods: {
    handleContextMenu ({ event, node }) {
      const ref = node.dataRef
      const menus = [
        { key: 'add2table', icon: '', title: '添加到数据表' },
        { key: 'removeRefer', icon: '', title: '移除数据引用' }
      ]
      if (ref.type === 'column') {
        menus.push(
          { key: 'calculate', icon: '', title: '创建计算字段', format: 'Calculate' },
          {
            key: 'cast',
            icon: '',
            title: '转换数据类型',
            type: 'sub-menu',
            children: [
              { key: 'cast2string', icon: '', title: '字符串', format: 'String' },
              { key: 'cast2integer', icon: '', title: '整数', format: 'Number' },
              { key: 'cast2long', icon: '', title: '长整数', format: 'Number' },
              { key: 'cast2double', icon: '', title: '小数', format: 'Number' },
              { key: 'cast2date', icon: '', title: '日期', format: 'Date' }
            ]
          },
          { key: 'location', icon: '', title: '地理位置', format: 'Location' }
        )
      }
      MenuUtil.context(event, menus, menu => {
        switch (menu.key) {
          case 'add2table':
            if (ref.type === 'table') {
              for (const index in ref.data.columns) {
                this.add(ref.data, ref.data.columns[index].name)
              }
            } else {
              this.add(ref.data, ref.name)
            }
            return true
          case 'removeRefer':
            for (let index = this.sortTable.rows.length - 1; index >= 0; index--) {
              const item = this.sortTable.rows[index]
              if (item.table !== ref.data.table) continue
              if (ref.type === 'table' || (ref.type === 'column' && item.column === ref.name)) {
                this.sortTable.rows.splice(index, 1)
              }
            }
            return true
          case 'location':
          case 'cast2date':
          case 'calculate':
            this.form = this.add(ref.data, ref.name, { transform: menu.key, format: menu.format })
            this.formVisible = true
            return true
          case 'cast2string':
          case 'cast2integer':
          case 'cast2long':
          case 'cast2double':
            this.add(ref.data, ref.name, { transform: menu.key, format: menu.format })
            return true
          default:
            return false
        }
      }, { width: 140 })
    },
    tableDragOver (ev) {
      if (this.treeDragged) ev.preventDefault()
    },
    tableDrop (ev) {
      if (this.treeDragged.type === 'table') {
        for (const index in this.treeDragged.data.columns) {
          this.add(this.treeDragged.data, this.treeDragged.data.columns[index].name)
        }
      } else if (this.treeDragged.type === 'column') {
        this.add(this.treeDragged.data, this.treeDragged.name)
      }
    },
    add (table, columnName, options = {}) {
      const column = table.columns[columnName]
      const item = {
        enabled: true,
        viewable: true,
        name: columnName,
        title: columnName,
        table: table.table,
        column: columnName,
        format: options.format ?? column.format,
        type: column.type,
        transform: options.transform ?? '',
        options: options.options ?? {}
      }
      return this.sortTable.add(item)
    },
    edit () {
      const index = this.sortTable.selectedIndex()
      if (index === -1) return false
      this.form = this.sortTable.rows[index]
      this.formVisible = true
    },
    reset () {
      this.sortTable.reset(this.value)
    },
    collect () {
      return this.sortTable.rows
    },
    tableChange (page, pageSize) {
      Object.assign(this.pagination, { page, pageSize })
      this.search(true)
    },
    search (fromChange) {
      const data = {
        preview: { relation: this.relation, table: this.collect() },
        query: { filter: this.filter, sorter: this.sorter, page: fromChange ? this.pagination.page : 1, pageSize: this.pagination.pageSize }
      }
      if (this.loading) return false
      this.loading = true
      datasetService.search(data).then((result) => {
        this.pagination = Object.assign({}, this.pagination, RouteUtil.result(result))
        if (result.code === 0) {
          this.dataColumns = this.parseColumn(result.data.columns)
          this.dataRows = result.data.rows
        }
        this.loading = false
      })
    },
    parseColumn (columns) {
      const result = {}
      for (const index in columns) {
        const column = columns[index]
        if (!column.viewable) continue
        const item = { title: column.title, dataIndex: column.name, ellipsis: true }
        result[item.dataIndex] = item
      }
      return Object.values(result)
    }
  },
  created () {
    this.pagination = Object.assign({}, RouteUtil.pagination({ page: 1, pageSize: 5 }), this.pagination)
  },
  mounted () {
    this.reset()
  },
  destroyed () {
    this.$emit('input', this.collect())
  }
}
</script>

<style lang="less" scoped>
.fs-relation-layout {
  width: 100%;
  height: calc(100vh - 170px);
  .fs-relation-left {
    display: inline-block;
    width: 250px;
    & /deep/ .ant-card-body {
      padding: 0px 5px;
      height: calc(100vh - 254px);
      overflow-y: auto;
      overflow-x: hidden;
    }
    & /deep/ input {
      border-radius: 15px;
      width: 140px;
    }
    .icon {
      margin-right: 6px;
      font-size: 14px;
    }
  }
  .fs-relation-content {
    display: inline-block;
    width: calc(100% - 270px);
    margin-left: 20px;
    vertical-align: top;
    & /deep/ .ant-card-body {
      padding: 0px;
      height: calc(100vh - 254px);
      overflow: hidden;
    }
    & /deep/ .ant-table-placeholder {
      border-top: none;
      border-bottom: none;
    }
    .fs-ui-left {
      height: 100%;
      width: calc(100% - 320px);
      display: inline-block;
      overflow: hidden;
      .fs-ui-main {
        width: 100%;
        height: calc(100% - 65px);
        overflow: hidden;
      }
      .fs-ui-bottom {
        width: 100%;
        height: 65px;
        text-align: right;
        padding: 15px;
        border-top: 1px solid #e8e8e8;
      }
    }
    .fs-ui-right {
      height: 100%;
      width: 320px;
      display: inline-block;
      border-left: 1px solid #e8e8e8;
      vertical-align: top;
      padding-top: 1px;
      .icon {
        margin-right: 6px;
        font-size: 14px;
      }
    }
  }
}
</style>
