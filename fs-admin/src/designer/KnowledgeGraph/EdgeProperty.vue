<script setup lang="ts">
import { onMounted, ref } from 'vue';
import * as ElementPlusIcons from '@element-plus/icons-vue';
import { QuestionFilled } from '@element-plus/icons-vue';
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
      <el-form :model="model" label-position="top">
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
        <el-form-item>
          <template #label>
            <span>关系键</span>
            <el-tooltip content="设置后，数据保存时按关系键匹配关系，可保留同一对实体之间的多段记录" placement="top">
              <el-icon class="field-help"><QuestionFilled /></el-icon>
            </el-tooltip>
          </template>
          <el-select v-model="model.data.mergeFields" multiple filterable allow-create default-first-option
            placeholder="选填，用于区分同一对实体之间的多条同类关系">
            <el-option v-for="field in (model.data.fields ?? [])" :key="field.name" :value="field.name" :label="field.title || field.name" />
          </el-select>
        </el-form-item>
        <el-form-item class="fs-form-inline">
          <template #label>
            <span>级联删除</span>
            <el-tooltip content="开启后，删除实体时可一并删除该关系；删除数据时若存在不级联的关系会要求显式确认" placement="top">
              <el-icon class="field-help"><QuestionFilled /></el-icon>
            </el-tooltip>
          </template>
          <el-switch v-model="model.data.cascadeDelete" />
        </el-form-item>
        <el-form-item label="" class="title">
          <span>可选属性</span>
          <el-button link :icon="ElementPlusIcons.EditPen" @click="fieldVisible=true" title="编辑字段" />
        </el-form-item>
        <DataSchemaTable v-model="model.data.fields" :types="config.fieldTypes" :flags="['required']" />
      </el-form>
    </el-tab-pane>
  </el-tabs>
  <el-dialog v-model="fieldVisible" title="字段编辑" width="760px" top="6vh" draggable>
    <DataSchemaTable v-model="model.data.fields" :types="config.fieldTypes" :flags="['required']" editable />
  </el-dialog>
</template>

<style lang="scss" scoped>
.field-tip {
  margin-left: 8px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.field-help {
  margin-left: 4px;
  vertical-align: -2px;
  color: var(--el-text-color-placeholder);
  cursor: help;
}
</style>
