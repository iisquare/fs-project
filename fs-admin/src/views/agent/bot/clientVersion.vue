<script setup lang="ts">
import { onMounted, ref } from 'vue'
import * as ElementPlusIcons from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, TableInstance } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import ClientVersionApi from '@/api/agent/ClientVersionApi'
import FormUpload from '@/components/Form/FormUpload.vue'
import ApiUtil from '@/utils/ApiUtil'
import DateUtil from '@/utils/DateUtil'
import RouteUtil from '@/utils/RouteUtil'
import TableUtil from '@/utils/TableUtil'

/**
 * 智能体客户端版本管理：每种系统 / 架构可上传多个安装包版本。
 * 客户端启动时取同一系统 / 架构下最高的启用版本，低于该版本配置的最低支持版本即强制升级。
 */

const route = useRoute()
const router = useRouter()
const tableRef = ref<TableInstance>()
const loading = ref(false)
const searchable = ref(true)
const selection: any = ref([])
const columns = ref([
  { prop: 'id', label: 'ID' },
  { prop: 'platformText', label: '系统' },
  { prop: 'architectureText', label: '架构' },
  { prop: 'channelText', label: '渠道' },
  { prop: 'version', label: '安装包版本', slot: 'version' },
  { prop: 'minimumVersion', label: '最低支持版本' },
  { prop: 'rolloutPercentage', label: '灰度比例', slot: 'rollout' },
  { prop: 'rollback', label: '回滚目标', slot: 'rollback' },
  { prop: 'deltaAvailable', label: '差分', slot: 'delta' },
  { prop: 'fileSize', label: '文件大小', slot: 'fileSize' },
  { prop: 'sha512', label: 'SHA-512', hide: true },
  { prop: 'statusText', label: '状态' },
  { prop: 'releaseNotes', label: '发布日志', slot: 'releaseNotes' },
  { prop: 'createdUserInfo', label: '上传人', slot: 'createdUser' },
  { prop: 'createdTime', label: '上传时间', slot: 'createdTime' }
])
const config = ref<any>({
  ready: false,
  sorts: {},
  status: {},
  platforms: {},
  architectures: {},
  channels: {}
})
const rows = ref([])
const filterRef = ref<FormInstance>()
const filters = ref<any>(RouteUtil.query2filter(route, { advanced: false }))
const pagination = ref(RouteUtil.pagination(filters.value))

const handleRefresh = (filter2query: boolean, keepPage: boolean) => {
  tableRef.value?.clearSelection()
  Object.assign(filters.value, RouteUtil.pagination2filter(pagination.value, keepPage))
  filter2query && RouteUtil.filter2query(route, router, filters.value)
  loading.value = true
  ClientVersionApi.list(filters.value).then((result: any) => {
    RouteUtil.result2pagination(pagination.value, result)
    rows.value = result.data.rows
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}

onMounted(() => {
  handleRefresh(false, true)
  ClientVersionApi.config().then((result: any) => {
    Object.assign(config.value, { ready: true }, ApiUtil.data(result))
  }).catch(() => {})
})

const infoVisible = ref(false)
const formVisible = ref(false)
const formLoading = ref(false)
const form = ref<any>({})
const formRef = ref<FormInstance>()
const rules = ref({
  minimumVersion: [{ required: true, message: '请输入最低支持版本', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
})

const handleShow = (scope: any) => {
  form.value = Object.assign({}, scope.row)
  infoVisible.value = true
}

const handleEdit = (scope: any) => {
  form.value = Object.assign({}, scope.row, {
    status: scope.row.status + '',
    rolloutPercentage: scope.row.rolloutPercentage ?? 100,
    allowRollback: scope.row.rollback === true
  })
  formVisible.value = true
}

const handleSubmit = () => {
  formRef.value?.validate((valid: boolean) => {
    if (!valid || formLoading.value) return
    formLoading.value = true
    ClientVersionApi.save(form.value, { success: true }).then(() => {
      handleRefresh(false, true)
      formVisible.value = false
    }).catch(() => {}).finally(() => {
      formLoading.value = false
    })
  })
}

const handleDelete = () => {
  TableUtil.selection(selection.value).then((ids: any) => {
    loading.value = true
    ClientVersionApi.delete(ids, { success: true }).then(() => {
      handleRefresh(false, true)
    }).catch(() => {
      loading.value = false
    })
  }).catch(() => {})
}

const handleStop = (scope: any) => {
  TableUtil.confirm(
    `确认紧急停用 v${scope.row.version}？停用后客户端会回退到下一个可用版本。`,
    '紧急停止发布'
  ).then(() => {
    ClientVersionApi.save(
      Object.assign({}, scope.row, {
        status: '2',
        allowRollback: scope.row.rollback === true
      }),
      { success: true }
    ).then(() => handleRefresh(false, true)).catch(() => {})
  }).catch(() => {})
}

const uploadVisible = ref(false)
const uploadForm = ref<any>({})
const uploadFile = ref<File>()
const uploadLoading = ref(false)
const uploadProgress = ref(0)
const uploadLoaded = ref(0)
const uploadTotal = ref(0)

const handleOpenUpload = () => {
  uploadForm.value = {
    platform: 'win32',
    architecture: 'x64',
    channel: 'stable',
    rolloutPercentage: 100,
    allowRollback: false,
    status: '1'
  }
  uploadFile.value = undefined
  uploadProgress.value = 0
  uploadLoaded.value = 0
  uploadTotal.value = 0
  uploadVisible.value = true
}

const handleUpload = () => {
  if (uploadLoading.value) return
  if (!uploadFile.value) {
    ElMessage.warning('请选择安装包文件')
    return
  }
  if (!uploadForm.value.version) {
    ElMessage.warning('请填写安装包版本')
    return
  }
  if (!uploadForm.value.minimumVersion) {
    ElMessage.warning('请填写最低支持版本')
    return
  }
  uploadLoading.value = true
  uploadProgress.value = 0
  uploadLoaded.value = 0
  uploadTotal.value = uploadFile.value.size
  ClientVersionApi.upload(Object.assign({}, uploadForm.value, {
    file: uploadFile.value
  }), { success: true }, {
    onUploadProgress: (event: { loaded: number; total?: number }) => {
      uploadLoaded.value = event.loaded
      uploadTotal.value = event.total || uploadFile.value?.size || 0
      uploadProgress.value = uploadTotal.value > 0
        ? Math.min(100, Math.round((uploadLoaded.value / uploadTotal.value) * 100))
        : 0
    }
  }).then(() => {
    handleRefresh(false, true)
    uploadVisible.value = false
  }).catch(() => {}).finally(() => {
    uploadLoading.value = false
  })
}

const baseURL = import.meta.env.VITE_APP_API_URL

const handleDownload = (record: any) => {
  window.open(baseURL + '/agent/clientVersion/download?id=' + encodeURIComponent(record.id))
}

const formatFileSize = (bytes: number) => {
  if (!bytes) return '-'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

const platformOf = (value: string) => config.value.platforms?.[value] || value || '-'
const architectureOf = (value: string) => config.value.architectures?.[value] || value || '-'
const channelOf = (value: string) => config.value.channels?.[value] || value || '-'

const openStats = () => {
  void router.push('/agent/bot/clientUpdateStatistic')
}
</script>

<template>
  <el-card :bordered="false" shadow="never" class="fs-table-search" v-show="searchable">
    <form-search ref="filterRef" :model="filters">
      <form-search-item label="版本号" prop="version">
        <el-input v-model="filters.version" clearable placeholder="支持模糊检索" />
      </form-search-item>
      <form-search-item label="系统" prop="platform">
        <el-select v-model="filters.platform" placeholder="请选择" clearable>
          <el-option v-for="(value, key) in config.platforms" :key="key" :value="key" :label="value" />
        </el-select>
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
      <form-search-item label="架构" prop="architecture">
        <el-select v-model="filters.architecture" placeholder="请选择" clearable>
          <el-option v-for="(value, key) in config.architectures" :key="key" :value="key" :label="value" />
        </el-select>
      </form-search-item>
      <form-search-item label="渠道" prop="channel">
        <el-select v-model="filters.channel" placeholder="请选择" clearable>
          <el-option v-for="(value, key) in config.channels" :key="key" :value="key" :label="value" />
        </el-select>
      </form-search-item>
    </form-search>
  </el-card>
  <el-card :bordered="false" shadow="never" class="fs-table-card">
    <div class="fs-table-toolbar flex-between">
      <el-space>
        <el-button
          type="success"
          :icon="ElementPlusIcons.UploadFilled"
          v-permit="'agent:clientVersion:add'"
          @click="handleOpenUpload"
        >
          上传安装包
        </el-button>
        <el-button @click="openStats">更新统计</el-button>
        <button-delete
          v-permit="'agent:clientVersion:delete'"
          :disabled="selection.length === 0"
          @click="handleDelete"
        />
      </el-space>
      <el-space>
        <button-search @click="searchable = !searchable" />
        <button-refresh @click="handleRefresh(true, true)" :loading="loading" />
        <TableColumnSetting v-model="columns" :table="tableRef" :loading="loading" />
        <TableSort
          v-model="filters.sort"
          :columns="columns"
          :sortable="config.sorts"
          :loading="loading"
          @change="handleRefresh(true, true)"
        />
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
        <template #version="scope">
          <el-link type="primary" underline="never" @click="handleShow(scope)">
            v{{ scope.row.version }}
          </el-link>
        </template>
        <template #fileSize="scope">{{ formatFileSize(scope.row.fileSize) }}</template>
        <template #rollout="scope">{{ scope.row.rolloutPercentage ?? 100 }}%</template>
        <template #rollback="scope">{{ scope.row.rollback ? '是' : '否' }}</template>
        <template #delta="scope">{{ scope.row.deltaAvailable ? '已生成' : '整包' }}</template>
        <template #releaseNotes="scope">
          <span class="text-ellipsis">{{ scope.row.releaseNotes || '暂无' }}</span>
        </template>
        <template #createdUser="scope">{{ scope.row.createdUserInfo?.name || '-' }}</template>
        <template #createdTime="scope">{{ DateUtil.format(scope.row.createdTime) }}</template>
      </TableColumn>
      <el-table-column label="操作">
        <template #default="scope">
          <el-button link @click="handleShow(scope)" v-permit="'agent:clientVersion:'">查看</el-button>
          <el-button link @click="handleEdit(scope)" v-permit="'agent:clientVersion:modify'">编辑</el-button>
          <el-button
            v-if="scope.row.status === 1"
            link
            type="danger"
            v-permit="'agent:clientVersion:modify'"
            @click="handleStop(scope)"
          >
            紧急停用
          </el-button>
          <el-button link @click="handleDownload(scope.row)" v-permit="'agent:clientVersion:'">下载</el-button>
        </template>
      </el-table-column>
    </el-table>
    <TablePagination v-model="pagination" :loading="loading" @change="handleRefresh(true, true)" />
  </el-card>

  <el-drawer v-model="infoVisible" :title="'信息查看 - ' + form.id" size="60%">
    <el-descriptions :column="2" label-width="110px" border>
      <el-descriptions-item label="系统">{{ platformOf(form.platform) }}</el-descriptions-item>
      <el-descriptions-item label="架构">{{ architectureOf(form.architecture) }}</el-descriptions-item>
      <el-descriptions-item label="渠道">{{ channelOf(form.channel) }}</el-descriptions-item>
      <el-descriptions-item label="灰度比例">{{ form.rolloutPercentage ?? 100 }}%</el-descriptions-item>
      <el-descriptions-item label="回滚目标">{{ form.rollback ? '是' : '否' }}</el-descriptions-item>
      <el-descriptions-item label="差分下载">{{ form.deltaAvailable ? '已生成' : '仅整包' }}</el-descriptions-item>
      <el-descriptions-item label="安装包版本">v{{ form.version }}</el-descriptions-item>
      <el-descriptions-item label="最低支持版本">v{{ form.minimumVersion }}</el-descriptions-item>
      <el-descriptions-item label="状态">{{ form.statusText }}</el-descriptions-item>
      <el-descriptions-item label="文件大小">{{ formatFileSize(form.fileSize) }}</el-descriptions-item>
      <el-descriptions-item label="文件名" :span="2">{{ form.fileName }}</el-descriptions-item>
      <el-descriptions-item label="SHA-512" :span="2">
        <span class="mono">{{ form.sha512 }}</span>
      </el-descriptions-item>
      <el-descriptions-item label="上传人">{{ form.createdUserInfo?.name || '-' }}</el-descriptions-item>
      <el-descriptions-item label="上传时间">{{ DateUtil.format(form.createdTime) }}</el-descriptions-item>
      <el-descriptions-item label="发布日志" :span="2">
        <pre class="release-notes">{{ form.releaseNotes || '暂无' }}</pre>
      </el-descriptions-item>
    </el-descriptions>
  </el-drawer>

  <el-drawer v-model="formVisible" :close-on-click-modal="false" :show-close="false" :destroy-on-close="true" size="60%">
    <template #header="{ close, titleId, titleClass }">
      <h4 :id="titleId" :class="titleClass">{{ '信息修改 - ' + form.id }}</h4>
      <el-space>
        <el-button type="primary" @click="handleSubmit" :loading="formLoading">确定</el-button>
        <el-button @click="close">取消</el-button>
      </el-space>
    </template>
    <el-form ref="formRef" :model="form" :rules="rules">
      <el-descriptions :column="2" label-width="110px" border>
        <el-descriptions-item label="系统">{{ platformOf(form.platform) }}</el-descriptions-item>
        <el-descriptions-item label="架构">{{ architectureOf(form.architecture) }}</el-descriptions-item>
        <el-descriptions-item label="渠道">
          <el-select v-model="form.channel">
            <el-option v-for="(value, key) in config.channels" :key="key" :value="key" :label="value" />
          </el-select>
        </el-descriptions-item>
        <el-descriptions-item label="灰度比例">
          <el-input-number v-model="form.rolloutPercentage" :min="0" :max="100" />
        </el-descriptions-item>
        <el-descriptions-item label="回滚目标">
          <div class="switch-field">
            <el-switch v-model="form.allowRollback" />
            <span class="field-tip">
              开启后允许高于此版本的客户端降级到该版本，仅用于紧急回滚。
            </span>
          </div>
        </el-descriptions-item>
        <el-descriptions-item label="安装包版本">v{{ form.version }}</el-descriptions-item>
        <el-descriptions-item label="最低支持版本">
          <el-input v-model="form.minimumVersion" placeholder="例如 1.2.0" />
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-select v-model="form.status" placeholder="请选择">
            <el-option v-for="(value, key) in config.status" :key="key" :value="key" :label="value" />
          </el-select>
        </el-descriptions-item>
        <el-descriptions-item label="SHA-512" :span="2">
          <span class="mono">{{ form.sha512 }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="发布日志" :span="2">
          <el-input v-model="form.releaseNotes" type="textarea" :rows="12" />
        </el-descriptions-item>
      </el-descriptions>
    </el-form>
  </el-drawer>

  <el-drawer v-model="uploadVisible" :close-on-click-modal="false" :show-close="false" :destroy-on-close="true" size="700px">
    <template #header="{ close, titleId, titleClass }">
      <h4 :id="titleId" :class="titleClass">上传安装包</h4>
      <el-space>
        <el-button type="primary" @click="handleUpload" :loading="uploadLoading">确定</el-button>
        <el-button @click="close">取消</el-button>
      </el-space>
    </template>
    <el-descriptions :column="2" label-width="110px" border>
      <el-descriptions-item label="客户端系统">
        <el-select v-model="uploadForm.platform">
          <el-option v-for="(value, key) in config.platforms" :key="key" :value="key" :label="value" />
        </el-select>
      </el-descriptions-item>
      <el-descriptions-item label="架构">
        <el-select v-model="uploadForm.architecture">
          <el-option v-for="(value, key) in config.architectures" :key="key" :value="key" :label="value" />
        </el-select>
      </el-descriptions-item>
      <el-descriptions-item label="发布渠道">
        <el-select v-model="uploadForm.channel">
          <el-option v-for="(value, key) in config.channels" :key="key" :value="key" :label="value" />
        </el-select>
      </el-descriptions-item>
      <el-descriptions-item label="灰度比例">
        <el-input-number v-model="uploadForm.rolloutPercentage" :min="0" :max="100" />
      </el-descriptions-item>
      <el-descriptions-item label="回滚目标">
        <div class="switch-field">
          <el-switch v-model="uploadForm.allowRollback" />
          <span class="field-tip">
            开启后允许高于此版本的客户端降级到该版本，仅用于紧急回滚；普通新版本不要开启。
          </span>
        </div>
      </el-descriptions-item>
      <el-descriptions-item label="安装包版本">
        <el-input v-model="uploadForm.version" placeholder="例如 1.3.0" />
      </el-descriptions-item>
      <el-descriptions-item label="最低支持版本">
        <el-input v-model="uploadForm.minimumVersion" placeholder="低于此版本时强制升级" />
      </el-descriptions-item>
      <el-descriptions-item label="状态">
        <el-select v-model="uploadForm.status">
          <el-option v-for="(value, key) in config.status" :key="key" :value="key" :label="value" />
        </el-select>
      </el-descriptions-item>
      <el-descriptions-item label="安装包" :span="2">
        <FormUpload
          v-model="uploadFile"
          :uploading="uploadLoading"
          :progress="uploadProgress"
          :loaded="uploadLoaded"
          :total="uploadTotal"
          processing-text="文件上传完成，服务端正在计算摘要并保存..."
        />
        <div class="format-tip">
          支持格式：Windows *-setup.exe、macOS dmg、Linux AppImage
        </div>
      </el-descriptions-item>
      <el-descriptions-item label="发布日志" :span="2">
        <el-input v-model="uploadForm.releaseNotes" type="textarea" :rows="10" placeholder="本版本的更新内容、修复项与注意事项" />
      </el-descriptions-item>
    </el-descriptions>
  </el-drawer>

</template>

<style lang="scss" scoped>
.text-ellipsis {
  display: inline-block;
  max-width: 320px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: middle;
}

.mono {
  font-family: Consolas, 'Courier New', monospace;
  word-break: break-all;
}

.release-notes {
  margin: 0;
  font: inherit;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}

.format-tip {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  margin-top: 8px;
  line-height: 1.6;
}

.switch-field {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  min-height: 32px;
}

.field-tip {
  max-width: 280px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.6;
}

</style>
