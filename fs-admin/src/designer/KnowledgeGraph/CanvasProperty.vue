<script setup lang="ts">
import { ref } from 'vue';
import { QuestionFilled } from '@element-plus/icons-vue';

const active = ref('property')
const model: any = defineModel()
const tips: any = defineModel('tips', { type: null })
defineProps<{
  config: any,
  instance: any,
}>()
</script>

<template>
  <el-tabs v-model="active" class="tab-property">
    <el-tab-pane label="图谱属性" name="property">
      <el-form :model="model" label-position="top">
        <el-form-item label="" class="title">基础信息</el-form-item>
        <el-form-item label="标识" v-if="model.id">{{ model.id }}</el-form-item>
        <el-form-item>
          <template #label>
            <span>名称</span>
            <el-tooltip content="本体的显示名称，用于列表与选择器" placement="top">
              <el-icon class="field-help"><QuestionFilled /></el-icon>
            </el-tooltip>
          </template>
          <el-input v-model="model.name" />
        </el-form-item>
        <el-form-item>
          <template #label>
            <span>排序</span>
            <el-tooltip content="列表中的排序值，数值越大越靠前" placement="top">
              <el-icon class="field-help"><QuestionFilled /></el-icon>
            </el-tooltip>
          </template>
          <el-input-number v-model="model.sort" :controls="false" />
        </el-form-item>
        <el-form-item prop="status">
          <template #label>
            <span>状态</span>
            <el-tooltip content="禁用的本体不会出现在数据管理与检索的本体选择中" placement="top">
              <el-icon class="field-help"><QuestionFilled /></el-icon>
            </el-tooltip>
          </template>
          <el-select v-model="model.status" placeholder="请选择">
            <el-option v-for="(value, key) in config.status" :key="key" :value="key" :label="value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <template #label>
            <span>描述</span>
            <el-tooltip content="本体的用途说明，便于团队理解建模意图" placement="top">
              <el-icon class="field-help"><QuestionFilled /></el-icon>
            </el-tooltip>
          </template>
          <el-input type="textarea" v-model="model.description" />
        </el-form-item>
      </el-form>
    </el-tab-pane>
  </el-tabs>
</template>

<style lang="scss" scoped>
.field-help {
  margin-left: 4px;
  vertical-align: -2px;
  color: var(--el-text-color-placeholder);
  cursor: help;
}
</style>
