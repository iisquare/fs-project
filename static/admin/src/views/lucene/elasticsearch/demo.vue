<template>
  <section>
    <a-alert :message="tips.search" type="info" show-icon :closable="true" class="alert-tip" />
    <a-layout>
      <a-layout-sider theme="light" width="650">
        <a-card title="索引示例">
          <a-form-model
            :model="form"
            :label-col="{ span: 5 }"
            :wrapper-col="{ span: 16 }">
            <a-form-model-item label="服务地址">
              <a-input v-model="elasticsearchURL" placeholder="节点链接地址" :allowClear="true" />
            </a-form-model-item>
            <a-form-model-item label="词典编号" :wrapper-col="{ span: 8 }">
              <a-input v-model="form.dictSerial" placeholder="词典编号" />
            </a-form-model-item>
            <a-form-model-item label="关键词">
              <a-input v-model="form.inputKeyword" placeholder="关键词" />
            </a-form-model-item>
            <a-form-model-item label="提取分词">
              <a-checkbox v-model="form.useSynonym">同义词</a-checkbox>
              <a-checkbox v-model="form.useSmart">智能分词</a-checkbox>
              <a-checkbox v-model="form.useArabic">合并数值</a-checkbox>
              <a-checkbox v-model="form.useEnglish">合并字母</a-checkbox>
            </a-form-model-item>
            <a-form-model-item label="Query查询">
              <a-checkbox v-model="form.useSynonymQuery">同义词</a-checkbox>
              <a-checkbox v-model="form.useSmartQuery">智能分词</a-checkbox>
              <a-checkbox v-model="form.useArabicQuery">合并数值</a-checkbox>
              <a-checkbox v-model="form.useEnglishQuery">合并字母</a-checkbox>
            </a-form-model-item>
            <a-form-model-item label="Index索引">
              <a-checkbox v-model="form.useSynonymIndex">同义词</a-checkbox>
              <a-checkbox v-model="form.useSmartIndex">智能分词</a-checkbox>
              <a-checkbox v-model="form.useArabicIndex">合并数值</a-checkbox>
              <a-checkbox v-model="form.useEnglishIndex">合并字母</a-checkbox>
            </a-form-model-item>
            <a-form-model-item label="检索方式">
              <a-radio-group v-model="form.parserOperator" name="parserOperator">
                <a-radio value="or">OR_OPERATOR（至少满足一个分词）</a-radio>
                <a-radio value="and">AND_OPERATOR（满足全部分词结果）</a-radio>
              </a-radio-group>
            </a-form-model-item>
            <a-form-model-item label="结果条数">
              <a-input-number v-model="form.inputPageSize" :min="1" :max="1000"></a-input-number>
            </a-form-model-item>
            <a-form-model-item label="文档内容">
              <a-textarea v-model="form.inputContent" placeholder="每行为一个独立的文档" rows="8" />
            </a-form-model-item>
            <a-form-model-item :wrapper-col="{ span: 12, offset: 5 }">
              <a-space>
                <a-button type="primary" :loading="formLoading" @click="index">分析关键词</a-button>
                <a-button type="primary" :loading="formLoading" @click="demo">执行文档搜索</a-button>
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
      tips: {
        search: '提示：文档索引保存在临时内存中，执行完成后会立即释放。'
      },
      elasticsearchURL: '',
      formLoading: false,
      form: {
        useSmart: true,
        useArabic: true,
        useEnglish: true,
        useSynonymQuery: true,
        useSmartQuery: true,
        useArabicQuery: true,
        useEnglishQuery: true,
        useArabicIndex: true,
        useEnglishIndex: true,
        parserOperator: 'or',
        inputPageSize: 5,
        inputContent: ''
      }
    }
  },
  computed: {
    json () {
      return JSON.stringify(this.result, null, 4)
    }
  },
  methods: {
    index () {
      this.elasticsearchURL = elasticsearchService.saveURL(this.elasticsearchURL)
      if (!this.elasticsearchURL) {
        this.$message.warning('请确认连接服务地址')
        return false
      }
      if (this.formLoading) return false
      const param = {
        keyword: this.form.inputKeyword,
        dictSerial: this.form.dictSerial,
        useSynonym: this.form.useSynonym,
        useSmart: this.form.useSmart,
        useArabic: this.form.useArabic,
        useEnglish: this.form.useEnglish
      }
      this.formLoading = true
      elasticsearchService.dictIndex(param).then(result => {
        this.result = result
        this.formLoading = false
      })
    },
    demo () {
      this.elasticsearchURL = elasticsearchService.saveURL(this.elasticsearchURL)
      if (!this.elasticsearchURL) {
        this.$message.warning('请确认连接服务地址')
        return false
      }
      if (this.formLoading) return false
      const param = {
        keyword: this.form.inputKeyword,
        content: this.form.inputContent,
        parserOperator: this.form.parserOperator,
        pageSize: this.form.pageSize,
        dictSerial: this.form.dictSerial,
        useSynonymIndex: this.form.useSynonymIndex,
        useSmartIndex: this.form.useSmartIndex,
        useArabicIndex: this.form.useArabicIndex,
        useEnglishIndex: this.form.useEnglishIndex,
        useSynonymQuery: this.form.useSynonymQuery,
        useSmartQuery: this.form.useSmartQuery,
        useArabicQuery: this.form.useArabicQuery,
        useEnglishQuery: this.form.useEnglishQuery
      }
      this.formLoading = true
      elasticsearchService.dictDemo(param).then(result => {
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
