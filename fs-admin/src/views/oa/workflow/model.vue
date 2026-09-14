<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, shallowRef } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import * as ElementPlusIcons from '@element-plus/icons-vue'
import LayoutDesigner from '@/components/Layout/LayoutDesigner.vue'
import LayoutWidget from '@/components/Layout/LayoutWidget.vue'
import LayoutIcon from '@/components/Layout/LayoutIcon.vue'
import Property from '@/designer/Workflow/Property.vue'
import BpmnModeler from '@/designer/Workflow/bpmn/BpmnModeler'
import config from '@/designer/Workflow/config'
import templates from '@/designer/Workflow/bpmn/template'
import WorkflowApi from '@/api/oa/WorkflowApi'
import ApiUtil from '@/utils/ApiUtil'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const canvasRef = ref<HTMLDivElement>()
// bpmn-js 实例与元素不可被 Vue 深度代理，使用 shallowRef
const bpmn = shallowRef<any>(null)
const activeElement = shallowRef<any>(null)
const activeToolbar = ref<any>(null)
const loading = ref(false)
const tips = ref('')
// 新建流程时在流程属性面板中编辑名称、关联表单等信息
const initInfo = () => {
  return { id: 0, name: '', formId: null, sort: 0, status: 1, description: '', content: '' }
}
const info = ref<any>(initInfo())
const fileInputRef = ref<HTMLInputElement>()
// 画布尺寸变化（分隔面板拖拽、窗口缩放）时需告知 bpmn-js 重新计算视图
let resizeObserver: ResizeObserver | null = null

const zoom = (type: string) => {
  const canvas = bpmn.value?.canvas
  if (!canvas) return false
  switch (type) {
    case 'in': // 放大
      tips.value = '放大画布'
      canvas.zoom(canvas.zoom() + 0.1)
      break
    case 'out': // 缩小
      tips.value = '缩小画布'
      canvas.zoom(canvas.zoom() - 0.1)
      break
    default: // 自适应
      tips.value = '画布自适应'
      canvas.zoom('fit-viewport', 'auto')
  }
  return true
}

const canCommand = (cmd: string) => {
  switch (cmd) {
    case 'cut':
    case 'copy':
    case 'delete':
      return activeElement.value !== null
    case 'paste':
      return !!bpmn.value?._elementCopied
    default:
      return false
  }
}

const runCommand = (cmd: string) => {
  const instance = bpmn.value
  if (!instance) return false
  try {
    switch (cmd) {
      case 'undo': // 撤销
        instance.commandStack.undo()
        break
      case 'redo': // 重做
        instance.commandStack.redo()
        break
      case 'cut': // 剪切
        instance.copy(activeElement.value)
        if (activeElement.value) {
          instance.modeling.removeElements([activeElement.value])
        }
        break
      case 'copy': // 复制
        instance.copy(activeElement.value)
        break
      case 'paste': // 粘贴
        instance.paste()
        break
      case 'delete': // 删除
        if (activeElement.value) {
          instance.modeling.removeElements([activeElement.value])
        }
        break
    }
    tips.value = `执行 ${cmd} 指令完成`
  } catch (e: any) {
    tips.value = `执行 ${cmd} 指令异常: ${e.message}`
  }
  return true
}

const download = (type: string) => {
  if (!bpmn.value) return false
  tips.value = `导出${type}文件`
  bpmn.value.modeler[type === 'XML' ? 'saveXML' : 'saveSVG']({ format: true }).then((result: any) => {
    const data = type === 'XML' ? result.xml : result.svg
    const blob = new Blob([data], { type: type === 'XML' ? 'application/xml' : 'image/svg+xml' })
    const url = window.URL.createObjectURL(blob)
    const dom = document.createElement('a')
    dom.href = url
    dom.download = (info.value.name || 'process') + (type === 'XML' ? '.bpmn20.xml' : '.svg')
    dom.click()
    window.URL.revokeObjectURL(url)
  }).catch((e: any) => {
    tips.value = `导出${type}异常: ${e.message}`
  })
  return true
}

const bpmnImportXML = async (xml?: string, processId = '') => {
  if (!bpmn.value) return false
  if (!xml) {
    xml = templates.flowable
    // 新建流程尚无主键，使用时间戳作为默认流程标识以保证唯一
    if (!processId) processId = 'fs-' + (info.value.id || Date.now())
  }
  try {
    tips.value = '正在解析XML数据...'
    await bpmn.value.modeler.importXML(xml)
    const definitions = bpmn.value.modeler.getDefinitions()
    if (processId && definitions && definitions.rootElements.length > 0) {
      definitions.rootElements[0].id = processId
    }
    zoom('fit')
  } catch (e: any) {
    tips.value = `解析XML异常: ${e.message}`
  } finally {
    tips.value = '载入XML完成'
    activeElement.value = null
  }
  return true
}

const handleImportXML = (file: File) => {
  const reader = new FileReader()
  reader.onload = () => bpmnImportXML(reader.result as string)
  reader.readAsText(file)
  return true
}

const handleNewModel = () => {
  return bpmnImportXML(templates.flowable, 'fs-' + (info.value.id || Date.now()))
}

const handleDragStart = (event: any, widget: any) => {
  widget.callback(widget, bpmn.value, event)
}

// 画布工具需在画布上操作才生效，先记录待激活的工具，待鼠标进入画布时再执行
const handleToolbar = (toolbar: any) => {
  activeToolbar.value = activeToolbar.value === toolbar ? null : toolbar
}

const handleReload = () => {
  const id = route.query.id
  if (!id) {
    info.value = initInfo()
    bpmnImportXML()
    return true
  }
  loading.value = true
  tips.value = '正在载入数据信息...'
  WorkflowApi.info({ id, withForm: true }).then((result: any) => {
    if (!ApiUtil.succeed(result)) return false
    const data = ApiUtil.data(result)
    info.value = Object.assign({}, data, { formId: data.formId || null })
    bpmnImportXML(info.value.content || templates.flowable)
    return true
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
  return true
}

// 流程标识将作为流程定义标识（key）使用，需满足 BPMN ID 规范
const validateIdentity = () => {
  const definitions = bpmn.value?.modeler?.getDefinitions ? bpmn.value.modeler.getDefinitions() : null
  const root = definitions && definitions.rootElements.length > 0 ? definitions.rootElements[0] : null
  const id = (root?.id || '').trim()
  if (!id) {
    ElMessage.warning('请输入流程标识')
    return false
  }
  if (!/^[A-Za-z_][\w.-]*$/.test(id)) {
    ElMessage.warning('流程标识需以字母或下划线开头，且不能包含空格等特殊字符')
    return false
  }
  return true
}

const handleSave = () => {
  if (!bpmn.value) return false
  if (!info.value.name) {
    ElMessage.warning('请输入流程名称')
    return false
  }
  if (!validateIdentity()) return false
  loading.value = true
  tips.value = '正在保存...'
  bpmn.value.modeler.saveXML({ format: true }).then((result: any) => {
    const params: any = {
      id: info.value.id || undefined,
      name: info.value.name,
      description: info.value.description || '',
      sort: info.value.sort || 0,
      status: info.value.status || 1,
      content: result.xml
    }
    if (info.value.formId) params.formId = info.value.formId
    return WorkflowApi.save(params, { success: true })
  }).then((result: any) => {
    const data = result && result.data
    if (data && data.id) {
      info.value.id = data.id
      if (!route.query.id) { // 新建成功后携带主键，刷新页面仍可载入该流程
        router.replace({ path: route.path, query: Object.assign({}, route.query, { id: data.id }) })
      }
    }
    tips.value = '保存完成'
    return true
  }).catch((e: any) => {
    tips.value = `保存异常: ${e?.message || e}`
    return false
  }).finally(() => {
    loading.value = false
  })
  return true
}

const unHandledRejection = (event: any) => {
  tips.value = `未捕获的异常: ${event?.reason?.message || event?.reason}`
}

onMounted(() => {
  bpmn.value = new BpmnModeler(canvasRef.value, config, true)
  // 工具栏工具需在画布上按下鼠标后才生效
  bpmn.value.modeler._container.addEventListener('mouseenter', (event: any) => {
    if (activeToolbar.value !== null) {
      activeToolbar.value.callback(activeToolbar.value, bpmn.value, event)
      activeToolbar.value = null
    }
  })
  ;['shape.added', 'element.click'].forEach(name => {
    bpmn.value.modeler.on(name, (event: any) => {
      if (!event.element || event.element.type === 'label') return true
      if (event.element.parent && config.elements[event.element.type]) {
        activeElement.value = event.element
        tips.value = '选中元素 - ' + activeElement.value.id
      } else {
        activeElement.value = null
        tips.value = '选中画布'
      }
      if (name === 'shape.added' && event.element.type === 'bpmn:UserTask') {
        bpmn.value.fixedUserTask(event.element)
      }
      return true
    })
  })
  ;['shape.removed', 'connection.removed'].forEach(name => {
    bpmn.value.modeler.on(name, (event: any) => {
      if (!event.element || event.element.type === 'label') return true
      if (activeElement.value !== null) {
        tips.value = '选中元素已移除'
      }
      activeElement.value = null
      return true
    })
  })
  // 禁用双击编辑
  bpmn.value.modeler.on('element.dblclick', 10000, () => false)
  handleReload()
  if (typeof ResizeObserver !== 'undefined' && canvasRef.value) {
    resizeObserver = new ResizeObserver(() => bpmn.value?.canvas?.resized())
    resizeObserver.observe(canvasRef.value)
  }
  window.addEventListener('unhandledrejection', unHandledRejection)
})

onBeforeUnmount(() => {
  resizeObserver?.disconnect()
  resizeObserver = null
  window.removeEventListener('unhandledrejection', unHandledRejection)
})
</script>

<template>
  <LayoutDesigner splitter>
    <template #left>
      <LayoutWidget :widgets="config.widgets" @drag-start="handleDragStart" />
    </template>
    <template #top>
      <!-- 画布工具：选中后在画布上生效 -->
      <el-space class="toolbar">
        <LayoutBack to="/oa/workflow/list" />
        <el-divider direction="vertical" />
        <el-tooltip :content="toolbar.label" placement="bottom" :show-after="300" :key="toolbar.type" v-for="toolbar in config.toolbars">
          <el-button
            link
            :class="['toolbar-icon', activeToolbar === toolbar && 'selected']"
            @click="() => handleToolbar(toolbar)">
            <LayoutIcon :name="toolbar.icon" />
          </el-button>
        </el-tooltip>
        <el-divider direction="vertical" />
      </el-space>
      <el-space class="menus">
        <el-dropdown popper-class="fs-designer-menu">
          <el-button text :icon="ElementPlusIcons.FolderOpened">文件</el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item :icon="ElementPlusIcons.Refresh" @click="handleReload" :disabled="loading">重新载入</el-dropdown-item>
              <el-dropdown-item :icon="ElementPlusIcons.DocumentChecked" @click="handleSave" :disabled="loading">保存模型</el-dropdown-item>
              <el-dropdown-item :icon="ElementPlusIcons.DocumentAdd" @click="handleNewModel">新建模型</el-dropdown-item>
              <el-dropdown-item divided :icon="ElementPlusIcons.Upload" @click="() => fileInputRef?.click()">导入模型</el-dropdown-item>
              <el-dropdown-item divided :icon="ElementPlusIcons.Download" @click="() => download('XML')">导出为XML</el-dropdown-item>
              <el-dropdown-item :icon="ElementPlusIcons.Picture" @click="() => download('SVG')">导出为SVG</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <input ref="fileInputRef" type="file" accept=".xml" style="display:none"
          @change="(e: any) => e.target.files[0] && handleImportXML(e.target.files[0])" />
        <el-dropdown popper-class="fs-designer-menu">
          <el-button text :icon="ElementPlusIcons.EditPen">编辑</el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item :icon="ElementPlusIcons.RefreshLeft" @click="() => runCommand('undo')">撤销</el-dropdown-item>
              <el-dropdown-item :icon="ElementPlusIcons.RefreshRight" @click="() => runCommand('redo')">重做</el-dropdown-item>
              <el-dropdown-item divided :icon="ElementPlusIcons.Scissor" :disabled="!canCommand('cut')" @click="() => runCommand('cut')">剪切</el-dropdown-item>
              <el-dropdown-item :icon="ElementPlusIcons.CopyDocument" :disabled="!canCommand('copy')" @click="() => runCommand('copy')">复制</el-dropdown-item>
              <el-dropdown-item :icon="ElementPlusIcons.DocumentCopy" :disabled="!canCommand('paste')" @click="() => runCommand('paste')">粘贴</el-dropdown-item>
              <el-dropdown-item divided :icon="ElementPlusIcons.Delete" :disabled="!canCommand('delete')" @click="() => runCommand('delete')">删除</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <el-dropdown popper-class="fs-designer-menu" @command="zoom">
          <el-button text :icon="ElementPlusIcons.View">视图</el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="in" :icon="ElementPlusIcons.ZoomIn">放大</el-dropdown-item>
              <el-dropdown-item command="out" :icon="ElementPlusIcons.ZoomOut">缩小</el-dropdown-item>
              <el-dropdown-item divided command="fit" :icon="ElementPlusIcons.FullScreen">自动适应</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <el-divider direction="vertical" />
        <el-button type="primary" text :icon="ElementPlusIcons.Check" @click="handleSave" :loading="loading">保存</el-button>
      </el-space>
    </template>
    <template #default>
      <div class="canvas" ref="canvasRef"></div>
    </template>
    <template #right>
      <Property :bpmn="bpmn" :element="activeElement" :config="config" v-model="info"
        :key="activeElement ? activeElement.id : 'canvas'" />
    </template>
    <template #footer>
      <el-space>
        <LayoutIcon name="Opportunity" color="#409eff" />
        <div>{{ tips }}</div>
      </el-space>
    </template>
  </LayoutDesigner>
</template>

<style lang="scss" scoped>
.toolbar {
  .el-divider--vertical {
    margin: 0 6px;
  }
  .toolbar-icon {
    padding: 5px;
    color: var(--el-text-color-regular);
    &:hover {
      color: var(--el-color-primary);
      background: var(--el-fill-color-light);
    }
    &.selected {
      color: var(--el-color-primary);
      background: var(--el-color-primary-light-9);
    }
  }
}
// Element Plus 在 :focus-visible 时会给按钮加主色描边，
// 下拉菜单（hover 触发）展开/收起会把焦点还给触发按钮，导致鼠标悬停时出现延迟的蓝色外框
.toolbar,
.menus {
  :deep(.el-button) {
    &:focus,
    &:focus-visible {
      outline: none;
    }
  }
}
// 菜单项名称较短时（如撤销/重做）面板过窄，统一最小宽度
:global(.fs-designer-menu) {
  min-width: 120px;
}
.canvas {
  width: 100%;
  height: 100%;
  :deep(.djs-palette) {
    display: none;
  }
}
</style>
