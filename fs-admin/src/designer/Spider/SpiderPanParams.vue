<script setup lang="ts">
const model: any = defineModel()

</script>

<template>
  <el-form :model="model" label-width="auto">
    <el-form-item label="启动选项"><el-checkbox v-model="model.force" label="强制重采" /></el-form-item>
    <el-form-item label="来源地址"><el-input v-model="model.referer" placeholder="来源页面链接地址" /></el-form-item>
    <el-form-item label="种子链接"><el-input type="textarea" v-model="model.urls" placeholder="每个链接一行" :rows="10" /></el-form-item>
  </el-form>
</template>

<style lang="scss" scoped>
</style>
