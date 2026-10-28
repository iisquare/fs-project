<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { FormInstance } from 'element-plus'
import { ElMessage } from 'element-plus'
import SourceApi from '@/api/govern/SourceApi'
import ModelCompareApi from '@/api/govern/ModelCompareApi'
import ApiUtil from '@/utils/ApiUtil'
import LayoutHeading from '@/components/Layout/LayoutHeading.vue'
import GovernPageShell from '@/views/govern/components/GovernPageShell.vue'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const activeTab = ref('basic')
const formRef = ref<FormInstance>()
const config = ref<any>({ status: { 1: '启用', 2: '禁用' } })
const form = ref<any>({
  code: String(route.query.code || ''),
  name: '',
  type: 'mysql',
  version: '8.0',
  sort: 0,
  status: '1',
  description: '',
  content: {
    url: '',
    username: '',
    password: '',
  },
})
const rules = {
  code: [{
    required: true,
    message: '请输入数据源编码',
    trigger: 'blur',
  }, {
    pattern: /^[a-zA-Z0-9._-]+$/,
    message: '编码仅支持字母、数字、点、下划线和短横线',
    trigger: 'blur',
  }],
  name: [{ required: true, message: '请输入数据源名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择数据源类型', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
}

const load = () => {
  if (!form.value.code) return
  loading.value = true
  SourceApi.info({ code: form.value.code }, { warning: false }).then((result: any) => {
    const data = ApiUtil.data(result) ?? {}
    form.value = Object.assign({}, form.value, data, {
      status: String(data.status ?? 1),
      content: Object.assign({}, form.value.content, data.content ?? {}),
    })
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}

const testConnection = () => {
  formRef.value?.validate((valid: boolean) => {
    if (!valid) return
    loading.value = true
    ModelCompareApi.jdbc(form.value.content, { warning: false }).then((result: any) => {
      const data = ApiUtil.data(result) ?? {}
      ElMessage.success(`连接成功，可读取 ${Object.keys(data).length} 张数据表`)
    }).catch((result: any) => {
      ElMessage.error(ApiUtil.message(result) || '连接失败')
    }).finally(() => {
      loading.value = false
    })
  })
}

const save = () => {
  formRef.value?.validate((valid: boolean) => {
    if (!valid || loading.value) return
    loading.value = true
    SourceApi.save(form.value, { success: true }).then(() => {
      router.replace({ path: '/govern/system/source' }).catch(() => {})
    }).catch(() => {}).finally(() => {
      loading.value = false
    })
  })
}

onMounted(() => {
  load()
  SourceApi.config({ warning: false }).then((result: any) => {
    config.value = ApiUtil.data(result) ?? config.value
  }).catch(() => {})
})
</script>

<template>
  <GovernPageShell
    :title="route.query.code ? '编辑数据源' : '新增数据源'"
    description="配置数据库连接，供数据标准落地评估和质量检查复用"
    back="/govern/system/source"
    sticky
  >
    <template #actions>
      <el-button :loading="loading" @click="testConnection">测试连接</el-button>
      <el-button type="primary" :loading="loading" @click="save">保存</el-button>
    </template>
  <el-card shadow="never" v-loading="loading">
    <el-tabs v-model="activeTab">
      <el-tab-pane label="基础信息" name="basic">
        <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" class="source-form">
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="编码" prop="code">
                <el-input v-model="form.code" :disabled="!!route.query.code" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="名称" prop="name">
                <el-input v-model="form.name" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="类型" prop="type">
                <el-select v-model="form.type">
                  <el-option label="MySQL" value="mysql" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="版本">
                <el-input v-model="form.version" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="状态" prop="status">
                <el-select v-model="form.status">
                  <el-option v-for="(value, key) in config.status" :key="key" :label="value" :value="key" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="排序">
                <el-input-number v-model="form.sort" :min="0" />
              </el-form-item>
            </el-col>
            <el-col :span="24">
              <el-form-item label="描述">
                <el-input v-model="form.description" type="textarea" :rows="3" />
              </el-form-item>
            </el-col>
          </el-row>
        </el-form>
      </el-tab-pane>
      <el-tab-pane label="连接配置" name="connection">
        <LayoutHeading title="MySQL 连接" description="标准评估与质量检查会使用该连接读取数据库结构" />
        <el-form :model="form.content" label-width="100px" class="source-form">
          <el-form-item label="连接串">
            <el-input v-model="form.content.url" type="textarea" :rows="2" placeholder="jdbc:mysql://host:3306/database" />
          </el-form-item>
          <el-form-item label="用户名">
            <el-input v-model="form.content.username" />
          </el-form-item>
          <el-form-item label="密码">
            <el-input v-model="form.content.password" type="password" show-password />
          </el-form-item>
        </el-form>
        <el-alert type="info" :closable="false" show-icon title="只读账号即可完成元数据检查和数据质量检测，建议按最小权限分配账号。" />
      </el-tab-pane>
    </el-tabs>
  </el-card>
  </GovernPageShell>
</template>

<style scoped>
.source-form { max-width: 920px; }
</style>
