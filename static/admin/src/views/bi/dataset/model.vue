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
    <div class="fs-relation-layout" v-show="step === 1">
      <div class="fs-relation-left">
        <a-card class="fs-ui-top">
          <a-form-model>
            <service-auto-complete :search="sourceService.list" v-model="sourceSelected" placeholder="检索选择数据源" :loading="sourceLoading" />
          </a-form-model>
        </a-card>
        <a-card class="fs-ui-content" title="数据表" :loading="sourceLoading">
          <ul>
            <li v-for="table in sourceTables" :key="table.name">
              <a-icon class="icon" :component="icons.biTable" />
              <span>{{ table.name }}</span>
            </li>
          </ul>
          <a-empty v-if="Object.values(sourceTables).length === 0" description="请选择有效数据源" />
        </a-card>
      </div>
      <div class="fs-relation-right"></div>
    </div>
  </section>
</template>

<script>
import icons from '@/assets/icons'
import sourceService from '@/service/bi/source'
import datasetService from '@/service/bi/dataset'

export default {
  components: {
    ServiceAutoComplete: () => import('@/components/Service/AutoComplete')
  },
  data () {
    return {
      icons,
      sourceService,
      loading: false,
      config: {
        ready: false,
        status: []
      },
      step: 1,
      steps: [
        { title: '基础信息', icon: 'solution' },
        { title: '关联关系', icon: 'deployment-unit' },
        { title: '字段映射', icon: 'experiment' },
        { title: '定时任务', icon: 'clock-circle' }
      ],
      sourceSelected: undefined,
      sourceLoading: false,
      sourceTables: {},
      form: {},
      rules: {
        name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
        status: [{ required: true, message: '请选择状态', trigger: 'change' }]
      }
    }
  },
  watch: {
    sourceSelected () {
      this.schema()
    }
  },
  methods: {
    schema () {
      if (this.sourceLoading || !this.sourceSelected) return false
      this.sourceLoading = true
      sourceService.schema({ id: this.sourceSelected }).then(result => {
        this.sourceTables = result.code === 0 ? result.data : {}
        this.sourceLoading = false
      })
    },
    load () {
      if (!this.$route.query.id) return false
      this.loading = true
      datasetService.info({ id: this.$route.query.id }).then(result => {
        if (result.code !== 0) return false
        this.form = result.data
        try {
          if (result.data.content) {
            // const data = JSON.parse(result.data.content)
          }
        } catch (e) {
          this.$error({ title: '数据解析异常', content: e.message })
        } finally {
          this.loading = false
        }
      })
    },
    collect () {
      return Object.assign({})
    },
    submit () {
      this.$refs.form.validate(valid => {
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
.fs-relation-layout {
  width: 100%;
  height: calc(100vh - 170px);
  .fs-relation-left {
    width: 350px;
    margin-right: 20px;
    .fs-ui-top {
      margin-bottom: 20px;
    }
    .fs-ui-content {
      & /deep/ .ant-card-body {
        padding: 0px;
        height: calc(100vh - 340px);
        overflow-y: auto;
        overflow-x: hidden;
      }
      li {
        font-size: 12px;
        display: block;
        line-height: 26px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
        padding: 5px 2px;
        content: attr(title);
        cursor: move;
        &:hover {
          color: #409eff;
        }
        .icon {
          margin-right: 6px;
          font-size: 14px;
        }
      }
    }
  }
  .fs-relation-right {
    width: calc(100% - 370px);
  }
}
</style>
