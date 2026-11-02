<script setup lang="ts">
const model = defineModel<any>()
const props = defineProps<{
  modes: Record<string, string>
}>()
</script>

<template>
  <el-radio-group v-model="model.fetchMode">
    <el-radio-button v-for="(value, key) in props.modes" :key="key" :value="key">
      {{ value }}
    </el-radio-button>
  </el-radio-group>
</template>
