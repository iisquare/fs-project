<script setup lang="ts">
import { onMounted, ref } from 'vue';
import type { FormInstance, TableInstance } from 'element-plus';
import { ElMessage } from 'element-plus';
import RouteUtil from '@/utils/RouteUtil'
import { useRoute, useRouter } from 'vue-router';
import OntologyApi from '@/api/kg/OntologyApi';
import ApiUtil from '@/utils/ApiUtil';
import TableUtil from '@/utils/TableUtil';
import { useUserStore } from '@/stores/user';
import LayoutHeading from '@/components/Layout/LayoutHeading.vue';

const route = useRoute()
const router = useRouter()
const user = useUserStore()
const tableRef = ref<TableInstance>()
const loading = ref(false)
const searchable = ref(true)
const columns = ref([
  { prop: 'id', label: 'ID' },
  { prop: 'name', label: '本体名称' },
  { prop: 'entityCount', label: '实体数量' },
  { prop: 'relationshipCount', label: '关系数量' },
  { prop: 'version', label: '定义版本' },
  { prop: 'issueCount', label: '校验问题', slot: 'issue' },
  { prop: 'sort', label: '排序' },
  { prop: 'statusText', label: '状态' },
  { prop: 'description', label: '描述', hide: true },
])
const config = ref({
  ready: false,
  sorts: {},
  status: {},
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
  OntologyApi.list(filters.value).then((result: any) => {
    RouteUtil.result2pagination(pagination.value, result)
    rows.value = result.data.rows
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}
onMounted(() => {
  handleRefresh(false, true)
  OntologyApi.config().then((result: any) => {
    Object.assign(config.value, { ready: true }, ApiUtil.data(result))
  }).catch(() => {})
})
const handleAdd = (env: Event) => {
  RouteUtil.forward(route, router, env, {
      path: '/kg/modeling/er'
  })
}
const handleEdit = (scope: any, env: Event) => {
  RouteUtil.forward(route, router, env, {
      path: '/kg/modeling/er',
    query: {
      id: scope.row.id
    }
  })
}
const handleDelete = () => {
  TableUtil.selection(selection.value).then((ids: any) => {
    loading.value = true
    OntologyApi.delete(ids, { success: true }).then(() => {
      handleRefresh(false, true)
    }).catch(() => {
      loading.value = false
    })
  }).catch(() => {})
}

/* ---------------- 定义导出与导入 ---------------- */

const downloadJson = (data: any, filename: string) => {
  const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json;charset=utf-8' })
  const link = document.createElement('a')
  link.href = window.URL.createObjectURL(blob)
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  window.URL.revokeObjectURL(link.href)
}

const handleExport = () => {
  const record: any = selection.value?.[0]
  if (!record) return ElMessage.warning('请先勾选需要导出的本体')
  OntologyApi.model({ id: record.id }, { warning: false }).then((result: any) => {
    const model = ApiUtil.data(result) ?? {}
    downloadJson({
      name: model.name || record.name,
      description: record.description,
      content: { entities: model.entities ?? [], relationships: model.relationships ?? [] },
    }, `${model.name || record.name}_定义.json`)
    ElMessage.success('已导出本体定义')
  }).catch((result: any) => ElMessage.warning(ApiUtil.message(result)))
}

const importVisible = ref(false)
const importName = ref('')
const importContent = ref<any>(null)
const importLoading = ref(false)

const handleImportUpload = (file: any) => {
  const reader = new FileReader()
  reader.onload = () => {
    try {
      const data = JSON.parse(String(reader.result ?? '{}'))
      importContent.value = data.content ?? data
      importName.value = data.name ?? String(file.name ?? '').replace(/\.json$/i, '')
      if (!importContent.value?.entities && !importContent.value?.relationships) {
        return ElMessage.error('文件内容缺少 entities 或 relationships')
      }
      importVisible.value = true
    } catch (e) {
      ElMessage.error('文件不是合法的JSON')
    }
  }
  reader.readAsText(file.raw, 'utf-8')
}

const handleImportSubmit = () => {
  if (!importName.value) return ElMessage.warning('请填写本体名称')
  importLoading.value = true
  OntologyApi.save({
    name: importName.value,
    status: 1,
    content: importContent.value,
  }, { success: true }).then(() => {
    importVisible.value = false
    handleRefresh(false, true)
  }).catch((result: any) => {
    ElMessage.warning(ApiUtil.message(result))
  }).finally(() => {
    importLoading.value = false
  })
}
</script>

<template>
  <LayoutHeading title="本体管理" description="定义实体、关系与属性，作为图数据管理与检索的依据" />

  <el-card :bordered="false" shadow="never" class="fs-table-search" v-show="searchable">
    <form-search ref="filterRef" :model="filters">
      <form-search-item label="名称" prop="name">
        <el-input v-model="filters.name" clearable />
      </form-search-item>
      <form-search-item label="内容" prop="content">
        <el-input v-model="filters.content" clearable />
      </form-search-item>
      <form-search-item label="状态" prop="status">
        <el-select v-model="filters.status" placeholder="请选择" clearable>
          <el-option v-for="(value, key) in config.status" :key="key" :value="key" :label="value" />
        </el-select>
      </form-search-item>
      <form-search-item>
        <el-button type="primary" @click="handleRefresh(true, false)" :loading="loading">查询</el-button>
        <el-button @click="filterRef?.resetFields()">重置</el-button>
      </form-search-item>
    </form-search>
  </el-card>
  <el-card :bordered="false" shadow="never" class="fs-table-card">
    <div class="fs-table-toolbar flex-between">
      <el-space>
        <button-add v-permit="'kg:ontology:add'" @click="(e: Event) => handleAdd(e)" />
        <button-delete v-permit="'kg:ontology:delete'" :disabled="selection.length === 0" @click="handleDelete" />
        <el-button v-permit="'kg:ontology:'" :disabled="selection.length !== 1" @click="handleExport">导出定义</el-button>
        <el-upload v-permit="'kg:ontology:add'" :auto-upload="false" :show-file-list="false" accept=".json,application/json" :on-change="handleImportUpload">
          <el-button v-permit="'kg:ontology:add'">导入定义</el-button>
        </el-upload>
      </el-space>
      <el-space>
        <button-search @click="searchable = !searchable" />
        <button-refresh @click="handleRefresh(true, true)" :loading="loading" />
        <TableColumnSetting v-model="columns" :table="tableRef" />
        <TableSort v-model="filters.sort" :columns="columns" :sortable="config.sorts" @change="handleRefresh(true, true)" />
      </el-space>
    </div>
    <el-table
      ref="tableRef"
      :data="rows"
      :row-key="(record: any) => record.id"
      :border="true"
      v-loading="loading"
      table-layout="auto"
      @selection-change="(newSelection: any) => selection = newSelection"
    >
      <el-table-column type="selection" />
      <TableColumn :columns="columns">
        <template #role="scope">
          <el-space><el-tag v-for="item in scope.row.roles" :key="item.id">{{ item.name }}</el-tag></el-space>
        </template>
        <template #issue="scope">
          <el-tag v-if="(scope.row.issueCount ?? 0) > 0" type="warning" effect="plain">{{ scope.row.issueCount }} 项</el-tag>
          <el-tag v-else type="success" effect="plain">正常</el-tag>
        </template>
      </TableColumn>
      <el-table-column label="操作">
        <template #default="scope">
          <el-button link @click="(e: any) => handleEdit(scope, e)" v-permit="'kg:ontology:modify'">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>
    <TablePagination v-model="pagination" @change="handleRefresh(true, true)" />
  </el-card>

  <el-dialog v-model="importVisible" title="导入本体定义" width="520px">
    <el-alert type="info" :closable="false" show-icon title="导入会创建一个新的本体，不会覆盖现有定义" class="mb-10" />
    <el-form label-position="top">
      <el-form-item label="本体名称" required>
        <el-input v-model="importName" placeholder="请输入新本体名称" />
      </el-form-item>
      <el-form-item label="定义内容">
        <span>实体 {{ (importContent?.entities ?? []).length }} 个，关系 {{ (importContent?.relationships ?? []).length }} 个</span>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-space>
        <el-button type="primary" :loading="importLoading" @click="handleImportSubmit">导入</el-button>
        <el-button @click="importVisible = false">取消</el-button>
      </el-space>
    </template>
  </el-dialog>
</template>

<style lang="scss" scoped>
.mb-10 { margin-bottom: 10px; }
</style>
