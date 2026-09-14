<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import * as ElementPlusIcons from '@element-plus/icons-vue';
import { QuestionFilled } from '@element-plus/icons-vue';
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

// 主标签固定在标签集合首位，附加标签用于角色与分类标记
const syncLabels = () => {
  if (!model.value?.data) return
  const primary = model.value.data.label ?? ''
  const rest = (model.value.data.labels ?? []).filter((item: string) => item && item !== primary)
  model.value.data.labels = primary ? [primary, ...rest] : rest
}
const extraLabels = computed({
  get: () => (model.value?.data?.labels ?? []).filter((item: string) => item && item !== model.value.data.label),
  set: (value: string[]) => {
    const primary = model.value.data.label
    const list = [primary, ...(value ?? [])].filter((item: string) => !!item)
    model.value.data.labels = Array.from(new Set(list))
  },
})
watch(() => model.value?.data?.label, () => syncLabels())
onMounted(() => syncLabels())
</script>

<template>
  <el-tabs v-model="active" class="tab-property">
    <el-tab-pane label="实体属性" name="property">
      <el-form :model="model" label-position="top">
        <el-form-item label="" class="title">
          <span>基础信息</span>
          <el-popconfirm title="确认删除该元素？" @confirm="handleDelete" width="180">
            <template #reference>
                <LayoutIcon name="Delete" class="delete" />
            </template>
          </el-popconfirm>
        </el-form-item>
        <el-form-item label="实体名称"><el-input v-model="model.data.name" :placeholder="model.data.label ?? '选填，默认为标签名称'" /></el-form-item>
        <el-form-item label="实体标签">
          <el-input v-model="model.data.label" placeholder="必填，主标签，决定实体身份" />
        </el-form-item>
        <el-form-item>
          <template #label>
            <span>附加标签</span>
            <el-tooltip content="首位为主标签，附加标签用于角色或分类标记；数据检索按主标签匹配，可命中多标签节点" placement="top">
              <el-icon class="field-help"><QuestionFilled /></el-icon>
            </el-tooltip>
          </template>
          <el-select v-model="extraLabels" multiple filterable allow-create default-first-option
            placeholder="选填，节点将同时带有这些标签">
            <el-option v-for="item in extraLabels" :key="item" :value="item" :label="item" />
          </el-select>
        </el-form-item>
        <el-form-item class="fs-form-inline">
          <template #label>
            <span>允许扩展标签</span>
            <el-tooltip content="开启后，数据管理可为单条数据追加本体未声明的标签" placement="top">
              <el-icon class="field-help"><QuestionFilled /></el-icon>
            </el-tooltip>
          </template>
          <el-switch v-model="model.data.extendableLabels" />
        </el-form-item>
        <el-form-item label="实体描述"><el-input type="textarea" v-model="model.data.description" /></el-form-item>
        <el-form-item label="展示图标"><el-input v-model="model.data.icon" /></el-form-item>
        <el-form-item label="背景颜色" class="fs-form-inline"><el-color-picker v-model="model.data.color" /></el-form-item>
        <el-form-item label="主键字段">
          <el-autocomplete v-model="model.data.primaryField" :fetch-suggestions="query => UIUtil.fetchSuggestions(model.data.fields, query, 'name')" placeholder="必填，数据唯一标识" />
        </el-form-item>
        <el-form-item label="标题字段">
          <el-autocomplete v-model="model.data.captionField" :fetch-suggestions="query => UIUtil.fetchSuggestions(model.data.fields, query, 'name')" placeholder="必填，默认展示字段" />
        </el-form-item>
        <el-form-item class="fs-form-inline">
          <template #label>
            <span>允许扩展属性</span>
            <el-tooltip content="开启后，数据管理可保存本体未声明的属性" placement="top">
              <el-icon class="field-help"><QuestionFilled /></el-icon>
            </el-tooltip>
          </template>
          <el-switch v-model="model.data.extendable" />
        </el-form-item>
        <el-form-item label="" class="title">
          <span>字段列表</span>
          <el-button link :icon="ElementPlusIcons.EditPen" @click="fieldVisible=true" title="编辑字段" />
        </el-form-item>
        <DataSchemaTable v-model="model.data.fields" :types="config.fieldTypes" :flags="['required', 'display']" />
      </el-form>
    </el-tab-pane>
  </el-tabs>
  <el-dialog v-model="fieldVisible" title="字段编辑" width="900px" top="6vh" draggable>
    <DataSchemaTable v-model="model.data.fields" :types="config.fieldTypes" :flags="['required', 'display']" editable />
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
