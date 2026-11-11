<script setup lang="ts">
import { onMounted, ref } from 'vue'
import type { FormInstance } from 'element-plus'
import { ElMessage } from 'element-plus'
import EntryApi from '@/api/siteBackend/EntryApi'
import RuleApi from '@/api/siteBackend/RuleApi'
import SyncApi from '@/api/siteBackend/SyncApi'
import TemplateApi from '@/api/spider/TemplateApi'
import ApiUtil from '@/utils/ApiUtil'
import DateUtil from '@/utils/DateUtil'
import ElementUtil from '@/utils/ElementUtil'
import TableUtil from '@/utils/TableUtil'

const active = ref('entry')
const loading = ref(false)
const templates = ref<any[]>([])
const domains = ref<any[]>([])
const templateId = ref<number>()
const entryConfig: any = ref({ status: {}, entryTypes: {}, categoryDictionary: 'site-category' })
const ruleConfig: any = ref({ status: {} })
const entryFilters: any = ref({ page: 1, pageSize: 15, name: '', domain: '', categoryPath: [] })
const ruleFilters: any = ref({ page: 1, pageSize: 15, name: '', domain: '' })
const entryPagination = ref({ page: 1, pageSize: 15, total: 0 })
const rulePagination = ref({ page: 1, pageSize: 15, total: 0 })
const entries = ref<any[]>([])
const rules = ref<any[]>([])
const entrySelection = ref<any[]>([])
const ruleSelection = ref<any[]>([])

const entryColumns = [
  { prop: 'name', label: '站点名称', minWidth: 180 },
  { prop: 'domain', label: '域名', minWidth: 190 },
  { prop: 'path', label: '子目录', minWidth: 130 },
  { prop: 'categoryText', label: '一级类别', width: 120 },
  { prop: 'subCategoryText', label: '二级类别', width: 140 },
  { prop: 'entryType', label: '入口类型', width: 100 },
  { prop: 'official', label: '官方', width: 70 },
  { prop: 'lastSyncTime', label: '最近同步', width: 170 },
  { prop: 'statusText', label: '状态', width: 80 },
]
const ruleColumns = [
  { prop: 'name', label: '规则名称', minWidth: 180 },
  { prop: 'domain', label: '域名', minWidth: 190 },
  { prop: 'path', label: '路径', minWidth: 150 },
  { prop: 'lastSyncTime', label: '最近同步', width: 170 },
  { prop: 'statusText', label: '状态', width: 80 },
]

const loadTemplates = async () => {
  const result: any = await TemplateApi.list({ page: 1, pageSize: 500 })
  templates.value = (ApiUtil.data(result)?.rows ?? []).filter((item: any) => item.status === 1)
}
const loadEntries = () => {
  loading.value = true
  const params = Object.assign({}, entryFilters.value, {
    category: entryFilters.value.categoryPath?.[0] ?? '',
    subCategory: entryFilters.value.categoryPath?.[1] ?? '',
  })
  delete params.categoryPath
  EntryApi.list(params).then((result: any) => {
    const data = ApiUtil.data(result) ?? {}
    entries.value = data.rows ?? []
    Object.assign(entryPagination.value, { page: data.page, pageSize: data.pageSize, total: data.total })
    loadDomains()
  }).catch(() => {}).finally(() => { loading.value = false })
}
const loadDomains = () => {
  EntryApi.domains().then((result: any) => {
    domains.value = ApiUtil.data(result) ?? []
  }).catch(() => {})
}
const loadRules = () => {
  loading.value = true
  RuleApi.list(ruleFilters.value).then((result: any) => {
    const data = ApiUtil.data(result) ?? {}
    rules.value = data.rows ?? []
    Object.assign(rulePagination.value, { page: data.page, pageSize: data.pageSize, total: data.total })
  }).catch(() => {}).finally(() => { loading.value = false })
}

const entryVisible = ref(false)
const entryFormLoading = ref(false)
const entryFormRef = ref<FormInstance>()
const entryForm: any = ref({})
const openEntry = (row: any = {}) => {
  entryForm.value = Object.assign({
    status: '1',
    official: true,
    entryType: 'subdir',
    path: '/',
    collection: 'fs_spider_index',
    charset: 'utf-8',
    sort: 0,
  }, row, {
    status: String(row.status ?? 1),
    official: row.official === undefined ? true : Boolean(row.official),
    categoryPath: row.category ? [row.category, row.subCategory].filter(Boolean) : [],
  })
  entryVisible.value = true
}
const saveEntry = () => {
  entryFormRef.value?.validate((valid: boolean) => {
    if (!valid || entryFormLoading.value) return
    if (!Array.isArray(entryForm.value.categoryPath) || entryForm.value.categoryPath.length !== 2) {
      ElMessage.warning('请选择完整的网站一级和二级类别')
      return
    }
    entryFormLoading.value = true
    const payload = Object.assign({}, entryForm.value, {
      category: entryForm.value.categoryPath[0],
      subCategory: entryForm.value.categoryPath[1],
    })
    delete payload.categoryPath
    EntryApi.save(payload, { success: true }).then(() => {
      entryVisible.value = false
      loadEntries()
    }).catch(() => {}).finally(() => { entryFormLoading.value = false })
  })
}

const ruleVisible = ref(false)
const ruleFormLoading = ref(false)
const ruleFormRef = ref<FormInstance>()
const ruleForm: any = ref({})
const openRule = (row: any = {}) => {
  if (!domains.value.length) {
    ElMessage.warning('请先维护启用状态的网站入口')
    return
  }
  ruleForm.value = Object.assign({
    status: '1',
    ruleType: 'black',
    domain: domains.value[0]?.domain ?? '',
    path: '*',
    sort: 0,
  }, row, { status: String(row.status ?? 1) })
  ruleVisible.value = true
}
const saveRule = () => {
  ruleFormRef.value?.validate((valid: boolean) => {
    if (!valid || ruleFormLoading.value) return
    if (!ruleForm.value.domain) {
      ElMessage.warning('请选择黑名单所属网站域名')
      return
    }
    ruleFormLoading.value = true
    RuleApi.save(ruleForm.value, { success: true }).then(() => {
      ruleVisible.value = false
      loadRules()
    }).catch(() => {}).finally(() => { ruleFormLoading.value = false })
  })
}

const removeEntries = () => {
  TableUtil.selection(entrySelection.value)
    .then((ids: any) => EntryApi.delete(ids, { success: true }))
    .then(() => loadEntries())
    .catch(() => {})
}
const removeRules = () => {
  TableUtil.selection(ruleSelection.value)
    .then((ids: any) => RuleApi.delete(ids, { success: true }))
    .then(() => loadRules())
    .catch(() => {})
}
const syncSpider = () => {
  if (!templateId.value) {
    ElMessage.warning('请先选择 Spider 采集模板')
    return
  }
  const template = templates.value.find((item: any) => item.id === templateId.value)
  ElementUtil.confirm(`确认将网站及子目录作为白名单、所选网站域名下的规则作为黑名单，同步到“${template?.name || templateId.value}”并启动采集作业？`).then(() => {
    loading.value = true
    SyncApi.spider({ templateId: templateId.value, start: true }, { success: true }).then(() => {
      loadEntries()
      loadRules()
    }).catch(() => {}).finally(() => { loading.value = false })
  }).catch(() => {})
}

const renderDate = (value: any) => value ? DateUtil.format(value) : '尚未同步'

onMounted(() => {
  Promise.all([loadTemplates(), EntryApi.config(), RuleApi.config()]).then(([, entry, rule]) => {
    entryConfig.value = Object.assign(entryConfig.value, ApiUtil.data(entry))
    ruleConfig.value = Object.assign(ruleConfig.value, ApiUtil.data(rule))
  }).catch(() => {})
  loadEntries()
  loadRules()
  loadDomains()
})
</script>

<template>
  <el-card :bordered="false" shadow="never" class="site-toolbar">
    <div class="flex-between">
      <el-tabs v-model="active" class="site-tabs">
        <el-tab-pane label="网站入口（白名单）" name="entry" />
        <el-tab-pane label="域名黑名单" name="rule" />
      </el-tabs>
      <el-space>
        <span class="sync-label">同步模板</span>
        <el-select v-model="templateId" filterable placeholder="选择 Spider 模板" style="width: 220px">
          <el-option v-for="item in templates" :key="item.id" :value="item.id" :label="item.name" />
        </el-select>
        <el-button type="primary" @click="syncSpider">同步并采集</el-button>
      </el-space>
    </div>
  </el-card>

  <el-card v-show="active === 'entry'" :bordered="false" shadow="never" class="fs-table-card">
    <div class="fs-table-toolbar flex-between">
      <el-space>
        <el-input v-model="entryFilters.name" placeholder="站点名称" clearable style="width: 170px" @keyup.enter="loadEntries" />
        <el-input v-model="entryFilters.domain" placeholder="域名" clearable style="width: 180px" @keyup.enter="loadEntries" />
        <dictionary-cascader
          v-model="entryFilters.categoryPath"
          :dictionary="entryConfig.categoryDictionary"
          :level="2"
          clearable
          filterable
          placeholder="网站类别"
          style="width: 260px"
        />
        <el-button @click="loadEntries">查询</el-button>
      </el-space>
      <el-space>
        <el-button type="primary" @click="openEntry()">新增入口</el-button>
        <el-button :disabled="entrySelection.length === 0" @click="removeEntries">删除</el-button>
        <el-button @click="loadEntries">刷新</el-button>
      </el-space>
    </div>
    <div class="rule-hint">启用的网站及子目录会在同步时自动作为 Spider 白名单推送。</div>
    <el-table :data="entries" v-loading="loading" border row-key="id" @selection-change="(rows: any) => entrySelection = rows">
      <el-table-column type="selection" width="42" />
      <el-table-column
        v-for="column in entryColumns"
        :key="column.prop"
        :prop="column.prop"
        :label="column.label"
        :width="column.width"
        :min-width="column.minWidth"
      >
        <template #default="{ row }">
          <template v-if="column.prop === 'lastSyncTime'">{{ renderDate(row.lastSyncTime) }}</template>
          <template v-else-if="column.prop === 'official'">{{ row.official ? '是' : '否' }}</template>
          <template v-else>{{ row[column.prop] }}</template>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="170" fixed="right">
        <template #default="{ row }">
          <el-button link @click="openEntry(row)">编辑</el-button>
          <el-button link @click="openEntry({ ...row, id: undefined, name: row.name + '（子目录）' })">复制</el-button>
          <el-button link @click="openRule({ domain: row.domain, name: `${row.name} - 黑名单` })">黑名单</el-button>
        </template>
      </el-table-column>
    </el-table>
    <TablePagination v-model="entryPagination" :loading="loading" @change="loadEntries" />
  </el-card>

  <el-card v-show="active === 'rule'" :bordered="false" shadow="never" class="fs-table-card">
    <div class="fs-table-toolbar flex-between">
      <el-space>
        <el-input v-model="ruleFilters.name" placeholder="规则名称" clearable style="width: 170px" @keyup.enter="loadRules" />
        <el-input v-model="ruleFilters.domain" placeholder="域名" clearable style="width: 180px" @keyup.enter="loadRules" />
        <el-button @click="loadRules">查询</el-button>
      </el-space>
      <el-space>
        <el-button type="primary" @click="openRule()">新增黑名单</el-button>
        <el-button :disabled="ruleSelection.length === 0" @click="removeRules">删除</el-button>
        <el-button @click="loadRules">刷新</el-button>
      </el-space>
    </div>
    <div class="rule-hint">黑名单必须归属已维护的网站域名，仅同步所选网站域名下的黑名单路径。</div>
    <el-table :data="rules" v-loading="loading" border row-key="id" @selection-change="(rows: any) => ruleSelection = rows">
      <el-table-column type="selection" width="42" />
      <el-table-column
        v-for="column in ruleColumns"
        :key="column.prop"
        :prop="column.prop"
        :label="column.label"
        :width="column.width"
        :min-width="column.minWidth"
      >
        <template #default="{ row }">
          <template v-if="column.prop === 'lastSyncTime'">{{ renderDate(row.lastSyncTime) }}</template>
          <template v-else>{{ row[column.prop] }}</template>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="90" fixed="right">
        <template #default="{ row }"><el-button link @click="openRule(row)">编辑</el-button></template>
      </el-table-column>
    </el-table>
    <TablePagination v-model="rulePagination" :loading="loading" @change="loadRules" />
  </el-card>

  <el-drawer v-model="entryVisible" :title="entryForm.id ? '编辑网站入口' : '新增网站入口'" size="620px">
    <el-form ref="entryFormRef" :model="entryForm" label-width="96px">
      <el-form-item label="站点名称" prop="name" :rules="{ required: true, message: '请输入站点名称' }">
        <el-input v-model="entryForm.name" />
      </el-form-item>
      <el-form-item label="域名" prop="domain" :rules="{ required: true, message: '请输入域名' }">
        <el-input v-model="entryForm.domain" placeholder="www.miit.gov.cn" />
      </el-form-item>
      <el-form-item label="子目录"><el-input v-model="entryForm.path" placeholder="/jgsj/" /></el-form-item>
      <el-form-item label="入口地址">
        <el-input v-model="entryForm.url" placeholder="留空时按域名和子目录生成" />
      </el-form-item>
      <el-form-item label="类别">
        <dictionary-cascader
          v-model="entryForm.categoryPath"
          :dictionary="entryConfig.categoryDictionary"
          :level="2"
          filterable
          placeholder="请选择一级、二级类别"
          style="width: 100%"
        />
      </el-form-item>
      <el-form-item label="入口类型">
        <el-select v-model="entryForm.entryType" style="width: 100%">
          <el-option v-for="(label, key) in entryConfig.entryTypes" :key="key" :value="key" :label="label" />
        </el-select>
      </el-form-item>
      <el-form-item label="采集集合"><el-input v-model="entryForm.collection" /></el-form-item>
      <el-form-item label="页面编码"><el-input v-model="entryForm.charset" /></el-form-item>
      <el-form-item label="官方渠道"><el-switch v-model="entryForm.official" /></el-form-item>
      <el-form-item label="状态">
        <el-select v-model="entryForm.status" style="width: 100%">
          <el-option v-for="(label, key) in entryConfig.status" :key="key" :value="String(key)" :label="label" />
        </el-select>
      </el-form-item>
      <el-form-item label="排序"><el-input-number v-model="entryForm.sort" :controls="false" /></el-form-item>
      <el-form-item label="描述"><el-input v-model="entryForm.description" type="textarea" :rows="3" /></el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="entryVisible = false">取消</el-button>
      <el-button type="primary" :loading="entryFormLoading" @click="saveEntry">保存</el-button>
    </template>
  </el-drawer>

  <el-drawer v-model="ruleVisible" :title="ruleForm.id ? '编辑访问规则' : '新增访问规则'" size="560px">
    <el-form ref="ruleFormRef" :model="ruleForm" label-width="94px">
      <el-form-item label="规则名称" prop="name" :rules="{ required: true, message: '请输入规则名称' }">
        <el-input v-model="ruleForm.name" />
      </el-form-item>
      <el-form-item label="所属域名" prop="domain" :rules="{ required: true, message: '请选择黑名单所属域名' }">
        <el-select v-model="ruleForm.domain" filterable style="width: 100%" placeholder="选择已维护的网站域名">
          <el-option
            v-for="item in domains"
            :key="item.domain"
            :value="item.domain"
            :label="`${item.name} · ${item.domain}`"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="路径"><el-input v-model="ruleForm.path" placeholder="* 或 /news/*" /></el-form-item>
      <el-form-item label="状态">
        <el-select v-model="ruleForm.status" style="width: 100%">
          <el-option v-for="(label, key) in ruleConfig.status" :key="key" :value="String(key)" :label="label" />
        </el-select>
      </el-form-item>
      <el-form-item label="排序"><el-input-number v-model="ruleForm.sort" :controls="false" /></el-form-item>
      <el-form-item label="描述"><el-input v-model="ruleForm.description" type="textarea" :rows="3" /></el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="ruleVisible = false">取消</el-button>
      <el-button type="primary" :loading="ruleFormLoading" @click="saveRule">保存</el-button>
    </template>
  </el-drawer>
</template>

<style scoped lang="scss">
.site-toolbar {
  margin-bottom: 8px;
}
.site-tabs {
  :deep(.el-tabs__header) {
    margin: 0;
  }
  :deep(.el-tabs__content) {
    display: none;
  }
}
.sync-label {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}
.rule-hint {
  margin: -2px 0 10px;
  padding: 7px 10px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.5;
  background: var(--el-fill-color-light);
  border-left: 3px solid var(--el-color-primary-light-5);
}
</style>
