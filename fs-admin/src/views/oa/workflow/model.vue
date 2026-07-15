<script setup lang="ts">
import LayoutDesigner from '@/components/Layout/LayoutDesigner.vue'
import LayoutProperty from '@/components/Layout/LayoutProperty.vue'
import LayoutWidget from '@/components/Layout/LayoutWidget.vue'
import LayoutToolbar from '@/components/Layout/LayoutToolbar.vue'
import X6Container from '@/designer/X6/X6Container.vue'
import LayoutIcon from '@/components/Layout/LayoutIcon.vue'
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import config from '@/designer/Workflow/config'
import WorkflowApi from '@/api/oa/WorkflowApi'
import ApiUtil from '@/utils/ApiUtil'
import DesignUtil from '@/utils/DesignUtil'
import BPMNConverter from '@/designer/Workflow/bpmn/BPMNConverter'
import { registerBPMNNodes } from '@/designer/Workflow/nodes'

// Register BPMN node types before Flow instance creation
registerBPMNNodes()

const route = useRoute()
const router = useRouter()
const flowRef = ref()
const tips: any = ref({})
const diagram: any = ref(Object.assign(config.canvas.options(), { id: 0, name: '', content: '' }))
const activeItem: any = ref(({}))
const property = computed(() => {
  return DesignUtil.widgetFlowProperty(activeItem.value, config)
})

const options: any = {
}

const handleDragStart = (event: any, widget: any) => {
  flowRef.value.flow.startDrag(event, widget)
}

const loading = ref(false)

const handleSubmit = () => {
  loading.value = true
  const cells = flowRef.value.flow.toJSON().cells
  const processId = diagram.value.id ? `Process_${diagram.value.id}` : 'Process_iisquare'
  const processName = diagram.value.name || 'Process'
  const content = BPMNConverter.serialize(cells, processId, processName)
  const params = {
    id: diagram.value.id || undefined,
    name: diagram.value.name,
    content,
  }
  WorkflowApi.save(params, { success: true }).then((result: any) => {
    diagram.value.id = result.data.id
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}

const handleReload = () => {
  const id = route.query.id
  if (!id) return
  loading.value = true
  WorkflowApi.info({ id, withForm: true }).then(result => {
    Object.assign(diagram.value, ApiUtil.data(result))
    const content = diagram.value.content
    if (content) {
      try {
        const graph = BPMNConverter.parse(content)
        flowRef.value.flow.fromJSON(graph.cells)
      } catch (e: any) {
        tips.value.text = `解析BPMN XML异常: ${e.message}`
      }
    }
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}

const handleNewModel = () => {
  const processId = diagram.value.id ? `Process_${diagram.value.id}` : 'Process_iisquare'
  const xml = BPMNConverter.defaultTemplate(processId)
  try {
    const graph = BPMNConverter.parse(xml)
    flowRef.value.flow.fromJSON(graph.cells)
  } catch (e: any) {
    tips.value.text = `新建模型异常: ${e.message}`
  }
}

const handleExportXML = () => {
  const cells = flowRef.value.flow.toJSON().cells
  const processId = diagram.value.id ? `Process_${diagram.value.id}` : 'Process_iisquare'
  const processName = diagram.value.name || 'Process'
  const content = BPMNConverter.serialize(cells, processId, processName)
  const blob = new Blob([content], { type: 'application/xml' })
  const url = window.URL.createObjectURL(blob)
  const dom = document.createElement('a')
  dom.href = url
  dom.download = (diagram.value.name || 'process') + '.bpmn20.xml'
  dom.click()
  window.URL.revokeObjectURL(url)
}

const handleExportSVG = () => {
  flowRef.value.flow.graph.exportSVG().then((data: any) => {
    const blob = new Blob([data], { type: 'image/svg+xml' })
    const url = window.URL.createObjectURL(blob)
    const dom = document.createElement('a')
    dom.href = url
    dom.download = (diagram.value.name || 'process') + '.svg'
    dom.click()
    window.URL.revokeObjectURL(url)
  })
}

const handleImportXML = (file: File) => {
  const reader = new FileReader()
  reader.onload = () => {
    try {
      const graph = BPMNConverter.parse(reader.result as string)
      flowRef.value.flow.fromJSON(graph.cells)
      tips.value.text = '导入模型成功'
    } catch (e: any) {
      tips.value.text = `导入模型异常: ${e.message}`
    }
  }
  reader.readAsText(file)
  return false
}

const fileInputRef = ref<HTMLInputElement>()

onMounted(() => {
  WorkflowApi.config().then(result => {
    Object.assign(config, ApiUtil.data(result))
  }).catch(() => {})
  handleReload()
})
</script>

<template>
  <LayoutDesigner>
    <template #left>
      <LayoutWidget :widgets="config.widgets" @drag-start="handleDragStart" />
    </template>
    <template #top>
      <LayoutToolbar :toolbars="config.toolbars" :instance="flowRef" />
      <el-space>
        <el-dropdown>
          <el-button>文件</el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="handleReload" :disabled="loading">重新载入</el-dropdown-item>
              <el-dropdown-item @click="handleSubmit" :disabled="loading">保存模型</el-dropdown-item>
              <el-dropdown-item @click="handleNewModel">新建模型</el-dropdown-item>
              <el-dropdown-item divided @click="() => fileInputRef?.click()">导入模型</el-dropdown-item>
              <el-dropdown-item divided @click="handleExportXML">导出为XML</el-dropdown-item>
              <el-dropdown-item @click="handleExportSVG">导出为SVG</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <input ref="fileInputRef" type="file" accept=".xml" style="display:none"
          @change="(e: any) => e.target.files[0] && handleImportXML(e.target.files[0])" />
        <el-button type="primary" @click="handleSubmit" :loading="loading">保存</el-button>
        <el-button @click="router.go(-1)">返回</el-button>
      </el-space>
    </template>
    <template #default>
      <X6Container ref="flowRef" v-model="diagram" :active-item="activeItem" :tips="tips" :options="options"
        @update:active-item="v => activeItem = v" />
    </template>
    <template #right>
      <LayoutProperty v-model="diagram" :active-item="activeItem" :instance="flowRef" :config="config" :tips="tips"
        :property="property" />
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
</style>
