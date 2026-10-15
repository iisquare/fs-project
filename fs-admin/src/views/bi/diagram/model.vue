<script setup lang="ts">
import LayoutDesigner from '@/components/Layout/LayoutDesigner.vue';
import LayoutBack from '@/components/Layout/LayoutBack.vue';
import LayoutProperty from '@/components/Layout/LayoutProperty.vue';
import LayoutWidget from '@/components/Layout/LayoutWidget.vue';
import LayoutToolbar from '@/components/Layout/LayoutToolbar.vue';
import LayoutIcon from '@/components/Layout/LayoutIcon.vue';
import X6Container from '@/designer/X6/X6Container.vue';
import { computed, nextTick, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import config from '@/designer/BIDag/config'
import DesignUtil from '@/utils/DesignUtil';
import DiagramApi from '@/api/bi/DiagramApi';
import ApiUtil from '@/utils/ApiUtil';
import ElementUtil from '@/utils/ElementUtil';
import { ElMessage } from 'element-plus';

const route = useRoute()
const router = useRouter()
const flowRef = ref()
const tips: any = ref({ text: '' })
const activeItem: any = ref({})
const debugVisible = ref(false)
const debugRows: any = ref([])
const loading = ref(false)
const diagram: any = ref(Object.assign(config.canvas.options(), {
  name: '未命名编排',
  engine: 'spark',
  model: 'batch',
  clusterId: undefined,
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

/** 按当前计算引擎与处理模式过滤可用节点 */
const widgets = computed(() => {
  return config.widgets.map((group: any) => Object.assign({}, group, {
    children: group.children.filter((item: any) => supportsNode(item.support, diagram.value.engine, diagram.value.model)),
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
    Object.assign(diagram.value, data, { status: String(data.status), notify: data.notify ?? {} })
    flowRef.value.flow.fromJSON(data.content?.cells ?? [])
    nextTick(() => { loadingDiagram = false })
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}

const handleSubmit = () => {
  loading.value = true
  const params = Object.assign({}, diagram.value, {
    schedule: diagram.value.schedule ? true : false,
    autoRecover: diagram.value.autoRecover ? true : false,
    content: flowRef.value.flow.toJSON(),
  })
  DiagramApi.save(params, { success: true }).then((result: any) => {
    diagram.value.id = ApiUtil.data(result).id
    if (!route.query.id) router.replace({ query: { id: diagram.value.id } }).catch(() => {})
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

const handleDebug = () => {
  if (!diagram.value.id) {
    ElMessage.warning('请先保存编排流程')
    return
  }
  if (!activeItem.value || !activeItem.value.id || activeItem.value.shape === 'flow-edge') {
    ElMessage.warning('请先选择需要调试的节点')
    return
  }
  loading.value = true
  const nodeId = `dag_${diagram.value.id}_${activeItem.value.id}`
  DiagramApi.debug({ id: diagram.value.id, nodeId }, { success: true }).then((result: any) => {
    const data = ApiUtil.data(result) ?? {}
    const outputs = data.outputs ?? {}
    const colors: any = {}
    debugRows.value = Object.keys(outputs).map((key: string) => ({
      id: key,
      label: key.replace(`dag_${diagram.value.id}_`, ''),
      output: outputs[key],
    }))
    debugRows.value.forEach((row: any) => { colors[`dag_${diagram.value.id}_${row.label}`] = '#67c23a' })
    colors[nodeId] = '#409eff'
    flowRef.value.flow.highlight(colors)
    debugVisible.value = true
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}

onMounted(() => {
  DiagramApi.config().then((result: any) => {
    Object.assign(config, ApiUtil.data(result))
  }).catch(() => {})
  handleReload()
})
</script>

<template>
  <LayoutDesigner splitter>
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
        <el-button @click="handleDebug" :loading="loading">调试节点</el-button>
        <el-button @click="handleRun" :loading="loading">运行作业</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="loading">保存</el-button>
      </el-space>
    </template>
    <template #default>
      <X6Container ref="flowRef" v-model="diagram" :active-item="activeItem" :tips="tips" :options="options" @update:active-item="(v: any) => activeItem = v" />
    </template>
    <template #right>
      <LayoutProperty v-model="diagram" :active-item="activeItem" :instance="flowRef" :config="config" :tips="tips" :property="property" />
    </template>
    <template #footer>
      <el-space>
        <LayoutIcon name="Opportunity" color="#409eff" />
        <div>{{ tips.text }}</div>
      </el-space>
    </template>
  </LayoutDesigner>
  <el-drawer v-model="debugVisible" title="节点调试输出" size="60%" :destroy-on-close="true">
    <el-empty v-if="debugRows.length === 0" description="暂无调试输出" />
    <el-card v-for="row in debugRows" :key="row.id" shadow="never" class="debug-item">
      <template #header>
        <span>{{ row.label }}</span>
      </template>
      <pre class="debug-output">{{ JSON.stringify(row.output, null, 2) }}</pre>
    </el-card>
  </el-drawer>
</template>

<style lang="scss" scoped>
.debug-item {
  margin-bottom: 10px;
}
.debug-output {
  max-height: 320px;
  margin: 0;
  overflow: auto;
  font-size: 12px;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
