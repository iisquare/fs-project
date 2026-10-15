<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, TableInstance } from 'element-plus'
import RouteUtil from '@/utils/RouteUtil'
import { useRoute, useRouter } from 'vue-router'
import ProjectShareApi from '@/api/agent/ProjectShareApi'
import ProjectVersionApi from '@/api/agent/ProjectVersionApi'
import ApiUtil from '@/utils/ApiUtil'
import DateUtil from '@/utils/DateUtil'
import TableUtil from '@/utils/TableUtil'

/**
 * 分享项目的版本记录：一条 = 客户端的一次发布，存着当时的项目配置快照。
 *
 * 从「分享的项目」页带 projectId 进来时只列该项目的版本，列表上方给出项目信息与返回入口；
 * 不带 projectId 时是全部项目的版本总表（所属项目列即入口）。
 * 配置原文只读：管理端可以改版本号 / 版本说明 / 启用状态，也可以按与客户端一致的格式导出配置文件。
 */

const route = useRoute()
const router = useRouter()
const tableRef = ref<TableInstance>()
const loading = ref(false)
const searchable = ref(true)
const projectInfo: any = ref(null)
const projectLoading = ref(true)
const columns = ref([
  { prop: 'id', label: 'ID' },
  { prop: 'name', label: '版本号' },
  { prop: 'projectInfo', label: '所属项目', slot: 'project', hide: Boolean(route.query.projectId) },
  { prop: 'statusText', label: '状态' },
  { prop: 'contentSize', label: '配置大小', slot: 'contentSize' },
  { prop: 'description', label: '版本说明', slot: 'description' },
  { prop: 'createdUserInfo', label: '发布人', slot: 'createdUser' },
  { prop: 'createdTime', label: '发布时间', slot: 'createdTime' },
])
const config = ref({
  ready: false,
  sorts: {},
  status: {},
})
const rows = ref([])
const filterRef = ref<FormInstance>()
const filters = ref(RouteUtil.query2filter(route, { advanced: false, projectId: route.query.projectId }))
const pagination = ref(RouteUtil.pagination(filters.value))
const selection: any = ref([])
const handleRefresh = (filter2query: boolean, keepPage: boolean) => {
  tableRef.value?.clearSelection()
  Object.assign(filters.value, RouteUtil.pagination2filter(pagination.value, keepPage))
  filter2query && RouteUtil.filter2query(route, router, filters.value)
  loading.value = true
  ProjectVersionApi.list(filters.value).then((result: any) => {
    RouteUtil.result2pagination(pagination.value, result)
    rows.value = result.data.rows
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}
onMounted(() => {
  handleRefresh(false, true)
  ProjectVersionApi.config().then((result: any) => {
    Object.assign(config.value, { ready: true }, ApiUtil.data(result))
  }).catch(() => {})
  const projectId = route.query.projectId
  if (projectId) {
    projectLoading.value = true
    ProjectShareApi.info(projectId).then((result: any) => {
      projectInfo.value = ApiUtil.data(result)
    }).catch(() => {}).finally(() => {
      projectLoading.value = false
    })
  } else {
    projectLoading.value = false
  }
})
const infoVisible = ref(false)
const formVisible = ref(false)
const formLoading = ref(false)
const form: any = ref({})
const content: any = ref('')
const formRef: any = ref<FormInstance>()
const rules = ref({
  name: [{ required: true, message: '请输入版本号', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
})
/** 查看：版本详情带配置原文，列表行里没有（见服务端 strip） */
const handleShow = (scope: any) => {
  ProjectVersionApi.info(scope.row.id).then((result: any) => {
    const data = ApiUtil.data(result)
    const text = formatContent(data.content)
    // contentSize 由服务端按原文的 UTF-8 字节数给出，与列表里的「配置大小」同一口径
    form.value = Object.assign({}, scope.row, data)
    content.value = text
    infoVisible.value = true
  }).catch(() => {})
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
    ProjectVersionApi.save(form.value, { success: true }).then(() => {
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
    ProjectVersionApi.delete(ids, { success: true }).then(() => {
      handleRefresh(false, true)
    }).catch(() => {
      loading.value = false
    })
  }).catch(() => {})
}
const handleProject = (scope: any) => {
  router.push({ path: '/agent/bot/projectVersion', query: { projectId: scope.row.projectId } })
}
const formatContent = (value: any) => {
  if (!value) return '{}'
  try {
    return JSON.stringify(JSON.parse(value), null, 2)
  } catch (e) {
    return value
  }
}
const formatSize = (bytes: number) => {
  if (!bytes) return '-'
  if (bytes < 1024) return bytes + ' B'
  return (bytes / 1024).toFixed(1) + ' KB'
}
/**
 * 导出配置：与客户端「导出项目配置」同一份文件格式（type / version / project），
 * 客户端可直接导入——把服务端上的版本配置取回本地。
 */
const handleDownload = () => {
  let project: any
  try {
    project = JSON.parse(content.value || '{}')
  } catch (e) {
    ElMessage.warning('配置原文不是合法的 JSON，无法导出')
    return
  }
  const data = {
    type: 'fs-bot-project',
    version: 1,
    exportedAt: new Date().toISOString(),
    share: {
      projectId: form.value.projectId,
      projectName: form.value.projectInfo?.name || projectInfo.value?.name || '',
      versionId: form.value.id,
      versionName: form.value.name,
      description: form.value.description || '',
    },
    project,
  }
  const name = data.share.projectName || 'project'
  const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `${name}-${form.value.name}.json`
  link.click()
  URL.revokeObjectURL(url)
}
</script>

<template>
  <el-card :bordered="false" shadow="never" class="fs-table-search" v-show="searchable">
    <el-space>
      <LayoutBack to="/agent/bot/projectShare" />
      <el-skeleton v-if="projectLoading" :rows="2" animated />
      <el-space v-else-if="projectInfo">
        <h3>{{ projectInfo.name }}</h3>
        <el-tag>{{ projectInfo.statusText }}</el-tag>
        <span class="version-tip">分享人：{{ projectInfo.createdUserInfo?.name ? projectInfo.createdUserInfo.name : '-' }}</span>
      </el-space>
    </el-space>
    <el-divider />
    <form-search ref="filterRef" :model="filters">
      <form-search-item label="版本号" prop="name">
        <el-input v-model="filters.name" clearable />
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
        <button-delete v-permit="'agent:projectShare:delete'" :disabled="selection.length === 0" @click="handleDelete" />
      </el-space>
      <el-space>
        <button-search @click="searchable = !searchable" />
        <button-refresh @click="handleRefresh(true, true)" :loading="loading" />
        <TableColumnSetting v-model="columns" :table="tableRef" :loading="loading" />
        <TableSort v-model="filters.sort" :columns="columns" :sortable="config.sorts" :loading="loading" @change="handleRefresh(true, true)" />
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
        <template #project="scope">
          <el-link type="primary" underline="never" @click="handleProject(scope)">
            {{ scope.row.projectInfo?.name ? scope.row.projectInfo.name : ('#' + scope.row.projectId) }}
          </el-link>
        </template>
        <template #contentSize="scope">
          {{ formatSize(scope.row.contentSize) }}
        </template>
        <template #description="scope">
          {{ scope.row.description ? scope.row.description : '暂无' }}
        </template>
        <template #createdUser="scope">
          {{ scope.row.createdUserInfo?.name ? scope.row.createdUserInfo.name : '-' }}
        </template>
        <template #createdTime="scope">
          {{ DateUtil.format(scope.row.createdTime) }}
        </template>
      </TableColumn>
      <el-table-column label="操作" width="120">
        <template #default="scope">
          <el-button link @click="handleShow(scope)" v-permit="'agent:projectShare:'">查看</el-button>
          <el-button link @click="handleEdit(scope)" v-permit="'agent:projectShare:modify'">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>
    <TablePagination v-model="pagination" :loading="loading" @change="handleRefresh(true, true)" />
  </el-card>
  <el-drawer v-model="infoVisible" :title="'信息查看 - ' + form.id" size="70%">
    <el-descriptions :column="2" label-width="100px" border>
      <el-descriptions-item label="版本号">{{ form.name }}</el-descriptions-item>
      <el-descriptions-item label="所属项目">{{ form.projectInfo?.name ? form.projectInfo.name : ('#' + form.projectId) }}</el-descriptions-item>
      <el-descriptions-item label="状态">{{ form.statusText }}</el-descriptions-item>
      <el-descriptions-item label="配置大小">{{ formatSize(form.contentSize) }}</el-descriptions-item>
      <el-descriptions-item label="发布人">{{ form.createdUserInfo?.name }}</el-descriptions-item>
      <el-descriptions-item label="发布时间">{{ DateUtil.format(form.createdTime) }}</el-descriptions-item>
      <el-descriptions-item label="版本说明" :span="2" class-name="multiline-text">{{ form.description ? form.description : '暂无' }}</el-descriptions-item>
      <el-descriptions-item label="项目配置" :span="2">
        <el-button size="small" @click="handleDownload">导出配置</el-button>
        <span class="version-tip">导出文件与客户端「导出项目配置」同一格式，可在客户端导入</span>
      </el-descriptions-item>
    </el-descriptions>
    <CodeEditor v-model="content" :height="420" mode="javascript" :line-height="1.7" resizable class="content-editor" />
  </el-drawer>
  <el-drawer v-model="formVisible" :close-on-click-modal="false" :show-close="false" :destroy-on-close="true" size="60%">
    <template #header="{ close, titleId, titleClass }">
      <h4 :id="titleId" :class="titleClass">{{ '信息修改 - ' + form.id }}</h4>
      <el-space>
        <el-button type="primary" @click="handleSubmit" :loading="formLoading">确定</el-button>
        <el-button @click="close">取消</el-button>
      </el-space>
    </template>
    <el-form ref="formRef" :model="form" :rules="rules">
      <el-descriptions :column="2" label-width="100px" border>
        <el-descriptions-item label="版本号"><el-input v-model="form.name" /></el-descriptions-item>
        <el-descriptions-item label="所属项目">{{ form.projectInfo?.name ? form.projectInfo.name : ('#' + form.projectId) }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-select v-model="form.status" placeholder="请选择">
            <el-option v-for="(value, key) in config.status" :key="key" :value="key" :label="value" />
          </el-select>
        </el-descriptions-item>
        <el-descriptions-item label="配置大小">{{ formatSize(form.contentSize) }}</el-descriptions-item>
        <el-descriptions-item label="版本说明" :span="2"><el-input type="textarea" v-model="form.description" :rows="12" /></el-descriptions-item>
      </el-descriptions>
    </el-form>
  </el-drawer>
</template>

<style lang="scss" scoped>
.version-tip {
  margin-left: 8px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
/* 配置原文与上方表格之间留出间距，避免贴着字段表格读 */
.content-editor {
  margin-top: 12px;
}
</style>
