<script setup lang="ts">
import { ref } from 'vue'
import KnowledgeApi from '@/api/lm/KnowledgeApi'
import ApiUtil from '@/utils/ApiUtil'
import MetadataTable from '@/components/Data/MetadataTable.vue'

const loading = ref(false)
const form: any = ref({ metadata: {} })
const result = ref<any>(null)

const handleRecall = () => {
  if (!form.value.id || !form.value.query) return
  loading.value = true
  KnowledgeApi.recall(form.value).then((res: any) => {
    result.value = ApiUtil.data(res)
  }).finally(() => {
    loading.value = false
  })
}

const drawerVisible = ref(false)
const drawerData = ref<any>({})
const drawerType = ref('')

const showChunkDetail = (chunk: any) => {
  drawerType.value = 'chunk'
  Object.assign(drawerData.value, chunk, {
    name: getDocumentName(chunk.documentId),
    metadata: getDocumentMeta(chunk.documentId)
  })
  drawerVisible.value = true
}

const showSegmentDetail = (segment: any) => {
  drawerType.value = 'segment'
  Object.assign(drawerData.value, segment, {
    name: getDocumentName(segment.documentId),
    metadata: getDocumentMeta(segment.documentId)
  })
  drawerVisible.value = true
}

const getDocumentName = (documentId: number) => {
  if (!result.value) return ''
  const doc = result.value.documents?.find((d: any) => d.id === documentId)
  return doc ? doc.name : `文档ID: ${documentId}`
}

const getDocumentMeta = (documentId: number) => {
  if (!result.value) return {}
  const doc = result.value.documents?.find((d: any) => d.id === documentId)
  return doc?.metadata || {}
}

const getChunksBySegmentId = (segmentId: number) => {
  if (!result.value) return []
  return result.value.chunks?.filter((c: any) => c.segmentId === segmentId) || []
}

const formatScore = (score: number) => {
  return (score * 100).toFixed(2) + '%'
}

const getScoreType = (score: number) => {
  if (score >= 0.9) return 'danger'
  if (score >= 0.7) return 'warning'
  return 'info'
}
</script>

<template>
  <div class="recall-layout">
    <div class="recall-left">
      <el-card shadow="never" class="recall-form-card">
        <template #header>
          <span class="recall-form-title">检索条件</span>
        </template>
        <el-form label-position="top" size="default">
          <el-form-item label="知识库" required>
            <form-select v-model="form.id" :callback="KnowledgeApi.list" placeholder="请选择知识库" clearable />
          </el-form-item>
          <el-form-item label="检索内容" required>
            <el-input v-model="form.query" type="textarea" :rows="4" placeholder="请输入检索内容" />
          </el-form-item>
          <el-form-item label="召回数量">
            <el-input-number v-model="form.topK" :min="1" placeholder="不填则使用默认值" style="width: 100%;" />
          </el-form-item>
          <el-form-item label="评分阈值">
            <el-input-number v-model="form.score" :precision="2" :step="0.1" :min="0" :max="1" placeholder="不填则不限制" style="width: 100%;" />
          </el-form-item>
          <el-form-item label="元数据">
            <metadata-table v-model="form.metadata" :editable="true" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="loading" :disabled="!form.id || !form.query" @click="handleRecall" style="width: 100%">检索</el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </div>

    <div class="recall-right">
      <template v-if="result">
        <div class="recall-result-header">
          <span class="recall-kb-name">{{ result.name }}</span>
          <el-tag size="small" type="info" class="recall-scope-tag">召回范围({{ result.recallScope }})</el-tag>
          <span class="recall-count">共 {{ result.chunks?.length || 0 }} 个匹配块</span>
        </div>

        <el-scrollbar class="recall-result-body">
          <template v-if="result.recallScope === 'chunk'">
            <div
              v-for="chunk in result.chunks"
              :key="'c-' + chunk.id"
              class="recall-item recall-chunk-item"
              @click="showChunkDetail(chunk)"
            >
              <div class="recall-item-header">
                <span class="recall-item-id">Chunk #{{ chunk.id }}</span>
                <span class="recall-doc-name">{{ getDocumentName(chunk.documentId) }}</span>
                <el-tag class="recall-item-score" :type="getScoreType(chunk._score)" size="small" effect="dark">
                  {{ formatScore(chunk._score) }}
                </el-tag>
              </div>
              <div class="recall-item-content">{{ chunk.content }}</div>
            </div>
          </template>

          <!-- document-scope: segments with nested chunks -->
          <template v-else>
            <div
              v-for="segment in result.segments"
              :key="'s-' + segment.id"
              class="recall-segment-group"
            >
              <div class="recall-segment-header" @click="showSegmentDetail(segment)">
                <div class="recall-segment-title">
                  <el-tag type="primary" size="small">Segment #{{ segment.id }}</el-tag>
                  <span class="recall-doc-name">{{ getDocumentName(segment.documentId) }}</span>
                </div>
                <div class="recall-segment-content">{{ segment.content }}</div>
              </div>
              <div
                v-for="chunk in getChunksBySegmentId(segment.id)"
                :key="'c-' + chunk.id"
                class="recall-item recall-chunk-nested"
                @click="showChunkDetail(chunk)"
              >
                <div class="recall-item-header">
                  <span class="recall-item-id">Chunk #{{ chunk.id }}</span>
                  <el-tag class="recall-item-score" :type="getScoreType(chunk._score)" size="small" effect="dark">
                    {{ formatScore(chunk._score) }}
                  </el-tag>
                </div>
                <div class="recall-item-content">{{ chunk.content }}</div>
              </div>
            </div>
          </template>
        </el-scrollbar>
      </template>

      <el-empty v-else description="请在左侧选择知识库并输入检索内容" :image-size="120" />
    </div>
  </div>

  <el-drawer v-model="drawerVisible" :title="drawerType === 'chunk' ? '块详情' : '段落详情'" size="60%">
    <template v-if="drawerType === 'chunk'">
      <el-descriptions :column="2" border label-width="80px">
        <el-descriptions-item label="块ID">{{ drawerData.id }}</el-descriptions-item>
        <el-descriptions-item label="匹配度">
          <el-tag :type="getScoreType(drawerData._score)" effect="dark">{{ formatScore(drawerData._score) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="段落ID">{{ drawerData.segmentId }}</el-descriptions-item>
        <el-descriptions-item label="文档ID">{{ drawerData.documentId }}</el-descriptions-item>
        <el-descriptions-item label="文档标题" :span="2">{{ drawerData.name }}</el-descriptions-item>
        <el-descriptions-item :span="2" label="元数据">
          <metadata-table v-model="drawerData.metadata" />
        </el-descriptions-item>
        <el-descriptions-item label="内容" :span="2">
          <MarkdownEditor v-model="drawerData.content" />
        </el-descriptions-item>
      </el-descriptions>
    </template>
    <template v-else>
      <el-descriptions :column="2" border label-width="80px">
        <el-descriptions-item label="段落ID">{{ drawerData.id }}</el-descriptions-item>
        <el-descriptions-item label="文档ID">{{ drawerData.documentId }}</el-descriptions-item>
        <el-descriptions-item label="文档标题" :span="2">{{ drawerData.name }}</el-descriptions-item>
        <el-descriptions-item :span="2" label="元数据">
          <metadata-table v-model="drawerData.metadata" />
        </el-descriptions-item>
        <el-descriptions-item label="内容" :span="2">
          <MarkdownEditor v-model="drawerData.content" />
        </el-descriptions-item>
      </el-descriptions>
    </template>
  </el-drawer>
</template>

<style lang="scss" scoped>
.recall-layout {
  display: flex;
  height: calc(100vh - 120px);
  gap: 16px;
}

.recall-left {
  width: 550px;
  flex-shrink: 0;
}

.recall-form-card {
  height: 100%;
  border-radius: 0;
}

.recall-form-title {
  font-weight: 600;
  font-size: 15px;
}

.recall-right {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  background: var(--el-bg-color);
}

.recall-result-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  border-bottom: 1px solid var(--el-border-color-light);
  flex-shrink: 0;
}

.recall-kb-name {
  font-weight: 600;
  font-size: 16px;
}

.recall-count {
  color: var(--el-text-color-secondary);
  font-size: 13px;
  margin-left: auto;
}

.recall-result-body {
  flex: 1;
  padding: 16px;
}

.recall-item {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  padding: 12px;
  cursor: pointer;
  transition: border-color 0.2s, box-shadow 0.2s;

  &:hover {
    border-color: var(--el-color-primary-light-3);
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  }
}

.recall-chunk-item {
  margin-bottom: 10px;
}

.recall-item-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}

.recall-doc-name {
  color: var(--el-text-color-secondary);
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.recall-item-id {
  color: var(--el-text-color-placeholder);
  font-size: 12px;
}

.recall-item-score {
  margin-left: auto;
}

.recall-item-content {
  color: var(--el-text-color-regular);
  font-size: 14px;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 4;
  -webkit-box-orient: vertical;
  line-clamp: 4;
  overflow: hidden;
}

.recall-segment-group {
  margin-bottom: 20px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  overflow: hidden;
  background: var(--el-bg-color);
  transition: box-shadow 0.2s;

  &:hover {
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  }
}

.recall-segment-header {
  padding: 12px 16px;
  cursor: pointer;
  background: var(--el-fill-color-light);
  border-bottom: 1px solid var(--el-border-color-lighter);
  transition: background 0.2s;

  &:hover {
    background: var(--el-fill-color);
  }
}

.recall-segment-title {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}

.recall-segment-content {
  color: var(--el-text-color-secondary);
  font-size: 13px;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  line-clamp: 2;
  overflow: hidden;
  padding-left: 8px;
  border-left: 2px solid var(--el-color-primary-light-5);
}

.recall-chunk-nested {
  margin: 0;
  border: none;
  border-bottom: 1px solid var(--el-border-color-lighter);
  border-radius: 0;
  padding: 12px 16px 12px 26px;
  position: relative;

  &:last-child {
    border-bottom: none;
  }

  &:hover {
    background: var(--el-fill-color-light);
    box-shadow: none;
  }

  &::before {
    content: '';
    position: absolute;
    left: 16px;
    top: 0;
    bottom: 0;
    width: 2px;
    background: var(--el-color-warning-light-5);
  }
}
</style>
