<script setup lang="ts">
/**
 * 可折叠配置项 - 变量、条件、分支、分类等成组配置统一用它收起，节省属性面板高度。
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
    <div class="collapse-head" @click="handleToggle">
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
  padding: 8px;
  border-radius: 4px;
  background: var(--el-fill-color-lighter);
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
      color: var(--el-text-color-regular);
    }
    .summary {
      margin-left: 6px;
    }
    .delete {
      margin-left: auto;
      cursor: pointer;
      &:hover {
        color: var(--el-color-error);
      }
    }
  }
  &.is-collapsible .collapse-head:hover {
    color: var(--el-color-primary);
    .title {
      color: var(--el-color-primary);
    }
  }
  &.is-collapsible .collapse-head {
    cursor: pointer;
  }
  &.is-expanded .collapse-head {
    margin-bottom: 6px;
  }
}
</style>
