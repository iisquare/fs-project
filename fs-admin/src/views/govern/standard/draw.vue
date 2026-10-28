<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { FormInstance } from 'element-plus'
import { ElMessage } from 'element-plus'
import StandardApi from '@/api/govern/StandardApi'
import ModelColumnApi from '@/api/govern/ModelColumnApi'
import ApiUtil from '@/utils/ApiUtil'
import RouteUtil from '@/utils/RouteUtil'
import LayoutHeading from '@/components/Layout/LayoutHeading.vue'
import GovernPageShell from '@/views/govern/components/GovernPageShell.vue'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const activeTab = ref(String(route.query.mode || '') === 'pick' ? 'pick' : 'basic')
const formRef = ref<FormInstance>()
const config = ref<any>({ molds: {}, flags: {}, levels: {}, status: {}, columnTypes: [] })
const form = ref<any>({
  mold: 'column',
  catalog: String(route.query.catalog || '/'),
  code: String(route.query.code || ''),
  name: '',
  another: [],
  flag: [],
  level: 'advise',
  type: '',
  size: 0,
  digit: 0,
  nullable: false,
  sort: 0,
  status: '1',
  description: '',
})
const rules = {
  code: [{ required: true, message: '请输入标准编码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入标准名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择字段类型', trigger: 'change' }],
  level: [{ required: true, message: '请选择预警等级', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
}

const sourceLoading = ref(false)
const sourceRows = ref<any[]>([])
const sourceSelection = ref<any>(null)
const sourceFilters = ref<any>({
  page: 1,
  pageSize: 15,
  catalog: '',
  model: '',
  code: '',
  name: '',
})
const sourcePagination = ref<any>(RouteUtil.pagination(sourceFilters.value))

const load = () => {
  if (!form.value.code) return
  loading.value = true
  StandardApi.info({ catalog: form.value.catalog, code: form.value.code }, { warning: false }).then((result: any) => {
    const data = ApiUtil.data(result) ?? {}
    form.value = Object.assign({}, form.value, data, {
      status: String(data.status ?? 1),
      another: Array.isArray(data.another) ? data.another : String(data.another || '').split(',').filter(Boolean),
      flag: Array.isArray(data.flag) ? data.flag : String(data.flag || '').split(',').filter(Boolean),
      nullable: Boolean(data.nullable),
    })
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}

const loadSource = (keepPage = true) => {
  Object.assign(sourceFilters.value, RouteUtil.pagination2filter(sourcePagination.value, keepPage))
  sourceLoading.value = true
  ModelColumnApi.list(sourceFilters.value).then((result: any) => {
    RouteUtil.result2pagination(sourcePagination.value, result)
    sourceRows.value = ApiUtil.data(result)?.rows ?? []
  }).catch(() => {}).finally(() => {
    sourceLoading.value = false
  })
}

const applySource = () => {
  const row = sourceSelection.value
  if (!row) return ElMessage.warning('请选择字段')
  Object.assign(form.value, {
    code: form.value.code || row.code,
    name: form.value.name || row.name,
    type: row.type,
    size: row.size,
    digit: row.digit,
    nullable: Boolean(row.nullable),
  })
  activeTab.value = 'basic'
}

const save = () => {
  formRef.value?.validate((valid: boolean) => {
    if (!valid || loading.value) return
    loading.value = true
    StandardApi.save(form.value, { success: true }).then((result: any) => {
      const data = ApiUtil.data(result) ?? form.value
      form.value = Object.assign({}, form.value, data, {
        status: String(data.status ?? form.value.status),
        another: Array.isArray(data.another) ? data.another : form.value.another,
        flag: Array.isArray(data.flag) ? data.flag : form.value.flag,
        nullable: Boolean(data.nullable),
      })
      router.replace({
        path: route.path,
        query: { catalog: form.value.catalog, code: form.value.code },
      }).catch(() => {})
    }).catch(() => {}).finally(() => {
      loading.value = false
    })
  })
}

onMounted(() => {
  load()
  if (activeTab.value === 'pick') loadSource()
  StandardApi.config({ warning: false }).then((result: any) => {
    config.value = ApiUtil.data(result) ?? config.value
  }).catch(() => {})
})
</script>

<template>
  <GovernPageShell
    :title="form.code ? '编辑数据标准' : '新增数据标准'"
    description="维护字段技术属性、别名编码和落地评估参数"
    back="/govern/standard/list"
    sticky
  >
    <template #actions>
      <el-button type="primary" :loading="loading" @click="save">保存标准</el-button>
    </template>
  <el-card shadow="never" v-loading="loading">
    <el-tabs v-model="activeTab">
      <el-tab-pane v-if="route.query.mode === 'pick'" label="拾取字段" name="pick">
        <LayoutHeading title="从元数据拾取字段" description="选择字段后将自动填充标准编码、名称和字段属性" />
        <el-form :inline="true" :model="sourceFilters">
          <el-form-item label="包路径"><el-input v-model="sourceFilters.catalog" clearable /></el-form-item>
          <el-form-item label="模型编码"><el-input v-model="sourceFilters.model" clearable /></el-form-item>
          <el-form-item label="字段编码"><el-input v-model="sourceFilters.code" clearable /></el-form-item>
          <el-form-item label="字段名称"><el-input v-model="sourceFilters.name" clearable /></el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="sourceLoading" @click="loadSource(false)">查询</el-button>
            <el-button :disabled="!sourceSelection" @click="applySource">拾取</el-button>
          </el-form-item>
        </el-form>
        <el-table v-loading="sourceLoading" :data="sourceRows" row-key="code" border highlight-current-row @current-change="(row: any) => sourceSelection = row">
          <el-table-column prop="catalog" label="包路径" min-width="130" />
          <el-table-column prop="model" label="模型" min-width="130" />
          <el-table-column prop="code" label="字段编码" min-width="130" />
          <el-table-column prop="name" label="字段名称" min-width="140" />
          <el-table-column prop="type" label="类型" width="110" />
          <el-table-column prop="size" label="长度" width="80" />
          <el-table-column prop="digit" label="精度" width="80" />
        </el-table>
        <TablePagination v-model="sourcePagination" :loading="sourceLoading" @change="loadSource(true)" />
      </el-tab-pane>
      <el-tab-pane label="基础信息" name="basic">
        <el-form ref="formRef" :model="form" :rules="rules" label-width="96px" class="standard-form">
          <el-row :gutter="20">
            <el-col :span="12"><el-form-item label="所属目录"><el-input v-model="form.catalog" disabled /></el-form-item></el-col>
            <el-col :span="12"><el-form-item label="标准编码" prop="code"><el-input v-model="form.code" :disabled="!!route.query.code" /></el-form-item></el-col>
            <el-col :span="12"><el-form-item label="标准名称" prop="name"><el-input v-model="form.name" /></el-form-item></el-col>
            <el-col :span="12">
              <el-form-item label="别名编码">
                <el-select v-model="form.another" multiple filterable allow-create placeholder="输入别名编码" />
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
            <el-col :span="24"><el-form-item label="描述"><el-input v-model="form.description" type="textarea" :rows="4" /></el-form-item></el-col>
          </el-row>
        </el-form>
      </el-tab-pane>
      <el-tab-pane label="字段属性" name="field">
        <el-form :model="form" :rules="rules" label-width="96px" class="standard-form">
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="字段类型" prop="type">
                <el-select v-model="form.type" filterable allow-create default-first-option>
                  <el-option v-for="value in Object.values(config.columnTypes ?? {})" :key="String(value)" :label="String(value)" :value="String(value)" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12"><el-form-item label="字段长度"><el-input-number v-model="form.size" :min="0" /></el-form-item></el-col>
            <el-col :span="12"><el-form-item label="小数位数"><el-input-number v-model="form.digit" :min="0" /></el-form-item></el-col>
            <el-col :span="12"><el-form-item label="允许为空"><el-switch v-model="form.nullable" /></el-form-item></el-col>
          </el-row>
        </el-form>
      </el-tab-pane>
      <el-tab-pane label="评估参数" name="assess">
        <el-form :model="form" :rules="rules" label-width="96px" class="standard-form">
          <el-form-item label="评估标志">
            <el-select v-model="form.flag" multiple placeholder="选择落地评估标志">
              <el-option v-for="(value, key) in config.flags" :key="key" :label="`${key} - ${value}`" :value="key" />
            </el-select>
          </el-form-item>
          <el-form-item label="预警等级" prop="level">
            <el-select v-model="form.level">
              <el-option v-for="(value, key) in config.levels" :key="key" :label="`${key} - ${value}`" :value="key" />
            </el-select>
          </el-form-item>
        </el-form>
      </el-tab-pane>
    </el-tabs>
  </el-card>
  </GovernPageShell>
</template>

<style scoped>
.standard-form { max-width: 960px; }
</style>
