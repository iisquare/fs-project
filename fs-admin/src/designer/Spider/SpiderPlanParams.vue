<script setup lang="ts">
import PageApi from '@/api/spider/PageApi';

const model: any = defineModel()
const props = defineProps<{
  form: any,
}>()

const parameter = () => {
  return {
    templateId: props.form.id
  }
}
</script>

<template>
  <el-form :model="model" label-width="auto">
    <el-form-item label="启动页面">
      <form-select v-model="model.page" :callback="PageApi.list" :parameter="parameter" fieldValue="code" />
    </el-form-item>
    <el-form-item label="来源地址"><el-input v-model="model.referer" placeholder="来源页面链接地址" /></el-form-item>
    <el-form-item label="启动参数">
      <el-alert title="JSON数组，需至少包含一个对象" type="primary" />
      <CodeEditor v-model="model.args" :height="300" fold-gutter mode="javascript" />
    </el-form-item>
  </el-form>
</template>

<style lang="scss" scoped>
</style>
