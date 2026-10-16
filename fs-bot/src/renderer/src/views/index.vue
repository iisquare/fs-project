<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import TitleBar from '@renderer/components/Layout/TitleBar.vue'
import LayoutSidebar from '@renderer/components/Layout/LayoutSidebar.vue'
import { useSidebarStore, SIDEBAR_WIDTH_MIN, SIDEBAR_WIDTH_MAX } from '@renderer/stores/sidebar'
import { page } from '@renderer/router/config'

const sidebar = useSidebarStore()
const route = useRoute()
const isMac = window.electron.process.platform === 'darwin'
/**
 * 对话页自己管滚动（消息区滚、输入框固定在底部），需要撑满内容区高度与宽度；
 * 其余页面仍按「限宽居中 + 外层滚动」的常规排版。
 */
const fill = computed(() => route.path === page.home)
</script>

<template>
  <!-- macOS: 标题栏整条浮在窗口顶部（红绿灯在左），浮在侧边栏与内容区之上 -->
  <!-- Windows/Linux: 标题栏只盖住右侧内容区，侧边栏通到窗口上下沿 -->
  <div class="main-layout" :class="{ 'main-layout--mac': isMac }">
    <TitleBar v-if="isMac" class="main-layout__titlebar" />
    <!-- resize-end 在 splitter 根上（面板只抛 update:size）：拖完这一下才把宽度落库 -->
    <el-splitter class="main-splitter" @resize-end="sidebar.saveWidth()">
      <!-- 侧边栏：拖分隔条调整宽度，宽度记在 store 里（拖到多宽，下次打开还是多宽） -->
      <el-splitter-panel
        v-if="sidebar.visible"
        :size="sidebar.width"
        :min="SIDEBAR_WIDTH_MIN"
        :max="SIDEBAR_WIDTH_MAX"
        @update:size="sidebar.setWidth"
      >
        <LayoutSidebar />
      </el-splitter-panel>
      <el-splitter-panel>
        <!--
          mac 的标题栏整条浮在窗口最上面（红绿灯在左），它的拖拽区横跨整个窗口宽度：
          内容区顶上这一条始终要让出来，否则顶到最上面的控件（对话头部右侧的文件区开关）
          会落进拖拽区，看得见却点不动。Windows 那边标题栏本身占着这一条，不用额外留。
        -->
        <div class="main-right" :class="{ 'main-right--mac-titlebar': isMac }">
          <TitleBar v-if="!isMac" />
          <el-scrollbar class="main-content" :class="{ 'main-content--fill': fill }">
            <div class="content-wrapper" :class="{ 'content-wrapper--fill': fill }">
              <router-view />
            </div>
          </el-scrollbar>
        </div>
      </el-splitter-panel>
    </el-splitter>
  </div>
</template>

<style lang="scss" scoped>
.main-layout {
  height: 100vh;
  width: 100vw;
  overflow: hidden;
}

// ── macOS ────────────────────────────────────────────────

.main-layout--mac {
  position: relative;
}

.main-layout--mac .main-layout__titlebar {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
}

// ── 侧边栏与内容区 ──────────────────────────────────────

.main-splitter {
  height: 100%;

  /* 面板自带的 overflow: auto 会在侧边栏 / 内容区之外再套一层滚动条，
     滚动统一交给面板内部的组件自己管（侧边栏的会话列表、内容区的 el-scrollbar） */
  :deep(.el-splitter-panel) {
    overflow: hidden;
  }
}

.main-right {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;

  /* macOS：标题栏浮在窗口顶上，红绿灯落在左侧，拖拽区横跨整个窗口。
     这一条空档始终留给它——内容区顶上放着的控件否则会被拖拽区吃掉点击
     （侧边栏那边由 LayoutSidebar 自己留） */
  &--mac-titlebar {
    padding-top: calc(var(--title-bar-height) + var(--title-bar-offset));
  }
}

// ── shared ──────────────────────────────────────────────

.main-content {
  flex: 1;
  min-height: 0;
  background: var(--el-bg-color-page);

  /* 滚动内容至少铺满一屏：短页面也保持整屏底色；对话页由页面自己撑满 */
  :deep(.el-scrollbar__view) {
    min-height: 100%;
  }
}

/* 对话页：内容区正好一屏，滚动交给页面内部的消息区 */
.main-content--fill {
  :deep(.el-scrollbar__view) {
    height: 100%;
  }
}

.content-wrapper {
  width: 100%;
  max-width: 960px;
  margin: 0 auto;
}

/* 对话页：撑满内容区，滚动交给页面自己（消息区滚、输入框贴底） */
.content-wrapper--fill {
  max-width: none;
  height: 100%;
  margin: 0;
}
</style>
