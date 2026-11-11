<script setup lang="ts">
import PropertyPanel from './PropertyPanel.vue'
import PropertySection from '@/components/Layout/PropertySection.vue'

const model: any = defineModel()
const tips: any = defineModel('tips', { type: null })
defineProps<{
  config: any,
  instance: any,
}>()
</script>

<template>
  <property-panel title="连线属性" tab-title="连线属性">
    <el-form :model="model" label-position="top">
      <property-section title="基础信息" :reset-key="model.id">
        <el-form-item label="名称">
          <el-input v-model="model.data.name" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input type="textarea" v-model="model.data.description" />
        </el-form-item>
      </property-section>
    </el-form>
  </property-panel>
</template>

<style lang="scss" scoped>
</style>
