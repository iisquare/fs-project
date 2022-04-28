<template>
  <div class="fs-layout-box">
    <div class="fs-layout-header">
      <div class="fs-layout-back">
        <a-icon type="left" class="fs-ui-point" @click.native="$router.go(-1)" />
      </div>
      <div class="fs-layout-menu">
        <a-menu v-model="menus" mode="horizontal">
          <a-menu-item key="basic"><a-icon type="edit" />基础信息</a-menu-item>
          <a-menu-item key="column"><a-icon type="profile" />字段属性</a-menu-item>
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
        <a-form-model-item label="所属包" prop="catalog">
          <a-input v-model="form.catalog" placeholder="所属包路径" disabled />
        </a-form-model-item>
        <a-form-model-item label="编码" prop="code">
          <a-input v-model="form.code" placeholder="元数据编码" />
        </a-form-model-item>
        <a-form-model-item label="名称" prop="name">
          <a-input v-model="form.name" placeholder="元数据名称" />
        </a-form-model-item>
        <a-form-model-item label="主键" prop="pk">
          <a-select mode="tags" v-model="form.pk" placeholder="输入或选择字段编码">
            <a-select-option v-for="(item, index) in form.columns" :key="index" :value="item.code">{{ item.code }}</a-select-option>
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
    <model-column ref="column" :config="config" v-model="form.columns" v-if="menus.indexOf('column') !== -1" />
  </div>
</template>

<script>
import config from './design/config'
import modelService from '@/service/govern/model'

export default {
  components: {
    ModelColumn: () => import('./design/ModelColumn')
  },
  data () {
    return {
      loading: false,
      menus: ['basic'],
      form: {},
      rules: {
        code: [{ required: true, message: '请输入元数据编码', trigger: 'blur' }],
        name: [{ required: true, message: '请输入元数据名称', trigger: 'blur' }]
      },
      config: Object.assign(config, {
        ready: false,
        types: {},
        columnTypes: []
      })
    }
  },
  methods: {
    load () {
      this.loading = true
      if (!this.form.code) {
        return (this.loading = false)
      }
      modelService.info(this.form).then(result => {
        if (result.code !== 0) return false
        this.$set(this, 'form', result.data)
        this.loading = false
      })
    },
    save () {
      if (this.$refs.column) this.form.columns = this.$refs.column.collect()
      this.$refs.form.validate(valid => {
        if (!valid) this.menus = ['basic']
        if (!valid || this.loading) return false
        this.loading = true
        modelService.save(this.form, { success: true }).then(result => {
          if (result.code === 0) {
            this.$set(this, 'form', result.data)
            this.$refs.column && this.$refs.column.reset()
          }
          this.loading = false
        })
      })
    }
  },
  created () {
    this.form = {
      type: 'table',
      columns: [],
      catalog: this.$route.query.catalog,
      code: this.$route.query.code
    }
  },
  mounted () {
    this.load()
    modelService.config().then((result) => {
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
