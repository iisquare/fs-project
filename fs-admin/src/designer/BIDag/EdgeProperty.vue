<script setup lang="ts">
const model: any = defineModel()
const tips: any = defineModel('tips', { type: null })
defineProps<{
  config: any,
  instance: any,
}>()
</script>

<template>
  <el-tabs model-value="property" class="tab-property">
    <el-tab-pane label="连线属性" name="property">
      <el-form :model="model">
        <el-form-item label="" class="title">基础信息</el-form-item>
        <el-form-item label="名称">
          <el-input v-model="model.data.name" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input type="textarea" v-model="model.data.description" />
        </el-form-item>
      </el-form>
    </el-tab-pane>
  </el-tabs>
</template>

<style lang="scss" scoped>
</style>
