<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useSidebarStore } from '@renderer/stores/sidebar'
import { useFilesStore } from '@renderer/stores/files'
import { useWindowStore } from '@renderer/stores/window'

const props = withDefaults(defineProps<{
  showMaximize?: boolean
}>(), {
  showMaximize: true
})

const ipc = window.ipc
const isMaximized = ref(false)
const platform = window.electron.process.platform
const isMac = platform === 'darwin'
const sidebar = useSidebarStore()
const files = useFilesStore()
const win = useWindowStore()
/** 钉住前的文件区展开状态：钉住期间收起，取消钉住时恢复原样 */
let filesExpandedBeforePin = true

onMounted(async () => {
  isMaximized.value = await ipc.isMaximized()
  ipc.onMaximizeChange((maximized) => {
    isMaximized.value = maximized
  })
})

function togglePin() {
  const pinned = !win.pinned
  // 钉住后窗口只有 400×600：右侧文件区留下来会把对话列挤没，一并收起；
  // 取消钉住时回到钉住前那一眼的状态（展开状态只在本次运行内有效，两个方向都不落库）
  if (pinned) filesExpandedBeforePin = files.visible
  files.setVisible(pinned ? false : filesExpandedBeforePin)
  sidebar.setVisible(!pinned)
  win.pinned = pinned
  ipc.togglePin(pinned)
}
</script>

<template>
  <div class="title-bar" :class="{ 'title-bar--mac': isMac, 'title-bar--win': !isMac }">
    <!-- macOS: traffic lights on the left -->
    <div v-if="isMac" class="title-bar__mac-controls">
      <button class="mac-btn mac-btn--close" title="关闭" @click="ipc.close()">
        <svg width="6" height="6" viewBox="0 0 6 6">
          <path d="M0 0L6 6M6 0L0 6" stroke="currentColor" stroke-width="1" fill="none" />
        </svg>
      </button>
      <button class="mac-btn mac-btn--minimize" title="最小化" @click="ipc.minimize()">
        <svg width="6" height="1" viewBox="0 0 6 1">
          <rect width="6" height="1" fill="currentColor" />
        </svg>
      </button>
      <button v-if="showMaximize" class="mac-btn mac-btn--maximize" :title="isMaximized ? '还原' : '最大化'" @click="ipc.maximize()">
        <svg v-if="isMaximized" width="6" height="6" viewBox="0 0 6 6">
          <rect x="0.5" y="1.5" width="4" height="4" stroke="currentColor" stroke-width="1" fill="none" />
          <path d="M1.5 0.5V1.5H0.5" stroke="currentColor" stroke-width="1" fill="none" />
        </svg>
        <svg v-else width="6" height="6" viewBox="0 0 6 6">
          <rect x="0.5" y="0.5" width="5" height="5" stroke="currentColor" stroke-width="1" fill="none" />
        </svg>
      </button>
      <span class="mac-controls-divider" />
      <button class="mac-pin-btn" :class="{ 'mac-pin-btn--active': win.pinned }" :title="win.pinned ? '取消置顶' : '置顶'" @click="togglePin">
        <svg width="12" height="12" viewBox="0 0 24 24" :fill="win.pinned ? 'currentColor' : 'none'" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M12 2L15 9H21L16 14V22H8V14L3 9H9L12 2Z" />
        </svg>
      </button>
    </div>

    <!-- drag region fills remaining space -->
    <div class="title-bar__drag-region" />

    <!-- Windows/Linux: standard window controls on the right -->
    <div v-if="!isMac" class="title-bar__win-controls">
      <button class="win-btn win-btn--pin" :class="{ 'win-btn--pin-active': win.pinned }" :title="win.pinned ? '取消置顶' : '置顶'" @click="togglePin">
        <svg width="12" height="12" viewBox="0 0 24 24" :fill="win.pinned ? 'currentColor' : 'none'" stroke="currentColor" stroke-width="2">
          <path d="M12 2L15 9H21L16 14V22H8V14L3 9H9L12 2Z" stroke-linejoin="round" />
        </svg>
      </button>
      <button class="win-btn win-btn--minimize" title="最小化" @click="ipc.minimize()">
        <svg width="10" height="10" viewBox="0 0 10 10">
          <rect y="4.5" width="10" height="1" fill="currentColor" />
        </svg>
      </button>
      <button v-if="showMaximize" class="win-btn win-btn--maximize" :title="isMaximized ? '还原' : '最大化'" @click="ipc.maximize()">
        <svg v-if="isMaximized" width="10" height="10" viewBox="0 0 10 10">
          <rect x="1.5" y="0.5" width="7" height="7" stroke="currentColor" stroke-width="1" fill="none" />
          <path d="M1 1.5V2.5H0" stroke="currentColor" stroke-width="1" fill="none" />
        </svg>
        <svg v-else width="10" height="10" viewBox="0 0 10 10">
          <rect x="0.5" y="0.5" width="9" height="9" stroke="currentColor" stroke-width="1" fill="none" />
        </svg>
      </button>
      <button class="win-btn win-btn--close" title="关闭" @click="ipc.close()">
        <svg width="10" height="10" viewBox="0 0 10 10">
          <path d="M0 0L10 10M10 0L0 10" stroke="currentColor" stroke-width="1" fill="none" />
        </svg>
      </button>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.title-bar {
  display: flex;
  align-items: center;
  /* 高度见 --title-bar-height：按钮组贴着条高；上沿留一点边距，下沿不留 */
  height: var(--title-bar-height);
  margin: var(--title-bar-offset) 0 0;
  padding: 0;
  width: 100%;
  flex-shrink: 0;
  user-select: none;
}

.title-bar__drag-region {
  flex: 1;
  height: 100%;
  -webkit-app-region: drag;
}

// ── macOS traffic light buttons (left side) ──────────────

.title-bar__mac-controls {
  display: flex;
  align-items: center;
  gap: 8px;
  padding-left: 12px;
  -webkit-app-region: no-drag;
}

.mac-btn {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  border: none;
  padding: 0;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;

  svg {
    opacity: 0;
    transition: opacity 0.15s;
    color: rgba(0, 0, 0, 0.55);
  }

  &--close {
    background: #ff5f57;
  }

  &--minimize {
    background: #fdbc40;
  }

  &--maximize {
    background: #34c749;
  }
}

.title-bar__mac-controls:hover .mac-btn svg {
  opacity: 1;
}

.mac-btn:hover {
  filter: brightness(0.9);
}

.mac-controls-divider {
  width: 1px;
  height: 14px;
  background: rgba(0, 0, 0, 0.12);
  /* 不额外加左右边距：与红绿灯、图钉各留一个 gap（8px）就够，再加 4px 会把图钉推远 */
  margin: 0;
}

/**
 * 置顶（钉住）：与 Windows 那一枚同一套样式 —— 平时只有一枚图钉（不再画灰色实心圆），
 * 悬停给一层浅底，钉住后图标转主色。尺寸按 mac 的红绿灯排布收窄（24×24，与标题栏同高），
 * 不铺满 Windows 那 46px 的宽按钮；12px 的图钉只占 6px 内边，贴着圆点也不显突兀。
 */
.mac-pin-btn {
  width: 24px;
  height: var(--title-bar-height);
  border-radius: 4px;
  border: none;
  padding: 0;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  color: var(--el-text-color-secondary);
  transition:
    background 0.15s,
    color 0.15s;

  svg {
    width: 12px;
    height: 12px;
  }

  &:hover {
    background: var(--el-fill-color-darker);
  }

  &--active {
    color: var(--el-color-primary);
  }
}

// ── Windows/Linux standard buttons (right side) ──────────

.title-bar__win-controls {
  display: flex;
  height: 100%;
  -webkit-app-region: no-drag;
}

.win-btn {
  width: 46px;
  height: 100%;
  border: none;
  border-radius: 0;
  padding: 0;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  color: var(--el-text-color-primary);
  transition: background 0.15s;

  &:hover {
    background: var(--el-fill-color-darker);
  }

  &--close:hover {
    background: #e81123;
    color: #fff;
  }

  &--pin {
    color: var(--el-text-color-secondary);
  }

  &--pin-active {
    color: var(--el-color-primary);
  }
}
</style>
