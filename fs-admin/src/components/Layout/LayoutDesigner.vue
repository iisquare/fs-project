<script setup lang="ts">
/**
 * 设计器布局容器 - 标准的三栏布局：左侧面板(240px) + 中间画布区 + 右侧属性面板(350px) + 底部状态栏(35px)。
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
  rightSize = 350,
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
          <div class="right">
            <slot name="right"></slot>
          </div>
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
        <div class="right">
          <slot name="right"></slot>
        </div>
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
      width: calc(100% - 590px);
      display: inline-block;
      position: relative;
      overflow: auto;
      .top {
        height: 45px;
        width: 100%;
        @include flex-between();
        padding: 0 15px;
        box-sizing: border-box;
        border-bottom: solid 1px #e8e8e8;
      }
      .main {
        height: calc(100% - 45px);
        background: #fafafa;
        box-sizing: border-box;
        overflow: hidden;
      }
    }
    .right {
      width: 350px;
      display: inline-block;
      vertical-align: top;
      border-left: solid 1px var(--fs-layout-border-color);
      :deep(.el-form) {
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
            margin-bottom: 4px;
          }
        }
      }
      :deep(.tab-property) {
        --el-tabs-header-height: 45px;
        .el-tabs__header {
          margin-bottom: 0;
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
          padding: 15px;
        }
        .el-form-item.title {
          color: rgba(0, 0, 0, 0.85);
          font-weight: 600;
          font-size: 14px;
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
          height: calc(100vh - var(--fs-layout-header-height) - var(--el-tabs-header-height) - 35px);
          overflow-y: auto;
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
