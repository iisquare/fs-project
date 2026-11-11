<script setup lang="ts">
import PropertySection from '@/components/Layout/PropertySection.vue';

const model: any = defineModel()
const tips: any = defineModel('tips', { type: null })
const props = defineProps<{
  config: any,
  instance: any,
}>()

const handleDelete = () => {
  props.instance.flow.remove(model.value)
}
</script>

<template>
  <PropertySection title="基础信息" :reset-key="model.id">
    <template #extra>
      <el-popconfirm title="确认删除该元素？" @confirm="handleDelete" width="180">
        <template #reference>
          <LayoutIcon name="Delete" class="delete" />
        </template>
      </el-popconfirm>
    </template>
    <el-form-item label="类型">{{ model.data.type }}</el-form-item>
    <el-form-item label="名称">
      <el-input v-model="model.data.name" />
    </el-form-item>
    <el-form-item label="描述">
      <el-input type="textarea" v-model="model.data.description" />
    </el-form-item>
  </PropertySection>
  <PropertySection title="流程配置" :reset-key="model.id">
    <el-form-item label="跳过执行">
      <el-switch v-model="model.data.skipped" />
    </el-form-item>
  </PropertySection>
</template>

<style lang="scss" scoped>
</style>
