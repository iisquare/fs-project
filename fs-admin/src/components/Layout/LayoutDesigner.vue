<script setup lang="ts">
</script>

<template>
  <div class="box">
    <div class="content">
      <div class="left">
        <slot name="left"></slot>
      </div>
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
    .left {
      width: 240px;
      height: 100%;
      display: inline-block;
      box-sizing: border-box;
      border-right: solid 1px var(--fs-layout-border-color);
      overflow-y: auto;
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
      height: 100%;
      width: 350px;
      display: inline-block;
      box-sizing: border-box;
      vertical-align: top;
      border-left: solid 1px var(--fs-layout-border-color);
      :deep(.el-form) {
        .el-form-item__label {
          width: 70px;
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
