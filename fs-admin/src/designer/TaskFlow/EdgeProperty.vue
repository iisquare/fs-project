<script setup lang="ts">
import { ref } from 'vue';

const active = ref('property')
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
  <el-tabs v-model="active" class="tab-property">
    <el-tab-pane label="连接属性" name="property">
      <el-form :model="model">
        <el-form-item label="" class="title">
          <span>基础信息</span>
          <el-popconfirm title="确认删除该元素？" @confirm="handleDelete" width="180">
            <template #reference>
               <LayoutIcon name="Delete" class="delete" />
            </template>
          </el-popconfirm>
        </el-form-item>
        <el-form-item label="类型">{{ model.shape }}</el-form-item>
        <el-form-item label="名称">
          <el-input v-model="model.data.name" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input type="textarea" v-model="model.data.description" />
        </el-form-item>
      </el-form>
    </el-tab-pane>
  </el-tabs>
</template>

<style lang="scss" scoped>
</style>
