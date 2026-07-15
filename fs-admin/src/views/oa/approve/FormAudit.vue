<script setup lang="ts">
const model: any = defineModel({ type: Object, default: () => ({ message: '', local: false }) })
</script>

<template>
  <el-form :model="model" label-position="top">
    <el-form-item label="审批意见">
      <el-input type="textarea" v-model="model.message" placeholder="请输入审批意见" :rows="3" />
    </el-form-item>
    <el-form-item label="">
      <el-checkbox v-model="model.local">仅内部可见</el-checkbox>
    </el-form-item>
  </el-form>
</template>

<style lang="scss" scoped>
</style>
