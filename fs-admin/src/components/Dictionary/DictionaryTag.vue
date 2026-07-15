<script setup lang="ts">
/**
 * 字典标签展示组件 - 将字典值以标签形式展示，支持按字典节点树渲染层级路径。
 *
 * @v-model  {*}             字典值（双向绑定主值），支持单个值或数组
 * @prop     {Object}        nodes       - 字典节点映射表（必填），key 为字典值，value 为 { label, parentId }
 * @prop     {Boolean}       iteratable  - 是否迭代渲染父级路径，默认 false
 *
 * 节点映射结构 (nodes):
 *   { [value: string]: { label: string, parentId: any } }
 *
 * @example
 * <dictionary-tag v-model="deptId" :nodes="deptNodeMap" :iteratable="true" />
 */
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
