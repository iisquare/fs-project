<script setup lang="ts">

const model: any = defineModel()
const tips: any = defineModel('tips', { type: null })
const props = defineProps<{
  config: any,
  instance: any,
}>()

const handleDelete = () => {
  props.instance.removeItem(model.value)
}
</script>

<template>
  <el-form-item label="" class="title">
    <span>基础信息</span>
    <el-popconfirm title="确认删除该元素？" @confirm="handleDelete" width="180">
      <template #reference>
          <LayoutIcon name="Delete" class="delete" />
      </template>
    </el-popconfirm>
  </el-form-item>
</template>

<style lang="scss" scoped>
</style>
