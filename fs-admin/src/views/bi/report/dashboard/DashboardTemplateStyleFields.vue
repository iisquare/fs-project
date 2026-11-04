<script setup lang="ts">
import { computed, nextTick, reactive, watch } from 'vue'
import type { DashboardStyle, DashboardWidgetType } from '@/views/bi/components/dashboard/model'

const props = defineProps<{
  modelValue: DashboardStyle
  componentType?: DashboardWidgetType
}>()
const emit = defineEmits<{
  'update:modelValue': [value: DashboardStyle]
}>()

const style = reactive<DashboardStyle>({ ...props.modelValue })
let syncingFromParent = false

watch(
  () => props.modelValue,
  (value) => {
    if (!value) return
    syncingFromParent = true
    Object.assign(style, value)
    nextTick(() => {
      syncingFromParent = false
    })
  },
  { deep: true },
)
watch(
  style,
  (value) => {
    if (syncingFromParent) return
    emit('update:modelValue', JSON.parse(JSON.stringify(value)))
  },
  { deep: true },
)

const fontFamilies = [
  { label: '系统默认', value: '"Microsoft YaHei", "PingFang SC", sans-serif' },
  { label: '等宽数字', value: '"DIN Alternate", "Bahnschrift", Consolas, monospace' },
  { label: '思源黑体', value: '"Source Han Sans SC", "Noto Sans CJK SC", sans-serif' },
  { label: '宋体', value: 'SimSun, serif' },
]
const fontWeightOptions = [
  { label: '细体 300', value: 300 },
  { label: '常规 400', value: 400 },
  { label: '中等 500', value: 500 },
  { label: '半粗 600', value: 600 },
  { label: '粗体 700', value: 700 },
]
const isChart = computed(() => ['Line', 'Bar', 'Area', 'Pie', 'Funnel', 'Radar', 'Gauge'].includes(props.componentType || ''))
const isDecoration = computed(() => (
  ['Border', 'DecorationHeader', 'DecorationScan', 'DecorationGrid', 'DecorationDivider'].includes(props.componentType || '')
))
</script>

<template>
  <div class="style-fields">
    <section class="style-section">
      <header>
        <strong>填充</strong>
        <small>背景颜色或背景图片</small>
      </header>
      <div class="field-row field-row--wide">
        <span>背景颜色</span>
        <el-color-picker v-model="style.backgroundColor" show-alpha />
        <el-input v-model="style.backgroundColor" />
      </div>
      <div class="field-row field-row--text">
        <span>背景图片</span>
        <el-input v-model="style.backgroundImage" clearable placeholder="图片地址，留空为纯色背景" />
      </div>
      <label class="style-field">
        <span>背景适应</span>
        <div class="choice-row">
          <button
            v-for="option in [
              { label: '覆盖', value: 'cover' },
              { label: '完整显示', value: 'contain' },
              { label: '拉伸', value: '100% 100%' },
            ]"
            :key="option.value"
            type="button"
            :class="{ 'is-active': style.backgroundSize === option.value }"
            @click="style.backgroundSize = option.value as DashboardStyle['backgroundSize']">
            {{ option.label }}
          </button>
        </div>
      </label>
    </section>

    <section class="style-section">
      <header>
        <strong>边框与形状</strong>
        <small>描边、圆角与透明度</small>
      </header>
      <div class="field-grid">
        <label class="style-field">
          <span>边框宽度</span>
          <el-input-number v-model="style.borderWidth" :controls="false" :min="0" :max="24" />
        </label>
        <label class="style-field">
          <span>边框线型</span>
          <div class="choice-row">
            <button
              v-for="option in [
                { label: '实线', value: 'solid' },
                { label: '虚线', value: 'dashed' },
                { label: '点线', value: 'dotted' },
              ]"
              :key="option.value"
              type="button"
              :class="{ 'is-active': style.borderStyle === option.value }"
              @click="style.borderStyle = option.value">
              {{ option.label }}
            </button>
          </div>
        </label>
        <div class="style-field">
          <span>边框颜色</span>
          <div class="color-input">
            <el-color-picker v-model="style.borderColor" />
            <el-input v-model="style.borderColor" />
          </div>
        </div>
        <label class="style-field">
          <span>圆角</span>
          <el-input-number v-model="style.borderRadius" :controls="false" :min="0" :max="160" />
        </label>
        <label class="style-field">
          <span>不透明度</span>
          <el-input-number v-model="style.opacity" :controls="false" :min="0.05" :max="1" :step="0.05" />
        </label>
        <label class="style-field">
          <span>内容溢出</span>
          <div class="choice-row">
            <button
              v-for="option in [
                { label: '隐藏', value: 'hidden' },
                { label: '滚动', value: 'auto' },
                { label: '可见', value: 'visible' },
              ]"
              :key="option.value"
              type="button"
              :class="{ 'is-active': style.overflow === option.value }"
              @click="style.overflow = option.value as DashboardStyle['overflow']">
              {{ option.label }}
            </button>
          </div>
        </label>
      </div>
    </section>

    <section class="style-section style-section--wide">
      <header>
        <strong>文字</strong>
        <small>字体、字号、颜色和对齐</small>
      </header>
      <div class="field-grid field-grid--three">
        <div class="style-field">
          <span>文字颜色</span>
          <div class="color-input">
            <el-color-picker v-model="style.color" />
            <el-input v-model="style.color" />
          </div>
        </div>
        <label class="style-field">
          <span>字体</span>
          <el-select v-model="style.fontFamily">
            <el-option v-for="item in fontFamilies" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </label>
        <label class="style-field">
          <span>字号</span>
          <el-input-number v-model="style.fontSize" :controls="false" :min="10" :max="200" />
        </label>
        <label class="style-field">
          <span>字重</span>
          <el-select v-model="style.fontWeight">
            <el-option v-for="item in fontWeightOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </label>
        <label class="style-field">
          <span>行高</span>
          <el-input-number v-model="style.lineHeight" :controls="false" :min="1" :max="4" :step="0.1" />
        </label>
        <label class="style-field">
          <span>字距</span>
          <el-input-number v-model="style.letterSpacing" :controls="false" :min="-5" :max="40" />
        </label>
        <label class="style-field">
          <span>水平对齐</span>
          <div class="choice-row">
            <button
              v-for="option in [
                { label: '左对齐', value: 'left' },
                { label: '居中', value: 'center' },
                { label: '右对齐', value: 'right' },
              ]"
              :key="option.value"
              type="button"
              :class="{ 'is-active': style.align === option.value }"
              @click="style.align = option.value as DashboardStyle['align']">
              {{ option.label }}
            </button>
          </div>
        </label>
        <label class="style-field">
          <span>垂直对齐</span>
          <div class="choice-row">
            <button
              v-for="option in [
                { label: '顶部', value: 'top' },
                { label: '居中', value: 'middle' },
                { label: '底部', value: 'bottom' },
              ]"
              :key="option.value"
              type="button"
              :class="{ 'is-active': style.verticalAlign === option.value }"
              @click="style.verticalAlign = option.value as DashboardStyle['verticalAlign']">
              {{ option.label }}
            </button>
          </div>
        </label>
      </div>
    </section>

    <section class="style-section style-section--wide">
      <header>
        <strong>内外边距</strong>
        <small>组件内容与四周的距离</small>
      </header>
      <div class="spacing-group">
        <span class="spacing-group__label">内边距</span>
        <label v-for="item in [
          { key: 'paddingTop', label: '上' },
          { key: 'paddingRight', label: '右' },
          { key: 'paddingBottom', label: '下' },
          { key: 'paddingLeft', label: '左' },
        ]" :key="item.key">
          <span>{{ item.label }}</span>
          <el-input-number
            v-model="style[item.key as 'paddingTop']"
            :controls="false"
            :min="0"
            :max="200" />
        </label>
      </div>
      <div class="spacing-group">
        <span class="spacing-group__label">外边距</span>
        <label v-for="item in [
          { key: 'marginTop', label: '上' },
          { key: 'marginRight', label: '右' },
          { key: 'marginBottom', label: '下' },
          { key: 'marginLeft', label: '左' },
        ]" :key="item.key">
          <span>{{ item.label }}</span>
          <el-input-number
            v-model="style[item.key as 'marginTop']"
            :controls="false"
            :min="0"
            :max="200" />
        </label>
      </div>
    </section>

    <section class="style-section">
      <header>
        <strong>阴影与滤镜</strong>
        <small>增强层次和玻璃质感</small>
      </header>
      <div class="field-grid">
        <label class="style-field">
          <span>阴影模糊</span>
          <el-input-number v-model="style.shadowBlur" :controls="false" :min="0" :max="120" />
        </label>
        <label class="style-field">
          <span>背景模糊</span>
          <el-input-number v-model="style.backdropBlur" :controls="false" :min="0" :max="40" />
        </label>
        <label class="style-field">
          <span>水平偏移</span>
          <el-input-number v-model="style.shadowOffsetX" :controls="false" :min="-100" :max="100" />
        </label>
        <label class="style-field">
          <span>垂直偏移</span>
          <el-input-number v-model="style.shadowOffsetY" :controls="false" :min="-100" :max="100" />
        </label>
        <div class="style-field field-grid__full">
          <span>阴影颜色</span>
          <div class="color-input">
            <el-color-picker v-model="style.shadowColor" show-alpha />
            <el-input v-model="style.shadowColor" />
          </div>
        </div>
      </div>
    </section>

    <section class="style-section">
      <header>
        <strong>标题与图表</strong>
        <small>标题栏和数据展示选项</small>
      </header>
      <label class="switch-row">
        <span>显示组件标题</span>
        <el-switch v-model="style.titleShow" />
      </label>
      <template v-if="style.titleShow">
        <label class="style-field">
          <span>标题文字</span>
          <el-input v-model="style.titleText" placeholder="留空时使用组件名称" />
        </label>
        <div class="field-grid">
          <label class="style-field">
            <span>标题字号</span>
            <el-input-number v-model="style.titleSize" :controls="false" :min="10" :max="80" />
          </label>
          <label class="style-field">
            <span>标题颜色</span>
            <div class="color-input">
              <el-color-picker v-model="style.titleColor" />
              <el-input v-model="style.titleColor" />
            </div>
          </label>
        </div>
      </template>
      <template v-if="isChart">
        <label class="switch-row">
          <span>显示图例</span>
          <el-switch v-model="style.showLegend" />
        </label>
        <label class="switch-row">
          <span>显示数值标签</span>
          <el-switch v-model="style.showLabel" />
        </label>
        <label v-if="['Line', 'Area'].includes(componentType || '')" class="switch-row">
          <span>平滑曲线</span>
          <el-switch v-model="style.smooth" />
        </label>
      </template>
    </section>

    <section v-if="isDecoration" class="style-section style-section--wide">
      <header>
        <strong>装饰效果</strong>
        <small>科技边框、光效与动效</small>
      </header>
      <div class="field-grid field-grid--three">
        <label class="style-field">
          <span>强调色</span>
          <div class="color-input">
            <el-color-picker v-model="style.borderAccent" />
            <el-input v-model="style.borderAccent" />
          </div>
        </label>
        <label class="style-field">
          <span>边框形态</span>
          <el-select v-model="style.borderVariant">
            <el-option label="科技切角" value="tech" />
            <el-option label="四角描边" value="corner" />
            <el-option label="细线" value="simple" />
          </el-select>
        </label>
        <label class="style-field">
          <span>扫描形态</span>
          <el-select v-model="style.decorationPattern">
            <el-option label="平视扫描" value="hud" />
            <el-option label="网格" value="grid" />
            <el-option label="波纹" value="wave" />
            <el-option label="雷达" value="radar" />
          </el-select>
        </label>
        <label class="style-field">
          <span>光晕强度</span>
          <el-input-number v-model="style.glow" :controls="false" :min="0" :max="40" />
        </label>
        <label class="switch-row">
          <span>启用动效</span>
          <el-switch v-model="style.animation" />
        </label>
      </div>
    </section>
  </div>
</template>

<style lang="scss" scoped>
.style-fields {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}
.style-section {
  min-width: 0;
  padding: 16px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  background: #fff;
  header {
    display: flex;
    align-items: baseline;
    gap: 8px;
    margin-bottom: 14px;
    strong {
      color: var(--el-text-color-primary);
      font-size: 14px;
      font-weight: 600;
    }
    small {
      color: var(--el-text-color-placeholder);
      font-size: 11px;
    }
  }
  &--wide {
    grid-column: 1 / -1;
  }
}
.field-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  &--three {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
  &__full {
    grid-column: 1 / -1;
  }
}
.style-field {
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
.field-row {
  display: grid;
  grid-template-columns: 76px 32px minmax(0, 1fr);
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
  > span {
    color: var(--el-text-color-secondary);
    font-size: 11px;
  }
  &--text {
    grid-template-columns: 76px minmax(0, 1fr);
  }
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
.choice-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(58px, 1fr));
  gap: 4px;
  padding: 3px;
  border-radius: 5px;
  background: var(--el-fill-color-light);
  button {
    height: 28px;
    padding: 0 6px;
    border: 0;
    border-radius: 3px;
    color: var(--el-text-color-secondary);
    background: transparent;
    font: inherit;
    font-size: 10px;
    cursor: pointer;
    transition: color 0.16s, background 0.16s, box-shadow 0.16s;
    &:hover {
      color: var(--el-color-primary);
    }
    &.is-active {
      color: var(--el-color-primary);
      background: #fff;
      box-shadow: 0 1px 4px rgba(26, 57, 86, 0.12);
    }
  }
}
.switch-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 30px;
  margin-bottom: 8px;
  color: var(--el-text-color-secondary);
  font-size: 11px;
}
.spacing-group {
  display: grid;
  grid-template-columns: 56px repeat(4, minmax(0, 1fr));
  align-items: end;
  gap: 8px;
  & + & {
    margin-top: 12px;
  }
  &__label {
    padding-bottom: 7px;
    color: var(--el-text-color-secondary);
    font-size: 11px;
  }
  label {
    display: flex;
    flex-direction: column;
    gap: 4px;
    min-width: 0;
    color: var(--el-text-color-placeholder);
    font-size: 10px;
    .el-input-number {
      width: 100%;
    }
  }
}
@media (max-width: 1200px) {
  .field-grid--three {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
@media (max-width: 1000px) {
  .style-fields,
  .field-grid,
  .field-grid--three {
    grid-template-columns: 1fr;
  }
  .spacing-group {
    grid-template-columns: repeat(4, minmax(0, 1fr));
    &__label {
      grid-column: 1 / -1;
      padding-bottom: 0;
    }
  }
}
</style>
