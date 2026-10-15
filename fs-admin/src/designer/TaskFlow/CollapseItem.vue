<script setup lang="ts">
/**
 * 可折叠配置项 - 条件分支的分支列表统一用它收起，节省属性面板高度。
 *
 * @prop {String}  title       - 标题，收起时展示（为空时只展示摘要标签）
 * @prop {Array}   tags        - 摘要标签，仅在收起时展示
 * @prop {Boolean} expanded    - 是否展开，由父级维护（配合 collapse.ts 的 useCollapse 使用）
 * @prop {Boolean} collapsible - 是否允许收起，false 时始终展开且不显示箭头
 * @prop {Boolean} removable   - 是否展示删除按钮，默认展示
 *
 * @emits toggle - 点击标题行切换展开状态
 * @emits delete - 点击删除按钮
 * @slot  head    - 标题后的自定义内容（展开与收起都展示）
 * @slot  default - 展开后的配置内容
 */
import { computed } from 'vue'
import { Delete } from '@element-plus/icons-vue'
import LayoutIcon from '@/components/Layout/LayoutIcon.vue'

// 注意：布尔 prop 未传时会被 Vue 转成 false，必须用默认值表达「默认允许收起」
const props = withDefaults(defineProps<{
  title?: string,
  tags?: any[],
  expanded?: boolean,
  collapsible?: boolean,
  removable?: boolean,
}>(), {
  title: '',
  tags: () => [],
  expanded: false,
  collapsible: true,
  removable: true,
})
const emit = defineEmits(['toggle', 'delete'])

const open = computed(() => false === props.collapsible || true === props.expanded)
const caret = computed(() => false !== props.collapsible)

const handleToggle = () => {
  if (false === props.collapsible) return
  emit('toggle')
}
</script>

<template>
  <div class="collapse-item" :class="{ 'is-expanded': open, 'is-collapsible': caret }">
    <div
      class="collapse-head"
      :role="caret ? 'button' : undefined"
      :tabindex="caret ? 0 : undefined"
      :aria-expanded="caret ? open : undefined"
      @click="handleToggle"
      @keydown.enter.prevent="handleToggle"
      @keydown.space.prevent="handleToggle">
      <LayoutIcon class="caret" v-if="caret" :name="open ? 'ArrowDown' : 'ArrowRight'" />
      <span class="title" v-if="title">{{ title }}</span>
      <template v-if="!open">
        <el-tag class="summary" size="small" effect="plain" :key="tag" v-for="tag in tags">{{ tag }}</el-tag>
      </template>
      <slot name="head"></slot>
      <el-icon class="delete" v-if="removable !== false" @click.stop="emit('delete')"><Delete /></el-icon>
    </div>
    <div class="collapse-body" v-if="open">
      <slot></slot>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.collapse-item {
  width: 100%;
  padding: 8px 10px;
  /* 灰色面板之上的白卡片（与全站 el-card 的用法一致：常态无描边、无阴影），
     只有悬停与展开才用主色描边表达状态，静态时不留框线 */
  border: solid 1px transparent;
  border-radius: 4px;
  background: var(--fs-panel-surface);
  transition: border-color 0.2s, background-color 0.2s;
  & + .collapse-item {
    margin-top: 6px;
  }
  .collapse-head {
    font-size: 12px;
    color: var(--el-text-color-secondary);
    @include flex-start();
    .caret {
      margin-right: 4px;
    }
    .title {
      font-weight: 500;
      color: var(--el-text-color-primary);
    }
    .summary {
      margin-left: 6px;
    }
    .delete {
      flex: none;
      margin-left: auto;
      /* 全局是 border-box，el-icon 又是固定 1em 宽：这里用 content-box 让 padding 只去和左侧内容
         留间距，不占掉图标自身的尺寸（否则删除图标会变小） */
      box-sizing: content-box;
      padding-left: 6px;
      cursor: pointer;
      &:hover {
        color: var(--el-color-error);
      }
    }
  }
  &.is-collapsible .collapse-head {
    cursor: pointer;
  }
  /* 悬停：只描边变色，保持白面干净 */
  &.is-collapsible:hover {
    border-color: var(--el-color-primary-light-5);
  }
  &.is-collapsible .collapse-head:hover {
    color: var(--el-color-primary);
    .title {
      color: var(--el-color-primary);
    }
  }
  /* 展开：主色描边 + 极浅主色底，当前正在编辑的项一眼可辨 */
  &.is-expanded {
    border-color: var(--el-color-primary-light-7);
    background: var(--el-color-primary-light-9);
    .collapse-head {
      margin-bottom: 6px;
    }
  }
}
</style>
