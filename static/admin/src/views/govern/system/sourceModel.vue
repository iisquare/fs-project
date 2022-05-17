<template>
  <div class="fs-layout-box">
    <div class="fs-layout-header">
      <div class="fs-layout-back">
        <a-icon type="left" class="fs-ui-point" @click.native="$router.go(-1)" />
      </div>
      <div class="fs-layout-menu">
        <a-menu v-model="menus" mode="horizontal">
          <a-menu-item key="basic"><a-icon type="edit" />基础信息</a-menu-item>
          <a-menu-item key="arg"><a-icon type="account-book" />参数配置</a-menu-item>
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
        <a-form-model-item label="编码" prop="code">
          <a-input v-model="form.code" placeholder="数据源编码" />
        </a-form-model-item>
        <a-form-model-item label="名称" prop="name">
          <a-input v-model="form.name" placeholder="数据源名称" />
        </a-form-model-item>
        <a-form-model-item label="类型" prop="type">
          <a-select v-model="form.type" placeholder="请选择" :allowClear="true">
            <a-select-option v-for="item in config.sourceTypes" :key="item.type" :value="item.type">{{ item.title }}</a-select-option>
          </a-select>
        </a-form-model-item>
        <a-form-model-item label="版本" prop="version">
          <a-input v-model="form.version" placeholder="数据源版本" />
        </a-form-model-item>
        <a-form-model-item label="状态" prop="status">
          <a-select v-model="form.status" placeholder="请选择" :allowClear="true">
            <a-select-option v-for="(value, key) in config.status" :key="key" :value="key">{{ value }}</a-select-option>
          </a-select>
        </a-form-model-item>
        <a-form-model-item label="排序">
          <a-input-number v-model="form.sort" :min="0" :max="200"></a-input-number>
        </a-form-model-item>
        <a-form-model-item label="描述">
          <a-textarea v-model="form.description" />
        </a-form-model-item>
      </a-form-model>
    </div>
    <source-arg
      :menus.sync="menus"
      :form="form"
      :config="config"
      v-model="form.content"
      v-show="menus.indexOf('arg') !== -1" />
  </div>
</template>

<script>
import config from './design/config'
import sourceService from '@/service/govern/source'

export default {
  components: {
    SourceArg: () => import('./design/SourceArg')
  },
  data () {
    return {
      loading: false,
      menus: ['basic'],
      form: {},
      rules: {
        code: [{ required: true, message: '请输入数据源编码', trigger: 'blur' }],
        name: [{ required: true, message: '请输入数据源名称', trigger: 'blur' }],
        type: [{ required: true, message: '请选择数据源类型', trigger: 'change' }],
        status: [{ required: true, message: '请选择状态', trigger: 'change' }]
      },
      config: Object.assign(config, {
        ready: false,
        status: {}
      })
    }
  },
  methods: {
    load () {
      this.loading = true
      if (!this.form.code) {
        return (this.loading = false)
      }
      sourceService.info(this.form).then(result => {
        if (result.code === 0) {
          result.data.status = result.data.status + ''
          this.$set(this, 'form', result.data)
        }
        this.loading = false
      })
    },
    save () {
      this.$refs.form.validate(valid => {
        if (!valid) this.menus = ['basic']
        if (!valid || this.loading) return false
        this.loading = true
        sourceService.save(this.form, { success: true }).then(result => {
          if (result.code === 0) {
            result.data.status = result.data.status + ''
            this.$set(this, 'form', result.data)
          }
          this.loading = false
        })
      })
    }
  },
  created () {
    this.form = {
      content: {},
      code: this.$route.query.code
    }
  },
  mounted () {
    this.load()
    sourceService.config().then((result) => {
      this.config.ready = true
      if (result.code === 0) {
        Object.assign(this.config, result.data)
      }
    })
  }
}
</script>

<style lang="less" scoped>
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
