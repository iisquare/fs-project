<script setup lang="ts">

defineProps<{
  options: any,
}>()

</script>

<template>
  <template v-for="option in options" :key="option.value">
    <el-option-group :label="option.label" v-if="option.children">
      <DictionaryGroupItem :options="option.children" />
    </el-option-group>
    <el-option :label="option.label" :value="option.value" v-else />
  </template>
</template>

<style lang="scss" scoped>
</style>
