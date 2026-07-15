<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import type { FormInstance, TableInstance } from 'element-plus';
import RouteUtil from '@/utils/RouteUtil'
import { useRoute, useRouter } from 'vue-router';
import KnowledgeSegmentApi from '@/api/lm/KnowledgeSegmentApi';
import KnowledgeChunkApi from '@/api/lm/KnowledgeChunkApi';
import KnowledgeApi from '@/api/lm/KnowledgeApi';
import ApiUtil from '@/utils/ApiUtil';
import DateUtil from '@/utils/DateUtil';
import TableUtil from '@/utils/TableUtil';
import { useUserStore } from '@/stores/user';
import KnowledgeDocumentApi from '@/api/lm/KnowledgeDocumentApi';

const truncateLength = 200

const route = useRoute()
const router = useRouter()
const user = useUserStore()
const tableRef = ref<TableInstance>()
const loading = ref(false)
const searchable = ref(true)
const documentInfo: any = ref(null)
const documentLoading = ref(true)
const columns = ref([
  { prop: 'id', label: 'ID', width: '80px', hide: true },
  { prop: 'content', label: '分段内容', slot: 'content', minWidth: '300px' },
  { prop: 'tokenSize', label: '字符数量', width: '100px' },
  { prop: 'statusText', label: '状态', slot: 'statusText', width: '100px' },
])
const statusTagType = (status: string): 'success' | 'warning' | 'danger' | 'info' => {
  const map: Record<string, 'success' | 'warning' | 'danger' | 'info'> = { '1': 'success', '0': 'warning', '-1': 'danger' }
  return map[status] || 'info'
}
const config = ref({
  ready: false,
  status: {},
})
const rows = ref([])
const filterRef = ref<FormInstance>()
const filters = ref(RouteUtil.query2filter(route, { advanced: false, agentIds: [], documentId: route.query.documentId }))
const pagination = ref(RouteUtil.pagination(filters.value))
const selection: any = ref([])
const handleRefresh = (filter2query: boolean, keepPage: boolean) => {
  tableRef.value?.clearSelection()
  Object.assign(filters.value, RouteUtil.pagination2filter(pagination.value, keepPage))
  filter2query && RouteUtil.filter2query(route, router, filters.value)
  loading.value = true
  KnowledgeSegmentApi.list(filters.value).then((result: any) => {
    RouteUtil.result2pagination(pagination.value, result)
    rows.value = result.data.rows
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}
onMounted(() => {
  handleRefresh(false, true)
  KnowledgeSegmentApi.config().then((result: any) => {
    Object.assign(config.value, { ready: true }, ApiUtil.data(result))
  }).catch(() => {})
  KnowledgeChunkApi.config().then((result: any) => {
    Object.assign(config.value, ApiUtil.data(result))
  }).catch(() => {})
  const documentId = route.query.documentId
  if (documentId) {
    documentLoading.value = true
    KnowledgeDocumentApi.info(documentId).then((result: any) => {
      documentInfo.value = ApiUtil.data(result)
    }).catch(() => {}).finally(() => {
      documentLoading.value = false
    })
  } else {
    documentLoading.value = false
  }
})
const infoVisible = ref(false)
const formVisible = ref(false)
const formLoading = ref(false)
const form: any = ref({})
const formRef: any = ref<FormInstance>()

const handleAdd = () => {
  form.value = {
    status: '1',
    documentId: route.query.documentId,
  }
  formVisible.value = true
}
const handleShow = (scope: any) => {
  form.value = Object.assign({}, scope.row)
  infoVisible.value = true
}
const handleEdit = (scope: any) => {
  form.value = Object.assign({}, scope.row, {
    status: scope.row.status + '',
  })
  formVisible.value = true
}
const handleSubmit = () => {
  if (formLoading.value) return
  formLoading.value = true
  KnowledgeSegmentApi.save(form.value, { success: true }).then(result => {
    handleRefresh(false, true)
    formVisible.value = false
  }).catch(() => {}).finally(() => {
    formLoading.value = false
  })
}
const handleDelete = () => {
  TableUtil.selection(selection.value).then((ids: any) => {
    loading.value = true
    KnowledgeSegmentApi.delete(ids, { success: true }).then(() => {
      handleRefresh(false, true)
    }).catch(() => {
      loading.value = false
    })
  }).catch(() => {})
}

// --- chunk management ---
const chunkSelection: Record<string, any[]> = reactive({})

const chunkFormVisible = ref(false)
const chunkFormLoading = ref(false)
const chunkForm: any = ref({})
const chunkFormRef: any = ref<FormInstance>()
const chunkFormSegmentId: any = ref(null)
const chunkRules = ref({
  content: [{ required: true, message: '请输入内容', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
})
const handleChunkAdd = (segmentId: any) => {
  chunkFormSegmentId.value = segmentId
  chunkForm.value = {
    status: '1',
    segmentId,
  }
  chunkFormVisible.value = true
}
const handleChunkEdit = (segmentId: any, row: any) => {
  chunkFormSegmentId.value = segmentId
  chunkForm.value = Object.assign({}, row, {
    status: row.status + '',
  })
  chunkFormVisible.value = true
}
const chunkEmbeddingLoading = ref(false)
const handleChunkEmbedding = () => {
  if (chunkEmbeddingLoading.value) return
  chunkEmbeddingLoading.value = true
  KnowledgeApi.embedding({
    knowledgeId: route.query.knowledgeId,
    content: chunkForm.value.content,
  }, { success: true }).then(result => {
    chunkForm.value.embedding = JSON.stringify(ApiUtil.data(result).data[0].embedding)
  }).catch(() => {}).finally(() => {
    chunkEmbeddingLoading.value = false
  })
}
const handleChunkSubmit = () => {
  if (chunkFormLoading.value) return
  chunkFormLoading.value = true
  KnowledgeChunkApi.save(chunkForm.value, { success: true }).then(() => {
    handleRefresh(false, true)
    chunkFormVisible.value = false
  }).catch(() => {}).finally(() => {
    chunkFormLoading.value = false
  })
}
const handleChunkDelete = (segmentId: any) => {
  const sel = chunkSelection[segmentId]
  TableUtil.selection(sel || []).then((ids: any) => {
    KnowledgeChunkApi.delete(ids, { success: true }).then(() => {
      handleRefresh(false, true)
    }).catch(() => {})
  }).catch(() => {})
}
</script>

<template>
  <el-card :bordered="false" shadow="never" class="fs-table-search" v-show="searchable">
    <el-space v-if="route.query.documentId">
      <LayoutBack :to="'/lm/knowledge/document?knowledgeId=' + route.query.knowledgeId" />
      <el-skeleton v-if="documentLoading" :rows="1" animated />
      <h3 v-else>{{ documentInfo?.name }}</h3>
    </el-space>
    <el-divider v-if="route.query.documentId" />
    <form-search ref="filterRef" :model="filters">
      <form-search-item label="内容" prop="content">
        <el-input v-model="filters.content" placeholder="请输入内容" clearable />
      </form-search-item>
      <form-search-item label="状态" prop="status">
        <el-select v-model="filters.status" placeholder="请选择" clearable>
          <el-option v-for="(value, key) in config.status" :key="key" :value="key" :label="value" />
        </el-select>
      </form-search-item>
      <form-search-item>
        <el-button type="primary" @click="handleRefresh(true, false)">查询</el-button>
        <el-button @click="filterRef?.resetFields()">重置</el-button>
      </form-search-item>
    </form-search>
  </el-card>
  <el-card :bordered="false" shadow="never" class="fs-table-card">
    <div class="fs-table-toolbar flex-between">
      <el-space>
        <button-add v-permit="'lm:knowledge:add'" @click="handleAdd" />
        <button-delete v-permit="'lm:knowledge:delete'" :disabled="selection.length === 0" @click="handleDelete" />
      </el-space>
      <el-space>
        <button-search @click="searchable = !searchable" />
        <button-refresh @click="handleRefresh(true, true)" :loading="loading" />
        <TableColumnSetting v-model="columns" :table="tableRef" />
      </el-space>
    </div>
    <el-table
      ref="tableRef"
      :data="rows"
      :row-key="(record: any) => record.id"
      :border="true"
      v-loading="loading"
      table-layout="auto"
      @selection-change="(s: any) => selection = s"
    >
      <el-table-column type="selection" />
      <el-table-column type="expand">
        <template #default="scope">
          <div class="expand-content">
            <div class="expand-toolbar" v-if="scope.row.chunks?.length">
              <span class="expand-title">分块列表（{{ scope.row.chunks.length }}）</span>
              <el-space>
                <button-add v-permit="'lm:knowledge:add'" @click="handleChunkAdd(scope.row.id)" size="small" />
                <button-delete v-permit="'lm:knowledge:delete'" :disabled="!(chunkSelection[scope.row.id] || []).length" @click="handleChunkDelete(scope.row.id)" size="small" />
              </el-space>
            </div>
            <div v-if="scope.row.chunks?.length" class="chunk-cards">
              <div
                v-for="chunk in scope.row.chunks"
                :key="chunk.id"
                class="chunk-card"
                :class="{ 'chunk-card--selected': (chunkSelection[scope.row.id] || []).some((s: any) => s.id === chunk.id) }"
                @click="() => {
                  const sel = chunkSelection[scope.row.id] || []
                  const idx = sel.findIndex((s: any) => s.id === chunk.id)
                  if (idx > -1) sel.splice(idx, 1)
                  else sel.push(chunk)
                  chunkSelection[scope.row.id] = [...sel]
                }"
              >
                <div class="chunk-card__header">
                  <div class="chunk-card__meta">
                    <span class="chunk-card__id">#{{ chunk.id }}</span>
                    <el-tag :type="statusTagType(chunk.status)" size="small" effect="plain">{{ chunk.statusText }}</el-tag>
                  </div>
                  <el-button link type="primary" @click.stop="handleChunkEdit(scope.row.id, chunk)" v-permit="'lm:knowledge:modify'">编辑</el-button>
                </div>
                <div class="chunk-card__body">
                  <span class="chunk-card__content">{{ chunk.content || '—' }}</span>
                </div>
              </div>
            </div>
            <el-empty v-else description="暂无分块" :image-size="60">
              <button-add v-permit="'lm:knowledge:add'" @click="handleChunkAdd(scope.row.id)" />
            </el-empty>
          </div>
        </template>
      </el-table-column>
      <TableColumn :columns="columns">
        <template #content="scope">
          <el-popover
            :width="400"
            trigger="click"
            :show-after="200"
            :popper-options="{
              modifiers: [
                { name: 'flip', options: { fallbackPlacements: ['left', 'top', 'bottom'] } },
                { name: 'preventOverflow', options: { padding: 16 } },
              ],
            }"
          >
            <template #reference>
              <span class="content-cell">
                {{ scope.row.content?.length > truncateLength ? scope.row.content.slice(0, truncateLength) + '...' : (scope.row.content || '—') }}
              </span>
            </template>
            <div class="content-popover">{{ scope.row.content }}</div>
          </el-popover>
        </template>
        <template #statusText="scope">
          <el-tag :type="statusTagType(scope.row.status)" size="small" effect="plain">
            {{ scope.row.statusText }}
          </el-tag>
        </template>
        <template #role="scope">
          <el-space><el-tag v-for="item in scope.row.roles" :key="item.id">{{ item.name }}</el-tag></el-space>
        </template>
      </TableColumn>
      <el-table-column label="操作" width="110px">
        <template #default="scope">
          <el-button link @click="handleShow(scope)" v-permit="'lm:knowledge:'">查看</el-button>
          <el-button link @click="handleEdit(scope)" v-permit="'lm:knowledge:modify'">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>
    <TablePagination v-model="pagination" @change="handleRefresh(true, true)" />
  </el-card>
  <el-drawer v-model="infoVisible" :title="'信息查看 - ' + form.id" size="80%">
    <el-descriptions border label-width="100px">
      <el-descriptions-item label="所属文档" :span="3">{{ form.documentInfo?.name }}</el-descriptions-item>
      <el-descriptions-item label="所属知识库">{{ form.knowledgeInfo?.name }}</el-descriptions-item>
      <el-descriptions-item label="状态">{{ form.statusText }}</el-descriptions-item>
      <el-descriptions-item label="字符数量">{{ form.tokenSize }}</el-descriptions-item>
      <el-descriptions-item label="创建者">{{ form.createdUserInfo?.name }}</el-descriptions-item>
       <el-descriptions-item label="创建时间" :span="2">{{ DateUtil.format(form.createdTime) }}</el-descriptions-item>
      <el-descriptions-item label="修改者">{{ form.updatedUserInfo?.name }}</el-descriptions-item>
      <el-descriptions-item label="修改时间" :span="2">{{ DateUtil.format(form.updatedTime) }}</el-descriptions-item>
      <el-descriptions-item label="分段内容" :span="3"><markdown-editor v-model="form.content" readonly :height="600" /></el-descriptions-item>
    </el-descriptions>
  </el-drawer>
  <el-drawer v-model="formVisible" :close-on-click-modal="false" :show-close="false" :destroy-on-close="true" size="80%">
    <template #header="{ close, titleId, titleClass }">
      <h4 :id="titleId" :class="titleClass">{{ '信息' + (form.id ? ('修改 - ' + form.id) : '添加') }}</h4>
      <el-space>
        <el-button type="primary" @click="handleSubmit" :loading="formLoading">确定</el-button>
        <el-button @click="close">取消</el-button>
      </el-space>
    </template>
    <el-descriptions border :column="2" label-width="100px">
      <el-descriptions-item label="字符数量"><el-input-number v-model="form.tokenSize" :min="0" /></el-descriptions-item>
      <el-descriptions-item label="状态">
        <el-select v-model="form.status" placeholder="请选择">
          <el-option v-for="(value, key) in config.status" :key="key" :value="key" :label="value" />
        </el-select>
      </el-descriptions-item>
      <el-descriptions-item label="分段内容" :span="2"><markdown-editor v-model="form.content" :height="600" /></el-descriptions-item>
    </el-descriptions>
  </el-drawer>
  <el-drawer v-model="chunkFormVisible" :close-on-click-modal="false" :show-close="false" :destroy-on-close="true" size="800px">
    <template #header="{ close, titleId, titleClass }">
      <h4 :id="titleId" :class="titleClass">{{ '分块' + (chunkForm.id ? ('修改 - ' + chunkForm.id) : '添加') }}</h4>
      <el-space>
        <el-button type="primary" @click="handleChunkSubmit" :loading="chunkFormLoading">确定</el-button>
        <el-button @click="close">取消</el-button>
      </el-space>
    </template>
    <el-form ref="chunkFormRef" :model="chunkForm" :rules="chunkRules" label-width="auto">
      <el-form-item label="状态" prop="status">
        <el-select v-model="chunkForm.status" placeholder="请选择">
          <el-option v-for="(value, key) in config.status" :key="key" :value="key" :label="value" />
        </el-select>
      </el-form-item>
      <el-form-item label="分块内容">
        <el-input type="textarea" v-model="chunkForm.content" :rows="20" />
      </el-form-item>
      <el-form-item label="操作">
        <el-button type="primary" @click="handleChunkEmbedding" :loading="chunkEmbeddingLoading" :disabled="!chunkForm.content">生成词向量</el-button>
      </el-form-item>
      <el-form-item label="词向量">
        <el-input type="textarea" v-model="chunkForm.embedding" :rows="8" style="flex: 1;" />
      </el-form-item>
    </el-form>
  </el-drawer>
</template>

<style lang="scss" scoped>
// --- 主表格内容列 ---
.content-cell {
  display: -webkit-box;
  line-clamp: 2;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  cursor: pointer;
  line-height: 1.6;
  color: var(--el-text-color-regular);
  transition: color .2s;

  &:hover {
    color: var(--el-color-primary);
  }
}

.content-popover {
  max-height: 400px;
  overflow-y: auto;
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.8;
  font-size: 14px;
  color: var(--el-text-color-regular);
}

// --- 展开行 ---
.expand-content {
  padding: 8px 16px 16px;
  background: var(--el-fill-color-lighter);
  border-radius: 6px;
}

.expand-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.expand-title {
  font-size: 13px;
  font-weight: 500;
  color: var(--el-text-color-secondary);
}

// --- 分块卡片 ---
.chunk-cards {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.chunk-card {
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  padding: 10px 14px;
  cursor: pointer;
  transition: border-color .25s, box-shadow .25s, background .25s;

  &:hover {
    border-color: var(--el-color-primary-light-3);
    box-shadow: 0 1px 6px rgba(var(--el-color-primary-rgb, 64 158 255), .08);
  }

  &--selected {
    border-color: var(--el-color-primary);
    background: var(--el-color-primary-light-9);
    box-shadow: 0 0 0 1px var(--el-color-primary-light-5);
  }

  &__header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 6px;
  }

  &__meta {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  &__id {
    font-size: 12px;
    color: var(--el-text-color-placeholder);
    font-family: monospace;
  }

  &__body {
    line-height: 1.7;
    color: var(--el-text-color-regular);
    font-size: 14px;
  }

  &__content {
    display: -webkit-box;
    line-clamp: 4;
    -webkit-line-clamp: 4;
    -webkit-box-orient: vertical;
    overflow: hidden;
    word-break: break-word;
  }
}

// --- 查看抽屉描述列表 ---
:deep(.multiline-text) {
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.8;
  max-height: 500px;
  overflow-y: auto;
}

// --- 表格微调 ---
:deep(.el-table__expanded-cell) {
  padding: 8px 12px !important;
}

:deep(.el-table .el-table__row) {
  &:has(.el-table__expanded-cell) {
    > td {
      background: var(--el-fill-color-lighter);
    }
  }
}
</style>
