<script setup lang="ts">
/**
 * 变量编辑字段 - 标题行（标题 + 插入变量 + 复制）与变量编辑器，供系统提示词、用户输入、
 * 回复内容、分类描述等可插入变量的文本复用；插入变量弹出面板与编辑器内的 `/` 提示共用变量数据。
 *
 * @v-model {String} 文本内容
 * @prop {String} title       - 字段标题
 * @prop {*}      instance    - 画布实例（X6Container 暴露的 flow）
 * @prop {*}      activeItem  - 当前激活的节点，用于排除自身
 * @prop {Number} height      - 编辑器高度(px)
 * @prop {String} placeholder - 空白占位提示文字
 * @prop {Boolean} compact    - 紧凑模式：标题与按钮同一行、不使用表单行样式，用于折叠卡片内部
 * @prop {Boolean} single     - 单行精简模式：只渲染一行编辑器，不带标题与操作按钮（变量用 `/` 唤起插入）
 * @prop {Boolean} collapsible - 是否可折叠（折叠后只显示标题、内容预览与操作按钮）
 * @prop {Boolean} defaultExpanded - 折叠时的初始状态，默认收起
 * @slot after - 编辑器下方的附加内容；折叠模式下仅在展开时渲染
 */
import { computed, ref } from 'vue'
import CollapseItem from './CollapseItem.vue'
import SectionSlice from './SectionSlice.vue'
import VariableActions from './VariableActions.vue'
import VariableEditor from './VariableEditor.vue'

const model: any = defineModel<string>()
const {
  title = '',
  instance,
  activeItem = {},
  height = 180,
  placeholder = '',
  compact = false,
  single = false,
  collapsible = false,
  defaultExpanded = false,
} = defineProps<{
  title?: string,
  instance?: any,
  activeItem?: any,
  height?: number,
  placeholder?: string,
  compact?: boolean,
  single?: boolean,
  collapsible?: boolean,
  defaultExpanded?: boolean,
}>()

const editorRef = ref()
const expanded = ref(defaultExpanded)

// 是否已填写：折叠时用状态圆点表示，圆点悬浮可看内容预览
const filled = computed(() => '' !== String(model.value ?? '').trim())
const filledTips = computed(() => {
  if (!filled.value) return '未填写'
  const plain = String(model.value).replace(/\{\{#[^#{}]+#\}\}/g, '变量').replace(/\s+/g, ' ')
  return `已填写：${plain.length > 40 ? plain.slice(0, 40) + '…' : plain}`
})

const handleInsert = (value: string) => {
  editorRef.value?.insert(value)
}

// 复制当前编辑器内容：成功后由按钮显示对号反馈
const handleCopy = () => editorRef.value?.copy()
</script>

<template>
  <!-- 单行精简模式：只有一行编辑器，标题与插入/复制按钮都省略 -->
  <VariableEditor
    ref="editorRef"
    v-if="single"
    v-model="model"
    :instance="instance"
    :active-item="activeItem"
    :single="true"
    :placeholder="placeholder" />
  <!-- 可折叠模式：折叠后只显示标题、内容预览与操作按钮 -->
  <CollapseItem
    v-else-if="collapsible"
    :title="title"
    :tags="[]"
    :removable="false"
    :expanded="expanded"
    @toggle="expanded = !expanded">
    <template #head>
      <!-- 状态圆点：已填写为主色实心，未填写为浅灰；悬浮显示内容预览 -->
      <span class="field-status" :class="{ 'is-filled': filled }" :title="filledTips"></span>
      <span class="field-actions" @click.stop>
        <VariableActions
          :instance="instance"
          :active-item="activeItem"
          :copy="handleCopy"
          @insert="handleInsert" />
      </span>
    </template>
    <VariableEditor
      ref="editorRef"
      v-model="model"
      :instance="instance"
      :active-item="activeItem"
      :height="height"
      :single="single"
      :placeholder="placeholder" />
    <slot name="after"></slot>
  </CollapseItem>
  <!-- 紧凑模式：标题 + 操作按钮一行，编辑器紧随其下（用于折叠卡片、窄容器内部） -->
  <div class="variable-field" v-else-if="compact">
    <div class="variable-field__head">
      <!-- 默认放标题，调用方可换成同行左侧的其它控件（如分类名称输入框） -->
      <slot name="head">
        <span class="label">{{ title }}</span>
      </slot>
      <VariableActions
        :instance="instance"
        :active-item="activeItem"
        :copy="handleCopy"
        @insert="handleInsert" />
    </div>
    <VariableEditor
      ref="editorRef"
      v-model="model"
      :instance="instance"
      :active-item="activeItem"
      :height="height"
      :single="single"
      :placeholder="placeholder" />
    <slot name="after"></slot>
  </div>
  <!-- 面板模式：分组标题样式的一行 + 独立编辑器行 -->
  <SectionSlice :title="title" v-if="!compact && !collapsible">
    <template #actions>
      <VariableActions
        :instance="instance"
        :active-item="activeItem"
        :copy="handleCopy"
        @insert="handleInsert" />
    </template>
    <el-form-item label="">
      <VariableEditor
        ref="editorRef"
        v-model="model"
        :instance="instance"
        :active-item="activeItem"
        :height="height"
        :single="single"
        :placeholder="placeholder" />
      <slot name="after"></slot>
    </el-form-item>
  </SectionSlice>
</template>

<style lang="scss" scoped>
.variable-field__head {
  margin-bottom: 4px;
  font-size: 12px;
  color: var(--el-text-color-regular);
  @include flex-start();
  gap: 6px;
  /* 左侧内容占满剩余宽度（默认是标题文字，也可以是调用方传入的输入框） */
  > :first-child {
    flex: 1;
    min-width: 0;
  }
}
/* 折叠模式下操作按钮靠右（CollapseItem 头部无删除按钮时用自动外边距占位） */
.field-actions {
  margin-left: auto;
  display: inline-flex;
}
/* 折叠模式下的填写状态圆点：不占宽度、不换行 */
.field-status {
  flex: none;
  width: 6px;
  height: 6px;
  margin-left: 6px;
  border-radius: 50%;
  background: var(--el-border-color);
  cursor: help;
  &.is-filled {
    background: var(--el-color-primary);
  }
}
</style>
