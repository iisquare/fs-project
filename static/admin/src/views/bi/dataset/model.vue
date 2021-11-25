<template>
  <section>
    <a-card class="fs-ui-ctl">
      <div class="fs-ui-left">
        <a-steps v-model="step" type="navigation">
          <a-step :title="item.title" v-for="(item, index) in steps" :key="index">
            <a-icon slot="icon" :type="item.icon" />
          </a-step>
        </a-steps>
      </div>
      <div class="fs-ui-right">
        <a-space>
          <a-divider type="vertical" />
          <a-button type="primary" :loading="loading" @click="submit">保存</a-button>
          <a-button :loading="loading" @click="cancel">返回</a-button>
        </a-space>
      </div>
    </a-card>
    <a-card v-show="step === 0">
      <a-form-model ref="form" :model="form" :rules="rules" :label-col="{ span: 5 }" :wrapper-col="{ span: 12 }">
        <a-form-model-item label="ID" prop="id">
          <a-input v-model="form.id" auto-complete="off"></a-input>
        </a-form-model-item>
        <a-form-model-item label="名称" prop="name">
          <a-input v-model="form.name" auto-complete="off"></a-input>
        </a-form-model-item>
        <a-form-model-item label="集合" prop="collection">
          <a-input v-model="form.collection" placeholder="由计算引擎自动生成"></a-input>
        </a-form-model-item>
        <a-form-model-item label="排序">
          <a-input-number v-model="form.sort" :min="0" :max="200"></a-input-number>
        </a-form-model-item>
        <a-form-model-item label="状态" prop="status">
          <a-select v-model="form.status" placeholder="请选择">
            <a-select-option v-for="(value, key) in config.status" :key="key" :value="key">{{ value }}</a-select-option>
          </a-select>
        </a-form-model-item>
        <a-form-model-item label="描述">
          <a-textarea v-model="form.description" />
        </a-form-model-item>
      </a-form-model>
    </a-card>
    <fs-relation ref="relation" v-model="relation" v-if="step === 1" />
    <fs-table ref="table" v-model="table" :relation="relation" v-if="step === 2" />
    <a-card v-show="step === 3" class="fs-ui-schedule">
      <a-form-model :model="schedule" :label-col="{ span: 5 }" :wrapper-col="{ span: 12 }">
        <a-form-model-item label="处理方式">
          <a-radio-group name="schedule" v-model="schedule.mode">
            <a-radio :value="mode.value" :key="mode.value" v-for="mode in modes">{{ mode.label }}</a-radio>
          </a-radio-group>
        </a-form-model-item>
      </a-form-model>
    </a-card>
  </section>
</template>

<script>
import datasetService from '@/service/bi/dataset'
import { triggerWindowResizeEvent } from '@/utils/util'

export default {
  components: {
    FsRelation: () => import('./design/Relation'),
    FsTable: () => import('./design/Table')
  },
  data () {
    return {
      loading: false,
      config: {
        ready: false,
        status: []
      },
      step: 0,
      steps: [
        { title: '基础信息', icon: 'solution' },
        { title: '关联关系', icon: 'deployment-unit' },
        { title: '字段映射', icon: 'experiment' },
        { title: '计划任务', icon: 'clock-circle' }
      ],
      form: {},
      rules: {
        name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
        status: [{ required: true, message: '请选择状态', trigger: 'change' }]
      },
      relation: {},
      table: [],
      schedule: { mode: '' },
      modes: [
        { label: '实时计算', value: '' },
        { label: '静态数据', value: 'static' }
      ]
    }
  },
  methods: {
    load () {
      if (!this.$route.query.id) return false
      this.loading = true
      datasetService.info({ id: this.$route.query.id }).then(result => {
        if (result.code !== 0) return false
        this.form = result.data
        try {
          if (result.data.content) {
            const data = JSON.parse(result.data.content)
            if (data.relation) this.relation = data.relation
            if (data.table) this.table = data.table
            if (data.schedule) Object.assign(this.schedule, data.schedule)
            this.$nextTick(() => {
              this.$refs.relation && this.$refs.relation.reset()
              this.$refs.table && this.$refs.table.reset()
            })
          }
        } catch (e) {
          this.$error({ title: '数据解析异常', content: e.message })
        } finally {
          this.loading = false
          triggerWindowResizeEvent()
        }
      })
    },
    collect () {
      if (this.$refs.relation) this.relation = this.$refs.relation.collect()
      if (this.$refs.table) this.table = this.$refs.table.collect()
      return Object.assign({
        relation: this.relation,
        table: this.table,
        schedule: this.schedule
      })
    },
    submit () {
      this.$refs.form.validate(valid => {
        if (!valid) this.step = 0
        if (!valid || this.loading) return false
        this.loading = true
        let data = this.collect()
        if (data === null) return (this.loading = false)
        data = Object.assign({}, this.form, { content: JSON.stringify(data) })
        datasetService.save(data, { success: true }).then(result => {
          if (result.code === 0) {
            this.form = result.data
          }
          this.loading = false
        })
      })
    },
    cancel () {
      this.$router.go(-1)
    }
  },
  mounted () {
    datasetService.config().then((result) => {
      this.config.ready = true
      if (result.code === 0) {
        Object.assign(this.config, result.data)
      }
    })
    this.load()
  }
}
</script>

<style lang="less" scoped>
.fs-ui-ctl {
  margin-bottom: 20px;
  .fs-ui-left {
    display: inline-block;
    width: calc(100% - 200px);
    vertical-align: middle;
  }
  .fs-ui-right {
    display: inline-block;
    width: 200px;
    vertical-align: middle;
  }
  & /deep/ .ant-card-body {
    padding: 0px;
    margin-bottom: -1px;
  }
  & /deep/ .ant-steps-item-icon {
    background: none;
  }
}
.fs-ui-schedule {
  & /deep/ .ant-radio-wrapper {
    display: block;
    line-height: 32px;
    span {
      vertical-align: middle;
    }
  }
}
</style>
