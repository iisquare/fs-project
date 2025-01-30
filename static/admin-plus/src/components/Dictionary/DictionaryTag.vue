<script setup lang="ts">
import { computed } from 'vue';
import DataUtil from '@/utils/DataUtil';

const {
  nodes,
  iteratable = false,
} = defineProps({
  nodes: { type: Object, required: true },
  iteratable: { type: Boolean, required: false }
})

const model = defineModel()

const items = computed(() => {
  return DataUtil.isArray(model.value) ? model.value : [model.value]
})

const format = (value: any) => {
  const result: any = []
  const node = nodes[value]
  if (!node) return result
  iteratable && result.push(...format(node.parentId))
  result.push(node.label)
  return result
}
</script>

<template>
  <el-space direction="vertical" alignment="stretch">
    <el-tag v-for="(item, index) in items" :key="index">{{ format(item).join(' / ') }}</el-tag>  
  </el-space>
</template>

<style lang="scss" scoped>
</style>
