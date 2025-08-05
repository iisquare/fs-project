<script setup lang="ts">
import { onMounted, ref } from 'vue';
import LayoutIcon from '../Layout/LayoutIcon.vue';
import * as ElementPlusIcons from '@element-plus/icons-vue';
import config from './config';

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

const fieldVisible = ref(false)

onMounted(() => {
  model.value.data = Object.assign(config.edge.options(), model.value.data)
})
</script>

<template>
  <el-tabs v-model="active" class="tab-property">
    <el-tab-pane label="关系属性" name="property">
      <el-form :model="model">
        <el-form-item label="" class="title">
          <span>基础信息</span>
          <el-popconfirm title="确认删除该元素？" @confirm="handleDelete" width="180">
            <template #reference>
               <LayoutIcon name="Delete" class="delete" />
            </template>
          </el-popconfirm>
        </el-form-item>
        <el-form-item label="关系名称"><el-input v-model="model.data.name" :placeholder="model.data.label ?? '选填，默认为标签名称'" /></el-form-item>
        <el-form-item label="关系标签"><el-input v-model="model.data.label" placeholder="必填，关联关系类型标签" /></el-form-item>
        <el-form-item label="关系描述"><el-input type="textarea" v-model="model.data.description" /></el-form-item>
        <el-form-item label="" class="title">
          <span>可选属性</span>
          <el-button link :icon="ElementPlusIcons.EditPen" @click="fieldVisible=true" title="编辑字段" />
        </el-form-item>
        <DataTable v-model="model.data.fields" :types="config.fieldTypes" />
      </el-form>
    </el-tab-pane>
  </el-tabs>
  <el-dialog v-model="fieldVisible" title="字段编辑" draggable>
    <DataTable v-model="model.data.fields" :types="config.fieldTypes" editable />
  </el-dialog>
</template>

<style lang="scss" scoped>
</style>
