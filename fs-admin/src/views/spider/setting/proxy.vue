<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import type { FormInstance } from 'element-plus'
import { ElMessage } from 'element-plus'
import ProxyApi from '@/api/spider/ProxyApi'
import ApiUtil from '@/utils/ApiUtil'
import DateUtil from '@/utils/DateUtil'
import ElementUtil from '@/utils/ElementUtil'

const active = ref('channel')
const channelLoading = ref(false)
const groupLoading = ref(false)
const statsLoading = ref(false)
const channels = ref<any[]>([])
const groups = ref<any[]>([])
const stats = ref<any[]>([])
const statsStatus = ref<any>({})
const statsDimension = ref('group')
const statsDate = ref('')
const channelConfig: any = ref({
  status: {},
  sorts: {},
  adapters: {},
  protocols: {},
  templates: {},
})
const groupConfig: any = ref({ status: {}, strategies: {} })
const channelVisible = ref(false)
const groupVisible = ref(false)
const channelSaving = ref(false)
const groupSaving = ref(false)
const channelFormRef = ref<FormInstance>()
const groupFormRef = ref<FormInstance>()
const channelForm: any = ref({})
const groupForm: any = ref({})
const channelAdvanced = ref(false)
const channelCredentialsJson = ref('{}')
const channelConfigJson = ref('{}')
const channelResourcesText = ref('')
const channelTesting = ref(false)
const channelTestVisible = ref(false)
const channelTestResult: any = ref({})
const channelRules = {
  name: [{ required: true, message: '请输入渠道名称', trigger: 'blur' }],
  adapter: [{ required: true, message: '请选择接入组件', trigger: 'change' }],
}
const groupRules = {
  name: [{ required: true, message: '请输入分组名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入分组标识', trigger: 'blur' }],
  channelId: [{ required: true, message: '请选择代理渠道', trigger: 'change' }],
}
const staticChannel = computed(() => channelForm.value.adapter === 'static')
const selectedChannel = computed(() => channels.value.find((item) => item.id === groupForm.value.channelId))
const channelSummary = computed(() => {
  const protocol = (channelForm.value.protocol || 'http').toUpperCase()
  if (staticChannel.value) return `静态资源池 · ${protocol}`
  const adapter = channelConfig.value.adapters?.[channelForm.value.adapter] || '渠道 API'
  return `${adapter} · ${protocol}`
})

const groupColumns = [
  { prop: 'id', label: 'ID' },
  { prop: 'name', label: '分组名称' },
  { prop: 'code', label: '分组标识' },
  { prop: 'channelInfo.name', label: '代理渠道' },
  { prop: 'protocol', label: '协议' },
  { prop: 'strategyText', label: '调度策略' },
  { prop: 'region', label: '区域参数' },
  { prop: 'leaseSeconds', label: '租约(秒)' },
  { prop: 'stickySeconds', label: '粘性(秒)' },
  { prop: 'concurrency', label: '最大租约' },
  { prop: 'statusText', label: '状态' },
]

const loadChannels = () => {
  channelLoading.value = true
  ProxyApi.channelList({ page: 1, limit: 1000 }).then((result: any) => {
    channels.value = ApiUtil.data(result)?.rows || []
  }).catch(() => {}).finally(() => {
    channelLoading.value = false
  })
}

const decorateGroups = (rows: any[]) => rows.map((row) => ({
  ...row,
  strategyText: groupConfig.value.strategies[row.strategy] || row.strategy,
}))

const loadGroups = () => {
  groupLoading.value = true
  ProxyApi.groupList({ page: 1, limit: 1000 }).then((result: any) => {
    groups.value = decorateGroups(ApiUtil.data(result)?.rows || [])
  }).catch(() => {}).finally(() => {
    groupLoading.value = false
  })
}

const loadStats = () => {
  statsLoading.value = true
  ProxyApi.stats({
    dimension: statsDimension.value,
    date: statsDate.value || undefined,
  }).then((result: any) => {
    const data = ApiUtil.data(result)
    if (statsDimension.value === 'status') {
      statsStatus.value = data || {}
      stats.value = []
    } else {
      stats.value = data || []
      statsStatus.value = {}
    }
  }).catch(() => {}).finally(() => {
    statsLoading.value = false
  })
}

const handleAddChannel = () => {
  const config = clone(channelConfig.value.templates?.static || {})
  channelForm.value = {
    adapter: 'static',
    protocol: 'http',
    status: 1,
    sort: 0,
    credentials: {
      token: '',
      username: '',
      password: '',
      headers: {},
    },
    config: normalizeProviderObjects(config),
  }
  channelResourcesText.value = resourceLines(config.resources)
  channelAdvanced.value = false
  channelTestResult.value = {}
  channelVisible.value = true
}

const handleEditChannel = (row: any) => {
  const credentials = Object.assign({
    token: '',
    username: '',
    password: '',
    headers: {},
  }, toObject(row.credentials))
  const config = Object.assign(
    clone(channelConfig.value.templates?.[row.adapter] || {}),
    toObject(row.config),
  )
  channelForm.value = {
    ...row,
    credentials,
    config: normalizeProviderObjects(config),
  }
  channelResourcesText.value = resourceLines(config.resources)
  channelAdvanced.value = false
  channelTestResult.value = {}
  channelVisible.value = true
}

const handleAdapterChange = (adapter: string) => {
  channelForm.value.config = Object.assign(
    clone(channelConfig.value.templates?.[adapter] || {}),
    channelForm.value.config || {},
  )
  channelForm.value.config = normalizeProviderObjects(channelForm.value.config)
  if (adapter === 'static') channelResourcesText.value = resourceLines(channelForm.value.config.resources)
}

const handleSaveChannel = () => {
  channelFormRef.value?.validate((valid) => {
    if (!valid || channelSaving.value) return
    const payload = channelPayload()
    if (!payload) return
    channelSaving.value = true
    ProxyApi.channelSave(payload, { success: true }).then(() => {
      channelVisible.value = false
      loadChannels()
      loadGroups()
    }).catch(() => {}).finally(() => {
      channelSaving.value = false
    })
  })
}

const handleToggleAdvanced = (advanced: string | number | boolean) => {
  if (Boolean(advanced)) {
    channelCredentialsJson.value = JSON.stringify(channelForm.value.credentials || {}, null, 2)
    channelConfigJson.value = JSON.stringify(channelForm.value.config || {}, null, 2)
    return
  }
  try {
    channelForm.value.credentials = JSON.parse(channelCredentialsJson.value || '{}')
    channelForm.value.config = normalizeProviderObjects(JSON.parse(channelConfigJson.value || '{}'))
    channelResourcesText.value = resourceLines(channelForm.value.config.resources)
  } catch {
    ElMessage.error('高级配置不是合法 JSON')
    channelAdvanced.value = true
  }
}

const handleTestChannel = () => {
  const payload = channelPayload()
  if (!payload || channelTesting.value) return
  channelTesting.value = true
  ProxyApi.channelTest({
    ...payload,
    batchSize: 1,
    region: '',
  }, { loading: false }).then((result: any) => {
    channelTestResult.value = ApiUtil.data(result) || {}
    channelTestVisible.value = true
  }).catch(() => {}).finally(() => {
    channelTesting.value = false
  })
}

const handleTestChannelRow = (row: any) => {
  handleEditChannel(row)
  handleTestChannel()
}

const generateAccessKey = () => {
  const value = typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function'
    ? crypto.randomUUID().replace(/-/g, '')
    : Math.random().toString(36).slice(2) + Date.now().toString(36)
  groupForm.value.accessKey = value
}

const handleChannelChange = (channelId: number) => {
  const channel = channels.value.find((item) => item.id === channelId)
  if (channel?.protocol) groupForm.value.protocol = channel.protocol
}

const generateGroupCode = () => {
  groupForm.value.code = `proxy-${Date.now().toString(36)}`
}

const channelPayload = () => {
  let credentials = channelForm.value.credentials || {}
  let config = channelForm.value.config || {}
  if (channelAdvanced.value) {
    try {
      credentials = JSON.parse(channelCredentialsJson.value || '{}')
      config = JSON.parse(channelConfigJson.value || '{}')
    } catch {
      ElMessage.error('高级配置不是合法 JSON')
      return null
    }
  } else {
    credentials = clone(credentials)
    config = clone(config)
  }
  if (!channelAdvanced.value && channelForm.value.adapter === 'static') {
    config.resources = channelResourcesText.value
      .split(/\r?\n/)
      .map((item) => item.trim())
      .filter(Boolean)
  }
  return {
    ...channelForm.value,
    credentials,
    config,
  }
}

const toObject = (value: any) => {
  if (value && typeof value === 'object') return clone(value)
  try {
    return JSON.parse(value || '{}')
  } catch {
    return {}
  }
}

const clone = <T,>(value: T): T => JSON.parse(JSON.stringify(value ?? {}))

const resourceLines = (resources: any) => {
  if (Array.isArray(resources)) {
    return resources.map((item) => typeof item === 'string' ? item : JSON.stringify(item)).join('\n')
  }
  return typeof resources === 'string' ? resources : ''
}

const normalizeProviderObjects = (config: any) => {
  for (const key of ['params', 'headers', 'body']) {
    if (!config[key] || typeof config[key] !== 'object' || Array.isArray(config[key])) config[key] = {}
  }
  return config
}

const handleDeleteChannel = async (row: any) => {
  try {
    await ElementUtil.confirm(`确定删除代理渠道“${row.name}”？`)
    await ProxyApi.channelDelete([row.id], { success: true })
    loadChannels()
  } catch {}
}

const handleAddGroup = () => {
  const channel = channels.value.find((item) => item.status === 1)
  groupForm.value = {
    channelId: channel?.id,
    protocol: channel?.protocol || 'http',
    strategy: 'round_robin',
    batchSize: 1,
    leaseSeconds: 300,
    stickySeconds: 1800,
    maxUses: 100,
    concurrency: 0,
    connectTimeout: 10000,
    socketTimeout: 30000,
    failureThreshold: 3,
    status: 1,
    sort: 0,
  }
  groupVisible.value = true
}

const handleEditGroup = (row: any) => {
  groupForm.value = { ...row }
  groupVisible.value = true
}

const handleSaveGroup = () => {
  groupFormRef.value?.validate((valid) => {
    if (!valid || groupSaving.value) return
    groupSaving.value = true
    ProxyApi.groupSave(groupForm.value, { success: true }).then(() => {
      groupVisible.value = false
      loadGroups()
      loadStats()
    }).catch(() => {}).finally(() => {
      groupSaving.value = false
    })
  })
}

const handleDeleteGroup = async (row: any) => {
  try {
    await ElementUtil.confirm(`确定删除代理分组“${row.name}”？引用该分组的模板将无法申请代理。`)
    await ProxyApi.groupDelete([row.id], { success: true })
    loadGroups()
    loadStats()
  } catch {}
}

onMounted(async () => {
  try {
    const [channelResult, groupResult] = await Promise.all([
      ProxyApi.channelConfig(),
      ProxyApi.groupConfig(),
    ])
    Object.assign(channelConfig.value, ApiUtil.data(channelResult) || {})
    Object.assign(groupConfig.value, ApiUtil.data(groupResult) || {})
  } catch {}
  loadChannels()
  loadGroups()
  loadStats()
})
</script>

<template>
  <el-card :bordered="false" shadow="never" class="proxy-card">
    <el-tabs v-model="active">
      <el-tab-pane label="代理渠道" name="channel">
        <div class="tab-toolbar">
          <div class="tab-intro">
            <h3>代理渠道</h3>
            <p>维护静态代理池或渠道商提取接口，供多个代理分组复用。</p>
          </div>
          <el-space>
            <button-add v-permit="'spider:proxyChannel:add'" @click="handleAddChannel" />
            <button-refresh @click="loadChannels" :loading="channelLoading" />
          </el-space>
        </div>
        <el-table :data="channels" v-loading="channelLoading" border row-key="id">
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="name" label="名称" min-width="160" />
          <el-table-column prop="adapterText" label="接入组件" width="130" />
          <el-table-column prop="protocol" label="协议" width="90" />
          <el-table-column prop="endpoint" label="提取地址" min-width="260" show-overflow-tooltip />
          <el-table-column prop="statusText" label="状态" width="90" />
          <el-table-column prop="updatedTime" label="修改时间" width="170">
            <template #default="scope">{{ DateUtil.format(scope.row.updatedTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="190" fixed="right">
            <template #default="scope">
              <el-button link v-permit="'spider:proxyChannel:'" @click="handleTestChannelRow(scope.row)">测试</el-button>
              <el-button link v-permit="'spider:proxyChannel:modify'" @click="handleEditChannel(scope.row)">编辑</el-button>
              <el-button link type="danger" v-permit="'spider:proxyChannel:delete'" @click="handleDeleteChannel(scope.row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="代理分组" name="group">
        <div class="tab-toolbar">
          <div class="tab-intro">
            <h3>代理分组</h3>
            <p>按业务场景配置资源调度、租约和状态保持策略，模板直接引用分组。</p>
          </div>
          <el-space>
            <button-add v-permit="'spider:proxyGroup:add'" @click="handleAddGroup" />
            <button-refresh @click="loadGroups" :loading="groupLoading" />
          </el-space>
        </div>
        <el-table :data="groups" v-loading="groupLoading" border row-key="id">
          <el-table-column type="selection" width="48" />
          <TableColumn :columns="groupColumns" />
          <el-table-column label="操作" width="130" fixed="right">
            <template #default="scope">
              <el-button link v-permit="'spider:proxyGroup:modify'" @click="handleEditGroup(scope.row)">编辑</el-button>
              <el-button link type="danger" v-permit="'spider:proxyGroup:delete'" @click="handleDeleteGroup(scope.row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="运行统计" name="stats">
        <div class="tab-toolbar">
          <div class="tab-intro">
            <h3>运行统计</h3>
            <p>观察代理申请、成功率和租约占用，支持按分组、渠道、协议、资源和状态汇总。</p>
          </div>
          <button-refresh @click="loadStats" :loading="statsLoading" />
        </div>
        <div class="stats-toolbar">
          <el-space wrap>
            <el-radio-group v-model="statsDimension" @change="loadStats">
              <el-radio-button value="group">按分组</el-radio-button>
              <el-radio-button value="channel">按渠道</el-radio-button>
              <el-radio-button value="protocol">按协议</el-radio-button>
              <el-radio-button value="resource">按资源</el-radio-button>
              <el-radio-button value="status">服务状态</el-radio-button>
            </el-radio-group>
            <el-date-picker v-model="statsDate" type="date" value-format="YYYY-MM-DD" placeholder="统计日期" @change="loadStats" />
          </el-space>
        </div>
        <div v-if="statsDimension === 'status'" class="status-grid" v-loading="statsLoading">
          <el-card shadow="never"><span>分组总数</span><strong>{{ statsStatus.totalGroups || 0 }}</strong></el-card>
          <el-card shadow="never"><span>正常分组</span><strong class="success">{{ statsStatus.enabled || 0 }}</strong></el-card>
          <el-card shadow="never"><span>禁用分组</span><strong>{{ statsStatus.disabled || 0 }}</strong></el-card>
          <el-card shadow="never"><span>活动租约</span><strong>{{ statsStatus.active || 0 }}</strong></el-card>
        </div>
        <el-table v-else :data="stats" v-loading="statsLoading" border>
          <el-table-column prop="name" label="维度" min-width="180">
            <template #default="scope">{{ scope.row.name || scope.row.host || scope.row.id }}</template>
          </el-table-column>
          <el-table-column v-if="statsDimension === 'group'" prop="code" label="分组标识" />
          <el-table-column v-if="statsDimension === 'channel'" prop="adapter" label="接入组件" />
          <el-table-column v-if="statsDimension === 'resource'" prop="protocol" label="协议" />
          <el-table-column prop="total" label="申请次数" width="110" />
          <el-table-column prop="success" label="成功" width="90" />
          <el-table-column prop="failure" label="失败" width="90" />
          <el-table-column prop="active" label="活动租约" width="110" />
          <el-table-column prop="stateful" label="状态绑定" width="110" />
          <el-table-column prop="successRate" label="成功率" width="100">
            <template #default="scope">{{ scope.row.successRate }}%</template>
          </el-table-column>
          <el-table-column prop="avgLatency" label="平均耗时" width="110">
            <template #default="scope">{{ Math.round(scope.row.avgLatency) }} ms</template>
          </el-table-column>
          <el-table-column prop="lastTime" label="最近活跃" width="170">
            <template #default="scope">{{ DateUtil.format(scope.row.lastTime) }}</template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </el-card>

  <el-drawer
    v-model="channelVisible"
    :close-on-click-modal="false"
    :show-close="false"
    :destroy-on-close="true"
    size="78%"
    class="proxy-editor-drawer">
    <template #header="{ close, titleId, titleClass }">
      <div class="editor-header">
        <div class="editor-heading">
          <div class="editor-title-line">
            <h4 :id="titleId" :class="titleClass">{{ channelForm.id ? '编辑代理渠道' : '新增代理渠道' }}</h4>
            <el-tag effect="plain">{{ channelSummary }}</el-tag>
          </div>
          <p>配置渠道资源和提取参数，保存后可直接用于代理分组。</p>
        </div>
        <el-space :size="8">
          <el-button :loading="channelTesting" @click="handleTestChannel">测试提取</el-button>
          <el-button type="primary" :loading="channelSaving" @click="handleSaveChannel">保存</el-button>
          <el-button @click="close">关闭</el-button>
        </el-space>
      </div>
    </template>

    <el-form ref="channelFormRef" :model="channelForm" :rules="channelRules" label-position="top" class="proxy-config-form">
      <section class="config-section">
        <header class="section-heading">
          <div>
            <h5>基础信息</h5>
            <p>选择渠道类型后自动载入推荐配置，预置参数通常无需修改。</p>
          </div>
        </header>
        <div class="section-content">
          <el-row :gutter="20">
            <el-col :xs="24" :sm="12" :lg="8">
              <el-form-item label="渠道名称" prop="name">
                <el-input v-model="channelForm.name" maxlength="255" placeholder="例如：快代理-国内动态池" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12" :lg="8">
              <el-form-item label="接入组件" prop="adapter">
                <el-select v-model="channelForm.adapter" class="full-width" @change="handleAdapterChange">
                  <el-option v-for="(label, key) in channelConfig.adapters" :key="key" :value="key" :label="label" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12" :lg="4">
              <el-form-item label="默认协议">
                <el-select v-model="channelForm.protocol" class="full-width">
                  <el-option v-for="(label, key) in channelConfig.protocols" :key="key" :value="key" :label="label" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12" :lg="4">
              <el-form-item label="状态">
                <el-radio-group v-model="channelForm.status">
                  <el-radio v-for="(label, key) in channelConfig.status" :key="key" :value="Number(key)">{{ label }}</el-radio>
                </el-radio-group>
              </el-form-item>
            </el-col>
          </el-row>
        </div>
      </section>

      <section v-if="staticChannel" class="config-section">
        <header class="section-heading">
          <div>
            <h5>静态资源池</h5>
            <p>每行一个地址；分组申请代理时会从这里轮换选取。</p>
          </div>
          <el-tag type="info" effect="plain">支持用户名密码认证</el-tag>
        </header>
        <div class="section-content">
          <el-alert
            type="info"
            show-icon
            :closable="false"
            title="格式：host:port、host:port:username:password、http://username:password@host:port" />
          <el-form-item label="代理地址列表" class="resource-editor">
            <el-input
              v-model="channelResourcesText"
              type="textarea"
              :rows="12"
              resize="vertical"
              placeholder="127.0.0.1:8080&#10;127.0.0.1:8081:user:password&#10;socks5://user:password@127.0.0.1:1080" />
          </el-form-item>
        </div>
      </section>

      <template v-else>
        <section class="config-section">
          <header class="section-heading">
            <div>
              <h5>渠道认证</h5>
              <p>填写渠道商控制台提供的令牌或代理账号，敏感字段仅用于请求代理服务。</p>
            </div>
          </header>
          <div class="section-content">
            <el-row :gutter="20">
              <el-col :xs="24" :lg="12">
                <el-form-item label="API 令牌">
                  <el-input v-model="channelForm.credentials.token" placeholder="支持在地址中使用 ${token}" show-password />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :sm="12" :lg="6">
                <el-form-item label="代理账号">
                  <el-input v-model="channelForm.credentials.username" placeholder="可选" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :sm="12" :lg="6">
                <el-form-item label="代理密码">
                  <el-input v-model="channelForm.credentials.password" type="password" placeholder="可选" show-password />
                </el-form-item>
              </el-col>
              <el-col :span="24">
                <el-form-item label="认证请求头">
                  <metadata-table v-model="channelForm.credentials.headers" :editable="true" />
                  <div class="field-help">用于 Authorization、X-Api-Key 等请求头鉴权。</div>
                </el-form-item>
              </el-col>
            </el-row>
          </div>
        </section>

        <section class="config-section">
          <header class="section-heading">
            <div>
              <h5>提取接口</h5>
              <p>根据渠道商 API 文档填写地址、请求方式和响应位置。</p>
            </div>
          </header>
          <div class="section-content">
            <el-form-item label="提取地址">
              <el-input v-model="channelForm.endpoint" placeholder="https://api.example.com/get?token=${token}&num=${count}" />
              <div class="field-help">可用占位符：${token}、${count}、${protocol}、${region}、${username}、${password}</div>
            </el-form-item>
            <el-row :gutter="20">
              <el-col :xs="24" :sm="12" :lg="6">
                <el-form-item label="请求方式">
                  <el-select v-model="channelForm.config.method" class="full-width">
                    <el-option value="GET" label="GET" />
                    <el-option value="POST" label="POST" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :xs="24" :sm="12" :lg="6">
                <el-form-item label="响应格式">
                  <el-radio-group v-model="channelForm.config.responseType">
                    <el-radio value="json">JSON</el-radio>
                    <el-radio value="text">文本</el-radio>
                  </el-radio-group>
                </el-form-item>
              </el-col>
              <el-col :xs="24" :lg="8">
                <el-form-item label="资源路径">
                  <el-input v-model="channelForm.config.listPath" placeholder="/data/list" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :sm="12" :lg="4">
                <el-form-item label="代理有效期">
                  <el-input-number v-model="channelForm.config.expireSeconds" :min="60" class="full-width" />
                </el-form-item>
              </el-col>
              <el-col v-if="channelForm.config.responseType === 'text'" :xs="24" :sm="12">
                <el-form-item label="文本分隔符">
                  <el-input v-model="channelForm.config.textSeparator" placeholder="\r?\n" />
                </el-form-item>
              </el-col>
            </el-row>
          </div>
        </section>

        <section class="config-section collapse-section">
          <el-collapse>
            <el-collapse-item v-if="channelForm.config.responseType === 'json'" title="响应字段映射" name="mapping">
              <div class="collapse-tip">大多数预置渠道已自动配置，仅当渠道返回字段不同时修改。</div>
              <el-row :gutter="20">
                <el-col :xs="24" :sm="8"><el-form-item label="IP 字段"><el-input v-model="channelForm.config.hostField" placeholder="ip" /></el-form-item></el-col>
                <el-col :xs="24" :sm="8"><el-form-item label="端口字段"><el-input v-model="channelForm.config.portField" placeholder="port" /></el-form-item></el-col>
                <el-col :xs="24" :sm="8"><el-form-item label="协议字段"><el-input v-model="channelForm.config.protocolField" placeholder="protocol" /></el-form-item></el-col>
                <el-col :xs="24" :sm="8"><el-form-item label="账号字段"><el-input v-model="channelForm.config.usernameField" placeholder="username" /></el-form-item></el-col>
                <el-col :xs="24" :sm="8"><el-form-item label="密码字段"><el-input v-model="channelForm.config.passwordField" placeholder="password" /></el-form-item></el-col>
                <el-col :xs="24" :sm="8"><el-form-item label="过期字段"><el-input v-model="channelForm.config.expireField" placeholder="expire_at" /></el-form-item></el-col>
              </el-row>
            </el-collapse-item>
            <el-collapse-item title="附加请求参数与请求头" name="params">
              <div class="config-block-label">请求参数</div>
              <metadata-table v-model="channelForm.config.params" :editable="true" />
              <div class="config-block-label">请求头</div>
              <metadata-table v-model="channelForm.config.headers" :editable="true" />
              <template v-if="channelForm.config.method === 'POST'">
                <div class="config-block-label">POST 请求体</div>
                <metadata-table v-model="channelForm.config.body" :editable="true" />
              </template>
            </el-collapse-item>
          </el-collapse>
        </section>
      </template>

      <section class="config-section">
        <header class="section-heading">
          <div>
            <h5>备注与高级配置</h5>
            <p>仅特殊渠道需要编辑原始 JSON，普通配置请保持字段模式。</p>
          </div>
          <el-switch v-model="channelAdvanced" active-text="原始 JSON" inactive-text="字段配置" @change="handleToggleAdvanced" />
        </header>
        <div class="section-content">
          <template v-if="channelAdvanced">
            <el-form-item label="认证 JSON">
              <code-editor v-model="channelCredentialsJson" mode="javascript" :height="130" />
            </el-form-item>
            <el-form-item label="配置 JSON">
              <code-editor v-model="channelConfigJson" mode="javascript" :height="260" />
            </el-form-item>
          </template>
          <el-row :gutter="20">
            <el-col :xs="24" :sm="6">
              <el-form-item label="排序"><el-input-number v-model="channelForm.sort" :min="0" class="full-width" /></el-form-item>
            </el-col>
            <el-col :xs="24" :sm="18">
              <el-form-item label="备注"><el-input v-model="channelForm.description" placeholder="渠道用途、额度或负责人" /></el-form-item>
            </el-col>
          </el-row>
        </div>
      </section>

      <section v-if="channelTestVisible" class="config-section test-result">
        <header class="section-heading">
          <div>
            <h5>提取测试结果</h5>
            <p>共获取 {{ channelTestResult.count || 0 }} 个资源，当前展示前 {{ channelTestResult.sampled || 0 }} 个。</p>
          </div>
          <el-button link @click="channelTestVisible = false">收起结果</el-button>
        </header>
        <el-table :data="channelTestResult.resources || []" size="small" border>
          <el-table-column prop="protocol" label="协议" width="90" />
          <el-table-column prop="host" label="地址" />
          <el-table-column prop="port" label="端口" width="90" />
          <el-table-column label="认证" width="90">
            <template #default="scope">{{ scope.row.hasCredentials ? '已配置' : '无' }}</template>
          </el-table-column>
          <el-table-column label="过期时间" min-width="170">
            <template #default="scope">{{ DateUtil.format(scope.row.expireAt) }}</template>
          </el-table-column>
        </el-table>
      </section>
    </el-form>
  </el-drawer>

  <el-drawer
    v-model="groupVisible"
    :close-on-click-modal="false"
    :show-close="false"
    :destroy-on-close="true"
    size="72%"
    class="proxy-editor-drawer">
    <template #header="{ close, titleId, titleClass }">
      <div class="editor-header">
        <div class="editor-heading">
          <div class="editor-title-line">
            <h4 :id="titleId" :class="titleClass">{{ groupForm.id ? '编辑代理分组' : '新增代理分组' }}</h4>
            <el-tag v-if="selectedChannel" type="success" effect="plain">{{ selectedChannel.name }}</el-tag>
          </div>
          <p>统一管理该渠道下的资源选择、租约和状态保持策略。</p>
        </div>
        <el-space :size="8">
          <el-button type="primary" :loading="groupSaving" @click="handleSaveGroup">保存</el-button>
          <el-button @click="close">关闭</el-button>
        </el-space>
      </div>
    </template>

    <el-form ref="groupFormRef" :model="groupForm" :rules="groupRules" label-position="top" class="proxy-config-form">
      <section class="config-section">
        <header class="section-heading">
          <div>
            <h5>基础信息</h5>
            <p>分组是模板和代理渠道之间的资源边界，同一分组共享调度策略。</p>
          </div>
        </header>
        <div class="section-content">
          <el-row :gutter="20">
            <el-col :xs="24" :sm="12" :lg="8">
              <el-form-item label="分组名称" prop="name">
                <el-input v-model="groupForm.name" placeholder="例如：登录状态专用池" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12" :lg="8">
              <el-form-item label="分组标识" prop="code">
                <el-input v-model="groupForm.code" placeholder="account-pool">
                  <template #append><el-button @click="generateGroupCode">生成</el-button></template>
                </el-input>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12" :lg="4">
              <el-form-item label="状态">
                <el-select v-model="groupForm.status" class="full-width">
                  <el-option v-for="(label, key) in groupConfig.status" :key="key" :value="Number(key)" :label="label" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12" :lg="4">
              <el-form-item label="排序"><el-input-number v-model="groupForm.sort" :min="0" class="full-width" /></el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12">
              <el-form-item label="所属渠道" prop="channelId">
                <el-select v-model="groupForm.channelId" class="full-width" @change="handleChannelChange">
                  <el-option v-for="item in channels" :key="item.id" :value="item.id" :label="item.name" :disabled="item.status !== 1" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12" :lg="6">
              <el-form-item label="代理协议">
                <el-select v-model="groupForm.protocol" class="full-width">
                  <el-option v-for="(label, key) in channelConfig.protocols" :key="key" :value="key" :label="label" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :lg="6">
              <el-form-item label="区域参数">
                <el-input v-model="groupForm.region" placeholder="地区、运营商等渠道参数" />
              </el-form-item>
            </el-col>
          </el-row>
        </div>
      </section>

      <section class="config-section">
        <header class="section-heading">
          <div>
            <h5>资源调度</h5>
            <p>控制代理资源如何分配，以及单个分组允许同时占用多少租约。</p>
          </div>
        </header>
        <div class="section-content">
          <el-row :gutter="20">
            <el-col :xs="24" :sm="12" :lg="6">
              <el-form-item label="调度策略">
                <el-select v-model="groupForm.strategy" class="full-width">
                  <el-option v-for="(label, key) in groupConfig.strategies" :key="key" :value="key" :label="label" />
                </el-select>
                <div class="field-help">轮询稳定，随机分散，最少使用适合资源质量不均的场景。</div>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12" :lg="6">
              <el-form-item label="单次提取数量">
                <el-input-number v-model="groupForm.batchSize" :min="1" class="full-width" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12" :lg="6">
              <el-form-item label="最大活动租约">
                <el-input-number v-model="groupForm.concurrency" :min="0" class="full-width" />
                <div class="field-help">0 表示不额外限制。</div>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12" :lg="6">
              <el-form-item label="租约有效期">
                <el-input-number v-model="groupForm.leaseSeconds" :min="30" class="full-width" />
                <div class="field-help">单位：秒。</div>
              </el-form-item>
            </el-col>
          </el-row>
        </div>
      </section>

      <section class="config-section">
        <header class="section-heading">
          <div>
            <h5>状态保持与容错</h5>
            <p>有登录态的采集链路应使用粘性资源；失败达到阈值后会保留状态并更换代理。</p>
          </div>
        </header>
        <div class="section-content">
          <el-row :gutter="20">
            <el-col :xs="24" :sm="12" :lg="6">
              <el-form-item label="粘性资源时间">
                <el-input-number v-model="groupForm.stickySeconds" :min="60" class="full-width" />
                <div class="field-help">单位：秒。</div>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12" :lg="6">
              <el-form-item label="资源最大使用">
                <el-input-number v-model="groupForm.maxUses" :min="1" class="full-width" />
                <div class="field-help">单位：次。</div>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12" :lg="6">
              <el-form-item label="连续失败阈值">
                <el-input-number v-model="groupForm.failureThreshold" :min="1" class="full-width" />
                <div class="field-help">达到后自动换绑代理。</div>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12" :lg="6">
              <el-form-item label="连接超时">
                <el-input-number v-model="groupForm.connectTimeout" :min="1000" :step="1000" class="full-width" />
                <div class="field-help">单位：毫秒。</div>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12" :lg="6">
              <el-form-item label="执行超时">
                <el-input-number v-model="groupForm.socketTimeout" :min="1000" :step="1000" class="full-width" />
                <div class="field-help">单位：毫秒。</div>
              </el-form-item>
            </el-col>
          </el-row>
        </div>
      </section>

      <section class="config-section">
        <header class="section-heading">
          <div>
            <h5>对外服务</h5>
            <p>设置访问密钥后，其他系统可通过当前分组标识申请代理租约。</p>
          </div>
        </header>
        <div class="section-content">
          <el-form-item label="访问密钥">
            <el-input v-model="groupForm.accessKey" placeholder="留空则不开放外部调用" show-password>
              <template #append><el-button @click="generateAccessKey">生成密钥</el-button></template>
            </el-input>
            <div class="field-help">外部请求通过 X-Proxy-Access-Key 请求头携带该密钥。</div>
          </el-form-item>
          <el-form-item label="备注">
            <el-input v-model="groupForm.description" type="textarea" :rows="3" placeholder="用途、账号归属或额度说明" />
          </el-form-item>
        </div>
      </section>
    </el-form>
  </el-drawer>
</template>

<style lang="scss" scoped>
.proxy-card {
  margin-bottom: 20px;

  :deep(.el-card__body) {
    padding: 0 20px 20px;
  }

  :deep(.el-tabs__header) {
    margin-bottom: 18px;
  }
}

.tab-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 16px;
}

.tab-intro {
  min-width: 0;

  h3 {
    margin: 0;
    color: var(--el-text-color-primary);
    font-size: 16px;
    font-weight: 600;
  }

  p {
    margin: 4px 0 0;
    color: var(--el-text-color-secondary);
    font-size: 12px;
    line-height: 1.5;
  }
}

.stats-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
  padding: 12px 16px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  background: var(--el-fill-color-lighter);
}

.field-help {
  width: 100%;
  margin-top: 4px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.5;
}

.full-width {
  width: 100%;
}

.proxy-editor-drawer {
  :deep(.el-drawer__header) {
    margin: 0;
    padding: 18px 24px;
    border-bottom: 1px solid var(--el-border-color-lighter);
    background: var(--el-bg-color);
  }

  :deep(.el-drawer__body) {
    padding: 20px 24px 32px;
    background: var(--el-fill-color-lighter);
  }

  :deep(.el-drawer__close-btn) {
    display: none;
  }
}

.editor-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  gap: 24px;
}

.editor-heading {
  min-width: 0;

  h4 {
    margin: 0;
    color: var(--el-text-color-primary);
    font-size: 18px;
    font-weight: 600;
    line-height: 1.4;
  }

  p {
    margin: 4px 0 0;
    color: var(--el-text-color-secondary);
    font-size: 12px;
    line-height: 1.5;
  }
}

.editor-title-line {
  display: flex;
  align-items: center;
  gap: 10px;
}

.proxy-config-form {
  max-width: 1180px;
  margin: 0 auto;

  :deep(.el-form-item) {
    margin-bottom: 18px;
  }

  :deep(.el-form-item__label) {
    padding-bottom: 6px;
    color: var(--el-text-color-regular);
    font-weight: 500;
  }
}

.config-section {
  margin-bottom: 16px;
  overflow: hidden;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 10px;
  background: var(--el-bg-color);
}

.section-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 16px 20px;
  border-bottom: 1px solid var(--el-border-color-extra-light);
  background: linear-gradient(90deg, var(--el-fill-color-light), var(--el-bg-color));

  h5 {
    margin: 0;
    color: var(--el-text-color-primary);
    font-size: 14px;
    font-weight: 600;
    line-height: 1.4;
  }

  p {
    margin: 4px 0 0;
    color: var(--el-text-color-secondary);
    font-size: 12px;
    line-height: 1.5;
  }
}

.section-content {
  padding: 20px 20px 2px;
}

.resource-editor {
  margin-top: 16px;
}

.collapse-section {
  overflow: visible;

  :deep(.el-collapse) {
    border: 0;
  }

  :deep(.el-collapse-item__header) {
    min-height: 52px;
    padding: 0 20px;
    font-weight: 500;
  }

  :deep(.el-collapse-item__content) {
    padding: 16px 20px 2px;
  }
}

.collapse-tip {
  margin-bottom: 16px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.config-block-label {
  margin: 12px 0 8px;
  color: var(--el-text-color-regular);
  font-size: 13px;
  font-weight: 600;
}

.test-result {
  margin-top: 0;
}

.status-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(150px, 1fr));
  gap: 16px;

  .el-card :deep(.el-card__body) {
    display: flex;
    flex-direction: column;
    gap: 10px;
    color: var(--el-text-color-secondary);
  }

  strong {
    color: var(--el-text-color-primary);
    font-size: 28px;
  }

  .success {
    color: var(--el-color-success);
  }
}

@media (max-width: 900px) {
  .tab-toolbar {
    align-items: flex-start;
    flex-direction: column;
  }

  .status-grid {
    grid-template-columns: repeat(2, minmax(140px, 1fr));
  }

  .editor-header {
    align-items: flex-start;
    flex-direction: column;
    gap: 12px;
  }
}
</style>
