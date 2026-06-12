<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import LuceneApi from '@/api/server/LuceneApi'

const ES_URL_KEY = 'elasticsearchURL'
const DEFAULT_ES_URL = 'http://127.0.0.1:9200/_plugin/analysis-ik-online'

const types: Record<string, string> = {
  word: '分词词典',
  synonym: '同义词词典',
  quantifier: '量词词典',
  stopword: '停用词词典'
}

const result = ref({})
const elasticsearchURL = ref(DEFAULT_ES_URL)
const formLoading = ref(false)
const form = ref({
  dictSerial: '',
  forceNode: '0',
  word: true,
  synonym: false,
  quantifier: false,
  stopword: false
})

const json = computed(() => JSON.stringify(result.value, null, 4))

const saveURL = (url: string) => {
  if (window.localStorage) {
    window.localStorage.setItem(ES_URL_KEY, url)
  }
  return url
}

const handleSubmit = () => {
  saveURL(elasticsearchURL.value)
  if (!elasticsearchURL.value) {
    ElMessage.warning('请确认连接服务地址')
    return
  }
  if (formLoading.value) return
  const dicts: string[] = []
  for (const type in types) {
    if ((form.value as any)[type]) dicts.push(type)
  }
  const param: any = {
    dictSerial: form.value.dictSerial,
    dicts
  }
  if (form.value.forceNode === '1') param.forceNode = form.value.forceNode
  formLoading.value = true
  LuceneApi.dictReload(elasticsearchURL.value, param).then((res: any) => {
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
    <el-alert type="warning" show-icon :closable="true" class="alert-tip">
      注意：重新载入词典可能影响正在建立索引的数据，请谨慎操作！
    </el-alert>
    <el-row :gutter="15">
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>服务重载</template>
          <el-form :model="form" label-width="80px">
            <el-form-item label="服务地址">
              <el-input v-model="elasticsearchURL" placeholder="节点链接地址" clearable />
            </el-form-item>
            <el-form-item label="词典编号">
              <el-input v-model="form.dictSerial" placeholder="词典编号" />
            </el-form-item>
            <el-form-item label="运行模式">
              <el-radio-group v-model="form.forceNode">
                <el-radio value="0">集群模式</el-radio>
                <el-radio value="1">单实例主机</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="词典类型">
              <el-checkbox v-model="form.word">分词词典</el-checkbox>
              <el-checkbox v-model="form.synonym">同义词词典</el-checkbox>
              <el-checkbox v-model="form.quantifier">量词词典</el-checkbox>
              <el-checkbox v-model="form.stopword">停用词词典</el-checkbox>
            </el-form-item>
            <el-form-item>
              <el-space>
                <el-button type="primary" :loading="formLoading" @click="handleSubmit">执行</el-button>
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
