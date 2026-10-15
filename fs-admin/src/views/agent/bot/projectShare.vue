<script setup lang="ts">
import { onMounted, ref } from 'vue'
import type { FormInstance, TableInstance } from 'element-plus'
import RouteUtil from '@/utils/RouteUtil'
import { useRoute, useRouter } from 'vue-router'
import ProjectShareApi from '@/api/agent/ProjectShareApi'
import ApiUtil from '@/utils/ApiUtil'
import DateUtil from '@/utils/DateUtil'
import TableUtil from '@/utils/TableUtil'
import RoleApi from '@/api/member/RoleApi'

/**
 * 分享的项目：客户端（桌面端）从项目页发布上来的项目，一个项目下挂着若干版本记录。
 *
 * 项目由客户端创建（分享人即创建人），管理端负责查看与治理：名称 / 描述 / 图标、排序、
 * 授权角色（留空为所有登录用户可见）与启用状态；点项目名称或「版本记录」进版本页看每次发布的配置快照。
 */

const route = useRoute()
const router = useRouter()
const tableRef = ref<TableInstance>()
const loading = ref(false)
const searchable = ref(true)
const columns = ref([
  { prop: 'id', label: 'ID' },
  { prop: 'name', label: '项目名称', slot: 'name' },
  { prop: 'createdUserInfo', label: '分享人', slot: 'createdUser' },
  { prop: 'versionCount', label: '版本数', slot: 'versionCount' },
  { prop: 'latestVersion', label: '最新版本', slot: 'latestVersion' },
  { prop: 'roles', label: '授权角色', slot: 'role' },
  { prop: 'sort', label: '排序' },
  { prop: 'statusText', label: '状态' },
  { prop: 'description', label: '描述', hide: true, slot: 'description' },
  { prop: 'createdTime', label: '分享时间', slot: 'createdTime' },
])
const config = ref({
  ready: false,
  sorts: {},
  status: {},
})
const rows = ref([])
const filterRef = ref<FormInstance>()
const filters = ref(RouteUtil.query2filter(route, { advanced: false }))
const pagination = ref(RouteUtil.pagination(filters.value))
const selection: any = ref([])
const handleRefresh = (filter2query: boolean, keepPage: boolean) => {
  tableRef.value?.clearSelection()
  Object.assign(filters.value, RouteUtil.pagination2filter(pagination.value, keepPage))
  filter2query && RouteUtil.filter2query(route, router, filters.value)
  loading.value = true
  ProjectShareApi.list(filters.value).then((result: any) => {
    RouteUtil.result2pagination(pagination.value, result)
    rows.value = result.data.rows
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}
onMounted(() => {
  handleRefresh(false, true)
  ProjectShareApi.config().then((result: any) => {
    Object.assign(config.value, { ready: true }, ApiUtil.data(result))
  }).catch(() => {})
})
const infoVisible = ref(false)
const formVisible = ref(false)
const formLoading = ref(false)
const form: any = ref({})
const formRef: any = ref<FormInstance>()
const rules = ref({
  name: [{ required: true, message: '请输入项目名称', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
})
const handleShow = (scope: any) => {
  // 先用行数据把抽屉打开（列表已带版本数、最新版本），再取详情把角色、用户信息补齐到最新
  form.value = Object.assign({}, scope.row)
  infoVisible.value = true
  ProjectShareApi.info(scope.row.id).then((result: any) => {
    form.value = Object.assign({}, scope.row, ApiUtil.data(result))
  }).catch(() => {})
}
const handleEdit = (scope: any) => {
  form.value = Object.assign({}, scope.row, {
    status: scope.row.status + '',
    roleIds: scope.row.roleIds || [],
  })
  formVisible.value = true
}
const handleSubmit = () => {
  formRef.value?.validate((valid: boolean) => {
    if (!valid || formLoading.value) return
    formLoading.value = true
    ProjectShareApi.save(form.value, { success: true }).then(() => {
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
    ProjectShareApi.delete(ids, { success: true }).then(() => {
      handleRefresh(false, true)
    }).catch(() => {
      loading.value = false
    })
  }).catch(() => {})
}
const handleVersion = (scope: any) => {
  router.push({ path: '/agent/bot/projectVersion', query: { projectId: scope.row.id } })
}
</script>

<template>
  <el-card :bordered="false" shadow="never" class="fs-table-search" v-show="searchable">
    <form-search ref="filterRef" :model="filters">
      <form-search-item label="项目名称" prop="name">
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
        <template #name="scope">
          <el-link type="primary" underline="never" @click="handleVersion(scope)">{{ scope.row.name }}</el-link>
        </template>
        <template #createdUser="scope">
          {{ scope.row.createdUserInfo?.name ? scope.row.createdUserInfo.name : '-' }}
        </template>
        <template #versionCount="scope">
          <el-link type="primary" underline="never" @click="handleVersion(scope)">{{ scope.row.versionCount }}</el-link>
        </template>
        <template #latestVersion="scope">
          <template v-if="scope.row.latestVersion">
            <el-link type="primary" underline="never" @click="handleVersion(scope)">{{ scope.row.latestVersion.name }}</el-link>
            <span class="latest-tip">{{ DateUtil.format(scope.row.latestVersion.createdTime) }}</span>
          </template>
          <span v-else>暂无</span>
        </template>
        <template #role="scope">
          <el-space v-if="scope.row.roles && scope.row.roles.length">
            <el-tag v-for="item in scope.row.roles" :key="item.id">{{ item.name }}</el-tag>
          </el-space>
          <span v-else>全部用户</span>
        </template>
        <template #description="scope">
          {{ scope.row.description ? scope.row.description : '暂无' }}
        </template>
        <template #createdTime="scope">
          {{ DateUtil.format(scope.row.createdTime) }}
        </template>
      </TableColumn>
      <el-table-column label="操作">
        <template #default="scope">
          <el-button link @click="handleShow(scope)" v-permit="'agent:projectShare:'">查看</el-button>
          <el-button link @click="handleEdit(scope)" v-permit="'agent:projectShare:modify'">编辑</el-button>
          <el-button link @click="handleVersion(scope)" v-permit="'agent:projectShare:'">版本记录</el-button>
        </template>
      </el-table-column>
    </el-table>
    <TablePagination v-model="pagination" :loading="loading" @change="handleRefresh(true, true)" />
  </el-card>
  <el-drawer v-model="infoVisible" :title="'信息查看 - ' + form.id" size="60%">
    <el-descriptions :column="2" label-width="100px" border>
      <el-descriptions-item label="项目名称">{{ form.name }}</el-descriptions-item>
      <el-descriptions-item label="图标">
        <el-avatar :size="32" :src="form.icon">{{ form.name ? form.name.charAt(0) : '' }}</el-avatar>
        <span class="icon-tip">{{ form.icon ? form.icon : '未设置' }}</span>
      </el-descriptions-item>
      <el-descriptions-item label="状态">{{ form.statusText }}</el-descriptions-item>
      <el-descriptions-item label="排序">{{ form.sort }}</el-descriptions-item>
      <el-descriptions-item label="授权角色" :span="2">
        <el-space v-if="form.roles && form.roles.length">
          <el-tag v-for="item in form.roles" :key="item.id">{{ item.name }}</el-tag>
        </el-space>
        <span v-else>全部用户</span>
      </el-descriptions-item>
      <el-descriptions-item label="分享人">{{ form.createdUserInfo?.name }}</el-descriptions-item>
      <el-descriptions-item label="分享时间">{{ DateUtil.format(form.createdTime) }}</el-descriptions-item>
      <el-descriptions-item label="版本数">{{ form.versionCount }}</el-descriptions-item>
      <el-descriptions-item label="最新版本">
        <span v-if="form.latestVersion">{{ form.latestVersion.name }}（{{ DateUtil.format(form.latestVersion.createdTime) }}）</span>
        <span v-else>暂无版本</span>
      </el-descriptions-item>
      <el-descriptions-item label="更新时间">{{ DateUtil.format(form.updatedTime) }}</el-descriptions-item>
      <el-descriptions-item label="修改人">{{ form.updatedUserInfo?.name ? form.updatedUserInfo.name : '-' }}</el-descriptions-item>
      <el-descriptions-item label="描述" :span="2" class-name="multiline-text">{{ form.description ? form.description : '暂无' }}</el-descriptions-item>
    </el-descriptions>
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
        <el-descriptions-item label="项目名称"><el-input v-model="form.name" /></el-descriptions-item>
        <el-descriptions-item label="分享人">{{ form.createdUserInfo?.name }}</el-descriptions-item>
        <el-descriptions-item label="图标" :span="2"><el-input v-model="form.icon" placeholder="图标地址，客户端发布时同步" /></el-descriptions-item>
        <el-descriptions-item label="授权角色" :span="2">
          <form-select v-model="form.roleIds" :callback="RoleApi.list" multiple clearable />
        </el-descriptions-item>
        <el-descriptions-item label="排序"><el-input-number v-model="form.sort" /></el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-select v-model="form.status" placeholder="请选择">
            <el-option v-for="(value, key) in config.status" :key="key" :value="key" :label="value" />
          </el-select>
        </el-descriptions-item>
        <el-descriptions-item label="描述" :span="2"><el-input type="textarea" v-model="form.description" :rows="12" /></el-descriptions-item>
      </el-descriptions>
    </el-form>
  </el-drawer>
</template>

<style lang="scss" scoped>
.icon-tip {
  margin-left: 8px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.latest-tip {
  margin-left: 8px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
</style>
