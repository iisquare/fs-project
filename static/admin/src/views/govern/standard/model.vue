<template>
  <div class="fs-layout-box">
    <div class="fs-layout-header">
      <div class="fs-layout-back">
        <a-icon type="left" class="fs-ui-point" @click.native="$router.go(-1)" />
      </div>
      <div class="fs-layout-menu">
        <a-menu v-model="menus" mode="horizontal">
          <a-menu-item key="basic"><a-icon type="edit" />基础信息</a-menu-item>
          <a-menu-item key="assess"><a-icon type="alert" />评估参数</a-menu-item>
        </a-menu>
      </div>
      <div class="fs-layout-action">
        <a-space>
          <a-button type="primary" icon="save" :loading="loading" @click="save">保存</a-button>
        </a-space>
      </div>
    </div>
    <div class="fs-layout-box" v-show="menus.indexOf('basic') !== -1">
      <a-form-model ref="form" :model="form" :rules="rules" :label-col="{ span: 5 }" :wrapper-col="{ span: 18 }">
        <a-form-model-item label="主键" prop="id">
          <a-input v-model="form.id" auto-complete="off" />
        </a-form-model-item>
        <a-form-model-item label="名称" prop="name">
          <a-input v-model="form.name" auto-complete="off"></a-input>
        </a-form-model-item>
        <a-form-model-item label="状态" prop="status">
          <a-select v-model="form.status" placeholder="请选择">
            <a-select-option v-for="(value, key) in config.status" :key="key" :value="key">{{ value }}</a-select-option>
          </a-select>
        </a-form-model-item>
        <a-form-model-item label="排序">
          <a-input-number v-model="form.sort" :min="0"></a-input-number>
        </a-form-model-item>
        <a-form-model-item label="描述">
          <a-textarea v-model="form.description" />
        </a-form-model-item>
      </a-form-model>
    </div>
    <div class="fs-layout-box" v-show="menus.indexOf('assess') !== -1">
      <a-form-model :model="form" :rules="rules" :label-col="{ span: 5 }" :wrapper-col="{ span: 18 }">
        <a-form-model-item label="数据源" prop="source">
          <a-input v-model="form.source" placeholder="数据源编码"></a-input>
        </a-form-model-item>
        <a-form-model-item label="数据标准">
          <a-textarea v-model="form.content.code" placeholder="换行分割，支持目录路径" />
        </a-form-model-item>
        <a-form-model-item label="包含表名">
          <a-textarea v-model="form.content.include" placeholder="换行分割，非空启用包含模式" />
        </a-form-model-item>
        <a-form-model-item label="排除表名">
          <a-textarea v-model="form.content.exclude" placeholder="换行分割，支持正则表达式" />
        </a-form-model-item>
      </a-form-model>
    </div>
  </div>
</template>

<script>
import assessService from '@/service/govern/assess'

export default {
  data () {
    return {
      loading: false,
      menus: ['basic'],
      form: {},
      rules: {
        name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
        status: [{ required: true, message: '请选择状态', trigger: 'change' }]
      },
      config: {
        ready: false,
        status: {}
      }
    }
  },
  methods: {
    load () {
      this.loading = true
      if (!this.form.id) {
        return (this.loading = false)
      }
      assessService.info(this.form).then(result => {
        if (result.code !== 0) return false
        result.data.status += ''
        this.$set(this, 'form', result.data)
        this.loading = false
      })
    },
    save () {
      this.$refs.form.validate(valid => {
        if (!valid) this.menus = ['basic']
        if (!valid || this.loading) return false
        this.loading = true
        assessService.save(this.form, { success: true }).then(result => {
          if (result.code === 0) {
            result.data.status += ''
            this.$set(this, 'form', result.data)
          }
          this.loading = false
        })
      })
    }
  },
  created () {
    this.form = { id: this.$route.query.id, content: {} }
  },
  mounted () {
    this.load()
    assessService.config().then((result) => {
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
