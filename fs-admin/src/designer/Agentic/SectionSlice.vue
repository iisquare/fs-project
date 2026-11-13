<script setup lang="ts">
/**
 * 可折叠分组 - 属性面板的分组标题 + 分组内容：点击标题整块收起/展开，收起后只留标题（右侧箭头）。
 * 各节点属性面板的分组统一用它，长面板可以按需收起不常改的配置。
 *
 * @prop {String}  title     - 分组标题
 * @prop {Boolean} collapsed - 初始是否收起，默认展开
 * @slot title  - 标题内容，默认取 title 属性；需要标题后跟帮助图标等附加内容时用它
 * @slot default - 分组内容（若干个表单项/子组件）
 * @slot actions - 标题行右侧的额外操作（如删除按钮），点击不会触发收起
 */
import PropertySection from '@/components/Layout/PropertySection.vue'

const {
  title = '',
  collapsed = false,
} = defineProps<{
  title?: string,
  collapsed?: boolean,
}>()

</script>

<template>
  <PropertySection class="agent-property-section" :title="title" :open="!collapsed">
    <template v-if="$slots.title" #title><slot name="title"></slot></template>
    <template v-if="$slots.actions" #extra><slot name="actions"></slot></template>
    <slot></slot>
  </PropertySection>
</template>

<style lang="scss" scoped>
:deep(.property-section__title) {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

/* Agent 节点属性统一使用更清晰的标题层级与参数间距 */
:deep(.property-section.agent-property-section) {
  margin: 0 -12px;
  border-bottom-color: var(--el-border-color-lighter);

  > .property-section__header {
    height: 42px;
    padding: 0 14px;
    background: linear-gradient(180deg, var(--el-fill-color-blank), var(--el-fill-color-lighter));

    &:hover {
      background: var(--el-color-primary-light-9);
    }

    strong {
      font-size: 13px;
      font-weight: 650;
      letter-spacing: 0.01em;
    }
  }

  .property-section__chevron {
    width: 7px;
    height: 7px;
    border-width: 1.5px;
    border-color: var(--el-text-color-placeholder);
  }

  > .property-section__content {
    padding: 12px 14px 16px;

    > .el-form-item {
      margin-bottom: 14px;
    }

    > .el-form-item:last-child {
      margin-bottom: 0;
    }

    .el-form-item__label {
      padding-bottom: 5px;
      font-size: 12px;
      font-weight: 500;
      color: var(--el-text-color-regular);
    }
  }
}
</style>
