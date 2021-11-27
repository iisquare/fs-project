<template>
  <div class="fs-aggregate-layout">
    <div class="fs-aggregate-left" v-show="layout.left">
      <a-card class="fs-ui-top">
        <a-form-model>
          <service-auto-complete ref="dataset" :search="datasetService.list" v-model="form.datasetId" placeholder="检索选择数据集" :loading="loading" />
        </a-form-model>
      </a-card>
      <a-card title="数据字段" class="fs-ui-content" :loading="loading">
        <template slot="extra">
          <a-input-search v-model="columnSearch" placeholder="搜索字段" />
        </template>
        <a-table
          :columns="columns"
          :dataSource="rows"
          :pagination="false"
          :scroll="{ y: 'calc(100vh - 320px)' }"
          :rowKey="(record, index) => index">
          <a-space
            slot="icon"
            draggable
            @dragstart="columnDragged = record"
            @dragend="columnDragged = null"
            slot-scope="text, record">
            <a-icon class="icon" :component="icons['bi' + record.format]" />
            <span>{{ record.name }}</span>
          </a-space>
        </a-table>
      </a-card>
    </div>
    <div class="fs-aggregate-content">
      <a-card>
        <template slot="title">
          <a-space>
            <a-button shape="circle" :icon="layout.left ? 'double-left' : 'double-right'" @click="layout.left = !layout.left" />
            <a-popover placement="bottomLeft" trigger="click">
              <section slot="content"><query-filter v-model="filter" :fields="fields" /></section>
              <a-button icon="filter">筛选</a-button>
            </a-popover>
            <a-button icon="search" type="primary" @click="search" :loading="loading">更新</a-button>
          </a-space>
        </template>
        <template slot="extra">
          <a-space>
            <a-button type="primary" :loading="loading" @click="submit">保存</a-button>
            <a-button :loading="loading" @click="cancel">返回</a-button>
            <a-button shape="circle" :icon="layout.right ? 'double-right' : 'double-left'" @click="layout.right = !layout.right" />
          </a-space>
        </template>
        <div
          class="fs-agg-line"
          v-for="line in lines"
          :key="line.type"
          @dragover="event => aggDragOver(line.type, event)"
          @drop="event => aggDrop(line.type, event)">
          <a-space class="fs-agg-title">
            <a-icon :type="line.icon" />
            <span>{{ line.label }}</span>
          </a-space>
          <draggable
            v-model="aggregation[line.type]"
            class="fs-agg-list"
            :group="line.type"
            animation="340">
            <div class="fs-agg-item" v-for="(item, index) in aggregation[line.type]" :key="index" @contextmenu="ev => handleContextMenu(line.type, ev, item, index)">
              <a-checkbox v-model="item.enabled" />
              <span class="fs-agg-text" @click="itemEditing = item" :title="item.title">{{ item.title }}</span>
            </div>
          </draggable>
        </div>
      </a-card>
      <a-card :bordered="false" class="fs-agg-data">
        <matrix-table v-model="aggResult" :loading="loading" />
      </a-card>
    </div>
    <div class="fs-aggregate-right" v-show="layout.right">
      <a-card title="属性">
        <a-form-model labelAlign="left" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }" v-if="itemEditing.type">
          <a-form-model-item label="启用状态">
            <a-checkbox v-model="itemEditing.enabled">{{ itemEditing.type }}</a-checkbox>
          </a-form-model-item>
          <a-form-model-item label="展示名称">
            <a-input v-model="itemEditing.title" />
          </a-form-model-item>
          <a-form-model-item label="聚合字段">
            <a-auto-complete
              optionLabelProp="value"
              :allowClear="true"
              v-model="itemEditing.name"
              :filterOption="UIUtil.filterOption">
              <template slot="dataSource">
                <a-select-option :key="k" :value="v.value" v-for="(v, k) in simpleFields">{{ v.label }}</a-select-option>
              </template>
            </a-auto-complete>
          </a-form-model-item>
          <a-form-model-item label="排序方式" v-if="itemEditing.type !== 'metrics'">
            <a-select v-model="itemEditing.sort">
              <a-select-option :value="item.value" v-for="item in directions" :key="item.value">{{ item.label }}</a-select-option>
            </a-select>
          </a-form-model-item>
          <a-form-model-item label="聚合方式" v-if="itemEditing.type === 'metrics'">
            <a-select v-model="itemEditing.aggregation">
              <a-select-option :value="item.value" v-for="item in metrics" :key="item.value">{{ item.label }}</a-select-option>
            </a-select>
          </a-form-model-item>
          <a-form-model-item label="定制运算" v-if="itemEditing.type === 'levels'">
            <code-editor
              v-model="itemEditing.expression"
              mode="sql"
              :hints="hints"
              :height="100"
              :lineNumbers="false"
              :volatile="itemEditing" />
          </a-form-model-item>
        </a-form-model>
        <a-empty description="右键编辑字段" v-else />
      </a-card>
    </div>
  </div>
</template>

<script>
import draggable from 'vuedraggable'
import UIUtil from '@/utils/ui'
import icons from '@/assets/icons'
import MenuUtil from '@/utils/menu'
import datasetService from '@/service/bi/dataset'
import matrixService from '@/service/bi/matrix'

export default {
  components: {
    draggable,
    MatrixTable: () => import('./MatrixTable'),
    CodeEditor: () => import('@/components/Editor/CodeEditor'),
    QueryFilter: () => import('@/components/Service/QueryFilter'),
    QuerySorter: () => import('@/components/Service/QuerySorter'),
    ServiceAutoComplete: () => import('@/components/Service/AutoComplete')
  },
  data () {
    return {
      icons,
      UIUtil,
      datasetService,
      loading: false,
      columnSearch: '',
      columnDragged: null,
      datasetColumns: [],
      columns: [
        { title: '字段', scopedSlots: { customRender: 'icon' } },
        { title: '名称', dataIndex: 'title', ellipsis: true }
      ],
      lines: [
        { type: 'levels', label: '层级（Level）', icon: 'apartment' },
        { type: 'buckets', label: '维度（Bucket）', icon: 'pic-right' },
        { type: 'metrics', label: '度量（Metric）', icon: 'project' }
      ],
      metrics: [
        { label: '求和（SUM）', value: 'SUM' },
        { label: '计数（COUNT）', value: 'COUNT' },
        { label: '去重计数（COUNT_DISTINCT）', value: 'COUNT_DISTINCT' },
        { label: '最大（MAX）', value: 'MAX' },
        { label: '最小（MIN）', value: 'MIN' },
        { label: '平均（AVG）', value: 'AVG' }
      ],
      directions: [
        { label: '正序', value: 'asc' },
        { label: '倒序', value: 'desc' }
      ],
      layout: {
        left: true,
        right: true
      },
      form: {
        datasetId: undefined
      },
      filter: [],
      aggregation: {
        levels: [],
        metrics: [],
        buckets: []
      },
      itemEditing: {},
      aggResult: null
    }
  },
  computed: {
    fields () {
      const result = []
      this.datasetColumns.forEach(item => {
        result.push({ label: item.title, value: '`' + item.name + '`' })
      })
      return result
    },
    simpleFields () {
      const result = []
      this.datasetColumns.forEach(item => {
        result.push({ label: item.title, value: item.name })
      })
      return result
    },
    rows () {
      const content = this.columnSearch.toUpperCase()
      return this.datasetColumns.filter(item => {
        return item.name.toUpperCase().indexOf(content) >= 0 || item.title.toUpperCase().indexOf(content) >= 0
      })
    },
    hints () {
      const result = []
      this.datasetColumns.forEach((item, tableIndex) => {
        result.push({ text: '`' + item.name + '`', displayText: item.name })
      })
      return result
    }
  },
  watch: {
    'form.datasetId': {
      handler () {
        this.loadColumns()
      },
      immediate: true
    }
  },
  methods: {
    handleContextMenu (type, ev, item, index) {
      MenuUtil.context(ev, [
        { key: 'edit', icon: 'edit', title: '编辑字段' },
        { key: 'delete', icon: 'delete', title: '移除字段' },
        { key: 'deleteOther', icon: 'delete', title: '移除其他字段' },
        { key: 'deleteLeft', icon: 'delete', title: '移除左侧字段' },
        { key: 'deleteRight', icon: 'delete', title: '移除右侧字段' },
        { key: 'deleteAll', icon: 'delete', title: '移除全部字段' }
      ], menu => {
        this.itemEditing = {}
        switch (menu.key) {
          case 'edit':
            this.itemEditing = item
            break
          case 'delete':
            this.aggregation[type].splice(index, 1)
            break
          case 'deleteOther':
            this.aggregation[type].splice(0, this.aggregation[type].length, item)
            break
          case 'deleteLeft':
            this.aggregation[type].splice(0, index)
            break
          case 'deleteRight':
            this.aggregation[type].splice(index + 1, this.aggregation[type].length - index + 1)
            break
          case 'deleteAll':
            this.aggregation[type].splice(0, this.aggregation[type].length)
            break
          default:
            return false
        }
      })
    },
    aggDragOver (type, event) {
      if (this.columnDragged) event.preventDefault()
    },
    aggDrop (type, event) {
      if (!this.columnDragged) return false
      const column = this.columnDragged
      const item = { enabled: true, type, name: column.name, title: column.title, expression: '' }
      switch (type) {
        case 'levels':
        case 'buckets':
          item.sort = 'asc'
          break
        case 'metrics':
          item.aggregation = 'COUNT'
          break
      }
      this.aggregation[type].push(item)
      this.itemEditing = item
    },
    loadColumns () {
      if (!this.form.datasetId || this.form.datasetId < 1) {
        this.datasetColumns = []
        return true
      }
      if (this.loading) return false
      this.loading = true
      datasetService.columns({ id: this.form.datasetId }).then(result => {
        this.datasetColumns = result.code === 0 ? result.data.columns : []
        this.loading = false
      })
    },
    load () {
      if (!this.$route.query.id) return false
      this.loading = true
      matrixService.info({ id: this.$route.query.id }).then(result => {
        if (result.code !== 0) return false
        if (!result.data.datasetId || result.data.datasetId < 1) result.data.datasetId = undefined
        Object.assign(this.form, result.data)
        this.$refs.dataset && this.$refs.dataset.trigger()
        try {
          if (result.data.content) {
            const data = JSON.parse(result.data.content)
            if (data.filter) this.filter = data.filter
            const aggregation = data.aggregation ?? {}
            if (aggregation.levels) this.aggregation.levels = aggregation.levels
            if (aggregation.buckets) this.aggregation.buckets = aggregation.buckets
            if (aggregation.metrics) this.aggregation.metrics = aggregation.metrics
          }
        } catch (e) {
          this.$error({ title: '数据解析异常', content: e.message })
        } finally {
          this.loading = false
        }
      })
    },
    collect () {
      return Object.assign({
        filter: this.filter,
        aggregation: this.aggregation
      })
    },
    submit () {
      if (this.loading) return false
      this.loading = true
      let data = this.collect()
      if (data === null) return (this.loading = false)
      data = { id: this.form.id, datasetId: this.form.datasetId, content: JSON.stringify(data) }
      matrixService.save(data, { success: true }).then(result => {
        if (result.code === 0) {
          this.form = result.data
        }
        this.loading = false
      })
    },
    cancel () {
      this.$router.go(-1)
    },
    search () {
      if (this.loading) return false
      this.loading = true
      matrixService.search({ datasetId: this.form.datasetId, preview: this.collect() }).then(result => {
        if (result.code === 0) {
          this.aggResult = result.data
        }
        this.loading = false
      })
    }
  },
  mounted () {
    this.load()
  }
}
</script>

<style lang="less" scoped>
.fs-aggregate-layout {
  width: 100%;
  display: flex;
  .fs-aggregate-left {
    flex: 0 0 350px;
    height: calc(100vh - 100px);
    margin-right: 20px;
    .fs-ui-top {
      & /deep/ .ant-card-body {
        padding: 21px 24px;
      }
    }
    .fs-ui-content {
      margin: 20px 0px;
      & /deep/ .ant-card-body {
        padding: 0px;
      }
      & /deep/ input {
        border-radius: 15px;
        width: 180px;
      }
      .icon {
        cursor: move;
      }
      & /deep/ .ant-table-placeholder {
        border: none;
      }
    }
  }
  .fs-aggregate-content {
    flex: 1;
    width: 0px;
    & /deep/ .ant-card {
      margin-bottom: 20px;
      .ant-card-body {
        padding: 0px;
      }
    }
    .fs-agg-data {
      width: 100%;
      overflow-x: auto;
    }
    .fs-agg-line {
      position: relative;
      min-height: 43px;
      vertical-align: middle;
      border: 1px solid #e8e8e8;
      margin: 5px;
      .fs-agg-title {
        position: absolute;
        width: 160px;
        padding: 15px;
        height: 100%;
        border-right: 1px solid #e8e8e8;
      }
      .fs-agg-list {
        display: inline-block;
        vertical-align: top;
        padding-left: 160px;
        .fs-agg-item {
          display: inline-block;
          cursor: move;
          margin: 5px;
          background: #4996b2;
          width: 156px;
          border-radius: 15px;
          padding: 5px 15px;
          .fs-agg-text {
            color: white;
            display: inline-block;
            margin: 0px 15px;
            width: 80px;
            overflow: hidden;
            white-space: nowrap;
            text-overflow: ellipsis;
            vertical-align: bottom;
          }
        }
      }
    }
  }
  .fs-aggregate-right {
    flex: 0 0 350px;
    margin-left: 20px;
    .fs-aggregate-axis {
      margin-bottom: 20px;
      & /deep/ .ant-card-body {
        padding: 0px;
      }
      & /deep/ .ant-card-actions {
        border-top: none;
      }
      & /deep/ .ant-collapse-item {
        border-bottom: none;
      }
    }
  }
}

</style>
