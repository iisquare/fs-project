<script setup lang="ts">
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
