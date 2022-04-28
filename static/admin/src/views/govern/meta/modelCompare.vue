<template>
  <div class="fs-layout-box">
    <div class="fs-layout-header">
      <div class="fs-layout-back">
        <a-icon type="left" class="fs-ui-point" @click.native="$router.go(-1)" />
      </div>
      <div class="fs-layout-menu">
        <a-menu v-model="menus" mode="horizontal">
          <a-menu-item key="type"><a-icon type="block" />类型选择</a-menu-item>
          <a-menu-item key="arg"><a-icon type="account-book" />参数配置</a-menu-item>
          <a-menu-item key="column"><a-icon type="profile" />字段对比</a-menu-item>
          <a-menu-item key="basic"><a-icon type="edit" />信息确认</a-menu-item>
        </a-menu>
      </div>
      <div class="fs-layout-action">
        <a-space>
          <a-button type="primary" icon="save" :loading="loading" @click="save">保存</a-button>
        </a-space>
      </div>
    </div>
    <div class="fs-layout-type" v-show="menus.indexOf('type') !== -1">
      <div class="fs-type-item" :key="item.type" v-for="item in config.modelCompareTypes">
        <div :class="['fs-type-box', item.type === compareType && 'fs-type-selected']" @click="compareSelect(item)">
          <div class="fs-type-logo" :style="'background-image: url(' + item.logo + ');'"></div>
          <div class="fs-type-title">{{ item.title }}</div>
        </div>
      </div>
    </div>
    <div class="fs-layout-box" v-show="menus.indexOf('basic') !== -1">
      <a-form-model ref="form" :model="form" :rules="rules">
        <a-form-model-item label="所属包" prop="catalog">
          <a-input v-model="form.catalog" placeholder="所属包路径" />
        </a-form-model-item>
        <a-form-model-item label="编码" prop="code">
          <a-input v-model="form.code" placeholder="元数据编码" />
        </a-form-model-item>
        <a-form-model-item label="名称" prop="name">
          <a-input v-model="form.name" placeholder="元数据名称" />
        </a-form-model-item>
        <a-form-model-item label="排序">
          <a-input-number v-model="form.sort" :min="0" :max="200"></a-input-number>
        </a-form-model-item>
        <a-form-model-item label="描述">
          <a-textarea v-model="form.description" />
        </a-form-model-item>
      </a-form-model>
    </div>
    <model-compare-column ref="column" :source="compareColumns" :config="config" v-model="form.columns" v-if="menus.indexOf('column') !== -1" />
    <model-compare
      :menus.sync="menus"
      :type="compareType"
      :form="form"
      :config="config"
      v-model="compareColumns"
      v-show="menus.indexOf('arg') !== -1" />
  </div>
</template>

<script>
import config from './design/config'
import modelService from '@/service/govern/model'

export default {
  components: {
    ModelCompare: () => import('./design/ModelCompare'),
    ModelCompareColumn: () => import('./design/ModelCompareColumn')
  },
  data () {
    return {
      loading: false,
      menus: ['type'],
      form: {},
      rules: {
        code: [{ required: true, message: '请输入元数据编码', trigger: 'blur' }],
        name: [{ required: true, message: '请输入元数据名称', trigger: 'blur' }]
      },
      config: Object.assign(config, {
        ready: false,
        types: {},
        columnTypes: []
      }),
      compareType: '',
      compareColumns: []
    }
  },
  methods: {
    compareSelect (item) {
      this.compareType = item.type
      this.menus = ['arg']
    },
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
  .fs-layout-type {
    width: 100%;
    .fs-type-item {
      float: left;
      width: 25%;
      min-width: 250px;
      height: 300px;
      padding: 25px;
      text-align: center;
    }
    .fs-type-box {
      margin: 0 auto;
      width: 200px;
      cursor: pointer;
      border: #e8e8e8 solid 1px;
      &:hover {
        box-shadow: 5px 5px 8px #888888;
      }
    }
    .fs-type-selected {
      border: #8ddddd solid 1px;
    }
    .fs-type-logo {
      width: 100px;
      height: 100px;
      margin: 40px;
      background-position: center;
      background-repeat: no-repeat;
      background-size: contain;
    }
    .fs-type-title {
      width: 100%;
      height: 50px;
      line-height: 50px;
      font-weight: 800;
      text-align: center;
      border-top: #e8e8e8 solid 1px;
      background-color: #fafafa;
    }
  }
}
</style>
