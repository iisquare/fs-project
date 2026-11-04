<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import type { FormInstance } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Back, Check, RefreshLeft } from '@element-plus/icons-vue'
import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router'
import DashboardTemplateApi from '@/api/bi/DashboardTemplateApi'
import ApiUtil from '@/utils/ApiUtil'
import DashboardTemplateStyleFields from '@/views/bi/report/dashboard/DashboardTemplateStyleFields.vue'
import {
  commonStyle,
  componentStyleDefaults,
  createWidget,
  defaultDashboard,
  styleDefaults,
  widgetGroups,
  type DashboardModel,
  type DashboardStyle,
  type DashboardWidget,
  type DashboardWidgetType,
} from '@/views/bi/components/dashboard/model'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()
const form = ref({
  id: undefined as number | undefined,
  name: '',
  status: '1',
  sort: 0,
  description: '',
})
const config = ref<any>({ status: { 1: '启用', 2: '禁用' } })
const model = ref<DashboardModel>(createTemplateModel())
const savedSnapshot = ref('')
let syncingSpecialStyle = false
let allowRouteLeave = false

const typedWidgetGroups = widgetGroups.map((group) => ({
  ...group,
  children: group.children.map((item) => ({
    ...item,
    value: item.type as DashboardWidgetType,
  })),
}))
const componentOptions = typedWidgetGroups.flatMap((group) => (
  group.children.map((item) => ({
    group: group.name,
    label: item.label,
    value: item.value,
  }))
))
const canvasLayoutOptions = [
  { label: '自由定位', value: 'absolute', description: '组件自由摆放' },
  { label: '相对定位', value: 'relative', description: '文档流内偏移' },
  { label: '固定定位', value: 'fixed', description: '固定在画布视口' },
  { label: '弹性布局', value: 'flex', description: '自动排列与伸缩' },
  { label: '栅格布局', value: 'grid', description: '按行列对齐' },
] as const
const sections = [
  { id: 'template-meta', label: '模板信息' },
  { id: 'template-layout', label: '页面布局' },
  { id: 'template-common', label: '通用样式' },
  { id: 'template-special', label: '特殊组件' },
]
const specialWidgets = ref<Partial<Record<DashboardWidgetType, DashboardWidget>>>({})
const customTypes = computed(() => Object.keys(model.value.componentDefaults || {}) as DashboardWidgetType[])
const dirty = computed(() => Boolean(savedSnapshot.value) && savedSnapshot.value !== snapshot())
const previewStyle = computed(() => ({
  ...commonStyle(createWidget('Metric', {}, model.value.commonComponentStyle || undefined).style),
  margin: '0',
}))
const commonStyleForm = computed(() => model.value.commonComponentStyle as DashboardStyle)
const canvasPreviewStyle = computed(() => {
  const background = String(model.value.canvas.background || '').trim()
  const gradient = /gradient\(/i.test(background)
  return {
    aspectRatio: `${model.value.canvas.width} / ${model.value.canvas.height}`,
    backgroundColor: !gradient ? background || undefined : undefined,
    backgroundImage: model.value.canvas.backgroundImage
      ? `url("${model.value.canvas.backgroundImage}")`
      : gradient ? background : undefined,
    backgroundSize: 'cover',
    backgroundPosition: 'center',
  }
})
const canvasSolidColor = computed(() => {
  const background = String(model.value.canvas.background || '').trim()
  if (!background) return 'transparent'
  return /^(#|rgb|hsl)/i.test(background) ? background : '#020b1c'
})
const applyCanvasSolidColor = (color: string | null) => {
  model.value.canvas.background = color || ''
}
const scrollToSection = (id: string) => {
  document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function createTemplateModel(): DashboardModel {
  return {
    ...defaultDashboard(),
    commonComponentStyle: styleDefaults(),
    componentDefaults: {},
    widgets: [],
  }
}

const clone = <T,>(value: T): T => JSON.parse(JSON.stringify(value))
const updateCommonStyle = (style: DashboardStyle) => {
  model.value.commonComponentStyle = style
}
const updateSpecialStyle = (type: DashboardWidgetType, style: DashboardStyle) => {
  if (!specialWidgets.value[type]) return
  specialWidgets.value[type]!.style = style
}
const snapshot = () => JSON.stringify({ form: form.value, model: model.value })
const rememberSavedState = () => {
  savedSnapshot.value = snapshot()
}

const specialEnabled = (type: DashboardWidgetType) => Object.prototype.hasOwnProperty.call(
  model.value.componentDefaults || {},
  type,
)
const effectiveStyleFor = (type: DashboardWidgetType): DashboardStyle => {
  if (specialEnabled(type) && specialWidgets.value[type]) return specialWidgets.value[type]!.style
  return createWidget(type, {}, model.value.commonComponentStyle || undefined).style
}
const stylePreviewFor = (type: DashboardWidgetType) => ({
  ...commonStyle(effectiveStyleFor(type)),
  margin: '0',
})

const loadSpecialStyles = () => {
  syncingSpecialStyle = true
  const widgets: Partial<Record<DashboardWidgetType, DashboardWidget>> = {}
  componentOptions.forEach((item) => {
    widgets[item.value] = createWidget(
      item.value,
      { name: item.label },
      componentStyleDefaults(model.value, item.value),
    )
  })
  specialWidgets.value = widgets
  nextTick(() => {
    syncingSpecialStyle = false
  })
}

watch(
  specialWidgets,
  (widgets) => {
    if (syncingSpecialStyle) return
    Object.entries(widgets).forEach(([type, widget]) => {
      if (!widget || !specialEnabled(type as DashboardWidgetType)) return
      if (!model.value.componentDefaults) model.value.componentDefaults = {}
      model.value.componentDefaults[type as DashboardWidgetType] = clone(widget.style)
    })
  },
  { deep: true },
)

const toggleSpecialStyle = (type: DashboardWidgetType, enabled: boolean | string | number) => {
  if (!model.value.componentDefaults) model.value.componentDefaults = {}
  if (Boolean(enabled)) {
    const style = createWidget(
      type,
      {},
      model.value.commonComponentStyle || undefined,
    ).style
    model.value.componentDefaults[type] = clone(style)
  } else {
    delete model.value.componentDefaults[type]
  }
  loadSpecialStyles()
}
const resetCommonStyle = () => {
  model.value.commonComponentStyle = styleDefaults()
  loadSpecialStyles()
  ElMessage.success('通用组件样式已恢复为系统默认')
}
const removeAllSpecialStyles = async () => {
  try {
    await ElMessageBox.confirm('确定清除所有组件类型的个性化样式吗？', '清除个性化样式', {
      type: 'warning',
      confirmButtonText: '清除',
      cancelButtonText: '取消',
    })
    model.value.componentDefaults = {}
    loadSpecialStyles()
  } catch {
    // 用户取消。
  }
}

const parseTemplate = (content: string) => {
  if (!content) return createTemplateModel()
  try {
    const parsed = typeof content === 'string' ? JSON.parse(content) : content
    const defaults = createTemplateModel()
    return {
      ...defaults,
      canvas: Object.assign(defaults.canvas, parsed.canvas || {}),
      commonComponentStyle: Object.assign(styleDefaults(), parsed.commonComponentStyle || {}),
      componentDefaults: parsed.componentDefaults || {},
      widgets: [],
    }
  } catch (error: any) {
    ElMessage.error('模板配置解析失败：' + error.message)
    return createTemplateModel()
  }
}

const load = async () => {
  loading.value = true
  try {
    const configResult: any = await DashboardTemplateApi.config().catch(() => null)
    if (configResult && ApiUtil.succeed(configResult)) {
      Object.assign(config.value, ApiUtil.data(configResult))
    }
    if (!route.query.id) {
      model.value = createTemplateModel()
      form.value = { id: undefined, name: '', status: '1', sort: 0, description: '' }
    } else {
      const result: any = await DashboardTemplateApi.info(route.query.id)
      if (!ApiUtil.succeed(result)) return
      const data = result.data || {}
      form.value = {
        id: data.id,
        name: data.name || '',
        status: String(data.status || 1),
        sort: Number(data.sort) || 0,
        description: data.description || '',
      }
      model.value = parseTemplate(data.content)
    }
    await nextTick()
    loadSpecialStyles()
    rememberSavedState()
  } catch {
    // API 错误由统一请求层提示。
  } finally {
    loading.value = false
  }
}

const save = () => {
  formRef.value?.validate(async (valid: boolean) => {
    if (!valid || saving.value) return
    saving.value = true
    try {
      const content = JSON.stringify({
        version: 3,
        canvas: model.value.canvas,
        commonComponentStyle: model.value.commonComponentStyle || {},
        componentDefaults: model.value.componentDefaults || {},
        widgets: [],
      })
      const result: any = await DashboardTemplateApi.save({
        ...form.value,
        status: Number(form.value.status),
        content,
      }, { success: true })
      if (!ApiUtil.succeed(result)) return
      if (!form.value.id && result.data?.id) {
        form.value.id = result.data.id
        await router.replace({ path: route.path, query: { id: result.data.id } })
      }
      rememberSavedState()
    } catch {
      // API 错误由统一请求层提示。
    } finally {
      saving.value = false
    }
  })
}

const leave = async () => {
  if (allowRouteLeave || !dirty.value) return true
  try {
    await ElMessageBox.confirm('当前模板有未保存的修改，确定离开吗？', '未保存提醒', {
      type: 'warning',
      confirmButtonText: '离开',
      cancelButtonText: '继续编辑',
    })
    return true
  } catch {
    return false
  }
}
const handleBack = async () => {
  if (saving.value) return
  if (dirty.value) {
    try {
      await ElMessageBox.confirm('当前模板有未保存的修改，确定离开吗？', '未保存提醒', {
        type: 'warning',
        confirmButtonText: '离开',
        cancelButtonText: '继续编辑',
      })
    } catch {
      return
    }
  }
  allowRouteLeave = true
  try {
    await router.push('/bi/olap/dashboardTemplate').catch(() => {})
  } finally {
    allowRouteLeave = false
  }
}

const onBeforeUnload = (event: BeforeUnloadEvent) => {
  if (!dirty.value) return
  event.preventDefault()
  event.returnValue = ''
}

onBeforeRouteLeave(leave)
onMounted(() => {
  load()
  window.addEventListener('beforeunload', onBeforeUnload)
})
onBeforeUnmount(() => {
  window.removeEventListener('beforeunload', onBeforeUnload)
})
</script>

<template>
  <div class="template-editor">
    <header class="template-editor__header">
      <div class="template-editor__identity">
        <el-button text :icon="Back" title="返回" @click="handleBack" />
        <div>
          <strong>{{ form.id ? '编辑大屏模板' : '新建大屏模板' }}</strong>
          <small>页面布局、通用组件样式和特殊组件样式统一配置</small>
        </div>
      </div>
      <nav class="section-nav">
        <button
          v-for="section in sections"
          :key="section.id"
          type="button"
          @click="scrollToSection(section.id)">
          {{ section.label }}
        </button>
      </nav>
      <div class="template-editor__actions">
        <span v-if="dirty" class="dirty-mark">有未保存修改</span>
        <el-button type="primary" :icon="Check" :loading="saving" @click="save">保存模板</el-button>
      </div>
    </header>

    <el-form
      ref="formRef"
      v-loading="loading"
      :model="form"
      label-position="top"
      class="template-form">
      <el-card id="template-meta" shadow="never" class="template-card">
        <template #header>
          <div class="card-heading">
            <strong>模板信息</strong>
            <span>名称、状态和适用范围</span>
          </div>
        </template>
        <div class="meta-grid">
          <el-form-item
            label="模板名称"
            prop="name"
            :rules="[{ required: true, message: '请输入模板名称', trigger: 'blur' }]">
            <el-input v-model="form.name" maxlength="255" show-word-limit placeholder="例如：智慧运营蓝色模板" />
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-select v-model="form.status">
              <el-option v-for="(label, value) in config.status" :key="value" :label="label" :value="String(value)" />
            </el-select>
          </el-form-item>
          <el-form-item label="排序">
            <el-input-number v-model="form.sort" :controls="false" />
          </el-form-item>
          <el-form-item label="描述" class="meta-grid__description">
            <el-input v-model="form.description" type="textarea" :rows="2" placeholder="说明模板适用场景与视觉规范" />
          </el-form-item>
        </div>
      </el-card>

      <div class="layout-columns">
        <el-card id="template-layout" shadow="never" class="template-card">
          <template #header>
            <div class="card-heading">
              <strong>页面布局</strong>
              <span>作为新大屏的画布基准</span>
            </div>
          </template>
          <div class="field-group-grid">
            <section class="field-group">
              <header>
                <strong>设计尺寸</strong>
                <span>画布设计基准</span>
              </header>
              <div class="field-pair">
                <label class="form-field">
                  <span>宽度</span>
                  <el-input-number v-model="model.canvas.width" :controls="false" :min="800" :max="7680" />
                </label>
                <label class="form-field">
                  <span>高度</span>
                  <el-input-number v-model="model.canvas.height" :controls="false" :min="600" :max="4320" />
                </label>
              </div>
            </section>
            <section class="field-group">
              <header>
                <strong>显示尺寸</strong>
                <span>相对可用屏幕</span>
              </header>
              <div class="field-pair">
                <label class="form-field">
                  <span>宽度</span>
                  <div class="unit-input">
                    <el-input-number v-model="model.canvas.displayWidth" :controls="false" />
                    <el-select v-model="model.canvas.displayWidthUnit">
                      <el-option label="%" value="%" />
                      <el-option label="px" value="px" />
                    </el-select>
                  </div>
                </label>
                <label class="form-field">
                  <span>高度</span>
                  <div class="unit-input">
                    <el-input-number v-model="model.canvas.displayHeight" :controls="false" />
                    <el-select v-model="model.canvas.displayHeightUnit">
                      <el-option label="%" value="%" />
                      <el-option label="px" value="px" />
                    </el-select>
                  </div>
                </label>
              </div>
            </section>
          </div>
          <div class="setting-block">
            <strong>布局方式</strong>
            <div class="layout-choice-grid">
            <button
              v-for="option in canvasLayoutOptions"
              :key="option.value"
              type="button"
              :class="{ 'is-active': model.canvas.layout === option.value }"
              @click="model.canvas.layout = option.value">
              <span class="layout-choice-icon" :class="`layout-choice-icon--${option.value}`">
                <i v-for="index in option.value === 'grid' ? 6 : option.value === 'flex' ? 3 : 2" :key="index"></i>
              </span>
              <b>{{ option.label }}</b>
              <small>{{ option.description }}</small>
            </button>
            </div>
          </div>
          <div class="canvas-background">
            <div class="form-field">
              <span>画布背景</span>
              <div class="color-input">
                <el-color-picker
                  :model-value="canvasSolidColor"
                  show-alpha
                  @update:model-value="applyCanvasSolidColor" />
                <el-input v-model="model.canvas.background" placeholder="纯色或 CSS 渐变值" />
              </div>
              <small class="field-hint">选择器用于快速设置纯色；渐变值可直接在右侧输入。</small>
            </div>
            <label class="form-field">
              <span>背景图片地址</span>
              <el-input v-model="model.canvas.backgroundImage" clearable placeholder="留空时使用颜色或渐变背景" />
            </label>
          </div>

          <div v-if="model.canvas.layout === 'grid'" class="setting-block">
            <div class="setting-block__heading">
              <strong>栅格参数</strong>
              <label class="inline-switch">
                <span>辅助网格</span>
                <el-switch v-model="model.canvas.showGrid" />
              </label>
            </div>
            <div class="field-pair-grid">
              <label class="form-field">
                <span>列数</span>
                <el-input-number v-model="model.canvas.columns" :controls="false" :min="1" :max="48" />
              </label>
              <label class="form-field">
                <span>填充模式</span>
                <div class="segment-choice">
                  <button
                    type="button"
                    :class="{ 'is-active': !model.canvas.fill }"
                    @click="model.canvas.fill = false">固定行高</button>
                  <button
                    type="button"
                    :class="{ 'is-active': model.canvas.fill }"
                    @click="model.canvas.fill = true">行列等分</button>
                </div>
              </label>
              <label v-if="model.canvas.fill" class="form-field">
                <span>行数</span>
                <el-input-number v-model="model.canvas.rows" :controls="false" :min="1" :max="48" />
              </label>
              <label v-else class="form-field">
                <span>行高</span>
                <el-input-number v-model="model.canvas.rowHeight" :controls="false" :min="8" :max="200" />
              </label>
              <label class="form-field">
                <span>间距</span>
                <el-input-number v-model="model.canvas.gap" :controls="false" :min="0" :max="80" />
              </label>
            </div>
          </div>

          <div v-if="model.canvas.layout === 'flex'" class="setting-block">
            <strong>弹性布局参数</strong>
            <div class="field-pair-grid">
              <label class="form-field">
                <span>排列方向</span>
                <el-select v-model="model.canvas.flexDirection">
                  <el-option label="水平" value="row" />
                  <el-option label="垂直" value="column" />
                  <el-option label="水平反向" value="row-reverse" />
                  <el-option label="垂直反向" value="column-reverse" />
                </el-select>
              </label>
              <label class="form-field">
                <span>换行方式</span>
                <el-select v-model="model.canvas.flexWrap">
                  <el-option label="不换行" value="nowrap" />
                  <el-option label="自动换行" value="wrap" />
                  <el-option label="反向换行" value="wrap-reverse" />
                </el-select>
              </label>
              <label class="form-field">
                <span>主轴对齐</span>
                <el-select v-model="model.canvas.justifyContent">
                  <el-option label="起点" value="flex-start" />
                  <el-option label="居中" value="center" />
                  <el-option label="终点" value="flex-end" />
                  <el-option label="两端分散" value="space-between" />
                  <el-option label="环绕分散" value="space-around" />
                  <el-option label="均匀分散" value="space-evenly" />
                </el-select>
              </label>
              <label class="form-field">
                <span>交叉轴对齐</span>
                <el-select v-model="model.canvas.alignItems">
                  <el-option label="拉伸" value="stretch" />
                  <el-option label="起点" value="flex-start" />
                  <el-option label="居中" value="center" />
                  <el-option label="终点" value="flex-end" />
                  <el-option label="基线" value="baseline" />
                </el-select>
              </label>
              <label class="form-field form-field--full">
                <span>子项间距</span>
                <el-input-number v-model="model.canvas.gap" :controls="false" :min="0" :max="200" />
              </label>
            </div>
          </div>
        </el-card>

        <el-card shadow="never" class="template-card template-preview-card">
          <template #header>
            <div class="card-heading">
              <strong>画布预览</strong>
              <span>{{ model.canvas.width }} × {{ model.canvas.height }}</span>
            </div>
          </template>
          <div class="canvas-preview" :style="canvasPreviewStyle">
            <span>模板画布区域</span>
          </div>
          <div class="component-preview">
            <div class="component-preview__label">
              <span>通用样式预览</span>
              <strong>指标卡</strong>
            </div>
            <div class="component-preview__stage">
              <div class="component-preview__box" :style="previewStyle">
                <small>指标卡</small>
                <b>128.6</b>
              </div>
            </div>
          </div>
        </el-card>
      </div>

      <el-card id="template-common" shadow="never" class="template-card">
        <template #header>
          <div class="card-heading card-heading--actions">
            <div>
              <strong>通用组件样式</strong>
              <span>对所有组件生效，特殊组件样式会在其基础上覆盖</span>
            </div>
            <el-button :icon="RefreshLeft" @click="resetCommonStyle">恢复系统默认</el-button>
          </div>
        </template>
        <DashboardTemplateStyleFields
          :model-value="commonStyleForm"
          @update:model-value="updateCommonStyle" />
      </el-card>

      <el-card id="template-special" shadow="never" class="template-card">
        <template #header>
          <div class="card-heading card-heading--actions">
            <div>
              <strong>特殊组件个性化样式</strong>
              <span>只为需要区别于通用样式的组件类型启用覆盖，已启用 {{ customTypes.length }} 项</span>
            </div>
            <el-button v-if="customTypes.length" text type="danger" @click="removeAllSpecialStyles">清除全部个性化</el-button>
          </div>
        </template>
        <el-alert
          type="info"
          :closable="false"
          show-icon
          class="special-hint"
          title="开启个性化时会复制当前通用样式作为起点，之后该组件类型独立维护；关闭后重新跟随通用样式。" />
        <div class="special-type-groups">
          <section v-for="group in typedWidgetGroups" :key="group.id" class="special-type-group">
            <header>
              <strong>{{ group.name }}</strong>
              <span>{{ group.children.length }} 种组件</span>
            </header>
            <div class="special-type-grid">
              <article
                v-for="item in group.children"
                :key="item.type"
                class="special-type-card"
                :class="{ 'is-personalized': specialEnabled(item.value) }">
                <header class="special-type-card__header">
                  <div>
                    <strong>{{ item.label }}</strong>
                    <small>{{ specialEnabled(item.value) ? '个性化覆盖已启用' : '跟随通用组件样式' }}</small>
                  </div>
                  <el-switch
                    :model-value="specialEnabled(item.value)"
                    @update:model-value="toggleSpecialStyle(item.value, $event)" />
                </header>
                <div class="special-type-card__preview">
                  <div class="special-type-card__sample" :style="stylePreviewFor(item.value)">
                    <small>{{ item.label }}</small>
                    <b>{{ item.value === 'Metric' ? '128.6' : item.label }}</b>
                  </div>
                </div>
                <DashboardTemplateStyleFields
                  v-if="specialEnabled(item.value) && specialWidgets[item.value]"
                  :model-value="specialWidgets[item.value]?.style || styleDefaults()"
                  @update:model-value="updateSpecialStyle(item.value, $event)"
                  :component-type="item.value" />
              </article>
            </div>
          </section>
        </div>
      </el-card>
    </el-form>
  </div>
</template>

<style lang="scss" scoped>
.template-editor {
  min-height: 100%;
  padding: 16px 20px 40px;
  box-sizing: border-box;
  background: #f4f7fa;
  &__header {
    position: sticky;
    top: 0;
    z-index: 20;
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 20px;
    min-height: 58px;
    margin: -16px -20px 16px;
    padding: 0 20px;
    border-bottom: 1px solid var(--el-border-color-lighter);
    background: rgba(255, 255, 255, 0.96);
    backdrop-filter: blur(10px);
  }
  &__identity {
    display: flex;
    align-items: center;
    gap: 12px;
    min-width: 0;
    strong,
    small {
      display: block;
    }
    strong {
      color: var(--el-text-color-primary);
      font-size: 16px;
    }
    small {
      margin-top: 2px;
      color: var(--el-text-color-placeholder);
      font-size: 11px;
    }
  }
  &__actions {
    display: flex;
    align-items: center;
    gap: 12px;
    flex: none;
  }
}
.section-nav {
  display: flex;
  align-items: center;
  gap: 2px;
  padding: 3px;
  border-radius: 6px;
  background: var(--el-fill-color-light);
  button {
    height: 28px;
    padding: 0 10px;
    border: 0;
    border-radius: 4px;
    color: var(--el-text-color-secondary);
    background: transparent;
    font: inherit;
    font-size: 11px;
    cursor: pointer;
    &:hover {
      color: var(--el-color-primary);
      background: #fff;
    }
  }
}
.template-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.template-card {
  scroll-margin-top: 78px;
  border-radius: 10px;
  :deep(.el-card__header) {
    padding: 14px 18px;
  }
  :deep(.el-card__body) {
    padding: 18px;
  }
}
.card-heading {
  display: flex;
  align-items: baseline;
  gap: 10px;
  min-width: 0;
  strong {
    color: var(--el-text-color-primary);
    font-size: 15px;
    font-weight: 600;
  }
  span {
    color: var(--el-text-color-placeholder);
    font-size: 11px;
  }
  &--actions {
    justify-content: space-between;
  }
}
.meta-grid {
  display: grid;
  grid-template-columns: minmax(220px, 1.4fr) 160px 130px;
  gap: 0 16px;
  &__description {
    grid-column: 1 / -1;
  }
  :deep(.el-form-item) {
    margin-bottom: 14px;
  }
  :deep(.el-select),
  :deep(.el-input-number) {
    width: 100%;
  }
}
.layout-columns {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 340px;
  gap: 16px;
  align-items: start;
}
.field-group-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}
.field-group {
  min-width: 0;
  padding: 13px 14px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 7px;
  background: var(--el-fill-color-extra-light);
  > header {
    display: flex;
    align-items: baseline;
    gap: 7px;
    margin-bottom: 11px;
    strong {
      color: var(--el-text-color-primary);
      font-size: 12px;
    }
    span {
      color: var(--el-text-color-placeholder);
      font-size: 10px;
    }
  }
}
.field-pair,
.field-pair-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}
.form-field--full {
  grid-column: 1 / -1;
}
.layout-choice-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 7px;
  margin-top: 16px;
  button {
    display: flex;
    flex-direction: column;
    align-items: flex-start;
    gap: 3px;
    min-width: 0;
    padding: 9px;
    border: 1px solid var(--el-border-color-light);
    border-radius: 6px;
    color: var(--el-text-color-regular);
    background: #fff;
    font: inherit;
    text-align: left;
    cursor: pointer;
    transition: border-color 0.16s, background 0.16s;
    &:hover {
      border-color: var(--el-color-primary-light-5);
    }
    &.is-active {
      border-color: var(--el-color-primary);
      background: var(--el-color-primary-light-9);
    }
    b {
      font-size: 11px;
      font-weight: 500;
    }
    small {
      overflow: hidden;
      color: var(--el-text-color-placeholder);
      font-size: 9px;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }
}
.setting-block .layout-choice-grid {
  margin-top: 12px;
}
.canvas-background {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 14px;
  margin-top: 16px;
}
.color-input {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
  .el-input {
    flex: 1;
    min-width: 0;
  }
}
.field-hint {
  color: var(--el-text-color-placeholder);
  font-size: 10px;
  line-height: 1.5;
}
.layout-choice-icon {
  position: relative;
  display: block;
  width: 100%;
  height: 30px;
  margin-bottom: 3px;
  border-radius: 4px;
  background: var(--el-fill-color-light);
  i {
    position: absolute;
    display: block;
    border: 1px solid var(--el-color-primary);
    border-radius: 2px;
    background: var(--el-color-primary-light-8);
  }
  &--absolute,
  &--relative,
  &--fixed {
    i:first-child {
      left: 8px;
      top: 7px;
      width: 44%;
      height: 14px;
    }
    i:last-child {
      right: 8px;
      bottom: 5px;
      width: 29%;
      height: 11px;
    }
  }
  &--flex {
    display: flex;
    align-items: center;
    gap: 3px;
    padding: 6px;
    box-sizing: border-box;
    i {
      position: static;
      flex: 1;
      height: 16px;
    }
  }
  &--grid {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 2px;
    padding: 4px;
    box-sizing: border-box;
    i {
      position: static;
      width: auto;
      height: auto;
    }
  }
}
.form-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
  > span {
    color: var(--el-text-color-secondary);
    font-size: 11px;
  }
  .el-select,
  .el-input-number {
    width: 100%;
  }
}
.unit-input {
  display: flex;
  .el-input-number {
    flex: 1;
  }
  .el-select {
    flex: none;
    width: 72px;
    margin-left: 6px;
  }
}
.segment-choice {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 3px;
  padding: 3px;
  border-radius: 5px;
  background: var(--el-fill-color-light);
  button {
    height: 28px;
    border: 0;
    border-radius: 3px;
    color: var(--el-text-color-secondary);
    background: transparent;
    font: inherit;
    font-size: 10px;
    cursor: pointer;
    &.is-active {
      color: var(--el-text-color-primary);
      background: #fff;
      box-shadow: 0 1px 4px rgba(26, 57, 86, 0.12);
    }
  }
}
.setting-block {
  margin-top: 18px;
  padding-top: 14px;
  border-top: 1px dashed var(--el-border-color);
  > strong,
  .setting-block__heading > strong {
    display: block;
    margin-bottom: 12px;
    color: var(--el-text-color-primary);
    font-size: 12px;
  }
  .setting-block__heading {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    margin-bottom: 12px;
    > strong {
      margin-bottom: 0;
    }
  }
}
.inline-switch {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--el-text-color-secondary);
  font-size: 11px;
}
.template-preview-card {
  position: sticky;
  top: 74px;
}
.canvas-preview {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  border: 1px solid rgba(55, 92, 120, 0.18);
  border-radius: 6px;
  color: rgba(255, 255, 255, 0.5);
  font-size: 11px;
  overflow: hidden;
}
.component-preview {
  margin-top: 16px;
  &__label {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 8px;
    span {
      color: var(--el-text-color-placeholder);
      font-size: 10px;
    }
    strong {
      color: var(--el-text-color-secondary);
      font-size: 11px;
    }
  }
  &__stage {
    display: flex;
    align-items: center;
    justify-content: center;
    min-height: 150px;
    padding: 16px;
    box-sizing: border-box;
    border-radius: 7px;
    background:
      linear-gradient(45deg, #e9eef3 25%, transparent 25%),
      linear-gradient(-45deg, #e9eef3 25%, transparent 25%),
      linear-gradient(45deg, transparent 75%, #e9eef3 75%),
      linear-gradient(-45deg, transparent 75%, #e9eef3 75%);
    background-color: #f6f8fa;
    background-position: 0 0, 0 7px, 7px -7px, -7px 0;
    background-size: 14px 14px;
  }
  &__box {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 5px;
    width: 100%;
    min-height: 90px;
    box-sizing: border-box;
    text-align: center;
    small {
      color: inherit;
      font-size: 10px;
      opacity: 0.72;
    }
    b {
      color: inherit;
      font-size: 22px;
    }
  }
}
.special-type-groups {
  display: flex;
  flex-direction: column;
  gap: 22px;
}
.special-hint {
  margin-bottom: 16px;
}
.special-type-group {
  > header {
    display: flex;
    align-items: baseline;
    gap: 8px;
    margin-bottom: 10px;
    strong {
      color: var(--el-text-color-primary);
      font-size: 13px;
    }
    span {
      color: var(--el-text-color-placeholder);
      font-size: 10px;
    }
  }
}
.special-type-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 12px;
}
.special-type-card {
  min-width: 0;
  padding: 12px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  background: #fbfcfd;
  transition: border-color 0.18s, box-shadow 0.18s;
  &.is-personalized {
    grid-column: 1 / -1;
    border-color: var(--el-color-primary-light-5);
    background: #fff;
    box-shadow: 0 6px 20px rgba(31, 98, 155, 0.08);
  }
  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    margin-bottom: 10px;
    strong,
    small {
      display: block;
    }
    strong {
      color: var(--el-text-color-primary);
      font-size: 13px;
    }
    small {
      margin-top: 2px;
      color: var(--el-text-color-placeholder);
      font-size: 10px;
    }
  }
  &__preview {
    display: flex;
    align-items: center;
    justify-content: center;
    min-height: 110px;
    padding: 14px;
    box-sizing: border-box;
    border-radius: 6px;
    background:
      linear-gradient(45deg, #e9eef3 25%, transparent 25%),
      linear-gradient(-45deg, #e9eef3 25%, transparent 25%),
      linear-gradient(45deg, transparent 75%, #e9eef3 75%),
      linear-gradient(-45deg, transparent 75%, #e9eef3 75%);
    background-color: #f6f8fa;
    background-position: 0 0, 0 7px, 7px -7px, -7px 0;
    background-size: 14px 14px;
  }
  &__sample {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 5px;
    width: 100%;
    min-height: 72px;
    box-sizing: border-box;
    text-align: center;
    small {
      color: inherit;
      font-size: 10px;
      opacity: 0.72;
    }
    b {
      color: inherit;
      font-size: 18px;
    }
  }
  &.is-personalized &__preview {
    min-height: 150px;
    margin-bottom: 14px;
  }
  > :deep(.style-fields) {
    padding-top: 14px;
    border-top: 1px dashed var(--el-border-color);
  }
}
.dirty-mark {
  color: var(--el-color-warning);
  font-size: 11px;
}
@media (max-width: 1200px) {
  .section-nav {
    display: none;
  }
  .layout-columns {
    grid-template-columns: 1fr;
  }
  .template-preview-card {
    position: static;
  }
}
@media (max-width: 900px) {
  .meta-grid,
  .field-group-grid,
  .field-pair,
  .field-pair-grid,
  .layout-choice-grid,
  .canvas-background {
    grid-template-columns: 1fr;
  }
  .meta-grid__description {
    grid-column: auto;
  }
}
</style>
