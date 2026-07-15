<script setup lang="ts">
/**
 * 数据模型编辑器 - 支持通过 CSV 文本批量转换数据字段定义，或通过表格逐行编辑。
 *
 * @v-model  {FieldRow[]}   字段列表（双向绑定主值）
 * @prop     {String[]}      types    - 可选的字段类型列表，通过 v-model:types 传入
 *
 * 字段行结构 (FieldRow):
 *   { name: string, title: string, type: string }
 *   name  - 字段名（必填）
 *   title - 显示名称（选填，默认为字段名）
 *   type  - 数据类型（必填）
 *
 * @example
 * <data-schema v-model="fields" v-model:types="['String', 'Integer', 'Boolean', 'Date']" />
 */
import { ref } from 'vue';
import DataTable from './DataTable.vue';

const model: any = defineModel()
const types = defineModel<String[]>('types', { default: () => [] })
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
