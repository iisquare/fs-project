<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import MetaApi from '@/api/govern/MetaApi'
import ApiUtil from '@/utils/ApiUtil'
import LayoutHeading from '@/components/Layout/LayoutHeading.vue'
import MetadataGraph, { type GovernGraphEdge, type GovernGraphNode } from '@/views/govern/components/MetadataGraph.vue'
import GovernPageShell from '@/views/govern/components/GovernPageShell.vue'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const raw = ref<any>({ nodes: {}, relations: {}, models: {} })
const selected = ref<any>(null)
const filters = ref<any>({
  catalog: String(route.query.catalog || '/'),
  model: String(route.query.model || ''),
  code: String(route.query.code || ''),
  minHops: 0,
  maxHops: 2,
})

const graphNodes = computed<GovernGraphNode[]>(() => Object.values(raw.value.nodes ?? {}).map((item: any) => {
  const path = `${item.properties?.catalog || ''}${item.properties?.model || ''}`
  const model = raw.value.models?.[path]
  return {
    id: String(item.elementId ?? item.identity),
    name: model?.name || item.properties?.model || '未命名模型',
    subtitle: item.properties?.column || path,
    color: item.properties?.column ? '#67c23a' : '#409eff',
    data: item,
  }
}))
const graphEdges = computed<GovernGraphEdge[]>(() => Object.values(raw.value.relations ?? {}).map((item: any) => ({
  id: String(item.elementId ?? item.identity),
  source: String(item.startElementId ?? raw.value.nodes?.[item.start]?.elementId ?? item.start),
  target: String(item.endElementId ?? raw.value.nodes?.[item.end]?.elementId ?? item.end),
  label: item.properties?.relation || '',
  data: item,
})))

const search = () => {
  loading.value = true
  MetaApi.influence(filters.value, { warning: false }).then((result: any) => {
    raw.value = ApiUtil.data(result) ?? { nodes: {}, relations: {}, models: {} }
    selected.value = null
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}

onMounted(search)
</script>

<template>
  <GovernPageShell title="影响分析" description="沿字段依赖关系向下游分析变更影响范围" back="/govern/meta/model">
  <el-card shadow="never" class="search-card">
    <div class="analysis-filter">
      <el-form class="analysis-filter__form" :inline="true" :model="filters">
        <el-form-item label="所属包">
          <el-input v-model="filters.catalog" clearable />
        </el-form-item>
        <el-form-item label="模型编码">
          <el-input v-model="filters.model" clearable />
        </el-form-item>
        <el-form-item label="字段编码">
          <el-input v-model="filters.code" clearable />
        </el-form-item>
        <el-form-item label="关系深度">
          <el-input-number v-model="filters.minHops" :min="0" :max="10" controls-position="right" />
          <span class="range-separator">至</span>
          <el-input-number v-model="filters.maxHops" :min="0" :max="10" controls-position="right" />
        </el-form-item>
      </el-form>
      <div class="analysis-filter__action">
        <el-button type="primary" :loading="loading" @click="search">分析</el-button>
      </div>
    </div>
  </el-card>
  <div class="graph-layout" v-loading="loading">
    <el-card shadow="never" class="graph-card">
      <MetadataGraph
        :nodes="graphNodes"
        :edges="graphEdges"
        @node-click="selected = { type: 'node', value: $event }"
        @edge-click="selected = { type: 'edge', value: $event }"
        @blank-click="selected = null"
      />
    </el-card>
    <el-card shadow="never" class="detail-panel">
      <LayoutHeading title="属性信息" />
      <el-empty v-if="!selected" :image-size="70" description="点击节点或关系查看属性" />
      <el-descriptions v-else-if="selected.type === 'node'" :column="1" border>
        <el-descriptions-item label="所属包">{{ selected.value.data?.properties?.catalog }}</el-descriptions-item>
        <el-descriptions-item label="模型编码">{{ selected.value.data?.properties?.model }}</el-descriptions-item>
        <el-descriptions-item label="字段编码">{{ selected.value.data?.properties?.column || '模型级' }}</el-descriptions-item>
      </el-descriptions>
      <el-descriptions v-else :column="1" border>
        <el-descriptions-item label="来源包">{{ selected.value.data?.properties?.source_catalog }}</el-descriptions-item>
        <el-descriptions-item label="来源模型">{{ selected.value.data?.properties?.source_model }}</el-descriptions-item>
        <el-descriptions-item label="来源字段">{{ selected.value.data?.properties?.source_column }}</el-descriptions-item>
        <el-descriptions-item label="关联关系">{{ selected.value.data?.properties?.relation }}</el-descriptions-item>
        <el-descriptions-item label="目标包">{{ selected.value.data?.properties?.target_catalog }}</el-descriptions-item>
        <el-descriptions-item label="目标模型">{{ selected.value.data?.properties?.target_model }}</el-descriptions-item>
        <el-descriptions-item label="目标字段">{{ selected.value.data?.properties?.target_column }}</el-descriptions-item>
      </el-descriptions>
      <el-button
        v-if="selected?.type === 'node'"
        class="mt-10 w-full"
        type="primary"
        plain
        @click="router.push({ path: '/govern/meta/detail', query: { catalog: selected.value.data.properties.catalog, code: selected.value.data.properties.model, column: selected.value.data.properties.column } })"
      >
        查看元数据详情
      </el-button>
    </el-card>
  </div>
  </GovernPageShell>
</template>

<style scoped>
.analysis-filter {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
}
.analysis-filter__form {
  display: flex;
  flex: 1;
  flex-wrap: wrap;
  gap: 12px 20px;
  min-width: 0;
}
.analysis-filter__form :deep(.el-form-item) {
  margin-right: 0;
  margin-bottom: 0;
}
.analysis-filter__action {
  flex: none;
  padding-top: 1px;
}
.range-separator { margin: 0 8px; color: #909399; }
.graph-layout { display: grid; grid-template-columns: minmax(0, 1fr) 360px; gap: 15px; }
.graph-card :deep(.el-card__body) { height: 620px; padding: 0; }
.detail-panel { min-height: 620px; }
@media (max-width: 1100px) {
  .graph-layout { grid-template-columns: 1fr; }
}
@media (max-width: 760px) {
  .analysis-filter {
    flex-direction: column;
  }
  .analysis-filter__action {
    align-self: flex-end;
  }
}
</style>
