<script setup lang="ts">
import { ref, computed, nextTick, onMounted } from 'vue'
import { useConversationStore } from '@renderer/stores/conversation'
import { useProjectStore } from '@renderer/stores/project'
import { groupByTime, GROUP_LABELS, formatTime } from '@renderer/utils/TimeUtil'
import { useRouter } from 'vue-router'
import { page } from '@renderer/router/config'

const router = useRouter()
const conversationStore = useConversationStore()
const projectStore = useProjectStore()

const searchQuery = ref('')
const pageNum = ref(1)
const pageSize = 20

/** 行内改名：一次只改一行，标题定下后各轮对话都不再动它 */
const editingId = ref('')
const titleDraft = ref('')
const titleInput = ref<HTMLInputElement | null>(null)
/** 更多操作菜单开在哪一行：展开的那一行按钮点出主色，与其它行区分 */
const menuOpenId = ref('')

onMounted(async () => {
  await projectStore.fetchProjects()
  await conversationStore.fetchConversations()
})

const filtered = computed(() => {
  const keyword = searchQuery.value.trim()
  // 关键词为空时展示全部会话：进入页面即可直接回看历史
  if (!keyword) return conversationStore.conversations
  return conversationStore.conversations.filter((item) => item.title.includes(keyword))
})

const shown = computed(() => filtered.value.slice(0, pageNum.value * pageSize))

const grouped = computed(() => {
  return groupByTime(shown.value)
})

const groupKeys = ['today', 'yesterday', 'thisWeek', 'earlier'] as const

async function handleSelect(id: string) {
  // 正在改名字的这一行先把话说完：点它不当作切换会话
  if (editingId.value === id) return
  await conversationStore.select(id)
  router.push(page.home)
}

async function handleDelete(id: string): Promise<void> {
  await conversationStore.remove(id)
}

function projectName(projectId: string): string {
  return projectStore.find(projectId)?.name ?? ''
}

/**
 * 改名框元素：一次只渲染正在改的那一行，函数式 ref 拿到的是元素本身
 * （v-for 里的字符串 ref 会被收集成数组，取焦点时不方便）。
 */
function bindTitleInput(el: unknown): void {
  // 卸载时也会回调（给 null）：这里只认装上的那个，免得拆旧行把新行的元素顶掉
  if (el) titleInput.value = el as HTMLInputElement
}

async function startRename(conv: { id: string; title: string }): Promise<void> {
  if (editingId.value === conv.id) return
  editingId.value = conv.id
  titleDraft.value = conv.title || ''
  await nextTick()
  titleInput.value?.focus()
  titleInput.value?.select()
}

function commitTitle(): void {
  const id = editingId.value
  if (!id) return
  editingId.value = ''
  const title = titleDraft.value.trim()
  const current = conversationStore.conversations.find((item) => item.id === id)
  // 空标题与没改动都不写库：留空等于放弃这次改名，保持原样
  if (!title || !current || title === current.title) return
  void conversationStore.rename(id, title)
}

function cancelTitle(): void {
  editingId.value = ''
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
function onMenuCommand(command: string, conv: { id: string; title: string }): void {
  if ('rename' === command) {
    void startRename(conv)
    return
  }
  if ('remove' === command) void handleDelete(conv.id)
}

function onMenuVisible(conv: { id: string }, open: boolean): void {
  menuOpenId.value = open ? conv.id : ''
}
</script>

<template>
  <div class="search-page">
    <div class="search-header">
      <h2>对话检索</h2>
      <el-input
        v-model="searchQuery"
        placeholder="输入关键词检索对话..."
        clearable
        class="search-input"
      >
        <template #prefix>🔍</template>
      </el-input>
    </div>

    <div v-if="filtered.length === 0" class="search-empty">
      <el-empty :description="searchQuery.trim() ? '未找到匹配的对话' : '暂无对话记录'" />
    </div>
    <div v-else class="search-results">
      <template v-for="key in groupKeys" :key="key">
        <div v-if="grouped[key].length > 0" class="result-group">
          <div class="group-title">{{ GROUP_LABELS[key] }}</div>
          <div
            v-for="conv in grouped[key]"
            :key="conv.id"
            class="result-item"
            :class="{ editing: editingId === conv.id }"
            @click="handleSelect(conv.id)"
          >
            <input
              v-if="editingId === conv.id"
              :ref="bindTitleInput"
              v-model="titleDraft"
              class="result-title-input"
              maxlength="60"
              placeholder="会话标题"
              @click.stop
              @keydown="onTitleKeydown"
              @blur="commitTitle"
            />
            <!-- 标题：双击或走右侧「更多」里的重命名 -->
            <div v-else class="result-title" @dblclick.stop="startRename(conv)">
              {{ conv.title }}
            </div>
            <div class="result-time">{{ formatTime(conv.updatedAt) }}</div>
            <div v-if="projectName(conv.projectId)" class="result-app">
              <el-tag size="small">{{ projectName(conv.projectId) }}</el-tag>
            </div>
            <!-- 行右侧：更多操作（重命名与删除收进菜单，常显不占额外宽度） -->
            <div class="result-actions">
              <el-dropdown
                class="result-menu"
                trigger="click"
                placement="bottom-end"
                popper-class="history-conv-menu"
                @command="(command: string) => onMenuCommand(command, conv)"
                @visible-change="(open: boolean) => onMenuVisible(conv, open)"
              >
                <button
                  type="button"
                  class="result-more"
                  :class="{ 'is-open': menuOpenId === conv.id }"
                  title="更多操作"
                  @click.stop
                >
                  <!-- 更多：竖排三个点，与侧栏会话项一致的手绘线条口径 -->
                  <svg viewBox="0 0 16 16" width="14" height="14" aria-hidden="true">
                    <circle cx="8" cy="3.5" r="1.3" fill="currentColor" />
                    <circle cx="8" cy="8" r="1.3" fill="currentColor" />
                    <circle cx="8" cy="12.5" r="1.3" fill="currentColor" />
                  </svg>
                </button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="rename">
                      <!-- 菜单项保留图标：与侧栏会话项同一套图标口径 -->
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
        </div>
      </template>
      <div v-if="filtered.length > pageNum * pageSize" class="load-more">
        <el-button text @click="pageNum++">加载更多</el-button>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.search-page {
  padding: 24px 32px;
}

.search-header {
  margin-bottom: 24px;

  h2 {
    font-size: 20px;
    color: var(--ev-c-text-1);
    margin-bottom: 16px;
  }
}

.search-input {
  width: 100%;
}

.search-empty {
  padding: 40px 0;
}

.result-group {
  margin-bottom: 20px;
}

.group-title {
  padding: 8px 0;
  font-size: 12px;
  color: var(--ev-c-text-3);
  border-bottom: 1px solid var(--el-border-color-light);
  margin-bottom: 8px;
}

.result-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 6px;
  cursor: pointer;

  &:hover {
    background: var(--el-fill-color-light);
  }
}

.result-title {
  flex: 1;
  font-size: 14px;
  color: var(--ev-c-text-1);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.result-time {
  font-size: 12px;
  color: var(--ev-c-text-3);
  white-space: nowrap;
}

/* 行内改名：与标题同一字号与位置，只靠下划线提示可改 */
.result-title-input {
  flex: 1;
  min-width: 0;
  padding: 0;
  font-family: inherit;
  font-size: 14px;
  color: var(--ev-c-text-1);
  background: transparent;
  border: none;
  border-bottom: 1px solid var(--el-color-primary);
  outline: none;
}

.result-actions {
  flex: none;
  display: flex;
  align-items: center;
  gap: 4px;
}

/* 更多操作：常显的竖排三点，展开菜单时点出主色 */
.result-more {
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
  transition:
    color 0.2s,
    background-color 0.2s;

  &:hover,
  &.is-open {
    color: var(--el-color-primary);
    background: var(--el-fill-color-dark);
  }
}

.load-more {
  text-align: center;
  padding: 12px 0;
}
</style>

<!-- 下拉面板被 teleport 到 body，脱离组件根节点，只能用非 scoped 样式命中 -->
<style lang="scss">
.history-conv-menu .el-dropdown-menu__item.is-danger {
  color: var(--el-color-danger);

  &:not(.is-disabled):hover {
    /* 悬停时也保持危险色：默认的悬停主色会把这行「删除」染成蓝色 */
    color: var(--el-color-danger);
    background: var(--el-color-danger-light-9);
  }
}
</style>
