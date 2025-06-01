<script setup lang="ts">
/**
 * 通过CSV格式转换数据模型字段
 */
import { ref } from 'vue';
import DataTable from './DataTable.vue';
import CodeEditor from '../Editor/CodeEditor.vue';

const model: any = defineModel()
const types = defineModel('types', { type: Array<String>, default: [] })
const separator = ref(',')
const schema = ref('')
const editorRef = ref()
const handleText = () => {
  editorRef.value.setContent(model.value.map((item: any) => {
    return [item.name, item.title, item.type].join(separator.value)
  }).join('\n'))
}
const handleTable = () => {
  let rows = editorRef.value.getContent().split('\n')
  rows = rows.map((item: any) => {
    item = item.split(separator.value)
    return {
      name: item.length > 0 ? item[0] : '',
      title: item.length > 1 ? item[1] : '',
      type: item.length > 2 ? item[2] : '',
    }
  })
  model.value = rows
}
</script>
<template>
  <el-space class="toolbar">
    <el-input v-model="separator"><template #prepend>分隔符</template></el-input>
    <el-button @click="handleText">转换</el-button>
    <el-button @click="handleTable">解析</el-button>
  </el-space>
  <CodeEditor ref="editorRef" v-model="schema" :height="300" class="mb-20" />
  <DataTable v-model="model" :types="types" />
</template>

<style lang="scss" scoped>
.toolbar {
  margin-bottom: 15px;
}
</style>
