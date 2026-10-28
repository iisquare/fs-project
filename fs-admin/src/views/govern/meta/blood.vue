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
const raw = ref<any>({ nodes: {}, relations: {} })
const selected = ref<any>(null)
const filters = ref<any>({
  catalog: String(route.query.catalog || '/'),
  code: String(route.query.code || ''),
  column: String(route.query.column || ''),
  minHops: 0,
  maxHops: 2,
})

const graphNodes = computed<GovernGraphNode[]>(() => Object.values(raw.value.nodes ?? {}).map((item: any) => ({
  id: String(item.elementId ?? item.identity),
  name: item.properties?.name || item.properties?.code || '未命名模型',
  subtitle: item.properties?.catalog ? `${item.properties.catalog}${item.properties.code || ''}` : '',
  color: '#409eff',
  data: item,
})))

const edgePalette: Record<string, string> = {
  inherit: '#8b5cf6',
  depend: '#409eff',
  association: '#67c23a',
  aggregation: '#e6a23c',
  composition: '#f56c6c',
  foreign: '#14b8a6',
}
const graphEdges = computed<GovernGraphEdge[]>(() => Object.values(raw.value.relations ?? {}).map((item: any) => ({
  id: String(item.elementId ?? item.identity),
  source: String(item.startElementId ?? raw.value.nodes?.[item.start]?.elementId ?? item.start),
  target: String(item.endElementId ?? raw.value.nodes?.[item.end]?.elementId ?? item.end),
  label: item.properties?.relation || '',
  data: item,
})))

const search = () => {
  loading.value = true
  MetaApi.blood(filters.value, { warning: false }).then((result: any) => {
    raw.value = ApiUtil.data(result) ?? { nodes: {}, relations: {} }
    selected.value = null
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}

const openDetail = (node: GovernGraphNode) => {
  const data = node.data?.properties ?? {}
  router.push({
    path: '/govern/meta/detail',
    query: { catalog: data.catalog, code: data.code, column: filters.value.column },
  }).catch(() => {})
}

onMounted(search)
</script>

<template>
  <GovernPageShell title="血缘分析" description="沿模型关系向上游追溯数据来源，定位字段转换链路" back="/govern/meta/model">
  <el-card shadow="never" class="search-card">
    <el-form :inline="true" :model="filters">
      <el-form-item label="所属包">
        <el-input v-model="filters.catalog" placeholder="/" clearable />
      </el-form-item>
      <el-form-item label="模型编码">
        <el-input v-model="filters.code" clearable />
      </el-form-item>
      <el-form-item label="关系深度">
        <el-input-number v-model="filters.minHops" :min="0" :max="10" controls-position="right" />
        <span class="range-separator">至</span>
        <el-input-number v-model="filters.maxHops" :min="0" :max="10" controls-position="right" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="search">分析</el-button>
      </el-form-item>
    </el-form>
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
      <template v-else-if="selected.type === 'node'">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="模型路径">{{ selected.value.data?.properties?.id }}</el-descriptions-item>
          <el-descriptions-item label="所属包">{{ selected.value.data?.properties?.catalog }}</el-descriptions-item>
          <el-descriptions-item label="模型编码">{{ selected.value.data?.properties?.code }}</el-descriptions-item>
          <el-descriptions-item label="模型名称">{{ selected.value.data?.properties?.name }}</el-descriptions-item>
          <el-descriptions-item label="模型类型">{{ selected.value.data?.properties?.type }}</el-descriptions-item>
        </el-descriptions>
        <el-button class="mt-10 w-full" type="primary" plain @click="openDetail(selected.value)">查看元数据详情</el-button>
      </template>
      <el-descriptions v-else :column="1" border>
        <el-descriptions-item label="来源包">{{ selected.value.data?.properties?.source_catalog }}</el-descriptions-item>
        <el-descriptions-item label="来源模型">{{ selected.value.data?.properties?.source_model }}</el-descriptions-item>
        <el-descriptions-item label="来源字段">{{ selected.value.data?.properties?.source_column }}</el-descriptions-item>
        <el-descriptions-item label="关联关系">{{ selected.value.data?.properties?.relation }}</el-descriptions-item>
        <el-descriptions-item label="目标包">{{ selected.value.data?.properties?.target_catalog }}</el-descriptions-item>
        <el-descriptions-item label="目标模型">{{ selected.value.data?.properties?.target_model }}</el-descriptions-item>
        <el-descriptions-item label="目标字段">{{ selected.value.data?.properties?.target_column }}</el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
  </GovernPageShell>
</template>

<style scoped>
.range-separator { margin: 0 8px; color: #909399; }
.graph-layout { display: grid; grid-template-columns: minmax(0, 1fr) 360px; gap: 15px; }
.graph-card :deep(.el-card__body) { height: 620px; padding: 0; }
.detail-panel { min-height: 620px; }
@media (max-width: 1100px) {
  .graph-layout { grid-template-columns: 1fr; }
}
</style>
