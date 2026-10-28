<script setup lang="ts">
import { onMounted, ref } from 'vue';
import type { FormInstance, TableInstance } from 'element-plus';
import { useRoute, useRouter } from 'vue-router';
import UserApi from '@/api/member/UserApi';
import CreditApi from '@/api/lm/CreditApi';
import RateApi from '@/api/lm/RateApi';
import ApiUtil from '@/utils/ApiUtil';
import DateUtil from '@/utils/DateUtil';
import ElementUtil from '@/utils/ElementUtil';
import RouteUtil from '@/utils/RouteUtil';
import TableUtil from '@/utils/TableUtil';

const route = useRoute()
const router = useRouter()
const tableRef = ref<TableInstance>()
const loading = ref(false)
const searchable = ref(true)
const columns = ref([
  { prop: 'uid', label: 'UID' },
  { prop: 'uidUserInfo.serial', label: '用户账号' },
  { prop: 'uidUserInfo.name', label: '用户昵称' },
  { prop: 'remained', label: '剩余积分' },
  { prop: 'consumed', label: '总消耗' },
  { prop: 'remindEnabled', label: '余额预警' },
  { prop: 'remindThreshold', label: '预警阈值' },
  { prop: 'rateIds', label: '速率限制', slot: 'rateIds' },
  { prop: 'sort', label: '排序' },
  { prop: 'statusText', label: '状态' },
  { prop: 'description', label: '描述', hide: true },
  { prop: 'createdTime', label: '创建时间', formatter: DateUtil.render, hide: true },
  { prop: 'updatedTime', label: '修改时间', formatter: DateUtil.render, hide: true },
])
const config = ref({
  ready: false,
  status: {},
  sorts: {},
})
const rows = ref([])
const filterRef = ref<FormInstance>()
const filters = ref(RouteUtil.query2filter(route))
const pagination = ref(RouteUtil.pagination(filters.value))
const selection: any = ref([])

const handleRefresh = (filter2query: boolean, keepPage: boolean) => {
  tableRef.value?.clearSelection()
  Object.assign(filters.value, RouteUtil.pagination2filter(pagination.value, keepPage))
  filter2query && RouteUtil.filter2query(route, router, filters.value)
  loading.value = true
  CreditApi.list(filters.value).then((result: any) => {
    RouteUtil.result2pagination(pagination.value, result)
    rows.value = result.data.rows
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}

onMounted(() => {
  handleRefresh(false, true)
  CreditApi.config().then((result: any) => {
    Object.assign(config.value, { ready: true }, ApiUtil.data(result))
  }).catch(() => {})
})

const infoVisible = ref(false)
const formVisible = ref(false)
const formLoading = ref(false)
const form: any = ref({})
const formRef = ref<FormInstance>()
const rules = ref({
  uid: [{ required: true, message: '请选择用户', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
})

const handleAdd = () => {
  form.value = {
    rateIds: [],
    status: '1',
  }
  formVisible.value = true
}

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
  formRef.value?.validate((valid: boolean) => {
    if (!valid || formLoading.value) return
    formLoading.value = true
    CreditApi.save(form.value, { success: true }).then(() => {
      handleRefresh(false, true)
      formVisible.value = false
    }).catch(() => {}).finally(() => {
      formLoading.value = false
    })
  })
}

const handleDelete = () => {
  TableUtil.selection(selection.value, 'uid').then((ids: any) => {
    loading.value = true
    CreditApi.delete(ids, { success: true }).then(() => {
      handleRefresh(false, true)
    }).catch(() => {
      loading.value = false
    })
  }).catch(() => {})
}

interface UsageLimit {
  rateId: number
  rateName: string
  type: string
  typeText: string
  limit: number
  used: number
  remained: number
  unit: string
  interval: number
  beginTime: number
  resetTime: number
}

interface UsageInfo {
  uid: number
  status: number
  statusText: string
  available: boolean
  remained: number
  consumed: number
  updatedTime: number
  limits: UsageLimit[]
}

const usageVisible = ref(false)
const usageLoading = ref(false)
const usageResetting = ref('')
const usageUid = ref<number>()
const usageUser = ref<any>()
const usage = ref<UsageInfo>()
let usageRequest = 0

const numberValue = (value: any) => {
  const number = Number(value)
  return Number.isFinite(number) ? number : 0
}

const normalizeUsage = (data: any): UsageInfo => {
  const source = data ?? {}
  return {
    uid: numberValue(source.uid),
    status: numberValue(source.status),
    statusText: String(source.statusText ?? ''),
    available: !!source.available,
    remained: numberValue(source.remained),
    consumed: numberValue(source.consumed),
    updatedTime: numberValue(source.updatedTime),
    limits: Array.isArray(source.limits) ? source.limits.map((item: any) => ({
      rateId: numberValue(item.rateId),
      rateName: String(item.rateName ?? ''),
      type: String(item.type ?? ''),
      typeText: String(item.typeText ?? ''),
      limit: numberValue(item.limit),
      used: numberValue(item.used),
      remained: numberValue(item.remained),
      unit: String(item.unit ?? ''),
      interval: numberValue(item.interval),
      beginTime: numberValue(item.beginTime),
      resetTime: numberValue(item.resetTime),
    })) : [],
  }
}

const loadUsage = (uid = usageUid.value) => {
  const id = numberValue(uid)
  if (id < 1) return
  const request = ++usageRequest
  usageLoading.value = true
  CreditApi.usage({ uid: id }).then((result: any) => {
    if (request === usageRequest) usage.value = normalizeUsage(ApiUtil.data(result))
  }).catch(() => {}).finally(() => {
    if (request === usageRequest) usageLoading.value = false
  })
}

const handleUsageUserChange = (uid: any) => {
  usage.value = undefined
  loadUsage(uid)
}

const openUsage = (scope?: any) => {
  usageVisible.value = true
  if (!scope) {
    usageRequest++
    usageLoading.value = false
    usageUid.value = undefined
    usageUser.value = undefined
    usage.value = undefined
    return
  }
  usageUid.value = numberValue(scope.row.uid)
  usageUser.value = scope.row.uidUserInfo
  usage.value = undefined
  loadUsage(usageUid.value)
}

const formatUsageValue = (value: any) => {
  return numberValue(value).toLocaleString('zh-CN', { maximumFractionDigits: 6 })
}

const formatInterval = (seconds: number) => {
  if (seconds <= 0) return '—'
  if (seconds % 86400 === 0) return `${seconds / 86400} 天`
  if (seconds % 3600 === 0) return `${seconds / 3600} 小时`
  if (seconds % 60 === 0) return `${seconds / 60} 分钟`
  return `${seconds} 秒`
}

const usagePercent = (item: any) => {
  if (item.limit <= 0) return 0
  return Math.min(100, Math.max(0, item.used / item.limit * 100))
}

const usageTone = (item: any) => {
  const percent = usagePercent(item)
  if (percent >= 90) return 'is-danger'
  if (percent >= 70) return 'is-warning'
  return ''
}

const resetUsage = (item?: any) => {
  if (!usage.value || usageResetting.value) return
  const uid = usage.value.uid
  const action = item
    ? `「${item.rateName || '限流策略'}」的${item.typeText}`
    : '该账户当前窗口的全部限流用量'
  ElementUtil.confirm(`确定重置${action}吗？此操作只清零当前限流计数，不会修改积分余额、总消耗或限流配置。`).then(() => {
    usageResetting.value = item ? `${item.rateId}-${item.type}` : 'all'
    CreditApi.reset({
      uid,
      rateId: item?.rateId ?? 0,
      type: item?.type ?? '',
    }, { success: true }).then(() => {
      if (numberValue(usageUid.value) === uid) loadUsage(uid)
    }).catch(() => {}).finally(() => {
      usageResetting.value = ''
    })
  }).catch(() => {})
}
</script>

<template>
  <el-card :bordered="false" shadow="never" class="fs-table-search" v-show="searchable">
    <form-search ref="filterRef" :model="filters">
      <form-search-item label="用户" prop="uid">
        <form-select v-model="filters.uid" :callback="UserApi.list" clearable />
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
        <button-add v-permit="'lm:credit:add'" @click="handleAdd" />
        <button-delete v-permit="'lm:credit:delete'" :disabled="selection.length === 0" @click="handleDelete" />
        <el-button v-permit="'lm:credit:'" @click="openUsage()">查看账户用量</el-button>
      </el-space>
      <el-space>
        <button-search @click="searchable = !searchable" />
        <button-refresh @click="handleRefresh(true, true)" :loading="loading" />
        <TableColumnSetting v-model="columns" :table="tableRef" :loading="loading" />
        <TableSort v-model="filters.sort" :columns="columns" :sortable="config.sorts" :loading="loading" @change="handleRefresh(true, true)" />
      </el-space>
    </div>
    <el-table
      ref="tableRef"
      :data="rows"
      :row-key="(record: any) => record.uid"
      :border="true"
      v-loading="loading"
      table-layout="auto"
      @selection-change="(s: any) => selection = s"
    >
      <el-table-column type="selection" />
      <TableColumn :columns="columns">
        <template #rateIds="scope">
          <el-space wrap>
            <el-space><el-tag v-for="item in scope.row.rates" :key="item.id">{{ item.name }}</el-tag></el-space>
          </el-space>
        </template>
      </TableColumn>
      <el-table-column label="操作" width="180">
        <template #default="scope">
          <el-button link @click="openUsage(scope)" v-permit="'lm:credit:'">用量</el-button>
          <el-button link @click="handleShow(scope)" v-permit="'lm:credit:'">查看</el-button>
          <el-button link @click="handleEdit(scope)" v-permit="'lm:credit:modify'">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>
    <TablePagination v-model="pagination" :loading="loading" @change="handleRefresh(true, true)" />
  </el-card>

  <el-drawer v-model="infoVisible" :title="'信息查看 - ' + form.uid" size="70%">
    <el-descriptions border :column="2">
      <el-descriptions-item label="UID">{{ form.uid }}</el-descriptions-item>
      <el-descriptions-item label="状态">{{ form.statusText }}</el-descriptions-item>
      <el-descriptions-item label="用户账号">{{ form.uidUserInfo?.serial }}</el-descriptions-item>
      <el-descriptions-item label="用户昵称">{{ form.uidUserInfo?.name }}</el-descriptions-item>
      <el-descriptions-item label="剩余积分">{{ form.remained }}</el-descriptions-item>
      <el-descriptions-item label="总消耗">{{ form.consumed }}</el-descriptions-item>
      <el-descriptions-item label="余额预警">{{ form.remindEnabled }}</el-descriptions-item>
      <el-descriptions-item label="预警阈值">{{ form.remindThreshold }}</el-descriptions-item>
      <el-descriptions-item label="排序">{{ form.sort }}</el-descriptions-item>
      <el-descriptions-item label="速率限制" :span="2">
        <el-space><el-tag v-for="item in form.rates" :key="item.id">{{ item.name }}</el-tag></el-space>
      </el-descriptions-item>
      <el-descriptions-item label="描述" :span="2">{{ form.description || '暂无' }}</el-descriptions-item>
      <el-descriptions-item label="创建者">{{ form.createdUserInfo?.name ?? form.createdUid }}</el-descriptions-item>
      <el-descriptions-item label="创建时间">{{ DateUtil.format(form.createdTime) }}</el-descriptions-item>
      <el-descriptions-item label="修改者">{{ form.updatedUserInfo?.name ?? form.updatedUid }}</el-descriptions-item>
      <el-descriptions-item label="修改时间">{{ DateUtil.format(form.updatedTime) }}</el-descriptions-item>
    </el-descriptions>
  </el-drawer>

  <el-drawer v-model="formVisible" :close-on-click-modal="false" :show-close="false" :destroy-on-close="true" size="70%">
    <template #header="{ close, titleId, titleClass }">
      <h4 :id="titleId" :class="titleClass">{{ (form.createdUid ? '积分调整' : '用户开通') }}</h4>
      <el-space>
        <el-button type="primary" @click="handleSubmit" :loading="formLoading">确定</el-button>
        <el-button @click="close">取消</el-button>
      </el-space>
    </template>
    <el-form ref="formRef" :model="form" :rules="rules" label-width="auto">
      <el-descriptions border :column="2">
        <el-descriptions-item label="用户">
          <form-select v-model="form.uid" :callback="UserApi.list" :disabled="!!form.createdUid" clearable />
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-select v-model="form.status" placeholder="请选择">
            <el-option v-for="(value, key) in config.status" :key="key" :value="key" :label="value" />
          </el-select>
        </el-descriptions-item>
        <el-descriptions-item label="剩余积分">
          <el-input v-model="form.remained" />
        </el-descriptions-item>
        <el-descriptions-item label="总消耗">
          <el-input v-model="form.consumed" />
        </el-descriptions-item>
        <el-descriptions-item label="余额预警">
           <el-checkbox v-model="form.remindEnabled">启用余额预警</el-checkbox>
        </el-descriptions-item>
        <el-descriptions-item label="预警阈值">
          <el-input v-model="form.remindThreshold" />
        </el-descriptions-item>
        <el-descriptions-item label="速率限制" :span="2">
          <form-select v-model="form.rateIds" :callback="RateApi.list" multiple clearable />
        </el-descriptions-item>
        <el-descriptions-item label="排序" :span="2">
          <el-input-number v-model="form.sort" :min="0" />
        </el-descriptions-item>
        <el-descriptions-item label="描述" :span="2">
          <el-input type="textarea" v-model="form.description" :rows="4" />
        </el-descriptions-item>
      </el-descriptions>
    </el-form>
  </el-drawer>

  <el-drawer v-model="usageVisible" title="账户用量与限流" :destroy-on-close="true" size="78%">
    <div class="usage-query">
      <span class="usage-query__label">指定账户</span>
      <form-select
        v-model="usageUid"
        v-model:selected="usageUser"
        :callback="UserApi.list"
        placeholder="搜索用户账号或昵称"
        clearable
        @change="handleUsageUserChange"
      />
      <el-button type="primary" :disabled="!usageUid || usageLoading" @click="loadUsage()">
        {{ usage ? '刷新用量' : '查询用量' }}
      </el-button>
    </div>

    <el-empty v-if="!usageUid" description="请选择一个账户查看当前限流用量" />
    <div v-else-if="usageLoading && !usage" v-loading="true" class="usage-loading"></div>
    <template v-else-if="usage">
      <div class="usage-account">
        <div>
          <strong>{{ usageUser?.name || usageUser?.serial || `用户 ${usage.uid}` }}</strong>
          <span v-if="usageUser?.serial">@{{ usageUser.serial }}</span>
          <span>UID {{ usage.uid }}</span>
        </div>
        <el-tag :type="usage.status === 1 ? 'success' : usage.status === 2 ? 'warning' : 'info'">
          {{ usage.statusText }}
        </el-tag>
      </div>

      <el-descriptions border :column="3" class="usage-overview">
        <el-descriptions-item label="剩余积分">{{ formatUsageValue(usage.remained) }}</el-descriptions-item>
        <el-descriptions-item label="总消耗">{{ formatUsageValue(usage.consumed) }}</el-descriptions-item>
        <el-descriptions-item label="数据时间">{{ DateUtil.format(usage.updatedTime) }}</el-descriptions-item>
      </el-descriptions>

      <el-alert
        v-if="!usage.available"
        class="usage-alert"
        :title="usage.status ? '该账户积分状态已停用，当前可能无法继续调用模型。' : '该账户尚未开通积分，暂无生效的限流策略。'"
        type="warning"
        :closable="false"
        show-icon
      />

      <div class="usage-section-head">
        <div>
          <h4>当前窗口用量</h4>
          <p>数值来自网关实际限流计数；窗口结束后会自动清零。</p>
        </div>
        <el-button
          v-permit="'lm:credit:modify'"
          type="danger"
          plain
          :disabled="!usage.limits.length || !!usageResetting"
          :loading="usageResetting === 'all'"
          @click="resetUsage()"
        >
          重置全部用量
        </el-button>
      </div>

      <el-table v-if="usage.limits.length" :data="usage.limits" border table-layout="auto" v-loading="usageLoading">
        <el-table-column prop="rateName" label="限流策略" min-width="150" />
        <el-table-column label="类型" width="110">
          <template #default="scope">
            <span class="usage-type" :class="'is-' + scope.row.type">
              <i></i>{{ scope.row.typeText }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="当期用量" min-width="220">
          <template #default="scope">
            <div class="usage-progress" :class="usageTone(scope.row)">
              <div class="usage-progress__numbers">
                <strong>{{ formatUsageValue(scope.row.used) }}</strong>
                <span>/ {{ formatUsageValue(scope.row.limit) }} {{ scope.row.unit }}</span>
              </div>
              <div class="usage-progress__track">
                <span :style="{ width: `${usagePercent(scope.row)}%` }"></span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="剩余" width="130">
          <template #default="scope">
            {{ formatUsageValue(scope.row.remained) }} {{ scope.row.unit }}
          </template>
        </el-table-column>
        <el-table-column label="限流周期" width="110">
          <template #default="scope">每 {{ formatInterval(scope.row.interval) }}</template>
        </el-table-column>
        <el-table-column label="下次重置" min-width="170">
          <template #default="scope">{{ DateUtil.format(scope.row.resetTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="scope">
            <el-button
              v-permit="'lm:credit:modify'"
              link
              type="danger"
              :disabled="!!usageResetting"
              :loading="usageResetting === `${scope.row.rateId}-${scope.row.type}`"
              @click="resetUsage(scope.row)"
            >
              重置
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-else description="该账户没有生效中的限流策略" />
    </template>
  </el-drawer>
</template>

<style lang="scss" scoped>
.usage-query {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  background: var(--el-fill-color-lighter);
  border-radius: 8px;

  .el-select {
    width: min(420px, 50vw);
  }
}

.usage-query__label {
  flex-shrink: 0;
  font-size: 13px;
  color: var(--el-text-color-regular);
}

.usage-loading {
  min-height: 260px;
}

.usage-account {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-top: 20px;

  > div {
    display: flex;
    align-items: baseline;
    gap: 10px;
  }

  strong {
    font-size: 18px;
    color: var(--el-text-color-primary);
  }

  span {
    font-size: 12px;
    color: var(--el-text-color-secondary);
  }
}

.usage-overview {
  margin-top: 14px;
}

.usage-alert {
  margin-top: 14px;
}

.usage-section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  margin: 22px 0 12px;

  h4 {
    font-size: 15px;
    color: var(--el-text-color-primary);
  }

  p {
    margin-top: 4px;
    font-size: 12px;
    color: var(--el-text-color-secondary);
  }
}

.usage-type {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--el-text-color-regular);

  i {
    width: 7px;
    height: 7px;
    background: var(--el-color-info);
    border-radius: 50%;
  }

  &.is-request i {
    background: var(--el-color-primary);
  }

  &.is-token i {
    background: var(--el-color-success);
  }

  &.is-credit i {
    background: var(--el-color-warning);
  }
}

.usage-progress__numbers {
  display: flex;
  align-items: baseline;
  gap: 5px;
  font-size: 12px;
  color: var(--el-text-color-secondary);

  strong {
    font-size: 14px;
    color: var(--el-text-color-primary);
    font-variant-numeric: tabular-nums;
  }
}

.usage-progress__track {
  height: 6px;
  margin-top: 7px;
  overflow: hidden;
  background: var(--el-fill-color-dark);
  border-radius: 999px;

  span {
    display: block;
    height: 100%;
    background: var(--el-color-primary);
    border-radius: inherit;
  }
}

.usage-progress.is-warning .usage-progress__track span {
  background: var(--el-color-warning);
}

.usage-progress.is-danger .usage-progress__track span {
  background: var(--el-color-danger);
}

@media (max-width: 760px) {
  .usage-query {
    align-items: stretch;
    flex-direction: column;

    .el-select {
      width: 100%;
    }
  }

  .usage-account,
  .usage-section-head {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
