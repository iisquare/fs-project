<script setup lang="ts">
/**
 * 设计器布局容器 - 标准的三栏布局：左侧面板(240px) + 中间画布区 + 右侧属性面板(300px) + 底部状态栏(35px)。
 *
 * @prop {Boolean} splitter - 采用 Splitter 分隔面板布局，左右面板可通过分隔条拖拽调整宽度，默认 false
 *
 * @slot left    - 左侧面板内容（如组件库/图层树）
 * @slot top     - 中间区域顶部工具栏
 * @slot default - 中间画布主体内容
 * @slot right   - 右侧属性面板内容
 * @slot footer  - 底部状态栏
 *
 * @example
 * <layout-designer>
 *   <template #left><layout-widget :widgets="widgets" /></template>
 *   <template #top><layout-toolbar :toolbars="toolbars" /></template>
 *   <div id="canvas"></div>
 *   <template #right><layout-property v-model="config" :activeItem="active" /></template>
 *   <template #footer>状态信息</template>
 * </layout-designer>
 */
const {
  splitter = false,
  leftSize = 240,
  rightSize = 300,
  leftMin = 160,
  leftMax = 520,
  rightMin = 240,
  rightMax = 700,
} = defineProps({
  splitter: { type: Boolean, required: false },
  leftSize: { type: Number, required: false },
  rightSize: { type: Number, required: false },
  leftMin: { type: Number, required: false },
  leftMax: { type: Number, required: false },
  rightMin: { type: Number, required: false },
  rightMax: { type: Number, required: false },
})
</script>

<template>
  <div class="box">
    <div :class="['content', splitter && 'splitter']">
      <el-splitter v-if="splitter" lazy>
        <el-splitter-panel :size="leftSize" :min="leftMin" :max="leftMax" collapsible>
          <el-scrollbar class="left">
            <slot name="left"></slot>
          </el-scrollbar>
        </el-splitter-panel>
        <el-splitter-panel>
          <div class="center">
            <div class="top">
              <slot name="top"></slot>
            </div>
            <div class="main">
              <slot></slot>
            </div>
          </div>
        </el-splitter-panel>
        <el-splitter-panel :size="rightSize" :min="rightMin" :max="rightMax" collapsible>
          <el-scrollbar class="right">
            <slot name="right"></slot>
          </el-scrollbar>
        </el-splitter-panel>
      </el-splitter>
      <template v-else>
        <el-scrollbar class="left">
          <slot name="left"></slot>
        </el-scrollbar>
        <div class="center">
          <div class="top">
            <slot name="top"></slot>
          </div>
          <div class="main">
            <slot></slot>
          </div>
        </div>
        <el-scrollbar class="right">
          <slot name="right"></slot>
        </el-scrollbar>
      </template>
    </div>
    <div class="footer">
      <slot name="footer"></slot>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.box {
  width: 100%;
  height: 100%;
  .content {
    height: calc(100% - 35px);
    overflow: hidden;
    /* 滚动条置顶：页签栏与分组标题都是吸顶的不透明区域，层级低于滚动条时会把滚动条盖住 */
    :deep(.el-scrollbar__bar) {
      z-index: 20;
    }
    .left, .right {
      box-sizing: border-box;
      height: 100%;
    }
    .left {
      width: 240px;
      display: inline-block;
      border-right: solid 1px var(--fs-layout-border-color);
    }
    .center {
      height: 100%;
      // 非 splitter 布局下画布宽度 = 100% - 左侧面板 - 右侧面板，与 leftSize/rightSize 保持一致
      width: calc(100% - 540px);
      display: inline-block;
      position: relative;
      overflow: auto;
      .top {
        height: 45px;
        width: 100%;
        @include flex-between();
        padding: 0 15px;
        box-sizing: border-box;
        border-bottom: solid 1px var(--fs-layout-border-color);
        /* 顶部操作按钮与设计器排版统一：30px 高、13px 字号、6px 圆角；link 样式的图标按钮保持原样 */
        :deep(.el-button:not(.is-link)) {
          height: 30px;
          padding: 0 14px;
          font-size: 13px;
          border-radius: 6px;
        }
        /* 文字按钮更紧凑，图标与文字之间的间距由 Element Plus 统一处理 */
        :deep(.el-button.is-text) {
          padding: 0 10px;
        }
      }
      .main {
        height: calc(100% - 45px);
        background: #fafafa;
        box-sizing: border-box;
        overflow: hidden;
      }
    }
    .right {
      width: 300px;
      display: inline-block;
      vertical-align: top;
      border-left: solid 1px var(--fs-layout-border-color);
      /* 属性面板排版基准：正文与控件 13px、次要说明 12px，与左侧组件库、画布节点同一套字号 */
      --el-font-size-base: 13px;
      /* 面板内默认文字色与字号：纯文本取值、下拉选中项等直接继承，避免取值比标签又大又深 */
      font-size: 13px;
      color: var(--el-text-color-primary);
      :deep(.el-form) {
        /* 属性面板纵向空间有限：压缩表单项间距与标签行高，一屏展示更多有效编辑内容 */
        .el-form-item {
          margin-bottom: 8px;
        }
        /* Element Plus 的标签、按钮、下拉框字号按尺寸写死 14px，统一到面板基准 */
        .el-form-item--default {
          --font-size: 13px;
        }
        /* 标签与取值同字号、同颜色，避免标签显得比取值又小又浅 */
        .el-form-item__label {
          color: var(--el-text-color-primary);
        }
        .el-input,
        .el-textarea {
          --el-input-text-color: var(--el-text-color-primary);
        }
        .el-button,
        .el-select {
          font-size: 13px;
        }
        /* 分组标题：不加底色（与面板同色，吸顶时不透出下方内容），仅用左侧主色浅色短条做标记
           （9px 内边距 + 3px 边框 = 与字段标签同为 12px）；底色会与字段区域的浅灰底色混淆，体现不出区域差异 */
        .el-form-item.title {
          position: sticky;
          top: 0;
          /* 高于 Element Plus 表格内部层级（边框补丁 --el-table-index + 2、列宽拖拽 + 9），避免表格滚过时压住小标题 */
          z-index: 11;
          display: flex;
          align-items: center;
          margin: 8px -12px;
          padding: 5px 12px 5px 9px;
          border-left: solid 1px var(--el-color-primary-light-3);
          background-color: var(--fs-layout-background-color);
          font-size: 13px;
          font-weight: 600;
          color: var(--el-text-color-primary);
          &:first-child {
            margin-top: 0;
          }
          .el-form-item__content {
            line-height: 1.5;
          }
        }
        .el-form-item:last-child {
          margin-bottom: 0;
        }
        .el-form-item__label {
          width: auto;
          min-width: 70px;
          display: inline-flex;
          align-items: center;
          line-height: 1.4;
        }
        &.el-form--label-top {
          .el-form-item__label {
            width: 100%;
            min-width: 0;
            margin-bottom: 2px;
            line-height: 20px;
          }
          /* 数值输入等单行短控件：标签在左、数值框按固定宽度靠右，不再占满整行 */
          .el-form-item:has(.el-input-number) {
            display: flex;
            align-items: center;
            .el-form-item__label {
              flex: none;
              width: auto;
              min-width: 70px;
              margin-bottom: 0;
              padding-right: 8px;
            }
            .el-form-item__content {
              flex: 1;
              margin-left: 0;
              justify-content: flex-end;
              .el-input-number {
                flex: 0 1 150px;
                min-width: 0;
              }
            }
          }
        }
      }
      :deep(.tab-property) {
        --el-tabs-header-height: 45px;
        .el-tabs__header {
          /* 面板标题吸顶：内容交给外层面板的 scrollbar 滚动，滚动时页签栏常驻 */
          position: sticky;
          top: 0;
          /* 高于吸顶的分组标题与表格内部层级，保证页签栏始终在最上层 */
          z-index: 12;
          margin-bottom: 0;
          /* 与左侧组件库的分组标题同底同高，左右两侧面板视觉上成对 */
          background-color: var(--fs-layout-background-color);
          border-bottom: solid 1px var(--fs-layout-border-color);
        }
        .el-tabs__item {
          padding-left: 0px;
          padding-right: 0px;
          &::before, &::after {
            content: '';
            width: 15px;
            display: inline-block;
          }
        }
        .el-tab-pane {
          padding: 12px;
        }
        .el-form-item.title {
          /* 分组标题吸附在页签栏下方 */
          top: var(--el-tabs-header-height);
          .el-form-item__content {
            @include flex-between();
            .el-icon.delete {
              cursor: pointer;
              &:hover {
                color: var(--el-color-error);
              }
            }
          }
        }
        .el-tabs__content {
          /* 滚动交给外层 el-scrollbar，内容按自然高度铺开 */
          height: auto;
          overflow: visible;
        }
      }
    }
    &.splitter {
      :deep(.el-splitter-panel) {
        overflow: hidden;
      }
      .left {
        width: 100%;
        display: block;
        border-right: none;
      }
      .right {
        width: 100%;
        display: block;
        border-left: none;
      }
      .center {
        width: 100%;
        display: flex;
        flex-direction: column;
        overflow: hidden;
        .top {
          flex: none;
        }
        .main {
          flex: 1;
          height: auto;
          min-height: 0;
        }
      }
    }
  }
  .footer {
    height: 35px;
    padding: 2px 5px 2px 5px;
    box-sizing: border-box;
    border-top: solid 1px var(--fs-layout-border-color);
    @include flex-start();
    font-size: 12px;
  }
}
</style>
