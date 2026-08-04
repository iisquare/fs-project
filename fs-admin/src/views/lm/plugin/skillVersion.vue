<script setup lang="ts">
import { onMounted, ref } from 'vue';
import * as ElementPlusIcons from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import type { FormInstance, TableInstance } from 'element-plus';
import RouteUtil from '@/utils/RouteUtil'
import { useRoute, useRouter } from 'vue-router';
import SkillVersionApi from '@/api/lm/SkillVersionApi';
import SkillApi from '@/api/lm/SkillApi';
import ApiUtil from '@/utils/ApiUtil';
import FormUpload from '@/components/Form/FormUpload.vue';
import DateUtil from '@/utils/DateUtil';
import TableUtil from '@/utils/TableUtil';

const route = useRoute()
const router = useRouter()
const tableRef = ref<TableInstance>()
const loading = ref(false)
const searchable = ref(true)
const skillInfo: any = ref(null)
const skillLoading = ref(true)
const columns = ref([
  { prop: 'id', label: 'ID' },
  { prop: 'name', label: '版本名称' },
  { prop: 'fileId', label: '文件标识', hide: true },
  { prop: 'filepath', label: '存储路径', hide: true },
  { prop: 'fileSize', label: '文件大小', slot: 'fileSize' },
  { prop: 'statusText', label: '状态' },
  { prop: 'description', label: '描述' },
])
const config = ref({
  ready: false,
  status: {},
})
const rows = ref([])
const filterRef = ref<FormInstance>()
const filters = ref(RouteUtil.query2filter(route, { advanced: false, skillId: route.query.skillId }))
const pagination = ref(RouteUtil.pagination(filters.value))
const selection: any = ref([])
const handleRefresh = (filter2query: boolean, keepPage: boolean) => {
  tableRef.value?.clearSelection()
  Object.assign(filters.value, RouteUtil.pagination2filter(pagination.value, keepPage))
  filter2query && RouteUtil.filter2query(route, router, filters.value)
  loading.value = true
  SkillVersionApi.list(filters.value).then((result: any) => {
    RouteUtil.result2pagination(pagination.value, result)
    rows.value = result.data.rows
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}
onMounted(() => {
  handleRefresh(false, true)
  SkillApi.config().then((result: any) => {
    Object.assign(config.value, { ready: true }, ApiUtil.data(result))
  }).catch(() => {})
  const skillId = route.query.skillId
  if (skillId) {
    skillLoading.value = true
    SkillApi.info(skillId).then((result: any) => {
      skillInfo.value = ApiUtil.data(result)
    }).catch(() => {}).finally(() => {
      skillLoading.value = false
    })
  } else {
    skillLoading.value = false
  }
})
const infoVisible = ref(false)
const formVisible = ref(false)
const formLoading = ref(false)
const form: any = ref({})

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
  if (formLoading.value) return
  formLoading.value = true
  SkillVersionApi.save(form.value, { success: true }).then(result => {
    handleRefresh(false, true)
    formVisible.value = false
  }).catch(() => {}).finally(() => {
    formLoading.value = false
  })
}
const handleDelete = () => {
  TableUtil.selection(selection.value).then((ids: any) => {
    loading.value = true
    SkillVersionApi.delete(ids, { success: true }).then(() => {
      handleRefresh(false, true)
    }).catch(() => {
      loading.value = false
    })
  }).catch(() => {})
}
const uploadVisible = ref(false)
const uploadForm = ref<any>({})
const uploadFile = ref<File>()
const uploadFormLoading = ref(false)

const handleOpenUpload = () => {
  uploadForm.value = { skillId: route.query.skillId, status: '1' }
  uploadFile.value = undefined
  uploadVisible.value = true
}
const handleUploadSubmit = () => {
  if (uploadFormLoading.value) return
  const file = uploadFile.value
  if (!file) {
    ElMessage.warning('请选择文件')
    return
  }
  uploadFormLoading.value = true
  const params: any = Object.assign({}, uploadForm.value, {
    skillId: route.query.skillId,
    file,
  })
  SkillVersionApi.upload(params, { success: true }).then(result => {
    handleRefresh(false, true)
    uploadVisible.value = false
  }).catch(() => {}).finally(() => {
    uploadFormLoading.value = false
  })
}
const baseURL = import.meta.env.VITE_APP_API_URL

const handleDownload = (record: any) => {
  window.open(baseURL + '/lm/skillVersion/download?id=' + encodeURIComponent(record.id))
}
const formatFileSize = (bytes: number) => {
  if (!bytes) return '-'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}
</script>

<template>
  <el-card :bordered="false" shadow="never" class="fs-table-search" v-show="searchable">
    <el-space>
      <LayoutBack to="/lm/plugin/skill" />
      <el-skeleton v-if="skillLoading" :rows="2" animated />
      <el-space v-else>
        <h3>{{ skillInfo?.name }}</h3>
        <el-tag v-for="item in skillInfo?.labels" :key="item">{{ item }}</el-tag>
      </el-space>
    </el-space>
    <el-divider />
    <form-search ref="filterRef" :model="filters">
      <form-search-item label="名称" prop="name">
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
        <el-button type="success" :icon="ElementPlusIcons.UploadFilled" v-permit="'lm:skill:add'" @click="handleOpenUpload">上传版本</el-button>
        <button-delete v-permit="'lm:skill:delete'" :disabled="selection.length === 0" @click="handleDelete" />
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
      @selection-change="(s: any) => selection = s"
    >
      <el-table-column type="selection" />
      <TableColumn :columns="columns">
        <template #fileSize="scope">
          {{ formatFileSize(scope.row.fileSize) }}
        </template>
      </TableColumn>
      <el-table-column label="操作">
        <template #default="scope">
          <el-button link @click="handleShow(scope)" v-permit="'lm:skill:'">查看</el-button>
          <el-button link @click="handleEdit(scope)" v-permit="'lm:skill:modify'">编辑</el-button>
          <el-button link @click="handleDownload(scope.row)" v-permit="'lm:skill:'" v-if="scope.row.fileId">下载</el-button>
        </template>
      </el-table-column>
    </el-table>
    <TablePagination v-model="pagination" @change="handleRefresh(true, true)" />
  </el-card>
  <el-drawer v-model="infoVisible" :title="'信息查看 - ' + form.id" size="60%">
    <el-descriptions border :column="2" label-width="80px">
      <el-descriptions-item label="版本名称" :span="2">{{ form.name }}</el-descriptions-item>
      <el-descriptions-item label="文件大小">{{ formatFileSize(form.fileSize) }}</el-descriptions-item>
      <el-descriptions-item label="状态">{{ form.statusText }}</el-descriptions-item>
      <el-descriptions-item label="技能">{{ form.skillInfo?.name }}</el-descriptions-item>
      <el-descriptions-item label="文件标识">{{ form.fileId }}</el-descriptions-item>
      <el-descriptions-item label="存储路径" :span="2">{{ form.filepath }}</el-descriptions-item>
      <el-descriptions-item label="描述" :span="2">{{ form.description ? form.description : '暂无' }}</el-descriptions-item>
      <el-descriptions-item label="创建者">{{ form.createdUserInfo?.name }}</el-descriptions-item>
       <el-descriptions-item label="创建时间">{{ DateUtil.format(form.createdTime) }}</el-descriptions-item>
      <el-descriptions-item label="修改者">{{ form.updatedUserInfo?.name }}</el-descriptions-item>
      <el-descriptions-item label="修改时间">{{ DateUtil.format(form.updatedTime) }}</el-descriptions-item>
    </el-descriptions>
  </el-drawer>
  <el-drawer v-model="formVisible" :close-on-click-modal="false" :show-close="false" :destroy-on-close="true" size="800px">
    <template #header="{ close, titleId, titleClass }">
      <h4 :id="titleId" :class="titleClass">{{ '信息' + (form.id ? ('修改 - ' + form.id) : '添加') }}</h4>
      <el-space>
        <el-button type="primary" @click="handleSubmit" :loading="formLoading">确定</el-button>
        <el-button @click="close">取消</el-button>
      </el-space>
    </template>
    <el-descriptions border :column="2" label-width="80px">
      <el-descriptions-item label="版本名称" :span="2"><el-input v-model="form.name" /></el-descriptions-item>
      <el-descriptions-item label="技能">{{ form.skillInfo?.name }}</el-descriptions-item>
      <el-descriptions-item label="状态">
        <el-select v-model="form.status" placeholder="请选择">
          <el-option v-for="(value, key) in config.status" :key="key" :value="key" :label="value" />
        </el-select>
      </el-descriptions-item>
      <el-descriptions-item label="文件标识"><el-input v-model="form.fileId" /></el-descriptions-item>
      <el-descriptions-item label="存储路径"><el-input v-model="form.filepath" /></el-descriptions-item>
      <el-descriptions-item label="描述" :span="2"><el-input type="textarea" v-model="form.description" /></el-descriptions-item>
    </el-descriptions>
  </el-drawer>
  <el-drawer v-model="uploadVisible" :close-on-click-modal="false" :show-close="false" :destroy-on-close="true" size="800px">
    <template #header="{ close, titleId, titleClass }">
      <h4 :id="titleId" :class="titleClass">上传版本</h4>
      <el-space>
        <el-button type="primary" @click="handleUploadSubmit" :loading="uploadFormLoading">确定</el-button>
        <el-button @click="close">取消</el-button>
      </el-space>
    </template>
    <el-descriptions border :column="2" label-width="80px">
      <el-descriptions-item label="版本名称"><el-input v-model="uploadForm.name" placeholder="请输入版本名称" /></el-descriptions-item>
      <el-descriptions-item label="状态">
        <el-select v-model="uploadForm.status" placeholder="请选择" clearable>
          <el-option v-for="(value, key) in config.status" :key="key" :value="key" :label="value" />
        </el-select>
      </el-descriptions-item>
      <el-descriptions-item label="文件" :span="2">
        <FormUpload v-model="uploadFile" />
      </el-descriptions-item>
      <el-descriptions-item label="描述" :span="2"><el-input type="textarea" v-model="uploadForm.description" placeholder="请输入版本描述" /></el-descriptions-item>
    </el-descriptions>
  </el-drawer>
</template>

<style lang="scss" scoped>
</style>
