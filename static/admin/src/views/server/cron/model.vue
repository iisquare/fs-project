<template>
  <div class="fs-layout-box">
    <div class="fs-layout-header">
      <div class="fs-layout-back">
        <a-icon type="left" class="fs-ui-pointer" @click.native="$router.go(-1)" />
      </div>
      <div class="fs-layout-menu">
        <a-menu v-model="menus" mode="horizontal">
          <a-menu-item key="basic"><a-icon type="edit" />基础信息</a-menu-item>
          <a-menu-item key="schedule"><a-icon type="clock-circle" />定时调度</a-menu-item>
          <a-menu-item key="diagram"><a-icon type="branches" />流程设计</a-menu-item>
          <a-menu-item key="data"><a-icon type="money-collect" />默认参数</a-menu-item>
          <a-menu-item key="notify"><a-icon type="mail" />消息通知</a-menu-item>
        </a-menu>
      </div>
      <div class="fs-layout-action">
        <a-space>
          <a-button type="primary" icon="save" :loading="loading" @click="save">保存</a-button>
        </a-space>
      </div>
    </div>
    <div class="fs-layout-box" v-show="menus.indexOf('basic') !== -1">
      <a-form-model ref="form" :model="form" :rules="rules">
        <a-form-model-item label="流程名称" prop="name">
          <a-input v-model="form.name" placeholder="建议为服务主键标识" />
        </a-form-model-item>
        <a-form-model-item label="项目名称" prop="project">
          <a-input v-model="form.project" placeholder="建议按应用名称进行分组" />
        </a-form-model-item>
        <a-form-model-item label="排序">
          <a-input-number v-model="form.sort" :min="0" :max="200"></a-input-number>
        </a-form-model-item>
        <a-form-model-item label="描述">
          <a-textarea v-model="form.description" />
        </a-form-model-item>
      </a-form-model>
    </div>
    <div class="fs-layout-box" v-show="menus.indexOf('schedule') !== -1">
      <a-form-model :model="form" :rules="rules">
        <a-form-model-item label="定时任务" prop="expression">
          <a-input-search v-model="form.expression" placeholder="Crontab表达式，留空时不自动触发" enter-button="解析" @search="simulate" :allowClear="true" />
        </a-form-model-item>
        <a-form-model-item label="模拟触发">
          <a-textarea v-model="simulation" placeholder="输入表达式，点击解析按钮模拟触发结果" rows="5" disabled />
        </a-form-model-item>
      </a-form-model>
      <a-alert type="info" show-icon message="表达式格式：{秒} {分} {时} {日期} {月份} {星期} {年份}">
        <template slot="description">
          <p>（1）*：表示匹配该域的任意值。</p>
          <p>（2）?：只能用在{日期}和{星期}两个域，表示不为该域指定值。</p>
          <p>（3）-：表示范围。</p>
          <p>（4）/：表示起始时间开始触发，然后每隔固定时间触发一次。</p>
          <p>（5）,：表示列出枚举值。</p>
          <p>（6）L：表示最后，只能出现在{日期}和{星期}两个域，在最近有效工作日触发。</p>
          <p>（7）W：表示有效工作日(周一到周五),只能出现在{星期}域，在最近有效工作日触发。</p>
          <p>（8）LW：这两个字符可以连用，表示在某个月最后一个工作日，即最后一个星期五。</p>
          <p>（9）#：用于指定月份中的第几周的哪一天，只能出现在{星期}域。</p>
        </template>
      </a-alert>
    </div>
    <fs-diagram ref="diagram" v-model="diagram" v-if="menus.indexOf('diagram') !== -1" />
    <div class="fs-layout-box" v-if="menus.indexOf('data') !== -1">
      <code-editor ref="data" v-model="form.data" mode="javascript" :height="500" />
    </div>
    <div class="fs-layout-box" v-if="menus.indexOf('notify') !== -1">
      <a-form-model :model="notify">
        <a-form-model-item label="触发时机">
          <a-checkbox-group v-model="notify.stage" :options="config.stages" />
        </a-form-model-item>
        <a-form-model-item label="失败通知">
          <a-textarea v-model="notify.failure" placeholder="采用英文分号分割" />
        </a-form-model-item>
        <a-form-model-item label="成功通知">
          <a-textarea v-model="notify.success" placeholder="采用英文分号分割" />
        </a-form-model-item>
      </a-form-model>
    </div>
  </div>
</template>

<script>
import cronParser from 'cron-parser'
import DateUtil from '@/utils/date'
import config from './design/config'
import cronService from '@/service/server/cron'

export default {
  components: {
    CodeEditor: () => import('@/components/Editor/CodeEditor'),
    FsDiagram: () => import('./design/Diagram')
  },
  data () {
    return {
      config,
      loading: false,
      menus: ['basic'],
      form: {},
      simulation: '',
      rules: {
        name: [{ required: true, message: '请输入流程名称', trigger: 'blur' }],
        project: [{ required: true, message: '请输入项目名称', trigger: 'blur' }]
      },
      notify: {},
      diagram: {}
    }
  },
  methods: {
    simulate (value, event) {
      if (!value) {
        this.simulation = ''
        return true
      }
      try {
        const interval = cronParser.parseExpression(value)
        const result = []
        for (let i = 0; i < 5; i++) {
          result.push(DateUtil.format(interval.next()))
        }
        this.simulation = result.join('\r')
        return true
      } catch (e) {
        this.simulation = e.message
        return false
      }
    },
    load () {
      this.loading = true
      const param = {
        name: this.$route.query.name,
        project: this.$route.query.project
      }
      if (!param.name) {
        return (this.loading = false)
      }
      cronService.flowInfo(param).then(result => {
        if (result.code !== 0) return false
        try {
          if (result.data.notify) {
            this.notify = JSON.parse(result.data.notify)
          }
          if (result.data.content) {
            this.diagram = JSON.parse(result.data.content)
            this.$refs.diagram && this.$refs.diagram.reset()
          }
        } catch (e) {
          this.$error({ title: '数据解析异常', content: e.message })
        } finally {
          this.$set(this, 'form', result.data)
          this.$refs.data && this.$refs.data.setContent(this.form.data)
          this.loading = false
        }
      })
    },
    save () {
      if (this.$refs.diagram) this.diagram = this.$refs.diagram.collect()
      this.form.notify = JSON.stringify(this.notify)
      this.form.content = JSON.stringify(this.diagram)
      this.$refs.form.validate(valid => {
        if (!valid) this.menus = ['basic']
        if (!valid || this.loading) return false
        this.loading = true
        cronService.flowSave(this.form, { success: true }).then(result => {
          if (result.code === 0) {
            this.$set(this, 'form', result.data)
            this.$refs.data && this.$refs.data.setContent(this.form.data)
          }
          this.loading = false
        })
      })
    }
  },
  mounted () {
    this.load()
  }
}
</script>

<style lang="less" scoped>
.fs-ui-pointer {
  cursor: pointer;
}
.fs-layout-box {
  width: 100%;
  .fs-layout-header {
    width: 100%;
    height: 48px;
    border-bottom: 1px solid #e8e8e8;
    .fs-layout-back {
      width: 100px;
      height: 100%;
      display: inline-block;
      padding: 0px 15px;
    }
    .fs-layout-menu {
      width: calc(100% - 200px);
      height: 100%;
      display: inline-block;
      text-align: center;
    }
    .fs-layout-action {
      width: 100px;
      height: 100%;
      display: inline-block;
    }
  }
  .fs-layout-box  {
    width: 650px;
    padding: 15px;
    margin: 0 auto;
  }
}
</style>
