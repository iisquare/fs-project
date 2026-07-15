<script setup lang="ts">
/**
 * 字典分组选项渲染组件 - 递归渲染分组选项，将带 children 的条目渲染为 el-option-group。
 *
 * @prop {OptionItem[]} options - 选项列表
 *
 * 选项项结构 (OptionItem):
 *   { label: string, value: any, children?: OptionItem[] }
 *
 * @example
 * <dictionary-group-item :options="treeOptions" />
 */
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
