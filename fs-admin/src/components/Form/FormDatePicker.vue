<script setup lang="ts">
/**
 * 日期时间选择器 - 基于 el-date-picker 的 datetime 类型封装，默认使用项目统一的日期格式。
 *
 * @v-model  {String}   选中日期（双向绑定主值），格式由 DateUtil.moment() 决定
 * @prop     {String}   placeholder - 占位文本
 *
 * @example
 * <form-date-picker v-model="createTime" placeholder="选择创建时间" />
 */
import DateUtil from '@/utils/DateUtil';

const { placeholder = '' } = defineProps({
  placeholder: { type: String, required: false }
})

const model: any = defineModel()
</script>

<template>
  <el-date-picker
    v-model="model"
    type="datetime"
    clearable
    :format="DateUtil.moment()"
    :value-format="DateUtil.moment()"
    :placeholder="placeholder" />
</template>

<style lang="scss" scoped>
</style>
