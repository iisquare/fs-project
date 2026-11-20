<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import LayoutIcon from '@/components/Layout/LayoutIcon.vue'
import LayoutHelp from '@/components/Layout/LayoutHelp.vue'
import DiagramApi from '@/api/bi/DiagramApi'
import ApiUtil from '@/utils/ApiUtil'
import ElementUtil from '@/utils/ElementUtil'
import PropertyPanel from './PropertyPanel.vue'
import PropertySection from '@/components/Layout/PropertySection.vue'

const model: any = defineModel()
const tips: any = defineModel('tips', { type: null })
const props = defineProps<{
  config: any,
  instance: any,
}>()

const jars: any = ref([])
const selectedJar: any = ref(null)
const total = ref(0)
const loading = ref(false)
const uploading = ref(false)
let searchTimer: ReturnType<typeof setTimeout> | undefined

const options = computed(() => {
  if (!model.value.data.options || typeof model.value.data.options !== 'object') {
    model.value.data.options = {}
  }
  return model.value.data.options
})

const localMode = computed({
  get: () => options.value.localMode === true
    || (!options.value.jarId && !options.value.jarURI && !!options.value.pkgClass),
  set: (value: boolean) => {
    options.value.localMode = value
    if (value) {
      options.value.jarId = ''
      options.value.jarURI = ''
      selectedJar.value = null
    } else if (!options.value.jarURI) {
      options.value.pkgClass = ''
    }
  },
})
const nodes = computed(() => (selectedJar.value?.nodes ?? [])
  .filter((node: any) => ['source', 'transform', 'sink'].includes(node.kind)))
const jarOptions = computed(() => {
  if (!selectedJar.value || jars.value.some((jar: any) => jar.id === selectedJar.value.id)) {
    return jars.value
  }
  return [selectedJar.value, ...jars.value]
})
const selectedNode = computed(() =>
  nodes.value.find((node: any) => node.className === options.value.pkgClass))

watch(selectedNode, (node: any) => {
  if (node) model.value.data.direction = node.kind
}, { immediate: true })

const kindLabel = (kind: string) => ({
  source: '数据源',
  transform: '数据转换',
  sink: '数据输出',
  config: '配置参数',
}[kind] ?? '作业节点')
const formatSize = (size: number) => {
  if (!size) return '0 B'
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / 1024 / 1024).toFixed(1)} MB`
}
const formatTime = (time: number) => time
  ? new Date(time).toLocaleString('zh-CN', { hour12: false })
  : ''
const jarOptionLabel = (jar: any) =>
  `${jar.name} · ${jar.shortHash || ''} · ${formatSize(jar.size)} · ${formatTime(jar.uploadedTime)}`

const loadJars = async (keyword = '') => {
  loading.value = true
  try {
    const result: any = await DiagramApi.jarList({ keyword, pageSize: 30 })
    const data = ApiUtil.data(result) ?? {}
    jars.value = data.rows ?? []
    total.value = Number(data.total ?? jars.value.length)
  } finally {
    loading.value = false
  }
}

const searchJars = (keyword: string) => {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => loadJars(keyword).catch(() => {}), 250)
}

const loadSelectedJar = async () => {
  if (!options.value.jarId) {
    selectedJar.value = null
    return
  }
  try {
    const result: any = await DiagramApi.jarInfo(String(options.value.jarId), { success: false })
    const jar = ApiUtil.data(result)
    if (!jar?.id) {
      selectedJar.value = null
      return
    }
    selectedJar.value = jar
    options.value.jarURI = jar.uri
  } catch (_) {
    selectedJar.value = null
  }
}

const chooseJar = (id: string) => {
  const jar = jars.value.find((item: any) => item.id === id)
    ?? (selectedJar.value?.id === id ? selectedJar.value : null)
  if (!jar) {
    options.value.jarId = ''
    options.value.jarURI = ''
    options.value.pkgClass = ''
    return
  }
  options.value.localMode = false
  options.value.jarId = jar.id
  options.value.jarURI = jar.uri
  selectedJar.value = jar
  if (!jar.nodes?.some((node: any) => node.className === options.value.pkgClass)) {
    options.value.pkgClass = ''
  }
}

const chooseJob = (className: string) => {
  options.value.pkgClass = className
}

const validateJarFile = (file: File) => {
  if (!/\.jar$/i.test(file.name)) {
    ElMessage.warning('请选择 JAR 文件')
    return false
  }
  return true
}

const handleUpload = async (request: any) => {
  if (uploading.value) return
  uploading.value = true
  try {
    const result: any = await DiagramApi.uploadJar(request.file, { success: true })
    const jar = ApiUtil.data(result)
    if (jar?.id) {
      jars.value = [jar, ...jars.value.filter((item: any) => item.id !== jar.id)]
      total.value++
      selectedJar.value = jar
      chooseJar(jar.id)
      request.onSuccess?.(result)
    }
  } catch (error: any) {
    request.onError?.(error)
  } finally {
    uploading.value = false
  }
}

const handleDelete = () => {
  props.instance.flow.remove(model.value)
}

const refreshJars = () => loadJars().catch(() => {})

const deleteJar = () => {
  const jar = selectedJar.value
  if (!jar) return
  ElementUtil.confirm(`确认删除 JAR“${jar.name}”吗？已引用该 JAR 的作业将无法继续执行。`).then(() => {
    return DiagramApi.deleteJar(jar.id, { success: true })
  }).then(() => {
    jars.value = jars.value.filter((item: any) => item.id !== jar.id)
    total.value = Math.max(0, total.value - 1)
    selectedJar.value = null
    options.value.localMode = false
    options.value.jarId = ''
    options.value.jarURI = ''
    options.value.pkgClass = ''
  }).catch(() => {})
}

onMounted(() => {
  Promise.all([
    loadJars().catch(() => {}),
    loadSelectedJar(),
  ])
})

onBeforeUnmount(() => {
  if (searchTimer) clearTimeout(searchTimer)
})
</script>

<template>
  <property-panel title="节点属性">
    <el-form :model="model" label-position="top">
      <property-section title="基础信息" :reset-key="model.id">
        <template #extra>
          <el-popconfirm title="确认删除该节点？" width="180" @confirm="handleDelete">
            <template #reference>
              <LayoutIcon name="Delete" class="delete" />
            </template>
          </el-popconfirm>
        </template>
        <el-form-item label="类型">{{ model.data.type }}</el-form-item>
        <el-form-item label="名称">
          <el-input v-model="model.data.name" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="model.data.description" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="并行度">
          <el-input-number v-model="model.data.parallelism" :min="1" :controls="false" />
        </el-form-item>
      </property-section>

      <property-section title="作业来源" :reset-key="model.id">
        <div class="source-mode">
          <div class="source-mode__label">
            <strong>本地类路径</strong>
            <LayoutHelp text="仅用于 IDE/测试进程 classpath；正式运行请使用 JAR" />
          </div>
          <el-switch v-model="localMode" />
        </div>
        <el-upload
          v-if="!localMode"
          class="jar-drop"
          drag
          accept=".jar"
          :multiple="false"
          :show-file-list="false"
          :http-request="handleUpload"
          :before-upload="validateJarFile"
          :disabled="uploading"
        >
          <div class="jar-drop__content">
            <LayoutIcon name="UploadFilled" class="jar-drop__icon" />
            <div>
              <strong>{{ uploading ? '正在上传 JAR…' : '拖拽 JAR 文件到此处' }}</strong>
              <span>{{ uploading ? '请稍候' : '或点击选择文件' }}</span>
            </div>
          </div>
        </el-upload>
        <div v-if="!localMode" class="jar-actions">
          <el-select
            v-model="options.jarId"
            class="jar-select"
            filterable
            remote
            :remote-method="searchJars"
            :remote-show-suffix="true"
            clearable
            placeholder="输入文件名、SHA-256 或作业类检索"
            :loading="loading"
            @change="chooseJar"
          >
            <el-option
              v-for="jar in jarOptions"
              :key="jar.id"
              :value="jar.id"
              :label="jarOptionLabel(jar)"
            />
          </el-select>
          <el-button text :loading="loading" :title="`刷新 JAR 列表（共 ${total} 个）`" @click="refreshJars">
            <LayoutIcon name="Refresh" />
          </el-button>
          <el-button v-if="selectedJar" text type="danger" @click="deleteJar">删除</el-button>
        </div>
        <el-descriptions v-if="!localMode && selectedJar" :column="1" size="small" border class="jar-detail">
          <el-descriptions-item label="文件名">{{ selectedJar.name }}</el-descriptions-item>
          <el-descriptions-item label="JAR标识">{{ selectedJar.id }}</el-descriptions-item>
          <el-descriptions-item label="SHA-256">
            <span class="jar-hash">{{ selectedJar.sha256 }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="上传时间">{{ formatTime(selectedJar.uploadedTime) }}</el-descriptions-item>
          <el-descriptions-item label="文件大小">{{ formatSize(selectedJar.size) }}</el-descriptions-item>
          <el-descriptions-item label="作业节点">{{ selectedJar.nodes?.length || 0 }} 个</el-descriptions-item>
        </el-descriptions>
        <el-form-item v-if="localMode" label="节点类型">
          <el-radio-group v-model="model.data.direction" class="node-kind-group">
            <el-radio-button value="source">数据源</el-radio-button>
            <el-radio-button value="transform">数据转换</el-radio-button>
            <el-radio-button value="sink">数据输出</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="localMode" label="本机作业类">
          <el-input
            v-model="options.pkgClass"
            class="local-class-input"
            type="textarea"
            :autosize="{ minRows: 2, maxRows: 5 }"
            resize="none"
            clearable
            placeholder="输入完整类名，如 com.example.MyTransformNode"
            @change="chooseJob"
          />
        </el-form-item>
      </property-section>

      <property-section v-if="!localMode" title="作业节点" :reset-key="model.id">
        <el-form-item label="JAR作业">
          <el-select
            v-model="options.pkgClass"
            filterable
            clearable
            :disabled="!selectedJar"
            :placeholder="selectedJar ? '选择 JAR 中的作业类' : '请先选择或上传 JAR'"
            popper-class="jar-job-select-popper"
            style="width: 100%"
            @change="chooseJob"
          >
            <el-option
              v-for="node in nodes"
              :key="node.className"
              :value="node.className"
              :label="node.name"
            >
              <div class="job-option">
                <span>{{ node.name }}</span>
                <small>{{ kindLabel(node.kind) }} · 输入 {{ node.inputPorts }} / 输出 {{ node.outputPorts }}</small>
              </div>
            </el-option>
          </el-select>
        </el-form-item>
        <el-empty v-if="selectedJar && nodes.length === 0" description="JAR 中未发现可用 DAGNode" :image-size="40" />
        <el-descriptions v-if="selectedNode" :column="1" border size="small" class="job-info">
          <el-descriptions-item label="作业类型">{{ kindLabel(selectedNode.kind) }}</el-descriptions-item>
          <el-descriptions-item label="类名">{{ selectedNode.className }}</el-descriptions-item>
          <el-descriptions-item label="端口">
            输入 {{ selectedNode.inputPorts }} / 输出 {{ selectedNode.outputPorts }}
          </el-descriptions-item>
        </el-descriptions>
      </property-section>

    </el-form>
  </property-panel>
</template>

<style lang="scss" scoped>
.jar-actions {
  display: flex;
  align-items: center;
  gap: 2px;
  width: 100%;
  min-width: 0;
  :deep(.el-button) {
    flex: none;
    margin-left: 0;
  }
}
.source-mode {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  min-width: 0;
  margin-bottom: 10px;
}
.source-mode__label {
  display: flex;
  align-items: center;
  flex: 1;
  min-width: 0;
  strong {
    color: var(--el-text-color-primary);
    font-size: 12px;
    font-weight: 500;
    line-height: 1.4;
  }
}
.jar-select {
  flex: 1 1 0;
  width: 0;
  min-width: 0;
}
.local-class-input :deep(.el-textarea__inner) {
  min-height: 52px !important;
  font-family: ui-monospace, SFMono-Regular, Consolas, "Liberation Mono", monospace;
  font-size: 11px;
  line-height: 1.5;
}
.node-kind-group {
  display: flex;
  width: 100%;
  min-width: 0;
  :deep(.el-radio-button) {
    flex: 1;
    min-width: 0;
  }
  :deep(.el-radio-button__inner) {
    width: 100%;
    padding: 7px 3px;
    font-size: 12px;
  }
}
:deep(.el-form-item),
:deep(.el-form-item__content) {
  min-width: 0;
}
.jar-drop {
  display: block;
  width: 100%;
  min-width: 0;
  margin: 8px 0;
  box-sizing: border-box;
  :deep(.el-upload) {
    width: 100%;
    min-width: 0;
  }
  :deep(.el-upload-dragger) {
    --el-upload-dragger-padding-horizontal: 12px;
    --el-upload-dragger-padding-vertical: 9px;
    background: var(--el-fill-color-lighter);
  }
}
.jar-drop__content {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
  min-width: 0;
  box-sizing: border-box;
  min-height: 32px;
  color: var(--el-text-color-regular);
  line-height: 1.45;
  text-align: left;
  strong,
  span {
    display: block;
  }
  strong {
    color: var(--el-text-color-primary);
    font-size: 12px;
    font-weight: 500;
  }
  span {
    color: var(--el-text-color-secondary);
    font-size: 11px;
  }
}
.jar-drop__icon {
  flex: none;
  color: var(--el-color-primary);
  font-size: 20px;
}
.jar-detail {
  display: block;
  width: 100%;
  min-width: 0;
  margin-top: 8px;
  :deep(.el-descriptions__body) {
    overflow: hidden;
  }
  :deep(.el-descriptions__table) {
    width: 100%;
    table-layout: fixed;
  }
  :deep(.el-descriptions__label) {
    width: 64px;
    white-space: nowrap;
  }
  :deep(.el-descriptions__content) {
    min-width: 0;
    overflow-wrap: anywhere;
    word-break: break-word;
  }
}
.jar-hash {
  font-family: monospace;
  font-size: 11px;
  word-break: break-all;
}
.job-option {
  display: flex;
  flex-direction: column;
  min-width: 0;
  line-height: 1.4;
  padding: 2px 0;
  span,
  small {
    overflow-wrap: anywhere;
  }
  small {
    color: var(--el-text-color-secondary);
  }
}
:global(.jar-job-select-popper .el-select-dropdown__item) {
  height: auto;
  min-height: 48px;
  padding: 6px 12px;
  line-height: normal;
  white-space: normal;
  overflow: visible;
  text-overflow: clip;
}
.job-info {
  width: 100%;
  margin-top: 8px;
  :deep(.el-descriptions__table) {
    width: 100%;
    table-layout: fixed;
  }
  :deep(.el-descriptions__label) {
    width: 72px;
  }
  :deep(.el-descriptions__content) {
    min-width: 0;
    white-space: normal;
    overflow-wrap: anywhere;
    word-break: break-all;
  }
}
</style>
