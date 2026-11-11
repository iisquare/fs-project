<script setup lang="ts">
import { onMounted } from 'vue';
import PropertySection from '@/components/Layout/PropertySection.vue';


const model: any = defineModel()
const tips: any = defineModel('tips', { type: null })
const props = defineProps<{
  config: any,
  instance: any,
}>()

const handleDelete = () => {
  props.instance.removeItem(model.value)
}

onMounted(() => {
  if (Object.keys(model.value.options).indexOf('field') !== -1) {
    if (!model.value.options.field) {
      model.value.options.field = model.value.id
    }
  }
})
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
    <slot></slot>
  </PropertySection>
</template>

<style lang="scss" scoped>
</style>
