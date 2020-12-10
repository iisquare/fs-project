<template>
  <section>
    <a-alert :message="tips.reload" type="warning" show-icon :closable="true" class="alert-tip" />
    <a-card style="margin-bottom: 25px;" title="服务信息">
      <div class="table-page-search-wrapper">
        <a-form-model layout="inline">
          <a-row :gutter="48" >
            <a-col :md="18" :sm="24">
              <a-form-model-item label="地址">
                <a-input v-model="elasticsearchURL" placeholder="节点链接地址" :allowClear="true" />
              </a-form-model-item>
            </a-col>
            <a-col :md="6" :sm="24">
              <a-button type="primary" style="margin-left: 8px" @click="connect">{{ filters.state ? '断开' : '连接' }}</a-button>
            </a-col>
          </a-row>
        </a-form-model>
      </div>
    </a-card>
    <a-layout>
      <a-layout-sider theme="light" width="650">
        <a-card :bordered="false" title="参数配置">
          <a-form-model
            :model="form"
            :label-col="{ span: 5 }"
            :wrapper-col="{ span: 16 }">
            <a-form-model-item label="词典编号" :wrapper-col="{ span: 8 }">
              <a-input v-model="form.dictSerial" placeholder="词典编号" />
            </a-form-model-item>
            <a-form-model-item label="运行模式">
              <a-radio-group v-model="form.forceNode" name="forceNode">
                <a-radio value="0">集群模式</a-radio>
                <a-radio value="1">单实例主机</a-radio>
              </a-radio-group>
            </a-form-model-item>
            <a-form-model-item label="词典类型">
              <a-checkbox v-model="form.word">分词词典</a-checkbox>
              <a-checkbox v-model="form.synonym">同义词词典</a-checkbox>
              <a-checkbox v-model="form.quantifier">量词词典</a-checkbox>
              <a-checkbox v-model="form.stopword">停用词词典</a-checkbox>
            </a-form-model-item>
            <a-form-model-item :wrapper-col="{ span: 12, offset: 5 }">
              <a-space>
                <a-button type="primary" :loading="formLoading" @click="submit">执行</a-button>
              </a-space>
            </a-form-model-item>
          </a-form-model>
        </a-card>
      </a-layout-sider>
      <a-layout-content id="result">
        <a-textarea v-model="json" />
      </a-layout-content>
    </a-layout>
  </section>
</template>

<script>
import elasticsearchService from '@/service/lucene/elasticsearch'

export default {
  data () {
    return {
      result: {},
      types: {
        word: '分词词典',
        synonym: '同义词词典',
        quantifier: '量词词典',
        stopword: '停用词词典'
      },
      filters: {
        state: false
      },
      tips: {
        reload: '注意：重新载入词典可能影响正在建立索引的数据，请谨慎操作！'
      },
      elasticsearchURL: '',
      formLoading: false,
      form: {
        dictSerial: '',
        forceNode: '0',
        word: true
      }
    }
  },
  computed: {
    json () {
      return JSON.stringify(this.result, null, 4)
    }
  },
  methods: {
    connect () {
      this.filters.state = !this.filters.state
      if (this.filters.state) {
        this.elasticsearchURL = elasticsearchService.saveURL(this.elasticsearchURL)
      }
    },
    submit () {
      if (!this.filters.state) {
        this.$message.warning('请确认连接服务地址')
        return false
      }
      if (this.formLoading) return false
      const dicts = []
      for (const type in this.types) {
        if (this.form[type]) dicts.push(type)
      }
      const param = {
        dictSerial: this.form.dictSerial,
        dicts
      }
      if (this.form.forceNode === '1') param.forceNode = this.form.forceNode
      this.formLoading = true
      elasticsearchService.dictReload(param).then(result => {
        this.result = result
        this.formLoading = false
      })
    }
  },
  created () {
    this.elasticsearchURL = elasticsearchService.baseURL()
  }
}
</script>

<style lang="less" scoped>
#result {
  padding-left: 15px;
  textarea {
    width: 100%;
    height: 100%;
    resize: none;
  }
}
.alert-tip {
  margin-bottom: 25px;
}
</style>
