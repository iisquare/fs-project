<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { FormInstance, TableInstance } from 'element-plus'
import { ElMessage } from 'element-plus'
import { VideoPlay } from '@element-plus/icons-vue'
import QualityPlanApi from '@/api/govern/QualityPlanApi'
import QualityRuleApi from '@/api/govern/QualityRuleApi'
import SourceApi from '@/api/govern/SourceApi'
import ApiUtil from '@/utils/ApiUtil'
import RouteUtil from '@/utils/RouteUtil'
import TableUtil from '@/utils/TableUtil'
import GovernPageShell from '@/views/govern/components/GovernPageShell.vue'

const route = useRoute()
const router = useRouter()
const tableRef = ref<TableInstance>()
const loading = ref(false)
const rows = ref<any[]>([])
const selection = ref<any[]>([])
const filters = ref<any>(RouteUtil.query2filter(route, { advanced: false, page: 1, pageSize: 15 }))
const pagination = ref<any>(RouteUtil.pagination(filters.value))
const filterRef = ref<FormInstance>()
const config = ref<any>({ status: {} })
const sources = ref<any[]>([])
const rules = ref<any[]>([])
const columns = ref([
  { prop: 'id', label: 'ID', width: 80 },
  { prop: 'name', label: '方案名称', minWidth: 180 },
  { prop: 'source', label: '数据源', minWidth: 150 },
  { prop: 'sort', label: '排序', width: 80 },
  { prop: 'statusText', label: '状态', width: 90, slot: 'status' },
  { prop: 'updatedUidName', label: '修改者', width: 110, hide: true },
])

const refresh = (filter2query: boolean, keepPage = true) => {
  tableRef.value?.clearSelection()
  Object.assign(filters.value, RouteUtil.pagination2filter(pagination.value, keepPage))
  if (filter2query) RouteUtil.filter2query(route, router, filters.value)
  loading.value = true
  QualityPlanApi.list(filters.value).then((result: any) => {
    RouteUtil.result2pagination(pagination.value, result)
    rows.value = ApiUtil.data(result)?.rows ?? []
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}

const formVisible = ref(false)
const formLoading = ref(false)
const formRef = ref<FormInstance>()
const form = ref<any>({})
const selectedRules = ref<number[]>([])
const rulesForm = {
  name: [{ required: true, message: '请输入方案名称', trigger: 'blur' }],
  source: [{ required: true, message: '请选择数据源', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
}

const selectedRuleNames = computed(() => selectedRules.value.map(id => rules.value.find(item => item.id === id)?.name || id))

const add = () => {
  form.value = { source: '', sort: 0, status: '1', description: '' }
  selectedRules.value = []
  formVisible.value = true
}

const edit = (row: any) => {
  form.value = Object.assign({}, row, { status: String(row.status) })
  selectedRules.value = String(row.content || '').split(',').map(Number).filter(Boolean)
  formVisible.value = true
}

const save = () => {
  formRef.value?.validate((valid: boolean) => {
    if (!valid || formLoading.value) return
    if (selectedRules.value.length === 0) {
      ElMessage.warning('请至少选择一条质检规则')
      return
    }
    formLoading.value = true
    QualityPlanApi.save(Object.assign({}, form.value, {
      content: selectedRules.value.join(','),
    }), { success: true }).then(() => {
      formVisible.value = false
      refresh(false)
    }).catch(() => {}).finally(() => {
      formLoading.value = false
    })
  })
}

const run = (row: any) => {
  TableUtil.confirm(`确认执行质检方案“${row.name}”？`).then(() => {
    loading.value = true
    QualityPlanApi.check({ id: row.id }, { success: true }).then(() => {
      refresh(false)
    }).catch(() => {}).finally(() => {
      loading.value = false
    })
  }).catch(() => {})
}

const remove = () => {
  if (selection.value.length === 0) return
  TableUtil.selection(selection.value).then((ids: any) => {
    loading.value = true
    QualityPlanApi.delete(ids, { success: true }).then(() => refresh(false)).catch(() => {
      loading.value = false
    })
  }).catch(() => {})
}

onMounted(() => {
  refresh(false)
  QualityPlanApi.config({ warning: false }).then((result: any) => config.value = ApiUtil.data(result) ?? config.value).catch(() => {})
  SourceApi.list({ page: 1, pageSize: 500 }, { warning: false }).then((result: any) => {
    sources.value = ApiUtil.data(result)?.rows ?? []
  }).catch(() => {})
  QualityRuleApi.list({ page: 1, pageSize: 500 }, { warning: false }).then((result: any) => {
    rules.value = ApiUtil.data(result)?.rows ?? []
  }).catch(() => {})
})
</script>

<template>
  <GovernPageShell title="质检方案" description="将多条质检规则组合到指定数据源，一键执行并形成可检索的质检报告">
  <el-card shadow="never" class="search-card">
    <form-search ref="filterRef" :model="filters">
      <form-search-item label="方案名称" prop="name"><el-input v-model="filters.name" clearable /></form-search-item>
      <form-search-item label="数据源" prop="source"><el-input v-model="filters.source" clearable /></form-search-item>
      <form-search-item label="状态" prop="status">
        <el-select v-model="filters.status" clearable placeholder="全部状态">
          <el-option v-for="(value, key) in config.status" :key="key" :label="value" :value="key" />
        </el-select>
      </form-search-item>
      <form-search-item>
        <el-button type="primary" :loading="loading" @click="refresh(true, false)">查询</el-button>
        <el-button @click="filterRef?.resetFields()">重置</el-button>
      </form-search-item>
    </form-search>
  </el-card>
  <el-card shadow="never" class="fs-table-card">
    <div class="fs-table-toolbar flex-between">
      <el-space>
        <button-add v-permit="'govern:qualityPlan:add'" @click="add" />
        <button-delete v-permit="'govern:qualityPlan:delete'" :disabled="selection.length === 0" @click="remove" />
      </el-space>
      <el-space>
        <button-refresh :loading="loading" @click="refresh(true)" />
        <TableColumnSetting v-model="columns" :table="tableRef" :loading="loading" />
      </el-space>
    </div>
    <el-table ref="tableRef" v-loading="loading" :data="rows" row-key="id" border @selection-change="(value: any[]) => selection = value">
      <el-table-column type="selection" width="45" />
      <TableColumn :columns="columns">
        <template #status="scope">
          <el-tag :type="scope.row.status === 1 ? 'success' : 'info'" effect="plain">
            {{ scope.row.statusText || config.status?.[scope.row.status] }}
          </el-tag>
        </template>
      </TableColumn>
      <el-table-column label="操作" width="190" fixed="right">
        <template #default="{ row }">
          <el-button link :icon="VideoPlay" :disabled="row.status !== 1" @click="run(row)">执行</el-button>
          <el-button link @click="router.push({ path: '/govern/quality/log', query: { planId: row.id } })">报告</el-button>
          <el-button link v-permit="'govern:qualityPlan:modify'" @click="edit(row)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>
    <TablePagination v-model="pagination" :loading="loading" @change="refresh(true)" />
  </el-card>

  <el-drawer v-model="formVisible" :title="form.id ? '编辑质检方案' : '新增质检方案'" size="62%" :destroy-on-close="true">
    <template #header="{ close, titleId, titleClass }">
      <h4 :id="titleId" :class="titleClass">{{ form.id ? `编辑质检方案 - ${form.id}` : '新增质检方案' }}</h4>
      <el-space>
        <el-button type="primary" :loading="formLoading" @click="save">保存</el-button>
        <el-button @click="close">取消</el-button>
      </el-space>
    </template>
    <el-form ref="formRef" :model="form" :rules="rulesForm" label-width="96px">
      <el-row :gutter="18">
        <el-col :span="12"><el-form-item label="方案名称" prop="name"><el-input v-model="form.name" /></el-form-item></el-col>
        <el-col :span="12">
          <el-form-item label="数据源" prop="source">
            <el-select v-model="form.source" filterable placeholder="选择启用数据源">
              <el-option v-for="item in sources" :key="item.code" :label="`${item.name} (${item.code})`" :value="item.code" :disabled="item.status !== 1" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="状态" prop="status">
            <el-select v-model="form.status">
              <el-option v-for="(value, key) in config.status" :key="key" :label="value" :value="key" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12"><el-form-item label="排序"><el-input-number v-model="form.sort" :min="0" /></el-form-item></el-col>
        <el-col :span="24"><el-form-item label="描述"><el-input v-model="form.description" type="textarea" :rows="3" /></el-form-item></el-col>
        <el-col :span="24">
          <el-form-item label="质检规则">
            <el-select v-model="selectedRules" multiple filterable placeholder="选择要执行的质检规则" class="w-full">
              <el-option v-for="item in rules" :key="item.id" :label="`${item.name} · ${item.logicIdName || '未分类'}`" :value="item.id">
                <div class="rule-option">
                  <span>{{ item.name }}</span>
                  <el-tag size="small" effect="plain">{{ item.type }}</el-tag>
                </div>
              </el-option>
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-alert
        type="info"
        :closable="false"
        show-icon
        :title="`已选择 ${selectedRules.length} 条规则`"
        :description="selectedRuleNames.join('、') || '请选择至少一条质检规则'"
      />
    </el-form>
  </el-drawer>
  </GovernPageShell>
</template>

<style scoped>
.rule-option { display: flex; align-items: center; justify-content: space-between; gap: 20px; }
</style>
