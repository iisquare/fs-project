<template>
  <a-card :body-style="{padding: '24px 32px'}" :bordered="false">
    <a-collapse v-model="activeKey">
      <a-collapse-panel key="info" header="基础信息">
        <a-form-model layout="inline">
          <a-row :gutter="48">
            <a-col :md="24" :sm="24">
              <a-form-model-item label="单据名称">{{ workflow.name }}</a-form-model-item>
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
        <a-button type="primary" :loading="loading" @click="submit(false, true)">提交</a-button>
        <a-button type="danger" :loading="loading" @click="submit(true, true)">强制提交</a-button>
        <a-button type="primary" :loading="loading" @click="submit(false, false)">保存</a-button>
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

export default {
  components: { FsForm, ApproveFormAudit: () => import('./FormAudit') },
  data () {
    return {
      formConfig,
      activeKey: ['info', 'bpmn', 'form', 'audit'],
      loading: false,
      workflow: {
        id: 0,
        name: '',
        form: {},
        audit: { local: false, message: '' }
      }
    }
  },
  methods: {
    async repaint (workflow) {
      try {
        await this.bpmn.modeler.importXML(workflow.content)
        this.bpmn.canvas.zoom('fit-viewport')
      } catch (e) {
        this.$error({ title: '解析流程图异常', content: e.message })
      }
    },
    load () {
      this.loading = true
      approveService.form({ workflowId: this.$route.query.id, withForm: true }).then(async result => {
        this.loading = false
        if (result.code === 0) {
          Object.assign(this.workflow, result.data)
          this.repaint(this.workflow)
        }
      })
    },
    submit (modeForce, modeComplete) {
      this.$refs.workflowForm.validate(valid => {
        if (!valid) {
          this.$message.warning('数据校验不通过')
          if (!modeForce) return
        }
        if (this.loading) return false
        const data = { workflowId: this.workflow.id, form: this.workflow.form, audit: this.workflow.audit, modeForce, modeComplete }
        this.loading = true
        approveService.submit(data).then(result => {
          this.loading = false
          if (result.code === 0) {
            this.$router.push({ path: '/oa/approve/history' })
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
@import '../workflow/bpmn/bpmn.less';
.fs-footer-action {
  padding-top: 15px;
  text-align: center;
}
</style>
