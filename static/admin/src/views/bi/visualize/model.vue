<template>
  <div class="fs-aggregate-layout">
    <div class="fs-aggregate-left" v-show="layout.left">
      <a-card class="fs-ui-top">
        <a-form-model>
          <service-auto-complete ref="dataset" :search="datasetService.list" v-model="form.datasetId" placeholder="检索选择数据集" :loading="loading" />
        </a-form-model>
      </a-card>
      <a-card class="fs-ui-content" :loading="loading">
        <a-table
          :columns="columns"
          :dataSource="datasetColumns"
          :pagination="false"
          :rowKey="(record, index) => index">
          <span slot="icon" slot-scope="text, record">
            <a-icon class="icon" :component="icons['bi' + record.format]" />
          </span>
        </a-table>
      </a-card>
    </div>
    <div class="fs-aggregate-content">
      <a-card :bodyStyle="{ padding: '0px' }">
        <template slot="title">
          <a-space>
            <a-button shape="circle" :icon="layout.left ? 'double-left' : 'double-right'" @click="layout.left = !layout.left" />
            <a-popover placement="bottomLeft" trigger="click">
              <a-list slot="content" :grid="{ gutter: 16, column: 3 }" :data-source="config.widgets" class="fs-chart-list">
                <a-list-item
                  slot="renderItem"
                  slot-scope="widget"
                  :class="['fs-chart-item', form.type === widget.type && 'fs-chart-selected']"
                  @click="form.type = widget.type">
                  <a-icon :component="icons[widget.icon]" />
                  <div>{{ widget.label }}</div>
                </a-list-item>
              </a-list>
              <a-button icon="align-left">类型</a-button>
            </a-popover>
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
        <fs-chart ref="chart" :config="config" :type="form.type" />
      </a-card>
    </div>
    <div class="fs-aggregate-right" v-show="layout.right">
      <a-tabs default-active-key="axis" :animated="false">
        <a-tab-pane key="axis" tab="坐标">
          <a-card title="Y-度量" class="fs-aggregate-axis">
            <a-collapse :bordered="false">
              <a-collapse-panel :key="index" :header="'Y-坐标：' + metric.label" v-for="(metric, index) in axis.metrics">
                <a-icon slot="extra" type="delete" @click.native="deleteMetric(metric, index)" />
                <a-form-model labelAlign="left" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
                  <a-form-model-item label="标签名称"><a-input v-model="metric.label" /></a-form-model-item>
                  <a-form-model-item label="聚合方式">
                    <a-select v-model="metric.aggregation">
                      <a-select-option :value="item.value" v-for="item in metrics" :key="item.value">{{ item.label }}</a-select-option>
                    </a-select>
                  </a-form-model-item>
                  <a-form-model-item label="聚合字段" v-if="metric.aggregation !== 'COUNT'">
                    <a-auto-complete
                      optionLabelProp="value"
                      :allowClear="true"
                      v-model="metric.field"
                      :filterOption="UIUtil.filterOption">
                      <template slot="dataSource">
                        <a-select-option :key="k" :value="v.value" v-for="(v, k) in fields">{{ v.label }}</a-select-option>
                      </template>
                    </a-auto-complete>
                  </a-form-model-item>
                  <a-form-model-item label="过滤条件">
                    <a-popover placement="bottomRight" trigger="click">
                      <section slot="content"><query-filter v-model="metric.filter" :fields="fields" /></section>
                      <a-button icon="filter">筛选</a-button>
                    </a-popover>
                  </a-form-model-item>
                </a-form-model>
              </a-collapse-panel>
            </a-collapse>
            <template slot="actions" class="ant-card-actions">
              <a-button icon="plus-circle" type="link" @click="addMetric">指标</a-button>
            </template>
          </a-card>
          <a-card title="X-维度" class="fs-aggregate-axis">
            <a-collapse :bordered="false">
              <a-collapse-panel :key="index" :header="'X-坐标：' + bucket.label" v-for="(bucket, index) in axis.buckets">
                <a-icon slot="extra" type="delete" @click="deleteBucket(bucket, index)" />
                <a-form-model labelAlign="left" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
                  <a-form-model-item label="层级名称"><a-input v-model="bucket.label" /></a-form-model-item>
                  <a-form-model-item label="聚合方式">
                    <a-select v-model="bucket.aggregation">
                      <a-select-option :value="item.value" v-for="item in buckets" :key="item.value">{{ item.label }}</a-select-option>
                    </a-select>
                  </a-form-model-item>
                  <a-form-model-item label="聚合字段" v-if="bucket.aggregation !== 'FILTER'">
                    <a-auto-complete
                      optionLabelProp="value"
                      :allowClear="true"
                      v-model="bucket.field"
                      :filterOption="UIUtil.filterOption">
                      <template slot="dataSource">
                        <a-select-option :key="k" :value="v.value" v-for="(v, k) in fields">{{ v.label }}</a-select-option>
                      </template>
                    </a-auto-complete>
                  </a-form-model-item>
                  <a-form-model-item label="分段间隔" v-if="bucket.aggregation === 'HISTOGRAM'">
                    <a-input v-model="bucket.interval" />
                  </a-form-model-item>
                  <a-form-model-item label="分段间隔" v-if="bucket.aggregation === 'DATE_HISTOGRAM'">
                    <a-select v-model="bucket.interval">
                      <a-select-option :value="item.value" v-for="item in intervals" :key="item.value">{{ item.label }}</a-select-option>
                    </a-select>
                  </a-form-model-item>
                  <section v-if="bucket.aggregation === 'FILTER'">
                    <draggable v-model="bucket.filters" group="bucket" handle=".fs-bucket-sort" chosenClass="fs-bucket-drop" animation="340">
                      <a-row class="fs-bucket-item" type="flex" v-for="(item, idx) in bucket.filters" :key="idx">
                        <a-col flex="32px"><a-button type="link" icon="deployment-unit" class="fs-bucket-sort" /></a-col>
                        <a-col flex="2"><a-input v-model="item.label" auto-complete="on" placeholder="标签名称" /></a-col>
                        <a-col flex="3px"></a-col>
                        <a-col flex="1">
                          <a-popover placement="bottomRight" trigger="click">
                            <section slot="content"><query-filter v-model="item.filter" :fields="fields" /></section>
                            <a-button icon="filter">筛选</a-button>
                          </a-popover>
                        </a-col>
                        <a-col flex="32px" class="fs-bucket-delete"><a-icon @click="deleteBucketFilter(bucket, item, idx)" type="minus-circle" /></a-col>
                      </a-row>
                    </draggable>
                    <a-row class="fs-bucket-item">
                      <a-col :span="24"><a-button @click="addBucketFilter(bucket)" icon="plus-circle" type="link">添加过滤条件</a-button></a-col>
                    </a-row>
                  </section>
                </a-form-model>
              </a-collapse-panel>
            </a-collapse>
            <template slot="actions" class="ant-card-actions">
              <a-button icon="plus-circle" type="link" @click="addBucket">钻取</a-button>
            </template>
          </a-card>
        </a-tab-pane>
      </a-tabs>
    </div>
  </div>
</template>

<script>
import draggable from 'vuedraggable'
import UIUtil from '@/utils/ui'
import icons from '@/assets/icons'
import datasetService from '@/service/bi/dataset'
import visualizeService from '@/service/bi/visualize'
import config from './design/config'

export default {
  components: {
    draggable,
    FsChart: () => import('./design/Chart'),
    QueryFilter: () => import('@/components/Service/QueryFilter'),
    QuerySorter: () => import('@/components/Service/QuerySorter'),
    ServiceAutoComplete: () => import('@/components/Service/AutoComplete')
  },
  data () {
    return {
      config,
      icons,
      UIUtil,
      datasetService,
      loading: false,
      datasetColumns: [],
      columns: [
        { title: '', scopedSlots: { customRender: 'icon' }, width: 46 },
        { title: '字段', dataIndex: 'name', ellipsis: true },
        { title: '名称', dataIndex: 'title', ellipsis: true }
      ],
      metrics: [
        { label: '求和（SUM）', value: 'SUM' },
        { label: '计数（COUNT）', value: 'COUNT' },
        { label: '去重计数（COUNT_DISTINCT）', value: 'COUNT_DISTINCT' },
        { label: '最大（MAX）', value: 'MAX' },
        { label: '最小（MIN）', value: 'MIN' },
        { label: '平均（AVG）', value: 'AVG' }
      ],
      buckets: [
        { label: '字段（TERM）', value: 'TERM' },
        { label: '过滤（FILTER）', value: 'FILTER' },
        { label: '分段（HISTOGRAM）', value: 'HISTOGRAM' },
        { label: '日期分段（DATE_HISTOGRAM）', value: 'DATE_HISTOGRAM' }
      ],
      intervals: [
        { label: '毫秒（MILLISECOND）', value: 'MILLISECOND' },
        { label: '秒（SECOND）', value: 'SECOND' },
        { label: '分钟（MINUTE）', value: 'MINUTE' },
        { label: '小时（HOUR）', value: 'HOUR' },
        { label: '日（DAY）', value: 'DAY' },
        { label: '月（MONTH）', value: 'MONTH' },
        { label: '季度（QUARTER）', value: 'QUARTER' },
        { label: '年（YEAR）', value: 'YEAR' }
      ],
      layout: {
        left: true,
        right: true
      },
      form: {
        type: 'Table',
        datasetId: undefined
      },
      filter: [],
      axis: {
        metrics: [],
        buckets: []
      }
    }
  },
  computed: {
    fields () {
      const result = []
      this.datasetColumns.forEach(item => {
        result.push({ label: item.title, value: '`' + item.name + '`' })
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
    addMetric () {
      this.axis.metrics.push({ aggregation: 'COUNT', field: '', label: '', filter: [] })
    },
    deleteMetric (metric, index) {
      this.axis.metrics.splice(index, 1)
    },
    addBucket () {
      this.axis.buckets.push({ aggregation: 'TERM', field: '', label: '', interval: '', filters: [] })
    },
    deleteBucket (bucket, index) {
      this.axis.buckets.splice(index, 1)
    },
    deleteBucketFilter (bucket, item, index) {
      bucket.filters.splice(index, 1)
    },
    addBucketFilter (bucket) {
      bucket.filters.push({ label: '', filter: [] })
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
      visualizeService.info({ id: this.$route.query.id }).then(result => {
        if (result.code !== 0) return false
        if (!result.data.datasetId || result.data.datasetId < 1) result.data.datasetId = undefined
        if (!result.data.type) result.data.type = this.form.type
        Object.assign(this.form, result.data)
        this.$refs.dataset && this.$refs.dataset.trigger()
        try {
          if (result.data.content) {
            const data = JSON.parse(result.data.content)
            if (data.filter) this.filter = data.filter
            if (data.axis) Object.assign(this.axis, data.axis)
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
        axis: this.axis
      })
    },
    submit () {
      if (this.loading) return false
      this.loading = true
      let data = this.collect()
      if (data === null) return (this.loading = false)
      data = { id: this.form.id, type: this.form.type, datasetId: this.form.datasetId, content: JSON.stringify(data) }
      visualizeService.save(data, { success: true }).then(result => {
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
      this.$refs.chart.preview(this.form.datasetId, this.collect()).then(result => {
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
    }
  }
  .fs-aggregate-content {
    flex: 1;
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
.fs-bucket-item {
  padding: 3px 0px 3px 0px;
  border:dashed 1px white;
  .fs-bucket-sort {
    color: lightslategray;
    cursor: move;
  }
  .fs-bucket-delete {
    text-align: center;
    line-height: 32px;
    vertical-align: middle;
    color: rgb(211, 69, 69);
  }
}
.fs-bucket-drop {
  border:dashed 1px lightblue;
}
.fs-chart-list {
  .fs-chart-item {
    text-align: center;
    vertical-align: middle;
    margin-top: 16px;
    cursor: pointer;
    border-radius: 5px;
    border: solid 1px white;
    &:hover {
      color: rgb(0, 191, 179);
    }
    .anticon {
      font-size: 36px;
      margin: 12px 0 8px;
      transition: transform .3s ease-in-out;
      will-change: transform;
    }
  }
  .fs-chart-selected {
    border: solid 1px #D3DAE6;
    background-color: #f6f7fa;
  }
}
</style>
