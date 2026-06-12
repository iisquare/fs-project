<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import LuceneApi from '@/api/server/LuceneApi'

const ES_URL_KEY = 'elasticsearchURL'
const DEFAULT_ES_URL = 'http://127.0.0.1:9200/_plugin/analysis-ik-online'

const result = ref({})
const elasticsearchURL = ref(DEFAULT_ES_URL)
const formLoading = ref(false)
const form = ref({
  dictSerial: '',
  inputKeyword: '',
  useSynonym: false,
  useSmart: true,
  useArabic: true,
  useEnglish: true,
  useSynonymQuery: true,
  useSmartQuery: true,
  useArabicQuery: true,
  useEnglishQuery: true,
  useSynonymIndex: false,
  useSmartIndex: false,
  useArabicIndex: true,
  useEnglishIndex: true,
  parserOperator: 'or',
  inputPageSize: 5,
  inputContent: ''
})

const json = computed(() => JSON.stringify(result.value, null, 4))

const saveURL = (url: string) => {
  if (window.localStorage) {
    window.localStorage.setItem(ES_URL_KEY, url)
  }
  return url
}

const handleIndex = () => {
  saveURL(elasticsearchURL.value)
  if (!elasticsearchURL.value) {
    ElMessage.warning('请确认连接服务地址')
    return
  }
  if (formLoading.value) return
  formLoading.value = true
  LuceneApi.dictIndex(elasticsearchURL.value, {
    keyword: form.value.inputKeyword,
    dictSerial: form.value.dictSerial,
    useSynonym: form.value.useSynonym,
    useSmart: form.value.useSmart,
    useArabic: form.value.useArabic,
    useEnglish: form.value.useEnglish
  }).then((res: any) => {
    result.value = res
  }).finally(() => {
    formLoading.value = false
  })
}

const handleDemo = () => {
  saveURL(elasticsearchURL.value)
  if (!elasticsearchURL.value) {
    ElMessage.warning('请确认连接服务地址')
    return
  }
  if (formLoading.value) return
  formLoading.value = true
  LuceneApi.dictDemo(elasticsearchURL.value, {
    keyword: form.value.inputKeyword,
    content: form.value.inputContent,
    parserOperator: form.value.parserOperator,
    pageSize: form.value.inputPageSize,
    dictSerial: form.value.dictSerial,
    useSynonymIndex: form.value.useSynonymIndex,
    useSmartIndex: form.value.useSmartIndex,
    useArabicIndex: form.value.useArabicIndex,
    useEnglishIndex: form.value.useEnglishIndex,
    useSynonymQuery: form.value.useSynonymQuery,
    useSmartQuery: form.value.useSmartQuery,
    useArabicQuery: form.value.useArabicQuery,
    useEnglishQuery: form.value.useEnglishQuery
  }).then((res: any) => {
    result.value = res
  }).finally(() => {
    formLoading.value = false
  })
}

onMounted(() => {
  if (window.localStorage && window.localStorage[ES_URL_KEY]) {
    elasticsearchURL.value = window.localStorage[ES_URL_KEY]
  }
})
</script>

<template>
  <section>
    <el-alert type="info" show-icon :closable="true" class="alert-tip">
      提示：文档索引保存在临时内存中，执行完成后会立即释放。
    </el-alert>
    <el-row :gutter="15">
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>索引示例</template>
          <el-form :model="form" label-width="80px">
            <el-form-item label="服务地址">
              <el-input v-model="elasticsearchURL" placeholder="节点链接地址" clearable />
            </el-form-item>
            <el-form-item label="词典编号">
              <el-input v-model="form.dictSerial" placeholder="词典编号" />
            </el-form-item>
            <el-form-item label="关键词">
              <el-input v-model="form.inputKeyword" placeholder="关键词" />
            </el-form-item>
            <el-form-item label="提取分词">
              <el-checkbox v-model="form.useSynonym">同义词</el-checkbox>
              <el-checkbox v-model="form.useSmart">智能分词</el-checkbox>
              <el-checkbox v-model="form.useArabic">合并数值</el-checkbox>
              <el-checkbox v-model="form.useEnglish">合并字母</el-checkbox>
            </el-form-item>
            <el-form-item label="Query查询">
              <el-checkbox v-model="form.useSynonymQuery">同义词</el-checkbox>
              <el-checkbox v-model="form.useSmartQuery">智能分词</el-checkbox>
              <el-checkbox v-model="form.useArabicQuery">合并数值</el-checkbox>
              <el-checkbox v-model="form.useEnglishQuery">合并字母</el-checkbox>
            </el-form-item>
            <el-form-item label="Index索引">
              <el-checkbox v-model="form.useSynonymIndex">同义词</el-checkbox>
              <el-checkbox v-model="form.useSmartIndex">智能分词</el-checkbox>
              <el-checkbox v-model="form.useArabicIndex">合并数值</el-checkbox>
              <el-checkbox v-model="form.useEnglishIndex">合并字母</el-checkbox>
            </el-form-item>
            <el-form-item label="检索方式">
              <el-radio-group v-model="form.parserOperator">
                <el-radio value="or">OR_OPERATOR（至少满足一个分词）</el-radio>
                <el-radio value="and">AND_OPERATOR（满足全部分词结果）</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="结果条数">
              <el-input-number v-model="form.inputPageSize" :min="1" :max="1000" />
            </el-form-item>
            <el-form-item label="文档内容">
              <el-input type="textarea" v-model="form.inputContent" placeholder="每行为一个独立的文档" :rows="8" />
            </el-form-item>
            <el-form-item>
              <el-space>
                <el-button type="primary" :loading="formLoading" @click="handleIndex">分析关键词</el-button>
                <el-button type="primary" :loading="formLoading" @click="handleDemo">执行文档搜索</el-button>
              </el-space>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-input type="textarea" :model-value="json" class="result-textarea" />
      </el-col>
    </el-row>
  </section>
</template>

<style lang="scss" scoped>
.alert-tip {
  margin-bottom: 25px;
}
.result-textarea {
  height: 100%;
  :deep(textarea) {
    height: 100%;
    resize: none;
  }
}
</style>
