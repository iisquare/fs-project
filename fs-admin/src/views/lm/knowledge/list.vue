<script setup lang="ts">
import { onMounted, ref } from 'vue';
import type { FormInstance, TableInstance } from 'element-plus';
import RouteUtil from '@/utils/RouteUtil'
import { useRoute, useRouter } from 'vue-router';
import KnowledgeApi from '@/api/lm/KnowledgeApi';
import ApiUtil from '@/utils/ApiUtil';
import DateUtil from '@/utils/DateUtil';
import TableUtil from '@/utils/TableUtil';
import RoleApi from '@/api/member/RoleApi';

const route = useRoute()
const router = useRouter()
const tableRef = ref<TableInstance>()
const loading = ref(false)
const searchable = ref(true)
const columns = ref([
  { prop: 'id', label: 'ID' },
  { prop: 'name', label: '知识库名称', slot: 'name' },
  { prop: 'recallTypeText', label: '召回类型' },
  { prop: 'recallScopeText', label: '召回范围' },
  { prop: 'topK', label: '召回数量' },
  { prop: 'score', label: '召回阈值' },
  { prop: 'embeddingModel', label: '词嵌入模型' },
  { prop: 'rerankModel', label: '重排序模型' },
  { prop: 'reranked', label: '启用重排序' },
  { prop: 'labels', label: '标签', slot: 'labels' },
  { prop: 'role', label: '授权角色', slot: 'role' },
  { prop: 'description', label: '描述', hide: true },
  { prop: 'sort', label: '排序' },
  { prop: 'statusText', label: '状态' },
])
const config: any = ref({
  ready: false,
  status: {},
  recallTypes: {},
  recallScopes: {},
  models: [],
})
const rows = ref([])
const filterRef = ref<FormInstance>()
const filters = ref(RouteUtil.query2filter(route, { advanced: false, agentIds: [] }))
const pagination = ref(RouteUtil.pagination(filters.value))
const selection: any = ref([])
const handleRefresh = (filter2query: boolean, keepPage: boolean) => {
  tableRef.value?.clearSelection()
  Object.assign(filters.value, RouteUtil.pagination2filter(pagination.value, keepPage))
  filter2query && RouteUtil.filter2query(route, router, filters.value)
  loading.value = true
  KnowledgeApi.list(filters.value).then((result: any) => {
    RouteUtil.result2pagination(pagination.value, result)
    rows.value = result.data.rows
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}
onMounted(() => {
  handleRefresh(false, true)
  KnowledgeApi.config().then((result: any) => {
    Object.assign(config.value, { ready: true }, ApiUtil.data(result))
  }).catch(() => {})
})
const infoVisible = ref(false)
const formVisible = ref(false)
const formLoading = ref(false)
const form: any = ref({})
const formRef: any = ref<FormInstance>()
const rules = ref({
  name: [{ required: true, message: '请输入知识库名称', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
})
const handleAdd = () => {
  form.value = {
    status: '1',
    topK: 3,
    recallType: 'hybrid',
    recallScope: 'chunk',
    splitSeparator: '\\r\\n',
    splitSegmentTokens: 2000,
    splitChunkTokens: 500,
    splitOverlayTokens: 50,
    labels: [],
    roleIds: [],
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
    roleIds: scope.row.roleIds || [],
    labels: scope.row.labels || [],
  })
  formVisible.value = true
}
const handleSubmit = () => {
  formRef.value?.validate((valid: boolean) => {
    if (!valid || formLoading.value) return
    formLoading.value = true
    KnowledgeApi.save(form.value, { success: true }).then(result => {
      handleRefresh(false, true)
      formVisible.value = false
    }).catch(() => {}).finally(() => {
      formLoading.value = false
    })
  })
}
const handleDelete = () => {
  TableUtil.selection(selection.value).then((ids: any) => {
    loading.value = true
    KnowledgeApi.delete(ids, { success: true }).then(() => {
      handleRefresh(false, true)
    }).catch(() => {
      loading.value = false
    })
  }).catch(() => {})
}
</script>

<template>
  <el-card :bordered="false" shadow="never" class="fs-table-search" v-show="searchable">
    <form-search ref="filterRef" :model="filters">
      <form-search-item label="名称" prop="name">
        <el-input v-model="filters.name" clearable />
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
      <TableColumn :columns="columns">
        <template #name="scope">
          <el-link type="primary" underline="never" @click="router.push({ path: '/lm/knowledge/document', query: { knowledgeId: scope.row.id } })">{{ scope.row.name }}</el-link>
        </template>
        <template #role="scope">
          <el-space><el-tag v-for="item in scope.row.roles" :key="item.id">{{ item.name }}</el-tag></el-space>
        </template>
        <template #labels="scope">
          <el-space><el-tag v-for="item in scope.row.labels" :key="item">{{ item }}</el-tag></el-space>
        </template>
      </TableColumn>
      <el-table-column label="操作">
        <template #default="scope">
          <el-button link @click="handleShow(scope)" v-permit="'lm:knowledge:'">查看</el-button>
          <el-button link @click="handleEdit(scope)" v-permit="'lm:knowledge:modify'">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>
    <TablePagination v-model="pagination" @change="handleRefresh(true, true)" />
  </el-card>
  <el-drawer v-model="infoVisible" :title="'信息查看 - ' + form.id" size="60%">
    <el-descriptions :column="2" border>
      <el-descriptions-item label="知识库名称">{{ form.name }}</el-descriptions-item>
      <el-descriptions-item label="启用重排序">{{ form.reranked }}</el-descriptions-item>
      <el-descriptions-item label="召回类型">{{ form.recallTypeText }}</el-descriptions-item>
      <el-descriptions-item label="召回范围">{{ form.recallScopeText }}</el-descriptions-item>
      <el-descriptions-item label="召回数量">{{ form.topK }}</el-descriptions-item>
      <el-descriptions-item label="召回阈值">{{ form.score }}</el-descriptions-item>
      <el-descriptions-item label="词嵌入模型">{{ form.embeddingModel }}</el-descriptions-item>
      <el-descriptions-item label="重排序模型">{{ form.rerankModel }}</el-descriptions-item>
      <el-descriptions-item label="标签">
        <el-space><el-tag v-for="item in form.labels" :key="item">{{ item }}</el-tag></el-space>
      </el-descriptions-item>
      <el-descriptions-item label="授权角色">
        <el-space><el-tag v-for="item in form.roles" :key="item.id">{{ item.name }}</el-tag></el-space>
      </el-descriptions-item>
      <el-descriptions-item label="段落分隔符">{{ form.splitSeparator }}</el-descriptions-item>
      <el-descriptions-item label="分段长度">{{ form.splitSegmentTokens }}</el-descriptions-item>
      <el-descriptions-item label="分块长度">{{ form.splitChunkTokens }}</el-descriptions-item>
      <el-descriptions-item label="重叠长度">{{ form.splitOverlayTokens }}</el-descriptions-item>
      <el-descriptions-item label="排序">{{ form.sort }}</el-descriptions-item>
      <el-descriptions-item label="状态">{{ form.statusText }}</el-descriptions-item>
      <el-descriptions-item label="描述" :span="2">{{ form.description ? form.description : '暂无' }}</el-descriptions-item>
      <el-descriptions-item label="创建者">{{ form.createdUserInfo?.name }}</el-descriptions-item>
      <el-descriptions-item label="创建时间">{{ DateUtil.format(form.createdTime) }}</el-descriptions-item>
      <el-descriptions-item label="修改者">{{ form.updatedUserInfo?.name }}</el-descriptions-item>
      <el-descriptions-item label="修改时间">{{ DateUtil.format(form.updatedTime) }}</el-descriptions-item>
    </el-descriptions>
  </el-drawer>
  <el-drawer v-model="formVisible" :close-on-click-modal="false" :show-close="false" :destroy-on-close="true" size="60%">
    <template #header="{ close, titleId, titleClass }">
      <h4 :id="titleId" :class="titleClass">{{ '信息' + (form.id ? ('修改 - ' + form.id) : '添加') }}</h4>
      <el-space>
        <el-button type="primary" @click="handleSubmit" :loading="formLoading">确定</el-button>
        <el-button @click="close">取消</el-button>
      </el-space>
    </template>
    <el-form ref="formRef" :model="form" :rules="rules">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="知识库名称"><el-input v-model="form.name" /></el-descriptions-item>
        <el-descriptions-item label="启用重排序">
          <el-switch v-model="form.reranked" :active-value="true" :inactive-value="false" />
        </el-descriptions-item>
        <el-descriptions-item label="召回类型">
          <el-radio-group v-model="form.recallType">
            <el-radio-button v-for="(value, key) in config.recallTypes" :key="key" :value="key" :label="value" />
          </el-radio-group>
        </el-descriptions-item>
        <el-descriptions-item label="召回范围">
          <el-radio-group v-model="form.recallScope">
            <el-radio-button v-for="(value, key) in config.recallScopes" :key="key" :value="key" :label="value" />
          </el-radio-group>
        </el-descriptions-item>
        <el-descriptions-item label="召回数量"><el-input-number v-model="form.topK" /></el-descriptions-item>
        <el-descriptions-item label="召回阈值">
          <el-input-number v-model="form.score" :precision="2" :step="0.1" placeholder="为0时不限制" />
        </el-descriptions-item>
        <el-descriptions-item label="词嵌入模型">
          <el-select v-model="form.embeddingModel" placeholder="请选择" filterable clearable>
            <el-option v-for="(item, key) in config.models" :key="key" :value="item.id" :label="item.id" />
          </el-select>
        </el-descriptions-item>
        <el-descriptions-item label="重排序模型">
          <el-select v-model="form.rerankModel" placeholder="请选择" filterable clearable>
            <el-option v-for="(item, key) in config.models" :key="key" :value="item.id" :label="item.id" />
          </el-select>
        </el-descriptions-item>
        <el-descriptions-item label="标签">
          <el-select v-model="form.labels" multiple filterable allow-create :reserve-keyword="false" default-first-option placeholder="输入后回车创建标签" />
        </el-descriptions-item>
        <el-descriptions-item label="授权角色">
          <form-select v-model="form.roleIds" :callback="RoleApi.list" multiple clearable />
        </el-descriptions-item>
        <el-descriptions-item label="段落分隔符"><el-input v-model="form.splitSeparator" /></el-descriptions-item>
        <el-descriptions-item label="分段长度"><el-input-number v-model="form.splitSegmentTokens" /></el-descriptions-item>
        <el-descriptions-item label="分块长度"><el-input-number v-model="form.splitChunkTokens" /></el-descriptions-item>
        <el-descriptions-item label="重叠长度"><el-input-number v-model="form.splitOverlayTokens" /></el-descriptions-item>
        <el-descriptions-item label="排序"><el-input-number v-model="form.sort" /></el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-select v-model="form.status" placeholder="请选择">
            <el-option v-for="(value, key) in config.status" :key="key" :value="key" :label="value" />
          </el-select>
        </el-descriptions-item>
        <el-descriptions-item label="描述"><el-input type="textarea" v-model="form.description" /></el-descriptions-item>
      </el-descriptions>
    </el-form>
  </el-drawer>
</template>

<style lang="scss" scoped>
</style>
