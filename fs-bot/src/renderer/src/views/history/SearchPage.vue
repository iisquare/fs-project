<script setup lang="ts">
import { ref, computed, nextTick, onBeforeUnmount, onMounted, watch } from 'vue'
import { useConversationStore } from '@renderer/stores/conversation'
import { useProjectStore } from '@renderer/stores/project'
import { useChatStore } from '@renderer/stores/chat'
import ConversationDb, { type HistoryHit } from '@renderer/db/ConversationDb'
import ApiUtil from '@renderer/utils/ApiUtil'
import { highlightParts, roleText, snippet } from '@renderer/utils/TextUtil'
import { groupByTime, GROUP_LABELS, formatTime } from '@renderer/utils/TimeUtil'
import { errorMessage } from '@renderer/utils/ErrorUtil'
import { confirmRemoveConversation } from '@renderer/utils/ConversationUtil'
import { useRouter } from 'vue-router'
import { page } from '@renderer/router/config'

const router = useRouter()
const conversationStore = useConversationStore()
const projectStore = useProjectStore()
const chatStore = useChatStore()

const searchQuery = ref('')
const pageNum = ref(1)
const pageSize = 20
const pageLoading = ref(false)
const pageError = ref('')
const searchError = ref('')

/** 一次检索取回的命中条数上限：本地库（ConversationDb.search）一次最多给 30 条 */
const SEARCH_LIMIT = 30
/** 每行最多列出几条命中片段：更多的收成一行计数，行高不至于被撑开 */
const SNIPPET_LIMIT = 2
/** 片段在关键词前后各留多少个字符：列表里两行读得完 */
const SNIPPET_SPAN = 40
/** 敲字时不必每个字符都查一次库：停手一下再查（本地库很快，这层只省掉中间的往返） */
const SEARCH_DEBOUNCE = 200

/** 一行里命中的消息片段 */
interface SearchSnippet {
  id: string
  role: string
  text: string
}

/** 检索结果的一行：一条会话 + 行内命中的片段（标题命中没有片段：标题本身就是命中的内容） */
interface SearchRow {
  id: string
  title: string
  projectId: string
  updatedAt: string
  snippets?: SearchSnippet[]
}

/** 关键词：空串表示没在检索，页面照旧列出全部会话 */
const keyword = computed(() => searchQuery.value.trim())
const searching = ref(false)
/** 内容检索的命中（按会话归拢成行） */
const hitRows = ref<SearchRow[]>([])
/** 命中数顶到上限：结果是被截断的，提示一句让用户把关键词收窄 */
const hitTruncated = ref(false)
/** 每次检索发一个令牌：结果回来时令牌已经变了（关键词又改了）就把这份丢掉 */
let searchToken = 0
let searchTimer = 0

/** 行内改名：一次只改一行，标题定下后各轮对话都不再动它 */
const editingId = ref('')
const titleDraft = ref('')
const titleInput = ref<HTMLInputElement | null>(null)
/** 更多操作菜单开在哪一行：展开的那一行按钮点出主色，与其它行区分 */
const menuOpenId = ref('')

async function loadPage(): Promise<void> {
  pageLoading.value = true
  pageError.value = ''
  const results = await Promise.allSettled([
    projectStore.fetchProjects(),
    conversationStore.fetchConversations()
  ])
  const failure = results.find((item): item is PromiseRejectedResult => 'rejected' === item.status)
  if (failure) pageError.value = errorMessage(failure.reason, '对话记录加载失败')
  pageLoading.value = false
}

onMounted(() => {
  void loadPage()
})

/**
 * 关键词变了就重新查一次：标题与消息正文一起查（本地库一条 SQL 查完，
 * 见 db/ConversationDb 的 search），命中按会话归拢成行、行内列出命中的片段。
 */
watch(keyword, (value) => {
  window.clearTimeout(searchTimer)
  // 换关键词等于换一份结果：分页从头开始
  pageNum.value = 1
  if (!value) {
    // 关键词清空：回到全部会话的清单（内容检索只认已落库的会话，草稿还没有消息）
    searchToken++
    searching.value = false
    hitRows.value = []
    hitTruncated.value = false
    searchError.value = ''
    return
  }
  searching.value = true
  searchTimer = window.setTimeout(() => void runSearch(value), SEARCH_DEBOUNCE)
})

onBeforeUnmount(() => window.clearTimeout(searchTimer))

async function runSearch(value: string): Promise<void> {
  const token = ++searchToken
  try {
    const result = await ConversationDb.search(value, '', SEARCH_LIMIT)
    // 这已经不是最新的一次检索（用户又改了关键词 / 清空了）：结果作废
    if (token !== searchToken) return
    const hits: HistoryHit[] = ApiUtil.succeed(result) ? ApiUtil.data(result) : []
    hitRows.value = groupHits(hits, value)
    hitTruncated.value = hits.length >= SEARCH_LIMIT
    searchError.value = ''
  } catch (error) {
    // 查库失败（库还没就绪之类）：留着上一次的结果，不把一份空清单盖上去
    searchError.value = errorMessage(error, '对话内容检索失败')
  } finally {
    // 过期的那次回来时新的还在跑，别把它关掉
    if (token === searchToken) searching.value = false
  }
}

function retryLoad(): void {
  if (pageError.value) {
    searchError.value = ''
    void loadPage()
    return
  }
  if (searchError.value && keyword.value) void runSearch(keyword.value)
}

/** 命中按会话归拢：库里标题命中排在前，同一个会话只占一行 */
function groupHits(hits: HistoryHit[], value: string): SearchRow[] {
  const rows: SearchRow[] = []
  const byId = new Map<string, SearchRow>()
  for (const hit of hits) {
    let row = byId.get(hit.conversationId)
    if (!row) {
      row = {
        id: hit.conversationId,
        title: hit.conversationTitle,
        projectId: hit.projectId,
        updatedAt: hit.updatedAt,
        snippets: []
      }
      byId.set(hit.conversationId, row)
      rows.push(row)
    }
    // 标题命中不用再给片段：标题在行上直接标出关键词了
    if ('title' === hit.target) continue
    row.snippets?.push({
      id: hit.messageId,
      role: hit.role,
      text: snippet(hit.content, value, SNIPPET_SPAN)
    })
  }
  return rows
}

/** 页面当前要展示的行：有关键词就是检索命中，没有就是全部会话 */
const results = computed<SearchRow[]>(() =>
  keyword.value ? hitRows.value : conversationStore.conversations
)

const shown = computed(() => results.value.slice(0, pageNum.value * pageSize))

/** 行内要列的命中片段（多于 SNIPPET_LIMIT 条的收成一行计数） */
function snippetRows(row: SearchRow): SearchSnippet[] {
  return (row.snippets ?? []).slice(0, SNIPPET_LIMIT)
}

function hiddenHits(row: SearchRow): number {
  return Math.max(0, (row.snippets?.length ?? 0) - SNIPPET_LIMIT)
}

/** 标题与片段里的关键词：切出来交给模板，命中段用 <mark> 标出 */
function partsOf(text: string): Array<{ text: string; hit: boolean }> {
  return highlightParts(text, keyword.value)
}

const grouped = computed(() => {
  return groupByTime(shown.value)
})

const groupKeys = ['today', 'yesterday', 'thisWeek', 'earlier'] as const

async function handleSelect(id: string): Promise<void> {
  // 正在改名字的这一行先把话说完：点它不当作切换会话
  if (editingId.value === id) return
  try {
    await conversationStore.select(id)
    router.push(page.home)
  } catch (error) {
    searchError.value = errorMessage(error, '会话内容加载失败')
  }
}

async function handleDelete(id: string): Promise<void> {
  const running = chatStore.find(id)?.running === true
  if (!(await confirmRemoveConversation(running))) return
  try {
    await conversationStore.remove(id)
    // 检索结果是查库那一刻的快照：删完重新查一次，别留着已经删掉的会话
    if (keyword.value) void runSearch(keyword.value)
  } catch (error) {
    searchError.value = errorMessage(error, '删除会话失败')
  }
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
  const current = results.value.find((item) => item.id === id)
  // 空标题与没改动都不写库：留空等于放弃这次改名，保持原样
  if (!title || !current || title === current.title) return
  void conversationStore.rename(id, title).then(() => {
    // 检索结果是快照（不是列表里那个对象）：改完名重新查一次，行上的标题与库里一致
    // （改成不再命中关键词的标题时，这一行也就跟着消失了）
    if (keyword.value) void runSearch(keyword.value)
  })
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
  <div class="search-page ui-page" v-loading="pageLoading">
    <div class="ui-page-header">
      <div class="ui-page-header__copy">
        <h1 class="ui-page-title">对话检索</h1>
        <p class="ui-page-desc">在本机保存的会话中检索标题与消息内容，命中片段会直接标出关键词。</p>
      </div>
    </div>

    <div class="ui-toolbar search-toolbar">
      <el-input
        v-model="searchQuery"
        placeholder="输入关键词检索会话标题与对话内容..."
        clearable
        class="ui-toolbar__search"
      >
        <template #prefix>
          <svg class="search-icon" viewBox="0 0 16 16" width="14" height="14" aria-hidden="true">
            <circle cx="7" cy="7" r="4.5" fill="none" stroke="currentColor" stroke-width="1.3" />
            <path
              d="m10.5 10.5 3 3"
              fill="none"
              stroke="currentColor"
              stroke-width="1.3"
              stroke-linecap="round"
            />
          </svg>
        </template>
      </el-input>
      <!-- 命中数顶到上限：说清楚这是截断过的，免得当成全部结果 -->
      <div v-if="keyword && hitTruncated" class="search-note">
        命中较多，仅展示前 {{ SEARCH_LIMIT }} 条，输入更具体的关键词可以缩小范围
      </div>
    </div>

    <el-alert
      v-if="pageError || searchError"
      class="page-alert"
      type="warning"
      :closable="false"
      show-icon
      :title="pageError || searchError"
    >
      <el-button text type="primary" size="small" @click="retryLoad">重试</el-button>
    </el-alert>

    <div v-if="!searching && results.length === 0" class="search-empty">
      <el-empty :description="keyword ? `没有命中「${keyword}」的会话` : '暂无对话记录'" />
    </div>
    <div v-else v-loading="searching" class="search-results">
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
            <!-- 首行：标题 / 时间 / 项目标签 / 更多操作，同一行垂直居中 -->
            <div class="result-head">
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
              <!-- 标题：双击或走右侧「更多」里的重命名；检索时标题里的关键词一并标出 -->
              <div v-else class="result-title" @dblclick.stop="startRename(conv)">
                <template v-for="(part, index) in partsOf(conv.title)" :key="index">
                  <mark v-if="part.hit">{{ part.text }}</mark>
                  <template v-else>{{ part.text }}</template>
                </template>
              </div>
              <div class="result-time">{{ formatTime(conv.updatedAt) }}</div>
              <!-- 项目标签：直接当标题行的一格（flex 项）——外面再套一层 div 的话，
                   行内标签会在那层的水平盒里按基线排，实测被顶得比中心低 2px 左右 -->
              <el-tag v-if="projectName(conv.projectId)" class="result-app" size="small">
                {{ projectName(conv.projectId) }}
              </el-tag>
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
            <!-- 命中的结果：另起一块接在标题行下方，逐条给出命中的消息片段（改名的这一会儿先收起来） -->
            <template v-if="editingId !== conv.id">
              <div v-if="snippetRows(conv).length > 0" class="result-hits">
                <div v-for="hit in snippetRows(conv)" :key="hit.id" class="result-snippet">
                  <span class="snippet-role">{{ roleText(hit.role) }}</span>
                  <span class="snippet-text">
                    <template v-for="(part, index) in partsOf(hit.text)" :key="index">
                      <mark v-if="part.hit">{{ part.text }}</mark>
                      <template v-else>{{ part.text }}</template>
                    </template>
                  </span>
                </div>
                <div v-if="hiddenHits(conv) > 0" class="snippet-more">
                  另有 {{ hiddenHits(conv) }} 条命中
                </div>
              </div>
            </template>
          </div>
        </div>
      </template>
      <div v-if="results.length > pageNum * pageSize" class="load-more">
        <el-button text @click="pageNum++">加载更多</el-button>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.page-alert {
  margin-bottom: 16px;
}

.search-icon {
  display: block;
  color: var(--ev-c-text-3);
}

/* 结果被截断的提示：跟着检索框，弱化成一行说明 */
.search-note {
  width: 100%;
  font-size: 12px;
  color: var(--ev-c-text-3);
}

.search-empty {
  padding: 40px 0;
}

.result-group {
  margin-bottom: 24px;
}

.group-title {
  padding: 2px 4px 10px;
  font-size: 12px;
  font-weight: 600;
  color: var(--ev-c-text-3);
}

/**
 * 一行会话：上面一块是标题行（标题 / 时间 / 项目标签 / 更多操作），
 * 下面一块是命中的结果——两块各有各的容器，互不干涉。
 */
.result-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 14px 16px;
  background: transparent;
  border: 1px solid transparent;
  border-radius: 12px;
  cursor: pointer;
  transition:
    border-color 0.2s,
    box-shadow 0.2s,
    background 0.2s;

  & + & {
    margin-top: 8px;
  }

  &:hover {
    background: var(--ui-surface-hover);
    border-color: color-mix(in srgb, var(--el-color-primary) 16%, var(--ui-card-border));
    box-shadow: none;
  }

  &.editing {
    border-color: color-mix(in srgb, var(--el-color-primary) 32%, var(--ui-card-border));
  }
}

/* 标题行：标题占满剩余宽度，时间 / 项目标签 / 更多操作跟在同一行里垂直居中 */
.result-head {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

/* 命中的结果：标题行下方独立一块，逐条往下排 */
.result-hits {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

/* 标题：占满标题行剩下的宽度，过长了在自己的格子里省略 */
.result-title {
  flex: 1;
  min-width: 0;
  font-size: 14px;
  font-weight: 550;
  color: var(--ev-c-text-1);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 关键词：主色标出。标题与片段共用一套口径（切片只切命中段，其余照常展示） */
.result-title mark,
.snippet-text mark {
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
  border-radius: 2px;
}

/* 命中的内容：角色 + 片段；关键词在片段中间，两行收不住时截掉的是两头 */
.result-snippet {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  min-width: 0;
  font-size: 12px;
  line-height: 1.5;
  color: var(--ev-c-text-2);
}

.snippet-role {
  flex: none;
  color: var(--ev-c-text-3);
}

.snippet-text {
  min-width: 0;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
  word-break: break-word;
}

.snippet-more {
  font-size: 12px;
  color: var(--ev-c-text-3);
}

/* 时间与项目标签：跟着标题行居中，不参与压缩（挤的是标题那一格） */
.result-time {
  flex: none;
  font-size: 12px;
  color: var(--ev-c-text-3);
  white-space: nowrap;
}

/* 项目标签本身就是标题行的一格（flex 项）：自己撑 20px，跟时间、菜单同一个中线 */
.result-app {
  flex: none;
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
