<script setup lang="ts">
/**
 * 节点基础信息 - 节点名称、描述与删除操作，所有节点的属性面板均复用该片段。
 */
const model: any = defineModel()
const tips: any = defineModel('tips', { type: null })
const props = defineProps<{
  config?: any,
  instance?: any,
}>()

const handleDelete = () => {
  props.instance?.flow?.remove(model.value)
}
</script>

<template>
  <el-form-item label="" class="title">
    <span>基础信息</span>
    <el-popconfirm title="确认删除该节点？" width="180" @confirm="handleDelete">
      <template #reference>
        <LayoutIcon name="Delete" class="delete" />
      </template>
    </el-popconfirm>
  </el-form-item>
  <el-form-item label="节点类型" class="fs-form-inline">{{ model.data?.type }}</el-form-item>
  <el-form-item label="节点名称">
    <el-input v-model="model.data.name" />
  </el-form-item>
  <el-form-item label="节点描述">
    <el-input v-model="model.data.description" type="textarea" :rows="2" />
  </el-form-item>
</template>

<style lang="scss" scoped>
</style>
