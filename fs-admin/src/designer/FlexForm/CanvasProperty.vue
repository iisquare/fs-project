<script setup lang="ts">
import { reactive, ref, watch } from 'vue';

const active = ref('property')
const model: any = defineModel()
const tips: any = defineModel('tips', { type: null })
defineProps<{
  config: any,
  instance: any,
}>()

const marks: any = reactive(Object.fromEntries([0, 6, 12, 18, 24].map(item => [item + '', item + ''])))
</script>

<template>
  <el-tabs v-model="active" class="tab-property">
    <el-tab-pane label="表单属性" name="property">
      <el-form :model="model">
        <el-form-item label="" class="title">基础信息</el-form-item>
        <el-form-item label="标识" v-if="model.id">{{ model.id }}</el-form-item>
        <el-form-item label="名称">
          <el-input v-model="model.name" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="model.sort" :controls="false" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="model.status" placeholder="请选择">
            <el-option v-for="(value, key) in config.status" :key="key" :value="key" :label="value" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input type="textarea" v-model="model.description" />
        </el-form-item>
        <el-form-item label="" class="title">标签配置</el-form-item>
        <el-form-item label="对齐方式">
          <el-radio-group v-model="model.content.labelPosition">
            <el-radio-button :label="item.label" :value="item.value" v-for="item in config.labelPositions" :key="item.value" />
          </el-radio-group>
        </el-form-item>
        <el-form-item label="标签宽度">
          <el-input-number v-model="model.content.labelWidth" :controls="false" :min="0">
            <template #suffix><span>px</span></template>
          </el-input-number>
        </el-form-item>
      </el-form>
    </el-tab-pane>
    <el-tab-pane label="列表配置" name="table">
      <el-form :model="model">
        <el-form-item label="分页大小">
          <el-input-number v-model="model.content.pageSize" :controls="false" :min="0" />
        </el-form-item>
        <el-form-item label="列表字段">
          <el-input type="textarea" v-model="model.content.column" placeholder="采用英文逗号分隔" />
        </el-form-item>
        <el-form-item label="默认排序">
          <el-input type="textarea" v-model="model.content.sort" placeholder="a,b.asc,c.desc" />
        </el-form-item>
      </el-form>
    </el-tab-pane>
  </el-tabs>
</template>

<style lang="scss" scoped>
</style>
