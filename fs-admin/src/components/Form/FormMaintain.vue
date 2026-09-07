<script setup lang="ts">
/**
 * SSE 任务执行组件
 *
 * 用于触发服务端 SSE 流式维护任务，并将服务端推送的任务计划、执行步骤、
 * 待处理数量、执行进度、过程日志实时展示出来。每次点击执行会创建一个会话，
 * 每次执行会刷新当前任务面板，实时展示执行过程和结果。
 *
 * @prop {String}  title   - 按钮名称，默认“执行”
 * @prop {String}  type    - el-button 类型，默认 primary
 * @prop {Boolean} plain   - 是否使用朴素按钮，默认 false
 * @prop {String}  app     - 服务前缀，对应 FetchEventSource 的 app 参数，默认 bi
 * @prop {String}  uri     - SSE 请求地址，例如 /maintain/reload
 * @prop {String}  method  - 请求方式，默认 POST
 * @prop {Function}  params  - 请求参数回调，返回参数对象
 * @prop {Function}  headers - 额外请求头回调，返回请求头对象
 * @prop {Any}     api     - 可选的接口描述，支持以下形式：
 *                           1. 字符串：等价于 uri
 *                           2. 对象：{ app, uri, method, params, headers }
 *                           3. 函数：(params) => 返回上述对象
 * @prop {String}  height  - 日志面板高度，默认 240px
 *
 * 服务端 SSE 数据格式（data 行内容为 JSON 字符串）：
 * {
 *   "action": "plan | start | step | progress | log | result | error",
 *   "code": 0,
 *   "message": "过程描述",
 *   "step": "步骤标识",
 *   "progress": 0-100,
 *   "total": 0,
 *   "level": "info | success | warning | error",  // 仅 log 事件
 *   "data": {}                                     // plan/result/error 事件
 * }
 */
import { computed, onBeforeUnmount, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import * as ElementPlusIcons from '@element-plus/icons-vue';
import FetchEventSource from '@/core/FetchEventSource';
import DateUtil from '@/utils/DateUtil';
import { useUserStore } from '@/stores/user';

const props = withDefaults(defineProps<{
  title?: string
  description?: string
  type?: '' | 'default' | 'primary' | 'success' | 'warning' | 'info' | 'danger' | 'text'
  plain?: boolean
  app?: string
  uri?: string
  method?: string
  params?: () => Record<string, any>
  headers?: () => Record<string, any>
  api?: any
  height?: string
}>(), {
  title: '执行',
  description: '',
  type: 'primary',
  plain: false,
  app: 'bi',
  uri: '',
  method: 'POST',
  params: () => ({}),
  headers: () => ({}),
  api: null,
  height: '240px',
})

const emit = defineEmits<{
  start: [session: any]
  message: [session: any, message: any]
  finish: [session: any, result: any]
  error: [session: any, error: any]
}>()

const user = useUserStore()
const active = ref<any>(null)

const activeRunning = computed(() => !!active.value?.running)
const buttonText = computed(() => activeRunning.value ? '停止' : props.title)
const statusType = computed(() => {
  switch (active.value?.status) {
    case 'success': return 'success'
    case 'warning': return 'warning'
    case 'error': return 'danger'
    case 'running': return 'primary'
    default: return 'info'
  }
})
const statusText = computed(() => {
  switch (active.value?.status) {
    case 'success': return '执行成功'
    case 'warning': return '执行完成（有告警）'
    case 'error': return '执行失败'
    case 'running': return '执行中'
    default: return '未开始'
  }
})
const progressStatus = computed(() => {
  switch (active.value?.status) {
    case 'error': return 'exception'
    case 'warning': return 'warning'
    case 'success': return 'success'
    default: return undefined
  }
})
const displaySteps = computed(() => {
  return [
    { key: '__start', name: '开始' },
    ...(active.value?.steps || []),
    { key: '__end', name: '结束' },
  ]
})
const activeIndex = computed(() => {
  const status = active.value?.status
  if (status === 'success' || status === 'warning' || status === 'error') {
    return displaySteps.value.length - 1
  }
  const key = active.value?.step
  const index = displaySteps.value.findIndex((step: any) => step.key === key)
  return index < 0 ? 0 : index
})
const stepStatus = (step: any, index: number) => {
  const status = active.value?.status
  if (!status || status === 'idle') return 'wait'
  if (index < activeIndex.value) return 'success'
  if (index === activeIndex.value) {
    if (status === 'error') return 'error'
    if (status === 'warning') return 'error'
    if (status === 'success') return 'success'
    return 'process'
  }
  return 'wait'
}

const resolveApi = () => {
  const params = typeof props.params === 'function' ? props.params() : (props.params || {})
  const headers = typeof props.headers === 'function' ? props.headers() : (props.headers || {})
  let config = props.api
  if (typeof config === 'function') {
    config = config(params)
  }
  if (typeof config === 'string') {
    config = { uri: config }
  }
  config = config || {}
  return {
    app: config.app || props.app || 'bi',
    uri: config.uri || props.uri,
    method: config.method || props.method || 'POST',
    headers: Object.assign({}, headers, config.headers || {}),
    params: Object.assign({}, params, config.params || {}),
  }
}

const appendLog = (session: any, message: any, level = 'info') => {
  if (message == null || message === '') return
  session.logs.push({
    time: DateUtil.format(new Date(), 'HH:mm:ss'),
    level,
    message: typeof message === 'string' ? message : JSON.stringify(message),
  })
  if (session.logs.length > 500) session.logs.shift()
}

const parseMessage = (message: any) => {
  let payload = message
  if (typeof payload === 'string') {
    try {
      payload = JSON.parse(payload)
    } catch (e) {
      return { action: 'log', data: { message: payload } }
    }
  }
  if (payload == null) return { action: 'log', data: {} }
  return payload
}

const handleMessage = (session: any, message: any) => {
  const packet = parseMessage(message)
  const action = packet.action || 'log'
  if (typeof packet.progress === 'number') {
    session.progress = Math.max(0, Math.min(100, packet.progress))
  }
  if (packet.step) session.step = packet.step
  if (typeof packet.total === 'number') session.total = packet.total
  if (action === 'plan' && packet.data) {
    if (Array.isArray(packet.data)) {
      session.steps = packet.data
    } else if (typeof packet.data === 'object') {
      session.steps = Object.keys(packet.data).map((key: string) => ({ key, name: packet.data[key] }))
    }
  }
  const content = packet.message || (action === 'result' && (packet.code === 0 ? '执行成功' : '执行完成')) || ''
  switch (action) {
    case 'plan':
      break
    case 'start':
      session.status = 'running'
      appendLog(session, content || '任务开始', 'info')
      break
    case 'step':
      session.status = 'running'
      appendLog(session, content, 'step')
      break
    case 'progress':
      session.status = 'running'
      appendLog(session, content, 'info')
      break
    case 'log':
      appendLog(session, content, packet.level || 'info')
      break
    case 'result':
      session.result = packet
      session.status = Number(packet.code) === 0 ? 'success' : 'warning'
      session.progress = 100
      appendLog(session, content, Number(packet.code) === 0 ? 'success' : 'warning')
      emit('finish', session, packet)
      break
    case 'error':
      session.result = packet
      session.status = 'error'
      session.progress = 100
      appendLog(session, content || '任务执行失败', 'error')
      emit('error', session, packet)
      break
    default:
      appendLog(session, content || JSON.stringify(packet), 'info')
  }
  emit('message', session, packet)
}

const createSession = () => {
  return {
    id: Date.now() + '-' + Math.random().toString(36).slice(2, 8),
    title: '执行',
    running: false,
    status: 'idle',
    progress: 0,
    step: '',
    steps: [],
    total: 0,
    logs: [],
    result: null,
    sse: null,
  }
}

const startSession = () => {
  const config = resolveApi()
  if (!config.uri) {
    ElMessage.warning('未配置 SSE 接口地址')
    return
  }
  const session: any = reactive(createSession())
  session.title = '执行'
  session.running = true
  session.status = 'running'
  active.value = session
  const task = new FetchEventSource(config.app, config.uri)
  task.setMethod(config.method)
  task.addHeaders(Object.assign({ 'X-Auth-Token': user.info.token }, config.headers))
  task.onOpen(() => {
    session.status = 'running'
    appendLog(session, `正在连接 ${config.uri}`, 'info')
    emit('start', session)
  }).onMessage((message: any) => {
    if (!message.data || message.data === '[DONE]') return
    handleMessage(session, message.data)
  }).onError((error: any) => {
    session.status = 'error'
    appendLog(session, error?.message || String(error), 'error')
    emit('error', session, error)
  }).onClose(() => {
    session.running = false
    if (session.status === 'running') {
      session.status = 'success'
      session.progress = 100
    }
  })
  session.sse = task
  task.send(config.params)
}

const handleClick = () => {
  if (activeRunning.value) {
    active.value.sse?.abort()
    return
  }
  startSession()
}

onBeforeUnmount(() => {
  active.value?.sse?.abort()
})
</script>

<template>
  <div class="form-maintain">
    <el-card shadow="never" class="form-maintain-card">
      <template #header>
        <div class="form-maintain-header">
          <div class="form-maintain-header-info">
            <div class="form-maintain-title-row">
              <span class="form-maintain-title">{{ title }}</span>
              <el-tag v-if="active" :type="statusType" effect="plain" size="small">{{ statusText }}</el-tag>
            </div>
            <div v-if="description" class="form-maintain-description">{{ description }}</div>
          </div>
          <div class="form-maintain-header-actions">
            <el-button
              :type="type"
              :plain="plain"
              :loading="activeRunning"
              :icon="activeRunning ? ElementPlusIcons.VideoPause : ElementPlusIcons.VideoPlay"
              @click="handleClick">
              {{ buttonText }}
            </el-button>
          </div>
        </div>
      </template>
      <div class="form-maintain-body">
        <el-steps v-if="displaySteps.length" :active="activeIndex" finish-status="success" align-center class="form-maintain-steps">
          <el-step v-for="(step, index) in displaySteps" :key="step.key" :title="step.name" :status="stepStatus(step, index)" />
        </el-steps>
        <el-progress :percentage="active ? active.progress : 0" :status="progressStatus" />
        <div class="form-maintain-meta">
          <span v-if="active && active.step">当前步骤：{{ active.step }}</span>
          <span v-if="active && active.total">待处理数量：{{ active.total }}</span>
        </div>
        <template v-if="active">
          <el-scrollbar v-if="active.logs.length" :height="height" class="form-maintain-log">
            <div
              v-for="(item, index) in active.logs"
              :key="index"
              class="form-maintain-line"
              :class="'is-' + item.level">
              <span class="form-maintain-time">{{ item.time }}</span>
              <span class="form-maintain-message">{{ item.message }}</span>
            </div>
          </el-scrollbar>
          <div v-else class="form-maintain-empty" :style="{ height }">
            <el-empty description="暂无日志" :image-size="60" />
          </div>
        </template>
        <div v-else class="form-maintain-empty" :style="{ height }">
          <el-empty description="暂无任务，点击右上角按钮开始执行" :image-size="60" />
        </div>
      </div>
    </el-card>
  </div>
</template>

<style lang="scss" scoped>
.form-maintain {
  .form-maintain-card {
    border: none;

    :deep(.el-card__header) {
      padding: 16px 20px;
      border-bottom: 1px solid var(--el-border-color-light);
    }

    :deep(.el-card__body) {
      padding: 16px 20px;
    }
  }

  .form-maintain-header {
    @include flex-between();
    gap: 12px;

    .form-maintain-header-info {
      min-width: 0;
    }

    .form-maintain-title-row {
      @include flex-start();
      gap: 10px;
    }

    .form-maintain-title {
      font-size: 16px;
      font-weight: 600;
      line-height: 24px;
    }

    .form-maintain-description {
      margin-top: 4px;
      color: var(--el-text-color-secondary);
      font-size: 12px;
    }

    .form-maintain-header-actions {
      @include flex-start();
      gap: 10px;
      flex: none;
    }
  }

  .form-maintain-body {
    .form-maintain-steps {
      margin-top: 16px;
      margin-bottom: 16px;
    }

    .form-maintain-meta {
      @include flex-start();
      gap: 16px;
      margin-top: 8px;
      color: var(--el-text-color-secondary);
      font-size: 12px;
    }


    .form-maintain-empty {
      @include flex-center();
      flex-direction: column;
      margin-top: 12px;
      min-height: 120px;
      border: 1px dashed var(--el-border-color-light);
      border-radius: 6px;
      background-color: var(--el-fill-color-lighter);
    }

    .form-maintain-log {
      margin-top: 12px;

      .form-maintain-line {
        @include flex-start();
        gap: 8px;
        font-size: 12px;
        line-height: 22px;
        padding: 2px 4px;
        border-radius: 3px;

        &:nth-child(odd) {
          background-color: var(--el-fill-color-lighter);
        }

        .form-maintain-time {
          color: var(--el-text-color-secondary);
          white-space: nowrap;
        }

        .form-maintain-message {
          word-break: break-all;
        }

        &.is-step {
          color: var(--el-color-primary);
        }

        &.is-success {
          color: var(--el-color-success);
        }

        &.is-warning {
          color: var(--el-color-warning);
        }

        &.is-error {
          color: var(--el-color-danger);
        }
      }
    }

  }
}
</style>
