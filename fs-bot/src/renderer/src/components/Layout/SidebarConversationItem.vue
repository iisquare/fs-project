<script setup lang="ts">
import { nextTick, ref } from 'vue'
import { formatTime } from '@renderer/utils/TimeUtil'

const props = defineProps<{
  conversation: {
    id: string
    title: string
    updatedAt: string
  }
  active?: boolean
  projectName?: string
  running?: boolean
  /** 这段对话有访问申请在等答复：有横幅在输入框上方，这里给个点提示（切到别的页面也不会漏） */
  waiting?: boolean
}>()

const emit = defineEmits<{
  select: [id: string]
  remove: [id: string]
  /** 重命名：标题改完交给列表页写进 store，标题没变就不发 */
  rename: [id: string, title: string]
}>()

/** 标题编辑态：就地改，回车 / 失焦保存，Esc 原样放下 */
const editing = ref(false)
const titleDraft = ref('')
const titleInput = ref<HTMLInputElement | null>(null)
/** 更多操作菜单是否展开：展开时按钮点出主色，与行内其它状态呼应 */
const menuOpen = ref(false)

/** 编辑中不切会话：点标题框是为了改名字，不该顺带跳到那段对话 */
function handleSelect(): void {
  if (!editing.value) emit('select', props.conversation.id)
}

async function startRename(): Promise<void> {
  if (editing.value) return
  editing.value = true
  titleDraft.value = props.conversation.title || ''
  await nextTick()
  titleInput.value?.focus()
  titleInput.value?.select()
}

function commitTitle(): void {
  if (!editing.value) return
  editing.value = false
  const title = titleDraft.value.trim()
  // 空标题与没改动都不写库：留空等于放弃这次改名，保持原样
  if (!title || title === props.conversation.title) return
  emit('rename', props.conversation.id, title)
}

function cancelTitle(): void {
  editing.value = false
}

/** 改名框按键：回车保存、Esc 放下；输入法组词中的回车不算确认（与对话输入框同一口径） */
function onTitleKeydown(payload: Event | KeyboardEvent): void {
  const event = payload as KeyboardEvent
  if (event.isComposing || 229 === event.keyCode) return
  if ('Escape' === event.key) {
    event.preventDefault()
    cancelTitle()
    return
  }
  if ('Enter' !== event.key) return
  event.preventDefault()
  commitTitle()
}

/** 菜单选择：重命名与删除都收在这一个按钮里，行宽只占一格 */
function onMenuCommand(command: string): void {
  if ('rename' === command) {
    void startRename()
    return
  }
  if ('remove' === command) emit('remove', props.conversation.id)
}
</script>

<template>
  <div class="conversation-item" :class="{ active, editing }" @click="handleSelect">
    <div class="conv-main">
      <input
        v-if="editing"
        ref="titleInput"
        v-model="titleDraft"
        class="conv-title-input"
        maxlength="60"
        placeholder="会话标题"
        @click.stop
        @keydown="onTitleKeydown"
        @blur="commitTitle"
      />
      <!-- 标题：双击或走右侧「更多」里的重命名 -->
      <div v-else class="conv-title" :title="conversation.title" @dblclick.stop="startRename">
        {{ conversation.title || '新对话' }}
      </div>
      <div class="conv-meta">
        <span v-if="projectName" class="conv-project">{{ projectName }}</span>
        <span class="conv-time">{{ formatTime(conversation.updatedAt) }}</span>
      </div>
    </div>
    <!-- 行右侧：待确认 / 运行状态 + 更多操作（重命名与删除收进菜单，常显不占额外宽度） -->
    <div class="conv-side">
      <span v-if="waiting" class="conv-waiting" title="有访问申请待确认">●</span>
      <span v-if="running" class="conv-running" title="后台运行中">●</span>
      <el-dropdown
        class="conv-menu"
        trigger="click"
        placement="bottom-end"
        popper-class="sidebar-conv-menu"
        @command="onMenuCommand"
        @visible-change="(open: boolean) => (menuOpen = open)"
      >
        <button
          type="button"
          class="conv-more"
          :class="{ 'is-open': menuOpen }"
          title="更多操作"
          @click.stop
        >
          <!-- 更多：竖排三个点，与消息操作条一致的手绘线条口径 -->
          <svg viewBox="0 0 16 16" width="14" height="14" aria-hidden="true">
            <circle cx="8" cy="3.5" r="1.3" fill="currentColor" />
            <circle cx="8" cy="8" r="1.3" fill="currentColor" />
            <circle cx="8" cy="12.5" r="1.3" fill="currentColor" />
          </svg>
        </button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="rename">
              <!-- 菜单项保留图标：与点开菜单前的操作图标同一口径 -->
              <template #icon>
                <svg viewBox="0 0 16 16" width="14" height="14" aria-hidden="true">
                  <path
                    d="M2.4 13.6l.5-2.6 7.5-7.5 2.1 2.1-7.5 7.5-2.6.5z"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="1.3"
                    stroke-linejoin="round"
                  />
                  <path
                    d="M9.6 4.3l2.1 2.1"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="1.3"
                    stroke-linecap="round"
                  />
                </svg>
              </template>
              重命名
            </el-dropdown-item>
            <el-dropdown-item command="remove" class="is-danger">
              <template #icon>
                <svg viewBox="0 0 16 16" width="14" height="14" aria-hidden="true">
                  <path
                    d="M2.8 4.6h10.4"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="1.3"
                    stroke-linecap="round"
                  />
                  <path
                    d="M6.4 4.6V3.5c0-.5.4-.9.9-.9h1.4c.5 0 .9.4.9.9v1.1"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="1.3"
                    stroke-linejoin="round"
                  />
                  <path
                    d="M4.4 4.6l.5 8.1c0 .6.5 1.1 1.1 1.1h3.9c.6 0 1.1-.5 1.1-1.1l.5-8.1"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="1.3"
                    stroke-linejoin="round"
                  />
                  <path
                    d="M6.8 7.2v3.8M9.2 7.2v3.8"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="1.1"
                    stroke-linecap="round"
                  />
                </svg>
              </template>
              删除
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.conversation-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
  padding: 8px 10px 8px 16px;
  cursor: pointer;
  border-radius: 6px;
  margin: 2px 8px;

  &:hover {
    background: var(--el-fill-color-light);

    .conv-more {
      opacity: 1;
    }
  }

  &.active {
    background: var(--el-color-primary-light-9);
  }

  /* 正在改名字的这一行：按钮常显，操作不会因为鼠标移开而消失 */
  &.editing .conv-more {
    opacity: 1;
  }
}

.conv-main {
  flex: 1;
  min-width: 0;
}

.conv-side {
  flex: none;
  display: flex;
  align-items: center;
  gap: 4px;
}

.conv-title {
  font-size: 14px;
  color: var(--ev-c-text-1);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 行内改名：与标题同一字号与位置，只靠下划线提示可改 */
.conv-title-input {
  width: 100%;
  padding: 0;
  font-family: inherit;
  font-size: 14px;
  color: var(--ev-c-text-1);
  background: transparent;
  border: none;
  border-bottom: 1px solid var(--el-color-primary);
  outline: none;
}

.conv-running {
  font-size: 10px;
  line-height: 1;
  color: var(--el-color-success);
}

/* 待确认用警示色：和「运行中」的绿点并排时不至于看混 */
.conv-waiting {
  font-size: 10px;
  line-height: 1;
  color: var(--el-color-warning);
}

.conv-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 2px;
  font-size: 12px;
  color: var(--ev-c-text-3);
}

.conv-project {
  max-width: 90px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.conv-time {
  font-size: 12px;
  color: var(--ev-c-text-3);
  white-space: nowrap;
}

/* 更多操作：竖排三点，悬停整行才显现（侧栏窄，平时让位给标题） */
.conv-more {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  padding: 0;
  color: var(--ev-c-text-3);
  background: transparent;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  opacity: 0;
  transition:
    opacity 0.2s,
    color 0.2s,
    background-color 0.2s;

  /* 本按钮悬停与菜单展开都点出主色；菜单展开时还要保持可见，鼠标移到面板上
     这一格不会跟着淡出（整行悬停的显现由行上那条规则管） */
  &:hover,
  &.is-open {
    opacity: 1;
    color: var(--el-color-primary);
    background: var(--el-fill-color-dark);
  }
}
</style>

<!-- 下拉面板被 teleport 到 body，脱离组件根节点，只能用非 scoped 样式命中 -->
<style lang="scss">
.sidebar-conv-menu .el-dropdown-menu__item.is-danger {
  color: var(--el-color-danger);

  &:not(.is-disabled):hover {
    /* 悬停时也保持危险色：默认的悬停主色会把这行「删除」染成蓝色 */
    color: var(--el-color-danger);
    background: var(--el-color-danger-light-9);
  }
}
</style>
