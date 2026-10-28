<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { FormInstance } from 'element-plus'
import AssessApi from '@/api/govern/AssessApi'
import SourceApi from '@/api/govern/SourceApi'
import ApiUtil from '@/utils/ApiUtil'
import LayoutHeading from '@/components/Layout/LayoutHeading.vue'
import GovernPageShell from '@/views/govern/components/GovernPageShell.vue'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const formRef = ref<FormInstance>()
const config = ref<any>({ status: {} })
const sources = ref<any[]>([])
const form = ref<any>({
  id: route.query.id ? Number(route.query.id) : undefined,
  name: '',
  source: '',
  sort: 0,
  status: '1',
  description: '',
  content: {
    code: '',
    include: '',
    exclude: '',
  },
})
const rules = {
  name: [{ required: true, message: '请输入方案名称', trigger: 'blur' }],
  source: [{ required: true, message: '请选择数据源', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
}

const load = () => {
  if (!form.value.id) return
  loading.value = true
  AssessApi.info({ id: form.value.id }, { warning: false }).then((result: any) => {
    const data = ApiUtil.data(result) ?? {}
    form.value = Object.assign({}, form.value, data, {
      status: String(data.status ?? 1),
      content: Object.assign({}, form.value.content, data.content ?? {}),
    })
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}

const save = (execute = false) => {
  formRef.value?.validate((valid: boolean) => {
    if (!valid || loading.value) return
    loading.value = true
    AssessApi.save(form.value, { success: true }).then((result: any) => {
      const data = ApiUtil.data(result) ?? form.value
      form.value = Object.assign({}, form.value, data, {
        status: String(data.status ?? form.value.status),
        content: Object.assign({}, form.value.content, data.content ?? {}),
      })
      router.replace({ path: route.path, query: { id: form.value.id } }).catch(() => {})
      if (execute) return AssessApi.check({ id: form.value.id }, { success: true })
    }).then(() => {
      if (execute) router.push({ path: '/govern/standard/log', query: { assess: form.value.id } }).catch(() => {})
    }).catch(() => {}).finally(() => {
      loading.value = false
    })
  })
}

onMounted(() => {
  load()
  AssessApi.config({ warning: false }).then((result: any) => config.value = ApiUtil.data(result) ?? config.value).catch(() => {})
  SourceApi.list({ page: 1, pageSize: 500 }, { warning: false }).then((result: any) => {
    sources.value = ApiUtil.data(result)?.rows ?? []
  }).catch(() => {})
})
</script>

<template>
  <GovernPageShell
    :title="form.id ? '编辑评估方案' : '新增评估方案'"
    description="选择数据源与适用标准，配置表名过滤条件后执行落地检查"
    back="/govern/standard/assess"
    sticky
  >
    <template #actions>
      <el-button :loading="loading" @click="save(false)">保存</el-button>
      <el-button type="primary" :loading="loading" @click="save(true)">保存并执行</el-button>
    </template>
  <el-card shadow="never" v-loading="loading">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" class="assess-form">
      <LayoutHeading title="方案信息" />
      <el-row :gutter="20">
        <el-col :span="12"><el-form-item label="方案名称" prop="name"><el-input v-model="form.name" /></el-form-item></el-col>
        <el-col :span="12">
          <el-form-item label="数据源" prop="source">
            <el-select v-model="form.source" filterable placeholder="选择启用的数据源">
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
      </el-row>
      <LayoutHeading title="评估参数" description="标准路径支持目录路径，包含和排除规则每行一条" />
      <el-form-item label="数据标准">
        <el-input v-model="form.content.code" type="textarea" :rows="5" placeholder="每行一个标准路径或目录路径" />
      </el-form-item>
      <el-form-item label="包含表名">
        <el-input v-model="form.content.include" type="textarea" :rows="3" placeholder="每行一个表名，留空表示不过滤" />
      </el-form-item>
      <el-form-item label="排除表名">
        <el-input v-model="form.content.exclude" type="textarea" :rows="3" placeholder="每行一个表名或正则表达式" />
      </el-form-item>
    </el-form>
  </el-card>
  </GovernPageShell>
</template>

<style scoped>
.assess-form { max-width: 1040px; }
</style>
