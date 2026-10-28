<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { FormInstance } from 'element-plus'
import ModelApi from '@/api/govern/ModelApi'
import ApiUtil from '@/utils/ApiUtil'
import LayoutHeading from '@/components/Layout/LayoutHeading.vue'
import ModelColumnEditor from '@/views/govern/components/ModelColumnEditor.vue'
import GovernPageShell from '@/views/govern/components/GovernPageShell.vue'

const route = useRoute()
const router = useRouter()
const formRef = ref<FormInstance>()
const loading = ref(false)
const activeTab = ref('basic')
const config = ref<any>({ columnTypes: [] })
const form = ref<any>({
  type: String(route.query.type || 'table'),
  catalog: String(route.query.catalog || '/'),
  code: route.query.code,
  name: '',
  pk: [],
  sort: 0,
  description: '',
  columns: [],
})
const rules = {
  catalog: [{ required: true, message: '所属包不能为空', trigger: 'blur' }],
  code: [{
    required: true,
    message: '请输入模型编码',
    trigger: 'blur',
  }, {
    pattern: /^[a-zA-Z0-9._-]+$/,
    message: '编码仅支持字母、数字、点、下划线和短横线',
    trigger: 'blur',
  }],
  name: [{ required: true, message: '请输入模型名称', trigger: 'blur' }],
}

const load = () => {
  if (!form.value.code) return
  loading.value = true
  ModelApi.info({ catalog: form.value.catalog, code: form.value.code }, { warning: false }).then((result: any) => {
    const data = ApiUtil.data(result) ?? {}
    form.value = Object.assign({}, form.value, data, {
      type: data.type || 'table',
      pk: Array.isArray(data.pk) ? data.pk : String(data.pk || '').split(',').filter(Boolean),
      columns: (data.columns ?? []).map((item: any) => Object.assign({}, item, { nullable: Boolean(item.nullable) })),
    })
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}

const save = () => {
  formRef.value?.validate((valid: boolean) => {
    if (!valid) {
      activeTab.value = 'basic'
      return
    }
    loading.value = true
    ModelApi.save(form.value, { success: true }).then((result: any) => {
      const data = ApiUtil.data(result) ?? form.value
      form.value = Object.assign({}, form.value, data, {
        pk: Array.isArray(data.pk) ? data.pk : String(data.pk || '').split(',').filter(Boolean),
        columns: (data.columns ?? form.value.columns ?? []).map((item: any) => Object.assign({}, item, { nullable: Boolean(item.nullable) })),
      })
      router.replace({
        path: route.path,
        query: { catalog: form.value.catalog, code: form.value.code, type: form.value.type },
      }).catch(() => {})
    }).catch(() => {}).finally(() => {
      loading.value = false
    })
  })
}

onMounted(() => {
  load()
  ModelApi.config({ warning: false }).then((result: any) => {
    config.value = ApiUtil.data(result) ?? config.value
  }).catch(() => {})
})
</script>

<template>
  <GovernPageShell
    :title="form.code ? '编辑元数据模型' : '新增元数据模型'"
    description="维护模型归属、主键和字段结构，保存后可继续配置模型关系"
    back="/govern/meta/model"
    sticky
  >
    <template #actions>
      <el-button type="primary" :loading="loading" @click="save">保存模型</el-button>
    </template>

  <el-card shadow="never" v-loading="loading">
    <el-tabs v-model="activeTab">
      <el-tab-pane label="基础信息" name="basic">
        <LayoutHeading title="模型信息" description="以包路径和编码唯一标识一个模型" />
        <el-form ref="formRef" :model="form" :rules="rules" label-width="96px" class="basic-form">
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="所属包" prop="catalog">
                <el-input v-model="form.catalog" disabled />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="模型编码" prop="code">
                <el-input v-model="form.code" :disabled="!!route.query.code" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="模型名称" prop="name">
                <el-input v-model="form.name" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="模型主键">
                <el-select v-model="form.pk" multiple filterable allow-create placeholder="选择或输入字段编码">
                  <el-option v-for="item in form.columns" :key="item.code" :label="item.code" :value="item.code" />
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
                <el-input v-model="form.description" type="textarea" :rows="4" />
              </el-form-item>
            </el-col>
          </el-row>
        </el-form>
      </el-tab-pane>
      <el-tab-pane label="字段属性" name="columns">
        <LayoutHeading title="字段属性" description="字段编码在模型内必须唯一，主键可从已维护字段中选择" />
        <ModelColumnEditor v-model="form.columns" :column-types="config.columnTypes" />
      </el-tab-pane>
    </el-tabs>
  </el-card>
  </GovernPageShell>
</template>

<style scoped>
.basic-form { max-width: 980px; }
</style>
