<script setup lang="ts">
/**
 * 搜索表单项 - 配合 FormSearch 使用的栅格表单项，基于 el-col + el-form-item，控制每行列数。
 *
 * @prop {String}  label   - 标签文本
 * @prop {String}  prop    - 校验字段名
 * @prop {Number}  span    - 栅格宽度（共24格），默认 6（一行4个）
 * @prop {Number}  offset  - 栅格偏移，默认 0
 * @prop {Number}  push    - 栅格右移，默认 0
 * @prop {Number}  pull    - 栅格左移，默认 0
 *
 * @slot default - 表单控件内容
 *
 * @example
 * <!-- 默认占 6 格（一行4个） -->
 * <form-search-item label="名称" prop="name">
 *   <el-input v-model="form.name" />
 * </form-search-item>
 *
 * <!-- 占 12 格（一行2个） -->
 * <form-search-item label="描述" span="12">
 *   <el-input v-model="form.desc" type="textarea" />
 * </form-search-item>
 */
import { computed } from 'vue';

const {
  span = 6,
  offset = 0,
  push = 0,
  pull = 0,
} = defineProps({
  label: { type: String, required: false },
  prop: { type: String, required: false },
  span: { type: Number, required: false },
  offset: { type: Number, required: false },
  push: { type: Number, required: false },
  pull: { type: Number, required: false },
})

const layout = computed(() => {
  return Object.assign({}, { span, offset, push, pull })
})
</script>

<template>
  <el-col v-bind="layout">
    <el-form-item :label="label" :prop="prop">
      <slot></slot>
    </el-form-item>
  </el-col>
</template>

<style lang="scss" scoped>
</style>
