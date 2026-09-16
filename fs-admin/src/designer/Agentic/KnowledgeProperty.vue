<script setup lang="ts">
/**
 * 知识检索节点属性 - 从知识库中查询与用户问题相关的文本内容。
 */
import { ref } from 'vue'
import KnowledgeApi from '@/api/agent/KnowledgeApi'
import MetadataTable from '@/components/Data/MetadataTable.vue'
import NodeSlice from './NodeSlice.vue'
import OutputSlice from './OutputSlice.vue'
import VariableSelect from './VariableSelect.vue'

const active = ref('property')
const model: any = defineModel()
const tips: any = defineModel('tips', { type: null })
defineProps<{
  config?: any,
  instance?: any,
}>()
</script>

<template>
  <el-tabs v-model="active" class="tab-property">
    <el-tab-pane label="节点属性" name="property">
      <el-form :model="model" label-position="top">
        <NodeSlice v-model="model" :instance="$props.instance" :config="$props.config" :tips="tips" />
        <el-form-item label="" class="title">检索配置</el-form-item>
        <el-form-item label="查询变量">
          <VariableSelect
            v-model="model.data.query"
            :instance="$props.instance"
            :active-item="model"
            allow-create
            placeholder="请选择查询内容来源" />
        </el-form-item>
        <el-form-item label="知识库">
          <form-select v-model="model.data.knowledgeIds" :callback="KnowledgeApi.list" multiple clearable placeholder="请选择知识库" />
        </el-form-item>
        <el-form-item label="检索方式">
          <el-select v-model="model.data.strategy" placeholder="请选择">
            <el-option :key="item.value" :value="item.value" :label="item.label" v-for="item in $props.config.knowledgeStrategies" />
          </el-select>
        </el-form-item>
        <el-form-item label="召回数量">
          <el-input-number v-model="model.data.topK" :min="1" :max="20" :controls="false" />
        </el-form-item>
        <el-form-item label="评分阈值">
          <el-input-number v-model="model.data.score" :precision="2" :step="0.1" :min="0" :max="1" :controls="false" />
        </el-form-item>
        <el-form-item label="" class="title">结果处理</el-form-item>
        <el-form-item label="重排序" class="fs-form-inline">
          <el-switch v-model="model.data.rerank.enabled" />
        </el-form-item>
        <el-form-item label="重排模型" v-if="model.data.rerank.enabled">
          <el-input v-model="model.data.rerank.model" placeholder="请输入重排模型名称" />
        </el-form-item>
        <el-form-item label="重排数量" v-if="model.data.rerank.enabled">
          <el-input-number v-model="model.data.rerank.topN" :min="1" :controls="false" />
        </el-form-item>
        <el-form-item label="元数据过滤">
          <metadata-table v-model="model.data.metadata" :editable="true" />
        </el-form-item>
        <OutputSlice :data="model.data" />
      </el-form>
    </el-tab-pane>
  </el-tabs>
</template>

<style lang="scss" scoped>
</style>
