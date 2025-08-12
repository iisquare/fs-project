<script setup lang="ts">
import { ref } from 'vue';
import * as ElementPlusIcons from '@element-plus/icons-vue';
import UIUtil from '@/utils/UIUtil';

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
</script>

<template>
  <el-tabs v-model="active" class="tab-property">
    <el-tab-pane label="实体属性" name="property">
      <el-form :model="model">
        <el-form-item label="" class="title">
          <span>基础信息</span>
          <el-popconfirm title="确认删除该元素？" @confirm="handleDelete" width="180">
            <template #reference>
                <LayoutIcon name="Delete" class="delete" />
            </template>
          </el-popconfirm>
        </el-form-item>
        <el-form-item label="实体名称"><el-input v-model="model.data.name" :placeholder="model.data.label ?? '选填，默认为标签名称'" /></el-form-item>
        <el-form-item label="实体标签"><el-input v-model="model.data.label" placeholder="必填，节点类型标签（表名）" /></el-form-item>
        <el-form-item label="实体描述"><el-input type="textarea" v-model="model.data.description" /></el-form-item>
        <el-form-item label="展示图标"><el-input v-model="model.data.icon" /></el-form-item>
        <el-form-item label="背景颜色"><el-color-picker v-model="model.data.color" /></el-form-item>
        <el-form-item label="主键字段">
          <el-autocomplete v-model="model.data.primaryField" :fetch-suggestions="query => UIUtil.fetchSuggestions(model.data.fields, query, 'name')" placeholder="必填，数据唯一标识" />
        </el-form-item>
        <el-form-item label="标题字段">
          <el-autocomplete v-model="model.data.captionField" :fetch-suggestions="query => UIUtil.fetchSuggestions(model.data.fields, query, 'name')" placeholder="必填，默认展示字段" />
        </el-form-item>
        <el-form-item label="" class="title">
          <span>字段列表</span>
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
