<script setup lang="ts">
import LayoutDesigner from '@/components/Layout/LayoutDesigner.vue';
import LayoutBack from '@/components/Layout/LayoutBack.vue';
import LayoutProperty from '@/components/Layout/LayoutProperty.vue';
import LayoutWidget from '@/components/Layout/LayoutWidget.vue';
import LayoutToolbar from '@/components/Layout/LayoutToolbar.vue';
import LayoutIcon from '@/components/Layout/LayoutIcon.vue';
import TableColumnSetting from '@/components/Table/TableColumnSetting.vue';
import X6Container from '@/designer/X6/X6Container.vue';
import { computed, nextTick, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import type { TableInstance } from 'element-plus';
import * as ElementPlusIcons from '@element-plus/icons-vue';
import config from '@/designer/BIDag/config'
import DesignUtil from '@/utils/DesignUtil';
import DiagramApi from '@/api/bi/DiagramApi';
import ApiUtil from '@/utils/ApiUtil';
import ElementUtil from '@/utils/ElementUtil';
import { ElMessage } from 'element-plus';
import RowDataDetail from '@/views/bi/components/RowDataDetail.vue';
import ButtonCopy from '@/components/Button/ButtonCopy.vue';

const route = useRoute()
const router = useRouter()
const flowRef = ref()
const tips: any = ref({ text: '' })
const activeItem: any = ref({})
const debugRuns: any = ref([])
const activeDebugRun: any = ref(null)
const debugLimit = ref(20)
let debugRunId = 0
const loading = ref(false)
const diagram: any = ref(Object.assign(config.canvas.options(), {
  name: '未命名编排',
  engine: 'spark',
  model: 'batch',
  clusterId: 0,
  expression: '',
  schedule: 0,
  notify: {},
  autoRecover: 0,
  maxRetry: 0,
  status: '1',
  sort: 0,
  description: '',
}))

const property = computed(() => {
  return DesignUtil.widgetFlowProperty(activeItem.value, config)
})

/** 判断节点是否支持指定计算引擎与处理模式（support: { 引擎: [处理模式] }） */
const supportsNode = (support: Record<string, string[]> | undefined, engine: string, model: string) =>
  ((support ?? {})[engine] ?? []).includes(model)

/** 保留全部节点，仅将当前计算引擎与处理模式下不可用的节点置灰 */
const widgets = computed(() => {
  return config.widgets.map((group: any) => Object.assign({}, group, {
    children: group.children.map((item: any) => {
      const disabled = !supportsNode(item.support, diagram.value.engine, diagram.value.model)
      return Object.assign({}, item, {
        disabled,
        title: disabled
          ? `${item.title || item.label}（当前引擎/模式不可用）`
          : item.title,
      })
    }),
  })).filter((group: any) => group.children.length > 0)
})

/** 画布上已存在、但当前计算引擎与处理模式不支持的节点名称 */
const unsupportedNodes = (engine: string, model: string) => {
  const cells = flowRef.value?.flow?.toJSON?.()?.cells ?? []
  return cells
    .filter((cell: any) => cell.data?.type)
    .filter((cell: any) => {
      const widget = DesignUtil.widgetByType(cell.data.type, config)
      return widget && !supportsNode(widget.support, engine, model)
    })
    .map((cell: any) => cell.data?.name || cell.id)
}

// 载入已有编排时引擎/模式会随数据变化，此时不提示节点不兼容
let loadingDiagram = false
watch(() => [diagram.value.engine, diagram.value.model], () => {
  if (loadingDiagram) return
  const names = unsupportedNodes(diagram.value.engine, diagram.value.model)
  if (names.length > 0) {
    ElMessage.warning(`当前引擎/模式下有 ${names.length} 个节点不受支持：${names.join('、')}`)
  }
})

const options: any = {}
const handleDragStart = (event: any, widget: any) => {
  flowRef.value.flow.startDrag(event, widget)
}

const handleReload = () => {
  const id = route.query.id
  if (!id) return
  loading.value = true
  DiagramApi.info(id).then((result: any) => {
    const data = ApiUtil.data(result)
    loadingDiagram = true
    Object.assign(diagram.value, data, {
      clusterId: Number(data.clusterId) > 0 ? Number(data.clusterId) : 0,
      status: String(data.status),
      notify: data.notify ?? {},
    })
    flowRef.value.flow.fromJSON(data.content?.cells ?? [])
    // 恢复此前版本统一放大的节点；其他手工尺寸保持不变。
    flowRef.value.flow.graph.getNodes().forEach((cell: any) => {
      if (cell.shape !== 'flow-node') return
      const size = cell.getSize()
      if (size.width !== 104 || size.height !== 82) return
      const position = cell.getPosition()
      cell.resize(60, 60)
      cell.position(position.x + 22, position.y + 11)
    })
    nextTick(() => { loadingDiagram = false })
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}

const saveDiagram = () => {
  const params = Object.assign({}, diagram.value, {
    schedule: diagram.value.schedule ? true : false,
    autoRecover: diagram.value.autoRecover ? true : false,
    content: flowRef.value.flow.toJSON(),
  })
  return DiagramApi.save(params, { success: false }).then((result: any) => {
    diagram.value.id = ApiUtil.data(result).id
    if (!route.query.id) router.replace({ query: { id: diagram.value.id } }).catch(() => {})
    return result
  })
}

const handleSubmit = () => {
  loading.value = true
  saveDiagram().then(() => {
    ElMessage.success('编排流程已保存')
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}

const handleRun = () => {
  if (!diagram.value.id) {
    ElMessage.warning('请先保存编排流程')
    return
  }
  const names = unsupportedNodes(diagram.value.engine, diagram.value.model)
  if (names.length > 0) {
    ElMessage.warning(`有 ${names.length} 个节点不被当前引擎/模式支持，请调整后再运行：${names.join('、')}`)
    return
  }
  ElementUtil.confirm('确认提交该编排作业？').then(() => {
    loading.value = true
    DiagramApi.run({ id: diagram.value.id, clusterId: diagram.value.clusterId }, { success: true }).then(() => {
      router.push({ path: '/bi/diagram/job', query: { diagramId: diagram.value.id } }).catch(() => {})
    }).catch(() => {}).finally(() => {
      loading.value = false
    })
  }).catch(() => {})
}

const handleDebug = async () => {
  if (!activeItem.value || !activeItem.value.id || activeItem.value.shape === 'flow-edge') {
    ElMessage.warning('请先选择需要调试的节点')
    return
  }
  loading.value = true
  const started = Date.now()
  const nodeLabel = activeItem.value.data?.name || activeItem.value.id
  try {
    // 调试前保存当前画布，保证未另外点击“保存”时也能立即验证最新配置。
    await saveDiagram()
    const nodeId = `dag_${diagram.value.id}_${activeItem.value.id}`
    const result: any = await DiagramApi.debug({
      id: diagram.value.id,
      nodeId,
      previewLimit: debugLimit.value,
    }, { success: false })
    const data = ApiUtil.data(result) ?? {}
    const outputs = data.outputs ?? {}
    const execution = data.execution ?? []
    const run: any = {
      id: ++debugRunId,
      nodeId,
      nodeLabel,
      execution,
      final: data.final ?? outputs[nodeId] ?? null,
      succeeded: data.succeeded !== false,
      message: data.message ?? '',
      elapsedMs: Date.now() - started,
      selectedId: execution.find((row: any) => row.id === nodeId)?.id
        ?? execution[execution.length - 1]?.id ?? '',
      limit: debugLimit.value,
      createdAt: Date.now(),
      outputs,
    }
    debugRuns.value.push(run)
    activeDebugRun.value = run
  } catch (error: any) {
    const run: any = {
      id: ++debugRunId,
      nodeId: '',
      nodeLabel,
      execution: [],
      final: error,
      succeeded: false,
      message: ApiUtil.message(error) || '节点调试失败',
      elapsedMs: Date.now() - started,
      selectedId: '',
      limit: debugLimit.value,
      createdAt: Date.now(),
    }
    debugRuns.value.push(run)
    activeDebugRun.value = run
  } finally {
    loading.value = false
  }
}

const debugExecution = computed(() => activeDebugRun.value?.execution ?? [])
const debugFinal = computed(() => activeDebugRun.value?.final ?? null)
const debugSucceeded = computed(() => activeDebugRun.value?.succeeded !== false)
const debugMessage = computed(() => activeDebugRun.value?.message ?? '')
const debugNodeId = computed(() => activeDebugRun.value?.nodeId ?? '')
const debugFailedNodes = computed(() =>
  debugExecution.value.filter((row: any) => row.succeed === false))
const debugFailureNode = computed(() => {
  const failed = debugFailedNodes.value.find((row: any) => row.error) ?? debugFailedNodes.value[0]
  return failed?.name
    || (failed?.id ? debugLabel(failed.id) : '')
    || activeDebugRun.value?.nodeLabel
    || '未知节点'
})
const debugErrorCopyText = computed(() => {
  const run = activeDebugRun.value
  if (!run) return ''
  return [
    `执行记录：#${run.id} ${run.nodeLabel}`,
    `失败节点：${debugFailureNode.value}`,
    `耗时：${run.elapsedMs} ms`,
    '',
    '错误信息：',
    debugMessage.value || '节点执行过程中发生异常，请查看任务执行明细。',
  ].join('\n')
})
const debugSelected = computed(() => {
  const run = activeDebugRun.value
  if (!run) return null
  return run.execution?.find((row: any) => row.id === run.selectedId) ?? null
})
const selectDebugExecution = (row: any) => {
  if (activeDebugRun.value) activeDebugRun.value.selectedId = row?.id ?? ''
}
const highlightDebugRun = (run: any) => {
  const colors: any = {}
  if (run) {
    for (const key of Object.keys(run.outputs ?? {})) colors[key] = '#67c23a'
    if (run.nodeId) colors[run.nodeId] = '#409eff'
  }
  flowRef.value?.flow?.highlight?.(colors)
}
const debugRunLabel = (run: any) => {
  return `#${run.id} ${run.nodeLabel} · ${run.succeeded ? '成功' : '失败'} · ${run.elapsedMs} ms`
}
const debugPreview = computed(() => debugSelected.value?.preview ?? debugFinal.value)
const debugSampleTableRef = ref<TableInstance>()
const debugSampleColumns = ref<any[]>([])
const debugSampleKeyword = ref('')
const debugRowDetailVisible = ref(false)
const debugRowDetailIndex = ref(-1)

/** 解析节点输出中可能被序列化为字符串的 JSON。 */
const parseDebugJson = (value: any) => {
  if (typeof value !== 'string') return value
  const text = value.trim()
  if (!text || !['{', '['].includes(text[0])) return value
  try {
    return JSON.parse(text)
  } catch {
    return value
  }
}

/**
 * 将不同节点的调试输出统一成二维样例。
 * Spark Dataset 返回 { schema, rows, limit }，其他节点可能是数组、字符串或普通对象。
 */
const debugPreviewData = computed(() => {
  let source: any = parseDebugJson(debugPreview.value)
  if (source && typeof source === 'object' && typeof source.preview === 'string') {
    const nested = parseDebugJson(source.preview)
    if (nested !== source.preview) source = nested
  }

  let rawRows: any[] = []
  let rawColumns: any[] = []
  let schema: Record<string, any> = {}
  let limit: any = null
  let message = ''
  if (Array.isArray(source)) {
    rawRows = source
  } else if (source && typeof source === 'object') {
    if (Array.isArray(source.rows)) {
      rawRows = source.rows
      rawColumns = Array.isArray(source.columns) ? source.columns : []
      schema = source.schema && typeof source.schema === 'object' ? source.schema : {}
      limit = source.limit ?? null
      message = typeof source.preview === 'string' ? source.preview : ''
    } else if (source.type === 'Dataset' || source.schema) {
      rawColumns = Array.isArray(source.columns) ? source.columns : []
      schema = source.schema && typeof source.schema === 'object' ? source.schema : {}
      limit = source.limit ?? null
      message = typeof source.preview === 'string' ? source.preview : ''
    } else {
      rawRows = [source]
    }
  } else if (source !== null && source !== undefined) {
    rawRows = [{ value: source }]
  }

  const rows = rawRows.map((raw: any) => {
    const row = parseDebugJson(raw)
    if (row === null || row === undefined) return {}
    if (Array.isArray(row)) return { value: row }
    if (typeof row === 'object') return row
    return { value: row }
  })

  const columns: any[] = []
  const columnNames = new Set<string>()
  const addColumn = (value: any, type = '') => {
    const name = String(
      typeof value === 'string'
        ? value
        : value?.prop ?? value?.name ?? value?.field ?? '',
    ).trim()
    if (!name || columnNames.has(name)) return
    columnNames.add(name)
    columns.push({
      name,
      prop: name,
      label: value?.label ?? value?.title ?? value?.comment ?? name,
      type: value?.type ?? value?.dataType ?? type ?? '',
      hide: false,
      fixed: false,
    })
  }

  rawColumns.forEach((column: any) => addColumn(column))
  Object.entries(schema).forEach(([name, type]) => addColumn(name, String(type ?? '')))
  rows.forEach((row: any) => Object.keys(row).forEach(name => addColumn(name)))

  return { rows, columns, limit, message }
})

const debugPreviewRows = computed(() => debugPreviewData.value.rows)
const debugVisibleColumns = computed(() =>
  debugSampleColumns.value.filter((column: any) => !column.hide))
const debugFilteredRows = computed(() => {
  const keyword = debugSampleKeyword.value.trim().toLowerCase()
  if (!keyword) return debugPreviewRows.value
  return debugPreviewRows.value.filter((row: any) =>
    debugSampleColumns.value.some((column: any) => {
      const value = row?.[column.prop]
      if (value === null || value === undefined) return false
      let text = value
      if (typeof value === 'object') {
        try { text = JSON.stringify(value) } catch { text = String(value) }
      }
      return String(text).toLowerCase().includes(keyword)
    }))
})
const debugColumnCount = computed(() => debugSampleColumns.value.length)
const debugRowCount = computed(() => debugPreviewRows.value.length)
const debugPreviewLimit = computed(() =>
  debugPreviewData.value.limit ?? activeDebugRun.value?.limit ?? null)
const debugLabel = (id: string) => (id ?? '').replace(`dag_${diagram.value.id}_`, '')
const debugExecutionRowClassName = ({ row }: any) => [
  row.id === debugNodeId.value ? 'debug-target-row' : '',
  row.succeed === false ? 'debug-failed-row' : '',
].filter(Boolean).join(' ')
const debugErrorText = (value: any) => {
  if (value === null || value === undefined) return ''
  if (typeof value === 'object') {
    try { return JSON.stringify(value) } catch { return String(value) }
  }
  return String(value)
}
const debugCell = (value: any) => {
  if (value === null || value === undefined) return 'NULL'
  if (value === '') return '空字符串'
  if (typeof value === 'object') {
    try { return JSON.stringify(value) } catch { return String(value) }
  }
  return String(value)
}
const debugCellKind = (value: any) => {
  if (value === null || value === undefined) return 'null'
  if (value === '') return 'empty-string'
  if (typeof value === 'object') return 'object'
  return 'value'
}
const openDebugRowDetail = (row: any) => {
  debugRowDetailIndex.value = debugFilteredRows.value.indexOf(row)
  debugRowDetailVisible.value = true
}
const syncDebugSample = () => {
  debugSampleColumns.value = debugPreviewData.value.columns.map((column: any) => ({ ...column }))
  debugSampleKeyword.value = ''
  debugRowDetailVisible.value = false
  debugRowDetailIndex.value = -1
}

onMounted(() => {
  DiagramApi.config().then((result: any) => {
    Object.assign(config, ApiUtil.data(result))
  }).catch(() => {})
  handleReload()
})

watch(activeDebugRun, (run: any) => {
  highlightDebugRun(run)
})

watch(
  () => [activeDebugRun.value?.id, activeDebugRun.value?.selectedId, debugPreview.value],
  syncDebugSample,
)
</script>

<template>
  <LayoutDesigner splitter unified-property class="diagram-designer" :right-size="300" :right-min="300">
    <template #left>
      <LayoutWidget :widgets="widgets" @drag-start="handleDragStart" />
    </template>
    <template #top>
      <el-space>
        <LayoutBack to="/bi/diagram/list" variant="text" label="返回" />
        <el-divider direction="vertical" />
        <LayoutToolbar :toolbars="config.toolbars" :instance="flowRef" />
      </el-space>
      <el-space>
        <span class="debug-limit-label">调试行数</span>
        <el-input-number v-model="debugLimit" :min="1" :max="1000" :controls="false" class="debug-limit" />
        <el-button @click="handleDebug" :loading="loading">调试节点</el-button>
        <el-button @click="handleRun" :loading="loading">运行作业</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="loading">保存</el-button>
      </el-space>
    </template>
    <template #default>
      <el-splitter class="dag-splitter" layout="vertical">
        <el-splitter-panel :min="220">
          <div class="dag-canvas">
            <X6Container ref="flowRef" v-model="diagram" :active-item="activeItem" :tips="tips" :options="options" @update:active-item="(v: any) => activeItem = v" />
          </div>
        </el-splitter-panel>
        <el-splitter-panel :size="320" :min="320" :max="900" collapsible>
          <section class="debug-console">
            <header class="debug-console__header">
              <div class="debug-console__title">
                <strong>调试控制台</strong>
                <template v-if="activeDebugRun">
                  <el-divider direction="vertical" />
                  <span class="debug-active-node">
                    #{{ activeDebugRun.id }} {{ activeDebugRun.nodeLabel }}
                  </span>
                  <el-tag :type="debugSucceeded ? 'success' : 'danger'" size="small" effect="light">
                    {{ debugSucceeded ? '成功' : '失败' }}
                  </el-tag>
                  <span class="debug-header-meta">{{ activeDebugRun.elapsedMs }} ms</span>
                  <span class="debug-header-time">
                    {{ new Date(activeDebugRun.createdAt).toLocaleString('zh-CN', { hour12: false }) }}
                  </span>
                </template>
              </div>
              <span class="debug-run-count">{{ debugRuns.length }} 条执行记录</span>
            </header>
            <div v-if="debugRuns.length" class="debug-console__tabs">
              <layout-tabs v-model="activeDebugRun" :tabs="debugRuns" :label="debugRunLabel" />
            </div>
            <el-scrollbar class="debug-console__scroll">
              <div v-if="!activeDebugRun" class="debug-console__empty">
                <el-empty description="选择画布节点并点击“调试节点”，每次执行结果都会保留在这里" :image-size="54" />
              </div>
              <div v-else class="debug-console__body">
              <section
                v-if="!debugSucceeded"
                class="debug-error"
                role="alert"
                aria-live="polite"
              >
                <header class="debug-error__header">
                  <span class="debug-error__icon">
                    <el-icon><ElementPlusIcons.CircleCloseFilled /></el-icon>
                  </span>
                  <div class="debug-error__content">
                    <div class="debug-error__title">
                      <strong>调试执行失败</strong>
                      <span class="debug-error__node">
                        失败节点
                        <b>{{ debugFailureNode }}</b>
                      </span>
                    </div>
                  </div>
                  <ButtonCopy
                    :content="debugErrorCopyText"
                    title="复制完整错误详情"
                    text="复制详情"
                  />
                </header>
                <pre v-if="debugMessage" class="debug-error__detail">{{ debugMessage }}</pre>
                <div v-else class="debug-error__empty">
                  服务端未返回详细错误信息，请结合任务执行明细排查失败原因。
                </div>
              </section>
              <div class="debug-execution">
                <div class="debug-section-title">
                  <span>任务执行明细</span>
                  <span>
                    {{ debugExecution.length }} 个节点
                    <template v-if="debugFailedNodes.length"> · {{ debugFailedNodes.length }} 个异常</template>
                  </span>
                </div>
                <div class="debug-table">
                  <el-table
                    :data="debugExecution"
                    size="small"
                    border
                    highlight-current-row
                    :row-class-name="debugExecutionRowClassName"
                    @current-change="selectDebugExecution"
                  >
                    <el-table-column label="节点" min-width="150">
                      <template #default="{ row }">{{ row.name || debugLabel(row.id) }}</template>
                    </el-table-column>
                    <el-table-column prop="type" label="类型" min-width="120" />
                    <el-table-column prop="outputType" label="输出类型" min-width="110" />
                    <el-table-column label="耗时" width="90">
                      <template #default="{ row }">{{ row.elapsedMs }} ms</template>
                    </el-table-column>
                    <el-table-column label="状态" width="80">
                      <template #default="{ row }">
                        <el-tag :type="row.succeed ? 'success' : 'danger'" size="small">
                          {{ row.succeed ? '成功' : '失败' }}
                        </el-tag>
                      </template>
                    </el-table-column>
                    <el-table-column label="异常" min-width="200">
                      <template #default="{ row }">
                        <el-popover
                          v-if="debugErrorText(row.error)"
                          trigger="hover"
                          placement="top-start"
                          :width="380"
                          :show-after="150"
                          :hide-after="100"
                          popper-class="debug-error-tip"
                        >
                          <template #reference>
                            <div class="debug-error-cell">
                              <el-icon v-if="row.succeed === false"><ElementPlusIcons.CircleCloseFilled /></el-icon>
                              <span>{{ debugErrorText(row.error) }}</span>
                            </div>
                          </template>
                          <div class="debug-error-tip__content">
                            <div class="debug-error-tip__header">
                              <strong>异常详情</strong>
                              <ButtonCopy
                                :content="debugErrorText(row.error)"
                                title="复制异常详情"
                                text="复制"
                              />
                            </div>
                            <pre class="debug-error-tip__text">{{ debugErrorText(row.error) }}</pre>
                          </div>
                        </el-popover>
                        <span v-else class="debug-no-error">—</span>
                      </template>
                    </el-table-column>
                  </el-table>
                </div>
              </div>
              <div class="debug-sample">
                <div class="debug-section-title debug-sample__header">
                  <div class="debug-sample__title">
                    <span>输出数据样例</span>
                    <small>
                      <template v-if="debugSampleKeyword">{{ debugFilteredRows.length }} / </template>
                      {{ debugRowCount }} 行 · {{ debugColumnCount }} 列
                      <template v-if="debugPreviewLimit"> · 上限 {{ debugPreviewLimit }}</template>
                    </small>
                  </div>
                  <div class="debug-sample__actions">
                    <el-input
                      v-model="debugSampleKeyword"
                      class="debug-sample__search"
                      size="small"
                      placeholder="过滤样例数据"
                      clearable
                      :prefix-icon="ElementPlusIcons.Search"
                    />
                    <TableColumnSetting
                      :key="`${activeDebugRun?.id || 0}-${activeDebugRun?.selectedId || 'final'}`"
                      v-model="debugSampleColumns"
                      :table="debugSampleTableRef"
                      text
                    />
                  </div>
                </div>
                <div class="debug-sample__table">
                  <el-empty
                    v-if="!debugVisibleColumns.length && debugRowCount > 0"
                    description="请通过列设置选择需要展示的字段"
                    :image-size="44"
                  />
                  <el-table
                    v-else
                    ref="debugSampleTableRef"
                    :data="debugFilteredRows"
                    :empty-text="debugSampleKeyword ? '无匹配数据' : (debugPreviewData.message || '该节点暂无可预览数据')"
                    size="small"
                    border
                    stripe
                    table-layout="auto"
                    @row-dblclick="openDebugRowDetail"
                  >
                    <el-table-column
                      v-for="field in debugVisibleColumns"
                      :key="field.prop"
                      :prop="field.prop"
                      :fixed="field.fixed"
                      :min-width="132"
                      show-overflow-tooltip
                    >
                      <template #header>
                        <div class="debug-sample__column-head">
                          <span>{{ field.label }}</span>
                          <small v-if="field.type">{{ field.type }}</small>
                        </div>
                      </template>
                      <template #default="{ row }">
                        <span
                          class="debug-sample__cell"
                          :class="`is-${debugCellKind(row[field.prop])}`"
                        >{{ debugCell(row[field.prop]) }}</span>
                      </template>
                    </el-table-column>
                  </el-table>
                </div>
              </div>
              </div>
            </el-scrollbar>
          </section>
          <RowDataDetail
            v-model="debugRowDetailVisible"
            :row-index="debugRowDetailIndex"
            :rows="debugFilteredRows"
            :columns="debugVisibleColumns"
            @changeRow="debugRowDetailIndex = $event"
          />
        </el-splitter-panel>
      </el-splitter>
    </template>
    <template #right>
      <LayoutProperty v-model="diagram" :active-item="activeItem" :instance="flowRef" :config="config" :context="diagram" :tips="tips" :property="property" />
    </template>
    <template #footer>
      <el-space>
        <LayoutIcon name="Opportunity" color="#409eff" />
        <div>{{ tips.text }}</div>
      </el-space>
    </template>
  </LayoutDesigner>
</template>

<style lang="scss" scoped>
.dag-splitter {
  height: 100%;
  width: 100%;
  background: var(--fs-layout-background-color, #f7fafc);
  :deep(.el-splitter-panel) {
    min-height: 0;
  }
}
.dag-canvas {
  height: 100%;
  min-height: 0;
  overflow: hidden;
}
.debug-console {
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  border-top: solid 1px var(--fs-layout-border-color);
  background: var(--fs-layout-background-color, #f7fafc);
  &__header {
    flex: none;
    min-height: 45px;
    padding: 0 15px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    border-bottom: solid 1px var(--fs-layout-border-color);
    background: var(--fs-layout-background-color, #f7fafc);
    box-sizing: border-box;
  }
  &__title {
    min-width: 0;
    display: flex;
    align-items: center;
    gap: 8px;
    strong {
      color: var(--el-text-color-primary);
      font-size: 13px;
      font-weight: 600;
      white-space: nowrap;
    }
    .debug-active-node {
      min-width: 0;
      color: var(--el-text-color-secondary);
      font-size: 12px;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
    :deep(.el-divider--vertical) {
      margin: 0 2px;
    }
  }
  &__tabs {
    flex: none;
    background: var(--el-bg-color);
    border-bottom: solid 1px var(--fs-layout-border-color);
  }
  &__scroll {
    flex: 1;
    min-height: 0;
    background: var(--el-bg-color);
    :deep(.el-scrollbar__wrap) {
      height: 100%;
    }
    :deep(.el-scrollbar__view) {
      min-height: 100%;
      display: flex;
      flex-direction: column;
    }
  }
  &__empty {
    flex: 1;
    min-height: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    background: var(--el-bg-color);
  }
  &__body {
    flex: none;
    min-height: auto;
    display: flex;
    flex-direction: column;
    box-sizing: border-box;
    background: var(--el-bg-color);
    overflow: visible;
  }
}
.debug-error {
  flex: none;
  margin: 0;
  overflow: hidden;
  border: 1px solid var(--el-color-danger-light-7);
  background: var(--el-bg-color);
  box-shadow: 0 1px 2px rgb(0 0 0 / 4%);
}
.debug-error__header {
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 36px;
  padding: 6px 12px;
  background: var(--el-color-danger-light-9);
  border-bottom: 1px solid var(--el-color-danger-light-8);
  box-sizing: border-box;
}
.debug-error__icon {
  flex: none;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border: 1px solid var(--el-color-danger-light-7);
  border-radius: 50%;
  color: var(--el-color-danger);
  background: var(--el-bg-color);
  .el-icon {
    font-size: 14px;
  }
}
.debug-error__content {
  flex: 1;
  min-width: 0;
}
.debug-error__title {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 7px;
  strong {
    color: var(--el-color-danger);
    font-size: 13px;
    font-weight: 600;
    line-height: 1.4;
  }
}
.debug-error__node {
  display: inline-flex;
  align-items: center;
  max-width: 100%;
  gap: 4px;
  padding: 1px 7px;
  overflow: hidden;
  border: 1px solid var(--el-color-danger-light-7);
  border-radius: 4px;
  color: var(--el-text-color-secondary);
  background: var(--el-bg-color);
  font-size: 11px;
  line-height: 18px;
  b {
    min-width: 0;
    overflow: hidden;
    color: var(--el-color-danger);
    font-weight: 500;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}
.debug-error__detail {
  margin: 0;
  padding: 9px 12px;
  color: var(--el-text-color-regular);
  background: var(--el-bg-color);
  font-family: ui-monospace, SFMono-Regular, Consolas, "Liberation Mono", monospace;
  font-size: 11px;
  line-height: 1.65;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}
.debug-error__empty {
  padding: 10px 12px;
  color: var(--el-text-color-secondary);
  background: var(--el-bg-color);
  font-size: 12px;
}
.debug-run-count {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
.debug-limit-label {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
.debug-limit {
  width: 72px;
}
.debug-header-meta {
  color: var(--el-text-color-regular);
  font-size: 12px;
  white-space: nowrap;
}
.debug-header-time {
  color: var(--el-text-color-placeholder);
  font-size: 11px;
  white-space: nowrap;
}
.debug-execution,
.debug-sample {
  flex: none;
  display: flex;
  flex-direction: column;
  min-width: 0;
  overflow: visible;
  background: var(--el-bg-color);
}
.debug-execution {
  border-bottom: solid 1px var(--fs-layout-border-color);
}
.debug-sample {
  min-height: 0;
}
.debug-section-title {
  flex: none;
  height: 38px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 12px;
  color: var(--el-text-color-primary);
  font-size: 13px;
  font-weight: 600;
  background: var(--el-fill-color-extra-light);
  border-bottom: solid 1px var(--fs-layout-border-color);
  > span {
    color: var(--el-text-color-secondary);
    font-size: 12px;
    font-weight: normal;
  }
}
.debug-table {
  flex: none;
  overflow: visible;
}
.debug-error-cell {
  display: flex;
  align-items: center;
  min-width: 0;
  gap: 5px;
  color: var(--el-color-danger-dark-2);
  cursor: help;
  .el-icon {
    flex: none;
    color: var(--el-color-danger);
    font-size: 13px;
  }
  span {
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}
.debug-error-tip__content {
  min-width: 0;
}
.debug-error-tip__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 7px;
  border-bottom: 1px solid var(--el-color-danger-light-8);
  strong {
    color: var(--el-color-danger);
    font-size: 12px;
    font-weight: 600;
  }
}
.debug-error-tip__text {
  margin: 0;
  padding-top: 8px;
  color: var(--el-text-color-regular);
  font-family: ui-monospace, SFMono-Regular, Consolas, "Liberation Mono", monospace;
  font-size: 12px;
  line-height: 1.65;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}
.debug-no-error {
  color: var(--el-text-color-placeholder);
}
.debug-sample__header {
  min-height: 44px;
  height: auto;
  gap: 8px;
  padding-top: 5px;
  padding-bottom: 5px;
}
.debug-sample__title {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 8px;
  span {
    flex: none;
    color: var(--el-text-color-primary);
    font-size: 13px;
    font-weight: 600;
  }
  small {
    min-width: 0;
    color: var(--el-text-color-secondary);
    font-size: 11px;
    font-weight: normal;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
}
.debug-sample__actions {
  flex: none;
  display: flex;
  align-items: center;
  gap: 6px;
}
.debug-sample__search {
  width: 132px;
  :deep(.el-input__inner) {
    font-size: 12px;
  }
}
.debug-sample__table {
  flex: none;
  min-height: 0;
  overflow: visible;
  :deep(.el-table) {
    --el-table-header-bg-color: #f8fafc;
    --el-table-row-hover-bg-color: #f3f8ff;
    --el-table-border-color: var(--el-border-color-extra-light);
    overflow: visible;
    font-size: 12px;
  }
  :deep(.el-table__body-wrapper),
  :deep(.el-table__body-wrapper .el-scrollbar),
  :deep(.el-table__body-wrapper .el-scrollbar__wrap) {
    overflow-x: auto;
    overflow-y: clip;
  }
  :deep(.el-table__body-header) {
    position: sticky;
    top: 0;
    z-index: 5;
  }
  :deep(.el-table__body-header th) {
    background: var(--el-table-header-bg-color);
    box-shadow: 0 1px 0 var(--el-border-color-light);
  }
  :deep(.el-table th.el-table__cell) {
    height: 42px;
    padding: 0;
  }
  :deep(.el-table td.el-table__cell) {
    height: 36px;
    padding: 0;
  }
  :deep(.el-table .cell) {
    padding-right: 12px;
    padding-left: 12px;
  }
  :deep(.el-table__row) {
    cursor: pointer;
  }
}
.debug-sample__column-head {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
  span {
    color: var(--el-text-color-primary);
    font-weight: 500;
  }
  small {
    max-width: 180px;
    color: var(--el-text-color-secondary);
    font-size: 11px;
    font-weight: normal;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
}
.debug-sample__cell {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: middle;
  &.is-null,
  &.is-empty-string {
    min-height: 18px;
    padding: 1px 6px;
    border-radius: 4px;
    line-height: 16px;
  }
  &.is-null {
    color: var(--el-text-color-secondary);
    background: var(--el-fill-color-light);
  }
  &.is-empty-string {
    color: var(--el-color-warning-dark-2);
    background: var(--el-color-warning-light-9);
    box-shadow: 0 0 0 1px var(--el-color-warning-light-7) inset;
  }
  &.is-object {
    color: var(--el-color-primary);
  }
}
:deep(.debug-target-row) {
  --el-table-tr-bg-color: var(--el-color-primary-light-9);
}
:deep(.debug-failed-row) {
  --el-table-tr-bg-color: var(--el-color-danger-light-9);
}
@media (max-width: 980px) {
  .debug-header-time {
    display: none;
  }
}
</style>
