<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElNotification } from 'element-plus'
import type { FormInstance, TableInstance } from 'element-plus'
import RouteUtil from '@/utils/RouteUtil'
import { useRoute, useRouter } from 'vue-router'
import CodeUtil from '@/utils/CodeUtil'
import ElementUtil from '@/utils/ElementUtil'
import LuceneApi from '@/api/server/LuceneApi'
import * as ElementPlusIcons from '@element-plus/icons-vue';

const route = useRoute()
const router = useRouter()
const tableRef = ref<TableInstance>()
const loading = ref(false)
const searchable = ref(true)
const columns = ref([
  { prop: 'id', label: 'ID' },
  { prop: 'catalogue', label: '目录' },
  { prop: 'typeText', label: '类型' },
  { prop: 'source', label: '来源' },
  { prop: 'content', label: '词条' },
])
const rows = ref([])
const filterRef = ref<FormInstance>()
const filters = ref(RouteUtil.query2filter(route, { sort: '' }))
const pagination = ref(RouteUtil.pagination(filters.value))
const selectedRows = ref<any[]>([])
const handleSelectionChange = (newSelection: any[]) => {
  selectedRows.value = newSelection
}

const handleRefresh = (filter2query: boolean, keepPage: boolean) => {
  selectedRows.value = []
  Object.assign(filters.value, RouteUtil.pagination2filter(pagination.value, keepPage))
  filter2query && RouteUtil.filter2query(route, router, filters.value)
  loading.value = true
  LuceneApi.dictionaryList(filters.value).then((result: any) => {
    RouteUtil.result2pagination(pagination.value, result)
    if (result.code === 0) {
      rows.value = result.data.rows
    }
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}

const infoVisible = ref(false)
const formVisible = ref(false)
const formLoading = ref(false)
const form: any = ref({})
const formRef: any = ref<FormInstance>()
const rules = ref({
  catalogue: [{ required: true, message: '请输入词库目录', trigger: 'blur' }],
  type: [{ required: true, message: '请选择词库类型', trigger: 'change' }],
  source: [{ required: true, message: '请输入词条来源', trigger: 'blur' }],
  content: [{ required: true, message: '请输入词条内容', trigger: 'blur' }]
})

const config: any = ref({ ready: false, type: {}, sort: {} })

const handleShow = (scope: any) => {
  form.value = Object.assign({}, scope.row)
  infoVisible.value = true
}
const handleEdit = (scope: any) => {
  form.value = Object.assign({}, scope.row)
  formVisible.value = true
}
const handleAdd = () => {
  form.value = Object.assign({}, filters.value, { source: '' })
  formVisible.value = true
}
const handleSubmit = () => {
  formRef.value?.validate((valid: boolean) => {
    if (!valid || formLoading.value) return
    formLoading.value = true
    LuceneApi.dictionarySave(form.value, { success: true }).then((result: any) => {
      if (result.code === 0) {
        formVisible.value = false
        handleRefresh(false, true)
      }
    }).catch(() => {}).finally(() => {
      formLoading.value = false
    })
  })
}
const handleBatchRemove = () => {
  ElementUtil.confirm('确认删除所选记录吗？').then(() => {
    const ids = selectedRows.value.map((row: any) => row.id)
    loading.value = true
    LuceneApi.dictionaryDelete(ids, { success: true }).then((result: any) => {
      if (result.code === 0) {
        handleRefresh(false, true)
      }
    }).catch(() => {
      loading.value = false
    })
  }).catch(() => {})
}
const handleUnique = () => {
  ElementUtil.confirm(`确认清理[词库目录=${filters.value.catalogue},词库类型=${filters.value.type}]下的重复词条吗？`).then(() => {
    loading.value = true
    LuceneApi.dictionaryUnique(filters.value).then((result: any) => {
      if (result.code === 0) {
        ElMessage.success('清理重复记录' + result.data + '条')
        handleRefresh(false, true)
      }
    }).catch(() => {
      loading.value = false
    })
  }).catch(() => {})
}

const baseURL = import.meta.env.VITE_APP_API_URL
const uploadAction = baseURL + '/proxy/upload?app=Lucene&uri=/dictionary/scel'
const uploading = ref(false)
const handleUploadChange = (file: any) => {
  switch (file.status) {
    case 'ready':
      uploading.value = true
      break
    case 'success':
      uploading.value = false
      const result = file.response
      if (result.code === 0) {
        ElNotification({ title: '状态：' + result.code, message: '消息:' + result.message, type: 'success' })
        form.value = Object.assign({}, filters.value, {
          source: file.name,
          content: Object.keys(result.data).join('\n')
        })
        formVisible.value = true
      } else {
        ElNotification({ title: '状态：' + result.code, message: '消息:' + result.message, type: 'warning' })
      }
      break
    case 'error':
      uploading.value = false
      ElNotification({ title: '请求异常', message: `${file.name} file upload failed.`, type: 'error' })
      break
  }
}

const handleDownload = () => {
  const data = CodeUtil.encodeBase64(JSON.stringify(filters.value))
  const params: string[] = []
  for (const key of ['app', 'uri', 'data']) {
    const value: any = { app: 'Lucene', uri: '/dictionary/plain', data }[key]
    params.push(key + '=' + encodeURIComponent(value))
  }
  window.open(baseURL + '/proxy/getResponse?' + params.join('&'))
}

onMounted(() => {
  handleRefresh(false, true)
  LuceneApi.dictionaryConfig().then((result: any) => {
    config.value.ready = true
    if (result.code === 0) {
      Object.assign(config.value, result.data)
    }
  })
})
</script>

<template>
  <section>
    <el-alert type="info" show-icon :closable="true" class="alert-tip">
      提示：基础词条为单个字或连续的词；同义词格式为"词一,词二,词三=>词一,词二,词三"，其中每个词项必须是分词词库中已存在的词条。
    </el-alert>
    <el-card shadow="never">
      <el-card shadow="never" class="fs-table-search" v-show="searchable">
        <form-search ref="filterRef" :model="filters">
          <form-search-item label="目录" prop="catalogue">
            <el-input v-model="filters.catalogue" clearable />
          </form-search-item>
          <form-search-item label="类型" prop="type">
            <el-select v-model="filters.type" placeholder="请选择" clearable>
              <el-option v-for="(value, key) in config.type" :key="key" :label="value" :value="key" />
            </el-select>
          </form-search-item>
          <form-search-item label="来源" prop="source">
            <el-input v-model="filters.source" placeholder="英文逗号分割" clearable />
          </form-search-item>
          <form-search-item>
            <el-button type="primary" @click="handleRefresh(true, false)">查询</el-button>
            <el-button @click="filterRef?.resetFields()">重置</el-button>
            <el-button @click="handleDownload">下载</el-button>
          </form-search-item>
          <form-search-item label="词条" prop="content" :span="12">
            <el-input v-model="filters.content" clearable />
          </form-search-item>
          <form-search-item label="排序" prop="sort">
            <el-select v-model="filters.sort" placeholder="请选择" clearable>
              <el-option v-for="(value, key) in config.sort" :key="key" :label="value" :value="key" />
            </el-select>
          </form-search-item>
          <form-search-item>
            <el-space>
              <el-button type="danger" @click="handleUnique" :loading="loading" v-permit="'lucene:dictionary:delete'">去重</el-button>
              <el-upload
                v-permit="'lucene:dictionary:add'"
                :action="uploadAction"
                accept=".scel"
                :with-credentials="true"
                :show-file-list="false"
                @change="handleUploadChange">
                <el-button :loading="uploading" :icon="ElementPlusIcons.UploadFilled">细胞词库</el-button>
              </el-upload>
            </el-space>
          </form-search-item>
        </form-search>
      </el-card>
      <el-card shadow="never" class="fs-table-card">
        <div class="fs-table-toolbar flex-between">
          <el-space>
            <button-add v-permit="'lucene:dictionary:add'" @click="handleAdd" />
            <button-delete v-permit="'lucene:dictionary:delete'" :disabled="selectedRows.length === 0" @click="handleBatchRemove" />
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
          :row-key="(record: any) => record.id"
          border
          v-loading="loading"
          table-layout="auto"
          @selection-change="handleSelectionChange"
        >
          <el-table-column type="selection" width="55" />
          <TableColumn :columns="columns" />
          <el-table-column label="操作" width="150px">
            <template #default="scope">
              <el-space>
                <el-button link @click="handleShow(scope)" v-permit="'lucene:dictionary:'">查看</el-button>
                <el-button link @click="handleEdit(scope)" v-permit="'lucene:dictionary:modify'">编辑</el-button>
              </el-space>
            </template>
          </el-table-column>
        </el-table>
        <TablePagination v-model="pagination" @change="handleRefresh(true, true)" />
      </el-card>
    </el-card>

    <!-- 查看 -->
    <el-drawer v-model="infoVisible" :title="'信息查看 - ' + form.id">
      <el-form :model="form" label-width="auto">
        <el-form-item label="目录">{{ form.catalogue }}</el-form-item>
        <el-form-item label="类型">{{ form.typeText }}</el-form-item>
        <el-form-item label="来源">{{ form.source }}</el-form-item>
        <el-form-item label="词条">{{ form.content }}</el-form-item>
        <el-form-item label="创建者">{{ form.createdUidName }}</el-form-item>
        <el-form-item label="创建时间">{{ form.createdTime }}</el-form-item>
        <el-form-item label="修改者">{{ form.updatedUidName }}</el-form-item>
        <el-form-item label="修改时间">{{ form.updatedTime }}</el-form-item>
      </el-form>
    </el-drawer>

    <!-- 编辑 -->
    <el-drawer v-model="formVisible" :close-on-click-modal="false" :show-close="false" :destroy-on-close="true">
      <template #header="{ close, titleId, titleClass }">
        <h4 :id="titleId" :class="titleClass">{{ '信息' + (form.id ? ('修改 - ' + form.id) : '添加') }}</h4>
        <el-space>
          <el-button type="primary" @click="handleSubmit" :loading="formLoading">确定</el-button>
          <el-button @click="close">取消</el-button>
        </el-space>
      </template>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="auto">
        <el-form-item label="ID" prop="id">
          <el-input v-model="form.id" />
        </el-form-item>
        <el-form-item label="目录" prop="catalogue">
          <el-input v-model="form.catalogue" />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-select v-model="form.type" placeholder="请选择">
            <el-option v-for="(value, key) in config.type" :key="key" :label="value" :value="key" />
          </el-select>
        </el-form-item>
        <el-form-item label="来源" prop="source">
          <el-input v-model="form.source" />
        </el-form-item>
        <el-form-item label="词条">
          <el-input type="textarea" v-model="form.content" placeholder="批量添加采用换行符分割" :rows="20" />
        </el-form-item>
      </el-form>
    </el-drawer>
  </section>
</template>

<style lang="scss" scoped>
.alert-tip {
  margin-bottom: 25px;
}
</style>
