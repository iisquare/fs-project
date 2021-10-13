<template>
  <a-card :body-style="{padding: '24px 32px'}" :bordered="false">
    <a-collapse v-model="activeKey">
      <a-collapse-panel key="info" header="基础信息">
        <a-form-model layout="inline">
          <a-row :gutter="48">
            <a-col :md="12" :sm="24">
              <a-form-model-item label="流程名称">{{ workflow.name }}</a-form-model-item>
            </a-col>
            <a-col :md="12" :sm="24">
              <a-form-model-item label="业务标识">{{ workflow.historicProcessInstanceInfo.businessKey }}</a-form-model-item>
            </a-col>
            <a-col :md="12" :sm="24">
              <a-form-model-item label="发起人">{{ workflow.historicProcessInstanceInfo.startUserName }}</a-form-model-item>
            </a-col>
            <a-col :md="12" :sm="24">
              <a-form-model-item label="持续时间">{{ workflowConfig.duration(workflow.historicProcessInstanceInfo.durationInMillis) }}</a-form-model-item>
            </a-col>
            <a-col :md="12" :sm="24">
              <a-form-model-item label="开始时间">{{ DateUtil.dateRender(workflow.historicProcessInstanceInfo.startTime) }}</a-form-model-item>
            </a-col>
            <a-col :md="12" :sm="24">
              <a-form-model-item label="结束时间">{{ DateUtil.dateRender(workflow.historicProcessInstanceInfo.endTime) }}</a-form-model-item>
            </a-col>
            <a-col :md="24" :sm="24">
              <a-form-model-item label="流程状态"><approve-process-status :historic="workflow.historicProcessInstanceInfo" :runtime="workflow.processInstanceInfo" /></a-form-model-item>
            </a-col>
            <a-col :md="24" :sm="24">
              <a-form-model-item label="撤销原因" v-if="workflow.historicProcessInstanceInfo.deleteReason !== null">{{ workflow.historicProcessInstanceInfo.deleteReason }}</a-form-model-item>
            </a-col>
          </a-row>
        </a-form-model>
      </a-collapse-panel>
      <a-collapse-panel key="bpmn" header="流程图">
        <div class="fs-layout-canvas" ref="canvas"></div>
      </a-collapse-panel>
      <a-collapse-panel key="instance" header="流程节点">
        <a-table
          :columns="columns"
          :rowKey="record => record.id"
          :dataSource="workflow.historicTaskInstances"
          :pagination="false"
          :loading="loading"
          :bordered="false"
        >
          <template slot="expandedRowRender" slot-scope="record" style="margin: 0">
            <p>{{ record.description || '暂无描述' }}</p>
          </template>
        </a-table>
      </a-collapse-panel>
      <a-collapse-panel key="form" header="表单数据">
        <fs-form
          v-model="workflow.form"
          ref="workflowForm"
          :frame="workflow.formInfo"
          :authority="authority"
          :config="formConfig"
          v-if="workflow.formInfo"/>
      </a-collapse-panel>
    </a-collapse>
    <div class="fs-footer-action">
      <a-space>
        <a-button :loading="loading" @click="cancel">返回</a-button>
      </a-space>
    </div>
  </a-card>
</template>

<script>
import formConfig from '../form/design/config'
import FsForm from '../form/design/FsForm'
import approveService from '@/service/oa/approve'
import workflowService from '@/service/oa/workflow'
import BPMN from '../workflow/bpmn/bpmn'
import workflowConfig from '../workflow/design/config'
import DataUtil from '@/utils/data'
import DateUtil from '@/utils/date'

export default {
  components: { FsForm, ApproveProcessStatus: () => import('./ProcessStatus') },
  data () {
    return {
      DateUtil,
      formConfig,
      workflowConfig,
      loading: false,
      activeKey: ['info', 'bpmn', 'instance', 'form'],
      workflow: {
        id: 0,
        processInstanceInfo: {},
        historicProcessInstanceInfo: {},
        historicTaskInstances: [],
        historicActivityInstances: []
      },
      columns: [
        { title: 'ID', dataIndex: 'id' },
        { title: '节点名称', dataIndex: 'name' },
        { title: '处理人', dataIndex: 'assigneeName' },
        { title: '审批备注', dataIndex: 'audit.message' },
        { title: '创建时间', dataIndex: 'createTime', customRender: DateUtil.dateRender, width: 170 },
        { title: '签收时间', dataIndex: 'claimTime', customRender: DateUtil.dateRender, width: 170 },
        { title: '结束时间', dataIndex: 'endTime', customRender: DateUtil.dateRender, width: 170 },
        { title: '工作时间', dataIndex: 'workTimeInMillis', customRender: this.duration, width: 170 },
        { title: '持续时间', dataIndex: 'durationInMillis', customRender: this.duration, width: 170 }
      ]
    }
  },
  computed: {
    authority () {
      return DataUtil.filtration(this.workflow.authority, { viewable: 'viewable' })
    },
    service () {
      /**
       * 根据taskId决定调用服务
       * 未定义 - 流程管理，调用管理接口
       * 为空时 - 历史任务，调用审批接口
       * 非空时 - 待办任务，调用审批接口
       */
      if (Object.keys(this.$route.query).indexOf('taskId') === -1) {
        return workflowService
      } else {
        return approveService
      }
    }
  },
  methods: {
    duration (text, record, index) {
      return this.workflowConfig.duration(text)
    },
    async repaint (workflow) {
      try {
        await this.bpmn.modeler.importXML(workflow.content)
        this.bpmn.colorful(workflow.historicActivityInstances)
        this.bpmn.canvas.zoom('fit-viewport')
      } catch (e) {
        this.$error({ title: '解析流程图异常', content: e.message })
      }
    },
    load () {
      this.loading = true
      const param = { processInstanceId: this.$route.query.processInstanceId, taskId: this.$route.query.taskId }
      this.service.process(param).then(async result => {
        this.loading = false
        if (result.code === 0) {
          result.data.historicTaskInstances.forEach(instance => {
            Object.assign(instance, this.workflowConfig.audit(result.data.comments, instance.id))
          })
          Object.assign(this.workflow, result.data)
          this.repaint(this.workflow)
        }
      })
    },
    cancel () {
      this.$router.go(-1)
    }
  },
  mounted () {
    this.bpmn = new BPMN(this.$refs.canvas, workflowConfig, false)
    this.load()
  }
}
</script>

<style lang="less" scoped>
@import '../workflow/bpmn/bpmn.less';
.fs-footer-action {
  padding-top: 15px;
  text-align: center;
}
</style>
