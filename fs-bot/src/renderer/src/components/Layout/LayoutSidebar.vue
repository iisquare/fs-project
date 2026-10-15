<script setup lang="ts">
import SidebarHeader from './SidebarHeader.vue'
import SidebarContent from './SidebarContent.vue'
import SidebarFooter from './SidebarFooter.vue'

const isMac = window.electron.process.platform === 'darwin'
</script>

<template>
  <aside class="layout-sidebar" :class="{ 'layout-sidebar--mac': isMac }">
    <SidebarHeader />
    <SidebarContent />
    <SidebarFooter />
  </aside>
</template>

<style lang="scss" scoped>
.layout-sidebar {
  display: flex;
  flex-direction: column;
  /* 宽度由外层 splitter 面板决定（拖分隔条调整，宽度记在 sidebar store 里），这里只负责铺满面板；
     也不再自己画右边框 —— 分隔条（hover 高亮主色）就是与内容区的分界线，避免两条线并排 */
  width: 100%;
  min-width: 0;
  height: 100%;
  background: var(--el-bg-color);

  /* macOS：标题栏整条浮在窗口顶部，侧边栏让出它的高度与上沿留白 */
  &.layout-sidebar--mac {
    padding-top: calc(var(--title-bar-height) + var(--title-bar-offset));
  }
}
</style>
