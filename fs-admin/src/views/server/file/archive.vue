<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElNotification } from 'element-plus'
import * as ElementPlusIcons from '@element-plus/icons-vue'
import type { FormInstance, TableInstance } from 'element-plus'
import RouteUtil from '@/utils/RouteUtil'
import { useRoute, useRouter } from 'vue-router'
import DateUtil from '@/utils/DateUtil'
import UIUtil from '@/utils/UIUtil'
import FileApi from '@/api/server/FileApi'
import CodeUtil from '@/utils/CodeUtil'
import TableUtil from '@/utils/TableUtil'
import FormUpload from '@/components/Form/FormUpload.vue'

const route = useRoute()
const router = useRouter()
const tableRef = ref<TableInstance>()
const loading = ref(false)
const searchable = ref(true)

const columns = ref([
  { prop: 'id', label: 'ID', width: '280px' },
  { prop: 'name', label: '名称' },
  { prop: 'bucket', label: '文件桶' },
  { prop: 'filepath', label: '文件路径', hide: true },
  { prop: 'suffix', label: '文件后缀', hide: true },
  { prop: 'sharable', label: '可分享', hide: true },
  { prop: 'traceIdentity', label: '溯源标识' },
  { prop: 'type', label: '类型', hide: true },
  { prop: 'size', label: '大小', formatter: UIUtil.renderFileSize },
  { prop: 'hash', label: '哈希值', hide: true },
  { prop: 'statusText', label: '状态', width: '70px' },
  { prop: 'updatedTime', label: '修改时间', formatter: DateUtil.render, width: '170px' },
])

const rows = ref([])
const filterRef = ref<FormInstance>()
const filters = ref(RouteUtil.query2filter(route, { deleted: 'without' }))
const pagination = ref(RouteUtil.pagination(filters.value))
const selection: any = ref([])

const config: any = ref({ ready: false, status: {} })

const handleRefresh = (filter2query: boolean, keepPage: boolean) => {
  tableRef.value?.clearSelection()
  Object.assign(filters.value, RouteUtil.pagination2filter(pagination.value, keepPage))
  filter2query && RouteUtil.filter2query(route, router, filters.value)
  loading.value = true
  FileApi.archiveList(filters.value).then((result: any) => {
    RouteUtil.result2pagination(pagination.value, result)
    rows.value = result.data.rows
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}

onMounted(() => {
  handleRefresh(false, true)
  FileApi.archiveConfig().then((result: any) => {
    config.value.ready = true
    if (result.code === 0) {
      Object.assign(config.value, result.data)
    }
  })
})

const handleDelete = () => {
  TableUtil.selection(selection.value).then((ids: any) => {
    loading.value = true
    FileApi.archiveDelete(ids, { success: true }).then(() => {
      handleRefresh(false, true)
    }).catch(() => {
      loading.value = false
    })
  }).catch(() => {})
}

const infoVisible = ref(false)
const form: any = ref({})

const handleShow = (scope: any) => {
  form.value = Object.assign({}, scope.row, {
    size: UIUtil.prettyFileSize(scope.row.size)
  })
  infoVisible.value = true
}

const formVisible = ref(false)
const formLoading = ref(false)
const formRef: any = ref<FormInstance>()
const rules: any = ref({})

const uploadVisible = ref(false)
const uploadFile = ref<File>()

const handleOpenUpload = () => {
  form.value = {
    bucket: filters.value.bucket || '',
    filepath: '',
    traceIdentity: '',
    status: '1'
  }
  rules.value = {
    bucket: [{ required: true, message: '请输入文件桶', trigger: 'blur' }],
    filepath: [{ required: true, message: '请输入文件路径', trigger: 'blur' }],
    status: [{ required: true, message: '请选择状态', trigger: 'change' }]
  }
  uploadFile.value = undefined
  uploadVisible.value = true
}

const handleUploadSubmit = () => {
  formRef.value?.validate((valid: boolean) => {
    if (!valid || formLoading.value) return
    if (!uploadFile.value) {
      ElNotification({ title: '请选择文件', message: '请选择待上传的文件', type: 'warning' })
      return
    }
    formLoading.value = true
    const params = Object.assign({}, form.value, { file: uploadFile.value })
    FileApi.archiveUpload(params, { success: true }).then((result: any) => {
      uploadVisible.value = false
      handleRefresh(false, true)
    }).catch(() => {}).finally(() => {
      formLoading.value = false
    })
  })
}

const handleEdit = (scope: any) => {
  form.value = Object.assign({}, scope.row, {
    status: scope.row.status + ''
  })
  rules.value = {
    name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
    status: [{ required: true, message: '请选择状态', trigger: 'change' }]
  }
  formVisible.value = true
}

const handleSubmit = () => {
  formRef.value?.validate((valid: boolean) => {
    if (!valid || formLoading.value) return
    formLoading.value = true
    FileApi.archiveSave(form.value, { success: true }).then(() => {
      formVisible.value = false
      handleRefresh(false, true)
    }).catch(() => {}).finally(() => {
      formLoading.value = false
    })
  })
}

const baseURL = import.meta.env.VITE_APP_API_URL

const handleDownload = (record: any) => {
  window.open(baseURL + '/file/archive/download?id=' + encodeURIComponent(record.id))
}
</script>

<template>
  <section>
    <el-card shadow="never" class="fs-table-search" v-show="searchable">
      <form-search ref="filterRef" :model="filters">
        <form-search-item label="">
          <form-deleted v-model="filters.deleted" @change="handleRefresh(true, false)" />
        </form-search-item>
        <form-search-item label="文件桶" prop="bucket">
          <el-input v-model="filters.bucket" clearable />
        </form-search-item>
        <form-search-item label="名称" prop="name">
          <el-input v-model="filters.name" clearable />
        </form-search-item>
        <form-search-item>
          <el-button type="primary" @click="handleRefresh(true, false)">查询</el-button>
          <el-button @click="filterRef?.resetFields()">重置</el-button>
          <button-advanced v-model="filters.advanced" />
        </form-search-item>
        <template v-if="filters.advanced">
          <form-search-item label="文件标识" prop="id">
            <el-input v-model="filters.id" clearable />
          </form-search-item>
          <form-search-item label="溯源标识" prop="traceIdentity">
            <el-input v-model="filters.traceIdentity" clearable />
          </form-search-item>
          <form-search-item label="文件路径" prop="filepath">
            <el-input v-model="filters.filepath" clearable />
          </form-search-item>
          <form-search-item label="状态" prop="status">
            <el-select v-model="filters.status" placeholder="请选择" clearable>
              <el-option v-for="(value, key) in config.status" :key="key" :label="value" :value="key" />
            </el-select>
          </form-search-item>
          <form-search-item label="类型" prop="type">
            <el-input v-model="filters.type" clearable />
          </form-search-item>
          <form-search-item label="后缀" prop="suffix">
            <el-input v-model="filters.suffix" clearable />
          </form-search-item>
          <form-search-item label="最小文件大小" prop="sizeBegin">
            <form-input-number v-model="filters.sizeBegin" />
          </form-search-item>
          <form-search-item label="最大文件大小" prop="sizeEnd">
            <form-input-number v-model="filters.sizeEnd" />
          </form-search-item>
          <form-search-item label="创建开始时间" prop="createdTimeBegin">
            <form-date-picker v-model="filters.createdTimeBegin" placeholder="开始时间" />
          </form-search-item>
          <form-search-item label="创建结束时间" prop="createdTimeEnd">
            <form-date-picker v-model="filters.createdTimeEnd" placeholder="结束时间" />
          </form-search-item>
          <form-search-item label="修改开始时间" prop="updatedTimeBegin">
            <form-date-picker v-model="filters.updatedTimeBegin" placeholder="开始时间" />
          </form-search-item>
          <form-search-item label="修改结束时间" prop="updatedTimeEnd">
            <form-date-picker v-model="filters.updatedTimeEnd" placeholder="结束时间" />
          </form-search-item>
        </template>
      </form-search>
    </el-card>
    <el-card shadow="never" class="fs-table-card">
      <div class="fs-table-toolbar flex-between">
        <el-space>
          <el-button type="success" :icon="ElementPlusIcons.UploadFilled" v-permit="'file:archive:upload'" @click="handleOpenUpload">上传</el-button>
          <button-delete v-permit="'file:archive:delete'" :disabled="selection.length === 0" @click="handleDelete" />
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
        :border="true"
        v-loading="loading"
        table-layout="auto"
        @selection-change="(newSelection: any) => selection = newSelection"
      >
        <el-table-column type="selection" />
        <TableColumn :columns="columns" />
        <el-table-column label="操作" width="165px">
          <template #default="scope">
            <el-space>
              <el-button link @click="handleShow(scope)" v-permit="'file:archive:'">查看</el-button>
              <el-button link @click="handleEdit(scope)" v-permit="'file:archive:save'">编辑</el-button>
              <el-button link @click.prevent="handleDownload(scope.row)" v-permit="'file:archive:download'">下载</el-button>
            </el-space>
          </template>
        </el-table-column>
      </el-table>
      <TablePagination v-model="pagination" @change="handleRefresh(true, true)" />
    </el-card>

    <el-drawer v-model="infoVisible" :title="'信息查看'">
      <el-form :model="form" label-width="auto">
        <el-form-item label="文件标识">{{ form.id }}</el-form-item>
        <el-form-item label="文件名称">{{ form.name }}</el-form-item>
        <el-form-item label="存储分类">{{ form.bucket }}</el-form-item>
        <el-form-item label="存储路径">{{ form.filepath }}</el-form-item>
        <el-form-item label="文件后缀">{{ form.suffix }}</el-form-item>
        <el-form-item label="可分享">{{ form.sharable ? '是' : '否' }}</el-form-item>
        <el-form-item label="溯源标识">{{ form.traceIdentity }}</el-form-item>
        <el-form-item label="文件类型">{{ form.type }}</el-form-item>
        <el-form-item label="文件大小">{{ form.size }}</el-form-item>
        <el-form-item label="头部摘要"><el-input v-model="form.digest" type="textarea" /></el-form-item>
        <el-form-item label="内容校验">{{ form.hash }}</el-form-item>
        <el-form-item label="状态">{{ form.statusText }}</el-form-item>
        <el-form-item label="创建者">{{ form.createdUserInfo?.name }}</el-form-item>
        <el-form-item label="创建时间">{{ DateUtil.format(form.createdTime) }}</el-form-item>
        <el-form-item label="修改者">{{ form.updatedUserInfo?.name }}</el-form-item>
        <el-form-item label="修改时间">{{ DateUtil.format(form.updatedTime) }}</el-form-item>
      </el-form>
    </el-drawer>

    <el-drawer v-model="formVisible" :close-on-click-modal="false" :show-close="false" :destroy-on-close="true">
      <template #header="{ close, titleId, titleClass }">
        <h4 :id="titleId" :class="titleClass">{{ '信息' + (form.id ? '修改' : '添加') }}</h4>
        <el-space>
          <el-button type="primary" @click="handleSubmit" :loading="formLoading">确定</el-button>
          <el-button @click="close">取消</el-button>
        </el-space>
      </template>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="auto">
        <el-form-item label="ID" prop="id">{{ form.id }}</el-form-item>
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入名称" />
        </el-form-item>
        <el-form-item label="文件桶" prop="bucket">
          <el-input v-model="form.bucket" placeholder="请输入文件桶" />
        </el-form-item>
        <el-form-item label="文件路径" prop="filepath">
          <el-input v-model="form.filepath" placeholder="请输入文件路径" />
        </el-form-item>
        <el-form-item label="文件后缀" prop="suffix">
          <el-input v-model="form.suffix" placeholder="请输入文件后缀" />
        </el-form-item>
        <el-form-item label="可分享" prop="sharable">
          <el-checkbox v-model="form.sharable" label="可通过图片模式进行访问" />
        </el-form-item>
        <el-form-item label="溯源标识" prop="traceIdentity">
          <el-input v-model="form.traceIdentity" placeholder="请输入溯源标识" />
        </el-form-item>
        <el-form-item label="文件类型" prop="type">
          <el-input v-model="form.type" placeholder="请输入文件类型" />
        </el-form-item>
        <el-form-item label="文件大小" prop="size">
          <el-input-number v-model="form.size" :min="0" placeholder="请输入文件大小" />
        </el-form-item>
        <el-form-item label="头部摘要" prop="digest">
          <el-input v-model="form.digest" type="textarea" placeholder="请输入头部摘要" />
        </el-form-item>
        <el-form-item label="哈希值" prop="hash">
          <el-input v-model="form.hash" placeholder="请输入哈希值" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="form.status" placeholder="请选择">
            <el-option v-for="(value, key) in config.status" :key="key" :label="value" :value="key" />
          </el-select>
        </el-form-item>
      </el-form>
    </el-drawer>
    <el-drawer v-model="uploadVisible" :close-on-click-modal="false" :show-close="false" :destroy-on-close="true" @open="handleOpenUpload">
      <template #header="{ close, titleId, titleClass }">
        <h4 :id="titleId" :class="titleClass">文件上传</h4>
        <el-space>
          <el-button type="primary" @click="handleUploadSubmit" :loading="formLoading">提交</el-button>
          <el-button @click="close">取消</el-button>
        </el-space>
      </template>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="auto">
        <el-form-item label="文件桶" prop="bucket">
          <el-input v-model="form.bucket" placeholder="请输入文件桶" />
        </el-form-item>
        <el-form-item label="文件路径" prop="filepath">
          <el-input v-model="form.filepath" placeholder="请输入文件路径" />
        </el-form-item>
        <el-form-item label="溯源标识" prop="traceIdentity">
          <el-input v-model="form.traceIdentity" placeholder="请输入溯源标识" />
        </el-form-item>
        <el-form-item label="文件状态" prop="status">
          <el-select v-model="form.status" placeholder="请选择状态">
            <el-option v-for="(value, key) in config.status" :key="key" :label="value" :value="key" />
          </el-select>
        </el-form-item>
        <el-form-item label="附加参数">
          <el-checkbox v-model="form.overwrite" label="覆盖已存在文件" />
          <el-checkbox v-model="form.sharable" label="可通过图片模式进行访问" />
        </el-form-item>
        <el-form-item label="上传文件">
          <FormUpload v-model="uploadFile" />
        </el-form-item>
      </el-form>
    </el-drawer>

  </section>
</template>

<style lang="scss" scoped>
</style>
