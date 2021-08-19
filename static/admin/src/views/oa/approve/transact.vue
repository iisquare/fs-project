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
              <a-form-model-item label="业务标识">{{ workflow.processInstanceInfo.businessKey }}</a-form-model-item>
            </a-col>
            <a-col :md="12" :sm="24">
              <a-form-model-item label="发起人">{{ workflow.processInstanceInfo.startUserName }}</a-form-model-item>
            </a-col>
            <a-col :md="12" :sm="24">
              <a-form-model-item label="开始时间">{{ dateRender(workflow.processInstanceInfo.startTime) }}</a-form-model-item>
            </a-col>
            <a-col :md="24" :sm="24">
              <a-form-model-item label="单据描述">{{ workflow.description }}</a-form-model-item>
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
          :authority="workflow.authority"
          :config="formConfig"
          v-if="workflow.formInfo"/>
        <a-empty v-else />
      </a-collapse-panel>
      <a-collapse-panel key="audit" header="备注信息">
        <approve-form-audit v-model="workflow.audit" :frame="workflow.formInfo" :config="formConfig" v-if="workflow.formInfo" />
      </a-collapse-panel>
    </a-collapse>
    <div class="fs-footer-action">
      <a-space>
        <a-button type="primary" :loading="loading" @click="submit(false, true, false)">提交</a-button>
        <a-button type="danger" :loading="loading" @click="submit(true, true, false)">强制提交</a-button>
        <a-button type="primary" :loading="loading" @click="submit(false, false, false)">保存</a-button>
        <a-button type="danger" :loading="loading" @click="submit(true, false, true)" v-if="rejectable">直接驳回</a-button>
        <a-button type="danger" :loading="loading" @click="submit(false, true, true)" v-if="rejectable">保存并驳回</a-button>
        <a-button :loading="loading" @click="cancel">返回</a-button>
      </a-space>
    </div>
  </a-card>
</template>

<script>
import formConfig from '../form/design/config'
import FsForm from '../form/design/FsForm'
import BPMN from '../workflow/bpmn/bpmn'
import workflowConfig from '../workflow/design/config'
import approveService from '@/service/oa/approve'
import DateUtil from '@/utils/date'

export default {
  components: { FsForm, ApproveFormAudit: () => import('./FormAudit') },
  data () {
    return {
      formConfig,
      workflowConfig,
      loading: false,
      activeKey: ['info', 'bpmn', 'instance', 'form', 'audit'],
      workflow: {
        id: 0,
        name: '',
        taskId: 0,
        form: {},
        audit: { local: false, message: '' },
        processInstanceInfo: {},
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
    userInfo () {
      return this.$store.state.user.data.info
    },
    rejectable () {
      return Number.parseInt(this.workflow.processInstanceInfo.startUserId) !== this.userInfo.id
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
      approveService.transact({ taskId: this.$route.query.taskId }).then(async result => {
        this.loading = false
        if (result.code === 0) {
          result.data.historicTaskInstances.forEach(instance => {
            Object.assign(instance, this.workflowConfig.audit(result.data.comments, instance.id))
          })
          Object.assign(this.workflow, result.data, this.workflowConfig.audit(result.data.comments, result.data.taskId))
          this.repaint(this.workflow)
        }
      })
    },
    submit (modeForce, modeComplete, modeReject) {
      this.$refs.workflowForm.validate(valid => {
        if (!valid) {
          this.$message.warning('数据校验不通过')
          if (!modeForce) return
        }
        if (this.loading) return false
        const data = { taskId: this.workflow.taskId, form: this.workflow.form, audit: this.workflow.audit, modeForce, modeComplete, modeReject }
        this.loading = true
        approveService.complete(data, { success: true }).then(result => {
          this.loading = false
          this.formLoading = false
          if (result.code === 0) {
            this.$router.push({ path: '/oa/approve/assignee' })
          }
        })
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
.fs-footer-action {
  padding-top: 15px;
  text-align: center;
}
</style>
