<script setup lang="ts">
import { useRouter } from 'vue-router'
import { page, projectCreateRoute } from '@renderer/router/config'
import { useProjectStore } from '@renderer/stores/project'
import { useConversationStore } from '@renderer/stores/conversation'
import LayoutIcon from './LayoutIcon.vue'
import BrandLogo from '@renderer/components/Brand/BrandLogo.vue'

const router = useRouter()
const projectStore = useProjectStore()
const conversationStore = useConversationStore()

/** 新对话下方的入口：图标下显示短文案，提示里保留完整页面名称 */
const entries = [
  { icon: 'menu.project', label: '项目', title: '项目管理', path: page.project },
  { icon: 'menu.schedule', label: '定时', title: '定时任务', path: page.schedule },
  { icon: 'menu.skills', label: '技能', title: '技能管理', path: page.skill },
  { icon: 'plus.setting', label: '设置', title: '系统设置', path: page.settings }
]

/**
 * 新对话：会话属于某个项目，所以先回首页的「选择项目」面板挑一个项目，
 * 挑完就在那个项目下开会话；正在跑的会话留在后台继续，不受影响。
 */
function newChat(): void {
  if (projectStore.projects.length < 1) {
    // 一个项目都没有：直接进新建项目表单
    router.push(projectCreateRoute())
    return
  }
  conversationStore.deselect()
  router.push(page.home)
}
</script>

<template>
  <div class="sidebar-header">
    <div class="header-top">
      <div class="header-brand">
        <BrandLogo :size="30" class="header-logo" />
        <span class="header-name">fs-bot</span>
      </div>
      <div class="header-actions">
        <el-button text size="small" @click="router.push(page.search)"> 🔍 </el-button>
      </div>
    </div>
    <div class="header-new-chat">
      <el-button type="primary" class="new-chat-btn" @click="newChat"> 开启新对话 </el-button>
      <div class="header-entries">
        <el-tooltip
          v-for="entry in entries"
          :key="entry.path"
          :content="entry.title"
          placement="bottom"
        >
          <button
            type="button"
            class="entry-button"
            :aria-label="entry.title"
            :class="{ active: router.currentRoute.value.path === entry.path }"
            @click="router.push(entry.path)"
          >
            <LayoutIcon class="entry-icon" :name="entry.icon" />
            <span class="entry-label">{{ entry.label }}</span>
          </button>
        </el-tooltip>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.sidebar-header {
  padding: 12px;
  border-bottom: 1px solid var(--el-border-color-light);
}

.header-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.header-brand {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-logo {
  flex-shrink: 0;
}

.header-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--ev-c-text-1);
}

.header-actions {
  display: flex;
  gap: 4px;
}

.header-new-chat {
  margin-top: 10px;

  .new-chat-btn {
    width: 100%;
  }
}

.header-entries {
  display: flex;
  justify-content: space-between;
  gap: 6px;
  margin-top: 8px;
}

.entry-button {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 3px;
  height: 48px;
  padding: 5px 2px 4px;
  font-size: 17px;
  color: var(--ev-c-text-2);
  background: transparent;
  border: 1px solid transparent;
  border-radius: 6px;
  cursor: pointer;
  transition:
    color 0.2s,
    background 0.2s;

  .entry-icon {
    flex-shrink: 0;
  }

  .entry-label {
    font-size: 11px;
    line-height: 1;
    white-space: nowrap;
  }

  &:hover {
    color: var(--el-color-primary);
    background: var(--el-fill-color-light);
  }

  &.active {
    color: var(--el-color-primary);
    background: var(--el-color-primary-light-9);
  }
}
</style>
