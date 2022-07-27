<template>
  <section>
    <a-card :bordered="false">
      <template slot="extra">
        <a-space>
          <a-button :loading="loading" @click.native="$router.go(-1)">返回</a-button>
          <a-button type="primary" :loading="loading" @click="reload">刷新</a-button>
        </a-space>
      </template>
      <a-descriptions :bordered="true" style="margin-bottom: 25px;">
        <a-descriptions-item label="ID">{{ form.id }}</a-descriptions-item>
        <a-descriptions-item label="项目名称">{{ form.project }}</a-descriptions-item>
        <a-descriptions-item label="流程名称">{{ form.name }}</a-descriptions-item>
        <a-descriptions-item label="并发度">{{ form.concurrent }}</a-descriptions-item>
        <a-descriptions-item label="并发策略">{{ form.concurrency }}</a-descriptions-item>
        <a-descriptions-item label="失败策略">{{ form.failure }}</a-descriptions-item>
        <a-descriptions-item label="运行状态">{{ form.state }}</a-descriptions-item>
        <a-descriptions-item label="开始时间">{{ DateUtil.format(form.createdTime) }}</a-descriptions-item>
        <a-descriptions-item label="结束时间">{{ DateUtil.format(form.updatedTime) }}</a-descriptions-item>
        <a-descriptions-item label="持续时间" :span="3">{{ form.durationPretty }}</a-descriptions-item>
      </a-descriptions>
      <div ref="canvas" id="fs-flow-canvas" />
      <a-table
        :columns="columns"
        :rowKey="(record, index) => record.stageId"
        :dataSource="form.stages"
        :expandedRowKeys="expandedRowKeys"
        @expandedRowsChange="(expandedRows) => this.expandedRowKeys = expandedRows"
        :pagination="false"
        :loading="loading"
        :bordered="true"
      >
        <span slot="timeline" slot-scope="text, record">
          <a-slider :min="form.min" :max="form.max" :range="true" :value="[record.min, record.max]" />
        </span>
        <span slot="action" slot-scope="text, record">
          <a-button-group>
            <a-button type="link" size="small" @click="detail(record)">详情</a-button>
          </a-button-group>
        </span>
      </a-table>
    </a-card>
    <!--展示界面-->
    <a-drawer title="节点信息" :visible="stageVisible" :width="'80%'" @close="() => stageVisible = false">
      <a-descriptions :bordered="true">
        <a-descriptions-item label="标识">{{ stageSelected.stageId }}</a-descriptions-item>
        <a-descriptions-item label="依赖" :span="2">{{ stageSelected.depend }}</a-descriptions-item>
        <a-descriptions-item label="类型">{{ stageSelected.type }}</a-descriptions-item>
        <a-descriptions-item label="名称">{{ stageSelected.name }}</a-descriptions-item>
        <a-descriptions-item label="标题">{{ stageSelected.title }}</a-descriptions-item>
        <a-descriptions-item label="配置" :span="3">
          <a-checkbox v-model="stageSelected.skipped" :disabled="true">跳过执行</a-checkbox>
        </a-descriptions-item>
        <a-descriptions-item label="参数" :span="3">{{ stageSelected.data }}</a-descriptions-item>
        <a-descriptions-item label="输出" :span="3"><p v-html="stageSelected.content"></p></a-descriptions-item>
      </a-descriptions>
    </a-drawer>
  </section>
</template>

<script>
import DateUtil from '@/utils/date'
import cronService from '@/service/server/cron'
import UIUtil from '@/utils/ui'
import Flow from '@/components/X6/flow'

export default {
  data () {
    return {
      UIUtil,
      DateUtil,
      columns: [
        { title: '节点', dataIndex: 'title' },
        { title: '类型', dataIndex: 'type' },
        { title: '时间线', scopedSlots: { customRender: 'timeline' }, width: 300 },
        { title: '开始时间', dataIndex: 'runTime', customRender: DateUtil.dateRender },
        { title: '结束时间', dataIndex: 'updatedTime', customRender: DateUtil.dateRender },
        { title: '持续时长', dataIndex: 'durationPretty' },
        { title: '运行状态', dataIndex: 'state' },
        { title: '操作', scopedSlots: { customRender: 'action' }, width: 80 }
      ],
      form: {
        stages: []
      },
      loading: false,
      expandedRowKeys: [],
      stageVisible: false,
      stageSelected: {},
      flow: null
    }
  },
  methods: {
    reload () {
      this.loading = true
      const param = { logId: this.$route.query.logId }
      cronService.flowLogStages(param).then(result => {
        if (result.code === 0) {
          const time = new Date().getTime()
          result.data.stages.forEach(item => {
            if (item.runTime === 0) {
              Object.assign(item, { min: 0, max: 0 })
            } else {
              Object.assign(item, {
                min: item.runTime - result.data.createdTime,
                max: (['RUNNING', 'WAITING'].indexOf(item.state) === -1 ? item.updatedTime : time) - result.data.createdTime
              })
            }
          })
          Object.assign(this.form, result.data, {
            min: 0,
            max: (result.data.state === 'RUNNING' ? time : result.data.updatedTime) - result.data.createdTime,
            stages: UIUtil.tableTree(result.data.stages, '', 'stageId')
          })
          this.expandedRowKeys = result.data.stages.map(item => item.stageId)
          this.flow.reset(JSON.parse(this.form.content))
          this.flow.fitting()
        }
        this.loading = false
      })
    },
    detail (record) {
      this.stageSelected = Object.assign({}, record, {
        skipped: record.skipped !== 0,
        content: record.content.replaceAll('\n', '<br>')
      })
      this.stageVisible = true
    }
  },
  mounted () {
    this.reload()
    this.flow = new Flow(this.$refs.canvas, { readonly: true })
  }
}
</script>
<style lang="less" scoped>
@import url('../../../components/X6/flow.less');

/deep/ .ant-slider-handle {
  display: none;
}
/deep/ #fs-flow-canvas {
  width: 100%;
  height: 300px;
  overflow: hidden;
  margin-bottom: 25px;
  border: 1px solid #e8e8e8;
  .widget()
}
</style>
