<script setup lang="ts">
const model: any = defineModel()

</script>

<template>
  <el-radio-group v-model="model">
    <el-radio-button label="全部" value="" />
    <el-radio-button label="已删除" value="only" />
    <el-radio-button label="未删除" value="without" />
  </el-radio-group>
</template>

<style lang="scss" scoped>
</style>
