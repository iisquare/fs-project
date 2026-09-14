<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import type { FormInstance, TableInstance } from 'element-plus'
import { ElMessage } from 'element-plus'
import * as ElementPlusIcons from '@element-plus/icons-vue'
import RouteUtil from '@/utils/RouteUtil'
import DateUtil from '@/utils/DateUtil'
import FormUtil from '@/utils/FormUtil'
import { useRoute, useRouter } from 'vue-router'
import ApiUtil from '@/utils/ApiUtil'
import TableUtil from '@/utils/TableUtil'
import FormDataApi from '@/api/oa/FormDataApi'
import FormFrameApi from '@/api/oa/FormFrameApi'

const route = useRoute()
const router = useRouter()
const tableRef = ref<TableInstance>()
const loading = ref(false)
const searchable = ref(true)
const columns = ref([
  { prop: '_id', label: 'ID' },
  { prop: 'frameId', label: '所属表单', slot: 'frame' },
  { prop: 'bpmWorkflowIdName', label: '流程名称', slot: 'workflow' },
  { prop: 'bpmInstanceId', label: '流程实例' },
  { prop: 'bpmStartUserIdName', label: '发起人', slot: 'submitter' },
  { prop: 'createdTime', label: '创建时间', formatter: DateUtil.render },
  { prop: 'updatedTime', label: '修改时间', formatter: DateUtil.render },
])
const config = ref<any>({
  ready: false,
  sort: {},
})
const rows = ref([])
const filterRef = ref<FormInstance>()
const filters = ref(RouteUtil.query2filter(route, {}))
const pagination = ref(RouteUtil.pagination(filters.value))
const selection: any = ref([])

const infoVisible = ref(false)
const infoRow = ref<any>({})

const formVisible = ref(false)
const formLoading = ref(false)
const formRef = ref<FormInstance>()
const form = ref<any>({})
const formRules = {
  frameId: [{ required: true, message: '所属表单不能为空', trigger: 'blur' }],
}
const contentError = ref('')

// 内容为 JSON 文本，实时校验并给出提示，避免只能在提交时才报错
const validateContent = () => {
  const text = (form.value.content || '').trim()
  if (!text) {
    contentError.value = ''
    return true
  }
  try {
    JSON.parse(text)
    contentError.value = ''
    return true
  } catch (e: any) {
    contentError.value = `JSON 格式异常：${e.message}`
    return false
  }
}

watch(() => form.value.content, () => {
  validateContent()
})

const handleFormatContent = () => {
  try {
    const data = JSON.parse((form.value.content || '').trim() || '{}')
    form.value.content = JSON.stringify(data, null, 2)
    contentError.value = ''
  } catch (e: any) {
    ElMessage.warning(`JSON 格式异常：${e.message}`)
  }
}

const handleRefresh = (filter2query: boolean, keepPage: boolean) => {
  tableRef.value?.clearSelection()
  Object.assign(filters.value, RouteUtil.pagination2filter(pagination.value, keepPage))
  filter2query && RouteUtil.filter2query(route, router, filters.value)
  loading.value = true
  FormDataApi.list(filters.value).then((result: any) => {
    RouteUtil.result2pagination(pagination.value, result)
    rows.value = result.data.rows
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}

onMounted(() => {
  handleRefresh(false, true)
  FormDataApi.config().then((result: any) => {
    Object.assign(config.value, { ready: true }, ApiUtil.data(result))
  }).catch(() => {})
})

const handleOpenForm = (record: any) => {
  const location = router.resolve({
    path: '/oa/form/list',
    query: { id: record.frameId }
  })
  window.open(location.href)
}

const handleShow = (scope: any) => {
  const record = scope.row
  infoRow.value = Object.assign({}, record, {
    content: record.content ? JSON.stringify(record.content, null, 2) : '',
  })
  infoVisible.value = true
}

const handleCopyContent = () => {
  FormUtil.copyToClipboard(String(infoRow.value.content || ''))
    .then(() => ElMessage.success('数据内容已复制到剪贴板'))
    .catch(() => ElMessage.warning('复制失败，请手动选择复制'))
}

const handleAdd = () => {
  form.value = { content: '{}' }
  contentError.value = ''
  formVisible.value = true
}

const handleEdit = (scope: any) => {
  const record = scope.row
  form.value = Object.assign({}, record, {
    content: record.content ? JSON.stringify(record.content, null, 2) : '',
  })
  contentError.value = ''
  formVisible.value = true
}

const handleSubmit = () => {
  formRef.value?.validate((valid: boolean) => {
    if (!valid || formLoading.value) return
    if (!validateContent()) {
      ElMessage.error(contentError.value)
      return
    }
    let data: any = {}
    try {
      data = Object.assign({}, form.value, {
        content: JSON.parse((form.value.content || '').trim() || '{}')
      })
    } catch (e: any) {
      ElMessage.error(`数据格式异常：${e.message}`)
      return
    }
    formLoading.value = true
    FormDataApi.save(data, { success: true }).then(() => {
      formVisible.value = false
      handleRefresh(false, true)
    }).catch(() => {}).finally(() => {
      formLoading.value = false
    })
  })
}

const handleDelete = () => {
  TableUtil.selection(selection.value).then((ids: any) => {
    loading.value = true
    FormDataApi.delete(ids, { success: true }).then(() => {
      handleRefresh(false, true)
    }).catch(() => {
      loading.value = false
    })
  }).catch(() => {})
}
</script>

<template>
  <el-card :bordered="false" shadow="never" class="fs-table-search" v-show="searchable">
    <form-search ref="filterRef" :model="filters">
      <form-search-item label="所属表单" prop="frameId">
        <form-select v-model="filters.frameId" :callback="FormFrameApi.list" clearable placeholder="输入表单名称检索" />
      </form-search-item>
      <form-search-item label="流程模型" prop="bpmWorkflowId">
        <el-input v-model="filters.bpmWorkflowId" clearable />
      </form-search-item>
      <form-search-item label="流程实例" prop="bpmInstanceId">
        <el-input v-model="filters.bpmInstanceId" clearable />
      </form-search-item>
      <form-search-item label="发起人" prop="bpmStartUserId">
        <el-input v-model="filters.bpmStartUserId" clearable />
      </form-search-item>
      <form-search-item label="创建用户" prop="createdUid">
        <el-input v-model="filters.createdUid" clearable />
      </form-search-item>
      <form-search-item label="修改用户" prop="updatedUid">
        <el-input v-model="filters.updatedUid" clearable />
      </form-search-item>
      <form-search-item label="排序" prop="sort">
        <el-select v-model="filters.sort" placeholder="请选择" clearable>
          <el-option v-for="(value, key) in config.sort" :key="key" :value="key" :label="value" />
        </el-select>
      </form-search-item>
      <form-search-item label="创建开始" prop="createdTimeBegin">
        <form-date-picker v-model="filters.createdTimeBegin" placeholder="开始时间" />
      </form-search-item>
      <form-search-item label="创建结束" prop="createdTimeEnd">
        <form-date-picker v-model="filters.createdTimeEnd" placeholder="结束时间" />
      </form-search-item>
      <form-search-item label="修改开始" prop="updatedTimeBegin">
        <form-date-picker v-model="filters.updatedTimeBegin" placeholder="开始时间" />
      </form-search-item>
      <form-search-item label="修改结束" prop="updatedTimeEnd">
        <form-date-picker v-model="filters.updatedTimeEnd" placeholder="结束时间" />
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
        <button-add v-permit="'oa:formData:add'" @click="handleAdd" />
        <button-delete v-permit="'oa:formData:delete'" :disabled="selection.length === 0" @click="handleDelete" />
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
      :row-key="(record: any) => record._id"
      :border="true"
      v-loading="loading"
      table-layout="auto"
      @selection-change="(newSelection: any) => selection = newSelection"
    >
      <el-table-column type="selection" />
      <TableColumn :columns="columns">
        <template #frame="scope">
          <el-link type="primary" @click="handleOpenForm(scope.row)">{{ scope.row.frameId }} - {{ scope.row.frameIdName }}</el-link>
        </template>
        <template #workflow="scope">
          {{ scope.row.bpmWorkflowIdName || scope.row.bpmWorkflowId }}
        </template>
        <template #submitter="scope">
          {{ scope.row.bpmStartUserIdName || scope.row.bpmStartUserId }}
        </template>
      </TableColumn>
      <el-table-column label="操作">
        <template #default="scope">
          <el-button link v-permit="'oa:formData:'" @click="() => handleShow(scope)">查看</el-button>
          <el-button link v-permit="'oa:formData:modify'" @click="() => handleEdit(scope)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>
    <TablePagination v-model="pagination" @change="handleRefresh(true, true)" />
  </el-card>

  <el-dialog v-model="infoVisible" :title="`信息查看 - ${infoRow._id}`" width="700">
    <el-descriptions :column="2" border size="small">
      <el-descriptions-item label="所属表单">{{ infoRow.frameId }} - {{ infoRow.frameIdName }}</el-descriptions-item>
      <el-descriptions-item label="流程名称">{{ infoRow.bpmWorkflowIdName || infoRow.bpmWorkflowId }}</el-descriptions-item>
      <el-descriptions-item label="流程实例">{{ infoRow.bpmInstanceId }}</el-descriptions-item>
      <el-descriptions-item label="发起人">{{ infoRow.bpmStartUserIdName || infoRow.bpmStartUserId }}</el-descriptions-item>
      <el-descriptions-item label="创建者">{{ infoRow.createdUidName }}</el-descriptions-item>
      <el-descriptions-item label="创建时间">{{ DateUtil.format(infoRow.createdTime) }}</el-descriptions-item>
      <el-descriptions-item label="修改者">{{ infoRow.updatedUidName }}</el-descriptions-item>
      <el-descriptions-item label="修改时间">{{ DateUtil.format(infoRow.updatedTime) }}</el-descriptions-item>
    </el-descriptions>
    <div class="content-view">
      <div class="content-view__head">
        <span>数据内容</span>
        <el-button link type="primary" :icon="ElementPlusIcons.CopyDocument" @click="handleCopyContent" v-if="infoRow.content">复制</el-button>
      </div>
      <pre class="content-view__body" v-if="infoRow.content">{{ infoRow.content }}</pre>
      <el-empty description="暂无数据内容" :image-size="60" v-else />
    </div>
  </el-dialog>

  <el-dialog
    v-model="formVisible"
    :title="form._id ? `信息修改 - ${form._id}` : '信息添加'"
    :close-on-click-modal="false"
    width="700">
    <el-form ref="formRef" :model="form" :rules="formRules" label-width="80px">
      <el-form-item>
        <el-alert title="内容中的预留字段，以表单输入为准！" type="info" show-icon :closable="false" />
      </el-form-item>
      <el-form-item label="ID" prop="_id">
        <el-input v-model="form._id" autocomplete="off" :disabled="!!form._id" placeholder="留空由数据库自动生成" />
      </el-form-item>
      <el-form-item label="所属表单" prop="frameId">
        <form-select v-model="form.frameId" :callback="FormFrameApi.list" clearable placeholder="输入表单名称检索" />
      </el-form-item>
      <el-form-item label="流程模型" prop="bpmWorkflowId">
        <el-input v-model="form.bpmWorkflowId" autocomplete="off" />
      </el-form-item>
      <el-form-item label="流程实例" prop="bpmInstanceId">
        <el-input v-model="form.bpmInstanceId" autocomplete="off" />
      </el-form-item>
      <el-form-item label="发起人" prop="bpmStartUserId">
        <el-input v-model="form.bpmStartUserId" autocomplete="off" />
      </el-form-item>
      <el-form-item label="内容" prop="content">
        <div class="content-edit">
          <el-input type="textarea" v-model="form.content" :rows="12" placeholder="JSON 格式的表单数据，例如 {&quot;field&quot;: &quot;value&quot;}" />
          <div class="content-edit__foot">
            <el-button link type="primary" :icon="ElementPlusIcons.MagicStick" @click="handleFormatContent">格式化</el-button>
            <span class="content-edit__error" v-if="contentError">{{ contentError }}</span>
          </div>
        </div>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="formVisible = false">取消</el-button>
      <el-button type="primary" @click="handleSubmit" :loading="formLoading">确定</el-button>
    </template>
  </el-dialog>
</template>

<style lang="scss" scoped>
.content-view {
  margin-top: 16px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 4px;
  overflow: hidden;
  &__head {
    height: 38px;
    padding: 0 12px;
    background: var(--fs-layout-background-color);
    border-bottom: 1px solid var(--el-border-color-lighter);
    @include flex-between();
    font-size: 13px;
    color: var(--el-text-color-regular);
  }
  &__body {
    max-height: 360px;
    margin: 0;
    padding: 12px;
    overflow: auto;
    font-family: Consolas, Monaco, 'Courier New', monospace;
    font-size: 12px;
    line-height: 20px;
    color: var(--el-text-color-primary);
    white-space: pre-wrap;
    word-break: break-all;
  }
}
.content-edit {
  width: 100%;
  &__foot {
    margin-top: 4px;
    @include flex-start();
    gap: 12px;
    line-height: 20px;
  }
  &__error {
    font-size: 12px;
    color: var(--el-color-danger);
  }
}
</style>
