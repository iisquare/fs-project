<script setup lang="ts">
import { onMounted, ref } from 'vue';
import type { FormInstance, TableInstance } from 'element-plus';
import RouteUtil from '@/utils/RouteUtil'
import { useRoute, useRouter } from 'vue-router';
import KnowledgeApi from '@/api/lm/KnowledgeApi';
import ApiUtil from '@/utils/ApiUtil';
import DateUtil from '@/utils/DateUtil';
import TableUtil from '@/utils/TableUtil';
import { useUserStore } from '@/stores/user';
import ModelApi from '@/api/lm/ModelApi';

const route = useRoute()
const router = useRouter()
const user = useUserStore()
const tableRef = ref<TableInstance>()
const loading = ref(false)
const searchable = ref(true)
const columns = ref([
  { prop: 'id', label: 'ID' },
  { prop: 'name', label: '知识库名称' },
  { prop: 'embeddingInfo.name', label: '词嵌入模型' },
  { prop: 'rerankerInfo.name', label: '重排序模型' },
  { prop: 'topK', label: '召回数量' },
  { prop: 'score', label: '召回阈值' },
  { prop: 'splitTypeText', label: '拆分方式' },
  { prop: 'description', label: '描述', hide: true },
  { prop: 'sort', label: '排序' },
  { prop: 'statusText', label: '状态' },
])
const config = ref({
  ready: false,
  status: {},
  splitTypes: {},
})
const rows = ref([])
const filterRef = ref<FormInstance>()
const filters = ref(RouteUtil.query2filter(route, { advanced: false, agentIds: [] }))
const pagination = ref(RouteUtil.pagination(filters.value))
const selection = ref([])
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
    splitType: 'chunk',
    splitSeparator: '\\r\\n',
    splitSegmentTokens: 2000,
    splitChunkTokens: 500,
    splitOverlayTokens: 50,
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
    }).catch(() => {})
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
      :row-key="record => record.id"
      :border="true"
      v-loading="loading"
      table-layout="auto"
      @selection-change="newSelection => selection = newSelection"
    >
      <el-table-column type="selection" />
      <TableColumn :columns="columns">
        <template #role="scope">
          <el-space><el-tag v-for="item in scope.row.roles" :key="item.id">{{ item.name }}</el-tag></el-space>
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
  <el-drawer v-model="infoVisible" :title="'信息查看 - ' + form.id">
    <el-form :model="form" label-width="auto">
      <el-form-item label="知识库名称">{{ form.name }}</el-form-item>
      <el-form-item label="词嵌入模型">{{ form.embeddingInfo?.name }}</el-form-item>
      <el-form-item label="重排序模型">{{ form.rerankerInfo?.name }}</el-form-item>
      <el-form-item label="召回数量">{{ form.topK }}</el-form-item>
      <el-form-item label="召回阈值">{{ form.score }}</el-form-item>
      <el-form-item label="拆分方式">{{ form.splitTypeText }}</el-form-item>
      <el-form-item label="分段长度">{{ form.splitSeparator }}</el-form-item>
      <el-form-item label="段落分隔符">{{ form.splitSegmentTokens }}</el-form-item>
      <el-form-item label="分块长度">{{ form.splitChunkTokens }}</el-form-item>
      <el-form-item label="重叠长度">{{ form.splitOverlayTokens }}</el-form-item>
      <el-form-item label="排序">{{ form.sort }}</el-form-item>
      <el-form-item label="状态">{{ form.statusText }}</el-form-item>
      <el-form-item label="描述">{{ form.description ? form.description : '暂无' }}</el-form-item>
      <el-form-item label="创建者">{{ form.createdUserInfo?.name }}</el-form-item>
      <el-form-item label="创建时间">{{ DateUtil.format(form.createdTime) }}</el-form-item>
      <el-form-item label="修改者">{{ form.updatedUserInfo?.name }}</el-form-item>
      <el-form-item label="修改时间">{{ DateUtil.format(form.updatedTime) }}</el-form-item>
    </el-form>
  </el-drawer>
  <el-drawer v-model="formVisible" :close-on-click-modal="false" :show-close="false" :destroy-on-close="true">
    <template #header="{ close, titleId, titleClass }">
      <h4 :id="titleId" :class="titleClass">{{ '信息' + (form.id ? ('修改 - ' + form.id) : '添加') }}</h4>
      <el-space>
        <el-button type="primary" @click="handleSubmit" :loading="formLoading">确定</el-button>
        <el-button @click="close">取消</el-button>
      </el-space>
    </template>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="auto">
      <el-form-item label="智能体名称" prop="name">
        <el-input v-model="form.name" />
      </el-form-item>
      <el-form-item label="词嵌入模型" prop="embeddingId">
        <form-select v-model="form.embeddingId" :callback="ModelApi.list" :parameter="() => { return { type: 'embedding' } }" clearable placeholder="请输入模型名称" />
      </el-form-item>
      <el-form-item label="重排序模型" prop="rerankerId">
        <form-select v-model="form.rerankerId" :callback="ModelApi.list" :parameter="() => { return { type: 'reranker' } }" clearable placeholder="请输入模型名称" />
      </el-form-item>
      <el-form-item label="召回数量">
        <el-input-number v-model="form.topK" />
      </el-form-item>
      <el-form-item label="召回阈值">
        <el-input-number v-model="form.score" :precision="2" :step="0.1" placeholder="为0时不限制"  />
      </el-form-item>
      <el-form-item label="拆分方式" prop="splitType">
        <el-radio-group v-model="form.splitType">
          <el-radio-button v-for="(value, key) in config.splitTypes" :key="key" :value="key" :label="value" />
        </el-radio-group>
      </el-form-item>
      <el-form-item label="段落分隔符" prop="splitSeparator">
        <el-input v-model="form.splitSeparator" />
      </el-form-item>
      <el-form-item label="分段长度">
        <el-input-number v-model="form.splitSegmentTokens" />
      </el-form-item>
      <el-form-item label="分块长度">
        <el-input-number v-model="form.splitChunkTokens" />
      </el-form-item>
      <el-form-item label="重叠长度">
        <el-input-number v-model="form.splitOverlayTokens" />
      </el-form-item>
      <el-form-item label="排序">
        <el-input-number v-model="form.sort" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="form.status" placeholder="请选择">
          <el-option v-for="(value, key) in config.status" :key="key" :value="key" :label="value" />
        </el-select>
      </el-form-item>
      <el-form-item label="描述">
        <el-input type="textarea" v-model="form.description" />
      </el-form-item>
    </el-form>
  </el-drawer>
</template>

<style lang="scss" scoped>
</style>
