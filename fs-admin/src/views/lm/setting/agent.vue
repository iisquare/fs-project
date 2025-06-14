<script setup lang="ts">
import { onMounted, ref } from 'vue';
import type { FormInstance, TableInstance } from 'element-plus';
import RouteUtil from '@/utils/RouteUtil'
import { useRoute, useRouter } from 'vue-router';
import AgentApi from '@/api/lm/AgentApi';
import ApiUtil from '@/utils/ApiUtil';
import DateUtil from '@/utils/DateUtil';
import TableUtil from '@/utils/TableUtil';
import ServerApi from '@/api/lm/ServerApi';
import { useUserStore } from '@/stores/user';
import ClientApi from '@/api/lm/ClientApi';
import RoleApi from '@/api/member/RoleApi';

const route = useRoute()
const router = useRouter()
const user = useUserStore()
const tableRef = ref<TableInstance>()
const loading = ref(false)
const searchable = ref(true)
const columns = ref([
  { prop: 'id', label: 'ID' },
  { prop: 'name', label: '智能体名称' },
  { prop: 'model', label: '模型名称' },
  { prop: 'maxTokens', label: '最大令牌' },
  { prop: 'temperature', label: '多样性' },
  { prop: 'role', label: '授权角色', slot: 'role' },
  { prop: 'sort', label: '排序' },
  { prop: 'statusText', label: '状态' },
])
const config = ref({
  ready: false,
  status: {},
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
  AgentApi.list(filters.value).then((result: any) => {
    RouteUtil.result2pagination(pagination.value, result)
    rows.value = result.data.rows
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}
onMounted(() => {
  handleRefresh(false, true)
  AgentApi.config().then((result: any) => {
    Object.assign(config.value, { ready: true }, ApiUtil.data(result))
  })
})
const infoVisible = ref(false)
const formVisible = ref(false)
const formLoading = ref(false)
const form: any = ref({})
const formRef: any = ref<FormInstance>()
const rules = ref({
  name: [{ required: true, message: '请输入智能体名称', trigger: 'blur' }],
  model: [{ required: true, message: '请输入模型名称', trigger: 'blur' }],
  token: [{ required: true, message: '请输认证标识', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
})
const handleAdd = () => {
  form.value = {
    maxTokens: 0,
    temperature: 0,
    status: '1',
    parameter: '{}',
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
    AgentApi.save(form.value, { success: true }).then(result => {
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
    AgentApi.delete(ids, { success: true }).then(() => {
      handleRefresh(false, true)
    }).catch(() => {})
  }).catch(() => {})
}
const handleCompress = () => {
  form.value.parameter = JSON.stringify(JSON.parse(form.value.parameter))
}
const handleFormat = () => {
  form.value.parameter = JSON.stringify(JSON.parse(form.value.parameter), null, 4)
}
</script>

<template>
  <el-card :bordered="false" shadow="never" class="fs-table-search" v-show="searchable">
    <form-search ref="filterRef" :model="filters">
      <form-search-item label="名称" prop="name">
        <el-input v-model="filters.name" clearable />
      </form-search-item>
      <form-search-item label="模型" prop="model">
        <el-input v-model="filters.model" clearable />
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
        <button-add v-permit="'lm:agent:add'" @click="handleAdd" />
        <button-delete v-permit="'lm:agent:delete'" :disabled="selection.length === 0" @click="handleDelete" />
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
          <el-button link @click="handleShow(scope)" v-permit="'lm:agent:'">查看</el-button>
          <el-button link @click="handleEdit(scope)" v-permit="'lm:agent:modify'">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>
    <TablePagination v-model="pagination" @change="handleRefresh(true, true)" />
  </el-card>
  <el-drawer v-model="infoVisible" :title="'信息查看 - ' + form.id">
    <el-form :model="form" label-width="auto">
      <el-form-item label="智能体名称">{{ form.name }}</el-form-item>
      <el-form-item label="模型名称">{{ form.model }}</el-form-item>
      <el-form-item label="认证标识">{{ form.token }}</el-form-item>
      <el-form-item label="系统提示词">{{ form.systemPrompt }}</el-form-item>
      <el-form-item label="最大输出令牌">{{ form.maxTokens }}</el-form-item>
      <el-form-item label="输出多样性">{{ form.temperature }}</el-form-item>
      <el-form-item label="自定义参数">
        <el-input type="textarea" v-model="form.parameter" :rows="5" disabled />
      </el-form-item>
      <el-form-item label="授权角色">
        <el-space><el-tag v-for="item in form.roles" :key="item.id">{{ item.name }}</el-tag></el-space>
      </el-form-item>
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
      <el-form-item label="模型名称" prop="model">
        <form-autocomplete v-model="form.model" :callback="ServerApi.list" fieldLabel="model" clearable placeholder="请输入模型名称" v-if="user.hasPermit('lm:server:')" />
        <el-input v-model="form.model" clearable placeholder="请输入模型名称" v-else />
      </el-form-item>
      <el-form-item label="认证标识">
        <form-autocomplete v-model="form.token" :callback="ClientApi.list" fieldLabel="token" clearable placeholder="请输入认证标识" v-if="user.hasPermit('lm:client:')" />
        <el-input v-model="form.token" clearable placeholder="请输入认证标识" v-else />
      </el-form-item>
      <el-form-item label="系统提示词">
        <el-input type="textarea" v-model="form.systemPrompt" placeholder="留空为不增加系统提示词" />
      </el-form-item>
      <el-form-item label="生成最大令牌数">
        <el-input-number v-model="form.maxTokens" placeholder="为0时采用系统默认配置"  />
      </el-form-item>
      <el-form-item label="生成多样性">
        <el-input-number v-model="form.temperature" :precision="2" :step="0.1" placeholder="为0时采用系统默认配置"  />
      </el-form-item>
      <el-form-item label="自定义参数">
        <el-input type="textarea" v-model="form.parameter" :rows="5" />
        <el-space class="mt-20">
          <el-button @click="handleCompress">压缩</el-button>
          <el-button @click="handleFormat">格式化</el-button>
        </el-space>
      </el-form-item>
      <el-form-item label="授权角色">
        <form-select v-model="form.roleIds" :callback="RoleApi.list" multiple clearable />
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
