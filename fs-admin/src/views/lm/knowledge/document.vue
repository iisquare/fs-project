<script setup lang="ts">
import { onMounted, ref } from 'vue';
import * as ElementPlusIcons from '@element-plus/icons-vue';
import type { FormInstance, TableInstance, UploadRawFile } from 'element-plus';
import RouteUtil from '@/utils/RouteUtil'
import { useRoute, useRouter } from 'vue-router';
import KnowledgeDocumentApi from '@/api/lm/KnowledgeDocumentApi';
import ApiUtil from '@/utils/ApiUtil';
import DateUtil from '@/utils/DateUtil';
import TableUtil from '@/utils/TableUtil';
import KnowledgeApi from '@/api/lm/KnowledgeApi';

const route = useRoute()
const router = useRouter()
const tableRef = ref<TableInstance>()
const loading = ref(false)
const searchable = ref(true)
const knowledgeInfo: any = ref(null)
const knowledgeLoading = ref(true)
const columns = ref([
  { prop: 'id', label: 'ID' },
  { prop: 'name', label: '文档名称', slot: 'name' },
  { prop: 'tokenSize', label: '字符数量' },
  { prop: 'fileId', label: '文件标识' },
  { prop: 'filepath', label: '存储路径', hide: true },
  { prop: 'statusText', label: '状态' },
])
const config = ref({
  ready: false,
  status: {},
})
const rows = ref([])
const filterRef = ref<FormInstance>()
const filters = ref(RouteUtil.query2filter(route, { advanced: false, knowledgeId: route.query.knowledgeId }))
const pagination = ref(RouteUtil.pagination(filters.value))
const selection: any = ref([])
const handleRefresh = (filter2query: boolean, keepPage: boolean) => {
  tableRef.value?.clearSelection()
  Object.assign(filters.value, RouteUtil.pagination2filter(pagination.value, keepPage))
  filter2query && RouteUtil.filter2query(route, router, filters.value)
  loading.value = true
  KnowledgeDocumentApi.list(filters.value).then((result: any) => {
    RouteUtil.result2pagination(pagination.value, result)
    rows.value = result.data.rows
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}
onMounted(() => {
  handleRefresh(false, true)
  KnowledgeDocumentApi.config().then((result: any) => {
    Object.assign(config.value, { ready: true }, ApiUtil.data(result))
  }).catch(() => {})
  const knowledgeId = route.query.knowledgeId
  if (knowledgeId) {
    knowledgeLoading.value = true
    KnowledgeApi.info(knowledgeId).then((result: any) => {
      knowledgeInfo.value = ApiUtil.data(result)
    }).catch(() => {}).finally(() => {
      knowledgeLoading.value = false
    })
  } else {
    knowledgeLoading.value = false
  }
})
const infoVisible = ref(false)
const formVisible = ref(false)
const formLoading = ref(false)
const form: any = ref({})


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
  KnowledgeDocumentApi.save(form.value, { success: true }).then(result => {
    handleRefresh(false, true)
    formVisible.value = false
  }).catch(() => {}).finally(() => {
    formLoading.value = false
  })
}
const handleDelete = () => {
  TableUtil.selection(selection.value).then((ids: any) => {
    loading.value = true
    KnowledgeDocumentApi.delete(ids, { success: true }).then(() => {
      handleRefresh(false, true)
    }).catch(() => {
      loading.value = false
    })
  }).catch(() => {})
}
const handleBeforeUpload = async (rawFile: UploadRawFile) => {
  if (formLoading.value) return
  formLoading.value = true
  const params = {
    knowledgeId: route.query.knowledgeId,
    file: rawFile,
  }
  KnowledgeDocumentApi.upload(params, { success: true }).then(result => {
    handleRefresh(false, true)
    formVisible.value = false
  }).catch(() => {}).finally(() => {
    formLoading.value = false
  })
  return false
}
const baseURL = import.meta.env.VITE_APP_API_URL

const handleDownload = (record: any) => {
  window.open(baseURL + '/lm/knowledgeDocument/download?id=' + encodeURIComponent(record.id))
}
</script>

<template>
  <el-card :bordered="false" shadow="never" class="fs-table-search" v-show="searchable">
    <el-space>
      <LayoutBack to="/lm/knowledge/list" />
      <el-skeleton v-if="knowledgeLoading" :rows="2" animated />
      <el-space v-else>
        <h3>{{ knowledgeInfo?.name }}</h3>
        <el-tag v-for="item in knowledgeInfo?.labels" :key="item">{{ item }}</el-tag>
      </el-space>
    </el-space>
    <el-divider />
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
        <el-upload
          v-permit="'lm:knowledge:add'"
          :multiple="true"
          :show-file-list="false"
          :disabled="formLoading"
          :before-upload="handleBeforeUpload"
        >
          <template #trigger>
            <el-button type="success" :icon="ElementPlusIcons.UploadFilled" :loading="formLoading">上传文档</el-button>
          </template>
        </el-upload>
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
          <el-link type="primary" underline="never" @click="router.push({ path: '/lm/knowledge/segment', query: { documentId: scope.row.id, knowledgeId: route.query.knowledgeId } })">{{ scope.row.name }}</el-link>
        </template>
        <template #role="scope">
          <el-space><el-tag v-for="item in scope.row.roles" :key="item.id">{{ item.name }}</el-tag></el-space>
        </template>
      </TableColumn>
      <el-table-column label="操作">
        <template #default="scope">
          <el-button link @click="handleShow(scope)" v-permit="'lm:knowledge:'">查看</el-button>
          <el-button link @click="handleEdit(scope)" v-permit="'lm:knowledge:modify'">编辑</el-button>
          <el-button link @click="handleDownload(scope.row)" v-permit="'lm:knowledge:'" v-if="scope.row.fileId">下载</el-button>
        </template>
      </el-table-column>
    </el-table>
    <TablePagination v-model="pagination" @change="handleRefresh(true, true)" />
  </el-card>
  <el-drawer v-model="infoVisible" :title="'信息查看 - ' + form.id" size="60%">
    <el-descriptions border :column="2" label-width="80px">
      <el-descriptions-item label="文档名称" :span="2">{{ form.name }}</el-descriptions-item>
      <el-descriptions-item label="字符数量">{{ form.tokenSize }}</el-descriptions-item>
      <el-descriptions-item label="状态">{{ form.statusText }}</el-descriptions-item>
      <el-descriptions-item label="知识库">{{ form.knowledgeInfo?.name }}</el-descriptions-item>
      <el-descriptions-item label="文件标识">{{ form.fileId }}</el-descriptions-item>
      <el-descriptions-item label="存储路径" :span="2">{{ form.filepath }}</el-descriptions-item>
      <el-descriptions-item label="创建者">{{ form.createdUserInfo?.name }}</el-descriptions-item>
       <el-descriptions-item label="创建时间">{{ DateUtil.format(form.createdTime) }}</el-descriptions-item>
      <el-descriptions-item label="修改者">{{ form.updatedUserInfo?.name }}</el-descriptions-item>
      <el-descriptions-item label="修改时间">{{ DateUtil.format(form.updatedTime) }}</el-descriptions-item>
    </el-descriptions>
    <el-divider content-position="left">元数据</el-divider>
    <metadata-table v-model="form.metadata" />
  </el-drawer>
  <el-drawer v-model="formVisible" :close-on-click-modal="false" :show-close="false" :destroy-on-close="true" size="800px">
    <template #header="{ close, titleId, titleClass }">
      <h4 :id="titleId" :class="titleClass">{{ '信息' + (form.id ? ('修改 - ' + form.id) : '添加') }}</h4>
      <el-space>
        <el-button type="primary" @click="handleSubmit" :loading="formLoading">确定</el-button>
        <el-button @click="close">取消</el-button>
      </el-space>
    </template>
    <el-descriptions border :column="2" label-width="80px">
      <el-descriptions-item label="文档名称" :span="2"><el-input v-model="form.name" /></el-descriptions-item>
      <el-descriptions-item label="知识库">{{ form.knowledgeInfo?.name }}</el-descriptions-item>
      <el-descriptions-item label="状态">
        <el-select v-model="form.status" placeholder="请选择">
          <el-option v-for="(value, key) in config.status" :key="key" :value="key" :label="value" />
        </el-select>
      </el-descriptions-item>
      <el-descriptions-item label="文件标识"><el-input v-model="form.fileId" /></el-descriptions-item>
      <el-descriptions-item label="字符数量"><el-input-number v-model="form.tokenSize" /></el-descriptions-item>
      <el-descriptions-item label="存储路径" :span="2"><el-input v-model="form.filepath" /></el-descriptions-item>
    </el-descriptions>
    <el-divider content-position="left">元数据</el-divider>
    <metadata-table v-model="form.metadata" :editable="true" />
  </el-drawer>
</template>

<style lang="scss" scoped>
</style>
