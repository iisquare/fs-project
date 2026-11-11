<script setup lang="ts">
defineProps<{
  title?: string
  tabTitle?: string
}>()
</script>

<template>
  <div class="diagram-inspector">
    <el-tabs model-value="property" class="tab-property">
      <el-tab-pane :label="tabTitle || title || '属性'" name="property">
        <slot></slot>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<style lang="scss" scoped>
.diagram-inspector {
  min-height: 100%;
  background: var(--fs-layout-background-color, #f7fafc);
  --el-font-size-base: 12px;
  font-size: 12px;
  color: var(--el-text-color-primary);

  :deep(.el-tab-pane .el-form) {
    .el-form-item {
      margin-bottom: 12px;
    }

    .el-form-item__label {
      display: flex;
      align-items: center;
      width: 100%;
      min-width: 0;
      margin-bottom: 4px;
      color: var(--el-text-color-secondary);
      font-size: 12px;
      line-height: 1.45;
    }

    .el-form-item__content {
      margin-left: 0;
      line-height: 1.5;
    }

    .el-input,
    .el-textarea,
    .el-select,
    .el-input-number,
    .el-autocomplete {
      width: 100%;
    }

    &.el-form--label-top {
      .el-form-item:has(.el-input-number):not(:has(.param-row)) {
        display: block;

        .el-form-item__label {
          width: 100%;
          min-width: 0;
          margin-bottom: 4px;
          padding-right: 0;
        }

        .el-form-item__content {
          display: flex;
          width: 100%;
          margin-left: 0;
          justify-content: flex-start;

          .el-input-number {
            flex: none;
          }
        }
      }
    }

    .el-form-item:last-child {
      margin-bottom: 0;
    }
  }

  :deep(.el-descriptions__label) {
    white-space: nowrap;
  }
}
</style>
