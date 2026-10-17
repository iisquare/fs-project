<script setup lang="ts">
/**
 * 会话文件区 - 对话右侧的一栏：上半是项目工作区的目录树，下半是这段对话交付出来的文件。
 *
 * 两个页签分开两件事：工作区是项目声明的目录树（主进程给的清单拼出来），
 * 交付文件是这段对话里模型产出 / 拿到地址的东西。当前页签记在本机（见 stores/files）。
 *
 * 工作区树按项目声明的目录取（workspace:files 一次给全量条目，这里按路径拼成树），
 * 目录点一下展开收起、文件点一下用默认程序打开，右侧按钮在文件夹中定位（同一条交互见 FileActionUtil）；
 * 整行还能往外拖：拖到资源管理器 / 别的应用就是把这个文件交给它（原生拖放由主进程发起），
 * 拖回本应用的对话列（消息区 / 输入区）就是一条 @ 引用；
 * 交付清单从消息的工具调用里抽（core/FileRefs 的 deliveredTimeline）：最近交付的排在最前，
 * 最后一条回复里点到过的算「最终交付」单列一组，其余收进「过程文件」并按需折叠——
 * 一次任务写过十几个脚本时，这样才找得到真正要交的东西。
 *
 * 一轮回复跑完（running 由真转假）自动重取一次工作区——这期间模型很可能写了新文件；
 * 也可以点页签条右上角的刷新按钮手动重取。
 */
import { computed, ref, watch } from 'vue'
import type { ChatMessage } from '@renderer/stores/chat'
import { deliveredTimeline, type DeliveredEntry } from '@renderer/core/FileRefs'
import { useFilesStore } from '@renderer/stores/files'
import FileActionUtil from '@renderer/utils/FileActionUtil'

const props = defineProps<{
  projectId: string
  /** 会话里当前分支的消息：交付清单按它汇总 */
  messages: ChatMessage[]
  /** 项目声明的工作区目录：树的根（为空时给出「未声明」的说明） */
  roots: Array<{ path: string }>
  /** 有没有一轮回复正在跑：跑完自动刷新工作区 */
  running?: boolean
}>()

const files = useFilesStore()

/** 工作区条目（workspace:files 的返回里只用到这几项） */
interface WorkspaceRow {
  name: string
  path: string
  dir: boolean
}

/** 目录树节点：目录带子节点，文件不带 */
interface FileNode {
  name: string
  path: string
  dir: boolean
  children: FileNode[]
}

/** 列表里的一行：树按当前展开状态压平成行，缩进用 depth 表示 */
type FileRow = FileNode & { depth: number }

/** 一屏最多渲染多少行：遍历到的条目上限是 3000，全展开铺出来太重，超出部分给一句说明 */
const MAX_ROWS = 800

const rows = ref<WorkspaceRow[]>([])
const loading = ref(false)
const error = ref('')
/** 展开的目录：只记路径，重取工作区后仍按它展开 */
const expanded = ref<string[]>([])
/** 首次取到树时把根目录展开一次：进来看得见工作区里有什么，之后按用户的展开状态走 */
const seeded = ref(false)

/** 路径归一：末尾的分隔符去掉，目录的路径因此可以直接用来比较 */
function normalize(path: string): string {
  return String(path ?? '').replace(/[\\/]+$/, '')
}

/** 路径末段：渲染进程里不引 Node 的 path（那个模块在主进程），按分隔符自己切 */
function baseName(path: string): string {
  const parts = normalize(path)
    .split(/[\\/]/)
    .filter((item) => !!item)
  return parts[parts.length - 1] ?? String(path ?? '')
}

/** 父路径：一路向上找挂点用 */
function parentOf(path: string): string {
  const text = normalize(path)
  const index = Math.max(text.lastIndexOf('\\'), text.lastIndexOf('/'))
  return 0 < index ? text.slice(0, index) : ''
}

/**
 * 拼树：项目声明的目录是根（即使里面空着也展示，工作区在哪一眼可见），
 * 其余条目按父路径挂到最近的已知目录下；找不到挂点的（理论上不该有）退到最外层。
 * 同级排序：目录在前，然后按名称排。
 */
const tree = computed<FileNode[]>(() => {
  const nodes = new Map<string, FileNode>()
  const forest: FileNode[] = []
  const nodeOf = (path: string, name: string, dir: boolean): FileNode => {
    const key = normalize(path)
    const exists = nodes.get(key)
    if (exists) return exists
    const node: FileNode = { name, path: key, dir, children: [] }
    nodes.set(key, node)
    return node
  }
  for (const root of props.roots ?? []) {
    const key = normalize(root?.path ?? '')
    if (!key) continue
    const node = nodeOf(key, baseName(key), true)
    if (!forest.includes(node)) forest.push(node)
  }
  for (const row of rows.value) {
    const key = normalize(row.path)
    if (!key || nodes.has(key)) continue
    const node = nodeOf(key, row.name || baseName(key), row.dir)
    // 父目录可能是树里的某一层，也可能就是根本身；都没有时退到最外层
    let parent = parentOf(key)
    let target: FileNode | undefined
    while (parent) {
      target = nodes.get(parent)
      if (target) break
      parent = parentOf(parent)
    }
    if (target) target.children.push(node)
    else forest.push(node)
  }
  const sort = (list: FileNode[]): FileNode[] =>
    list
      .sort((a, b) => (a.dir === b.dir ? a.name.localeCompare(b.name) : a.dir ? -1 : 1))
      .map((node) => ({ ...node, children: sort(node.children) }))
  return sort(forest)
})

/** 按展开状态压平：收起目录的子节点不出现在列表里；超过上限时截断，条数留给界面说明 */
const flattened = computed<{ rows: FileRow[]; total: number }>(() => {
  const list: FileRow[] = []
  let total = 0
  const walk = (nodes: FileNode[], depth: number): void => {
    for (const node of nodes) {
      total += 1
      if (list.length < MAX_ROWS) list.push({ ...node, depth })
      if (node.dir && expanded.value.includes(node.path)) walk(node.children, depth + 1)
    }
  }
  walk(tree.value, 0)
  return { rows: list, total }
})

const visibleRows = computed<FileRow[]>(() => flattened.value.rows)
const truncated = computed(() => Math.max(0, flattened.value.total - flattened.value.rows.length))

/** 过程文件默认只露前几项：一次任务写过十几个脚本时，列表铺太长反而找不到东西 */
const PROCESS_PREVIEW = 6
/** 过程文件是否展开全部（只在本次运行内记，不落库） */
const processExpanded = ref(false)

/** 交付清单：整段会话汇总，最近碰过的排在最前（见 core/FileRefs 的 deliveredTimeline） */
const delivered = computed<DeliveredEntry[]>(() => deliveredTimeline(props.messages))
const finalFiles = computed(() => delivered.value.filter((item) => item.final))
const processFiles = computed(() => delivered.value.filter((item) => !item.final))

/** 两组：最终交付在上（没有就不出现），过程文件在下 */
const groups = computed(() =>
  [
    { key: 'final', title: '最终交付', files: finalFiles.value },
    { key: 'process', title: '过程文件', files: processFiles.value }
  ].filter((group) => group.files.length > 0)
)

/** 一组里实际铺出来的行：过程文件按折叠状态截断 */
function rowsOf(group: { key: string; files: DeliveredEntry[] }): DeliveredEntry[] {
  if ('process' !== group.key || processExpanded.value) return group.files
  return group.files.slice(0, PROCESS_PREVIEW)
}

/** 取工作区的文件清单：一次给全量条目，树的拼装与排序都在 tree 里做 */
async function load(): Promise<void> {
  const project = String(props.projectId ?? '')
  if (!project) {
    rows.value = []
    return
  }
  loading.value = true
  error.value = ''
  try {
    const list = await window.ipc.workspace.files(project)
    rows.value = (list ?? []).map((item) => ({
      name: item.name,
      path: item.path,
      dir: item.dir
    }))
  } catch (problem) {
    error.value = problem instanceof Error ? problem.message : String(problem ?? '')
    rows.value = []
  } finally {
    loading.value = false
  }
}

/**
 * 项目声明的根目录一到就展开：进来看得见工作区里有什么。
 * 项目清单是异步取的（可能晚于文件清单），所以判据落在 roots 上，落到哪一次 load 都不合适。
 */
watch(
  () => (props.roots ?? []).map((root) => normalize(root?.path ?? '')).join('|'),
  () => {
    if (seeded.value) return
    const dirs = (props.roots ?? [])
      .map((root) => normalize(root?.path ?? ''))
      .filter((path) => !!path)
    if (!dirs.length) return
    expanded.value = dirs
    seeded.value = true
  },
  { immediate: true }
)

// 换项目重来：展开状态与「已展开过根目录」的标记都跟着重置
watch(
  () => String(props.projectId ?? ''),
  () => {
    expanded.value = []
    seeded.value = false
    void load()
  },
  { immediate: true }
)

/** 一轮跑完重取一次：模型这期间可能写了新文件，光靠手动刷新容易看到过期目录 */
watch(
  () => !!props.running,
  (value, previous) => {
    if (previous && !value) void load()
  }
)

function toggleDir(node: FileNode): void {
  expanded.value = expanded.value.includes(node.path)
    ? expanded.value.filter((path) => path !== node.path)
    : [...expanded.value, node.path]
}

function openPath(path: string): void {
  FileActionUtil.open(String(props.projectId ?? ''), path)
}

function revealPath(path: string): void {
  FileActionUtil.reveal(String(props.projectId ?? ''), path)
}

/** 拖出窗口：整行可拖（目录也行，拖到资源管理器就是复制这个文件夹） */
function dragPath(path: string, event: DragEvent): void {
  FileActionUtil.dragOut(event, String(props.projectId ?? ''), path)
}
</script>

<template>
  <div class="conversation-files">
    <!-- 两个页签：工作区目录树 / 这段对话交付的文件。刷新按钮只对工作区有意义，浮在页签条右侧 -->
    <el-tabs class="files-tabs" :model-value="files.tab" @update:model-value="files.setTab">
      <el-tab-pane name="workspace">
        <template #label>
          工作区<span v-if="roots?.length" class="tab-count">{{ roots.length }}</span>
        </template>
        <el-scrollbar class="files-body">
          <!-- 工作区：项目声明的目录树 -->
          <p v-if="!roots?.length" class="files-hint">项目未声明工作区目录</p>
          <p v-else-if="error" class="files-hint">{{ error }}</p>
          <p v-else-if="loading && !visibleRows.length" class="files-hint">正在读取目录…</p>
          <ul v-else class="file-tree">
            <li
              v-for="row in visibleRows"
              :key="row.path"
              class="file-row"
              draggable="true"
              @dragstart="dragPath(row.path, $event)"
            >
              <span class="row-indent" :style="{ width: `${row.depth * 12}px` }" />
              <!-- 目录：点一下展开收起 -->
              <button
                v-if="row.dir"
                type="button"
                class="row-main"
                :title="row.path"
                @click="toggleDir(row)"
              >
                <svg class="row-icon" viewBox="0 0 16 16" width="12" height="12" aria-hidden="true">
                  <path
                    v-if="expanded.includes(row.path)"
                    d="M4.4 2.6 11 8l-6.6 5.4"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="1.4"
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    transform="rotate(90 8 8)"
                  />
                  <path
                    v-else
                    d="M4.4 2.6 11 8l-6.6 5.4"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="1.4"
                    stroke-linecap="round"
                    stroke-linejoin="round"
                  />
                </svg>
                <span class="row-name">{{ row.name }}</span>
              </button>
              <!-- 文件：点一下用默认程序打开 -->
              <button
                v-else
                type="button"
                class="row-main"
                :title="row.path"
                @click="openPath(row.path)"
              >
                <svg class="row-icon" viewBox="0 0 16 16" width="12" height="12" aria-hidden="true">
                  <path
                    d="M6.4 1.8h3.2l2.6 2.6v7.8a1 1 0 0 1-1 1H4.8a1 1 0 0 1-1-1V2.8a1 1 0 0 1 1-1z"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="1.1"
                    stroke-linejoin="round"
                  />
                </svg>
                <span class="row-name">{{ row.name }}</span>
              </button>
              <el-tooltip content="在文件夹中显示" placement="top">
                <button type="button" class="row-action" @click="revealPath(row.path)">
                  <svg viewBox="0 0 16 16" width="11" height="11" aria-hidden="true">
                    <path
                      d="M1.9 3.5h3.9l1.2 1.6h7.1v7.2a.8.8 0 0 1-.8.8H2.7a.8.8 0 0 1-.8-.8z"
                      fill="none"
                      stroke="currentColor"
                      stroke-width="1.1"
                      stroke-linejoin="round"
                    />
                  </svg>
                </button>
              </el-tooltip>
            </li>
          </ul>
          <p v-if="truncated && !loading && !error" class="files-hint">
            还有 {{ truncated }} 项未显示（收起一些目录再看）
          </p>
        </el-scrollbar>
      </el-tab-pane>

      <!-- 交付的文件：这段对话里模型写出来 / 移动到位 / 服务端给回地址的东西 -->
      <el-tab-pane name="delivered">
        <template #label>
          交付文件<span v-if="delivered.length" class="tab-count">{{ delivered.length }}</span>
        </template>
        <el-scrollbar class="files-body">
          <p v-if="!delivered.length" class="files-hint">这段对话还没有产出文件</p>
          <!-- 两组：最终交付在上，过程文件在下（长了先折叠，别把要交的东西挤到看不见） -->
          <template v-else>
            <section v-for="group in groups" :key="group.key" class="files-group">
              <div class="group-head">
                <span class="group-title">{{ group.title }}</span>
                <span class="group-meta">{{ group.files.length }} 个</span>
                <button
                  v-if="'process' === group.key && group.files.length > PROCESS_PREVIEW"
                  type="button"
                  class="group-toggle"
                  @click="processExpanded = !processExpanded"
                >
                  {{ processExpanded ? '收起' : `展开全部 ${group.files.length} 个` }}
                </button>
              </div>
              <ul class="file-list">
                <li
                  v-for="file in rowsOf(group)"
                  :key="file.url || file.path"
                  class="file-row"
                  :draggable="!file.url"
                  @dragstart="dragPath(file.path, $event)"
                >
                  <a
                    v-if="file.url"
                    class="row-main"
                    :href="file.url"
                    target="_blank"
                    rel="noopener noreferrer"
                    :title="file.url"
                  >
                    <svg
                      class="row-icon"
                      viewBox="0 0 16 16"
                      width="12"
                      height="12"
                      aria-hidden="true"
                    >
                      <path
                        d="M6.4 1.8h3.2l2.6 2.6v7.8a1 1 0 0 1-1 1H4.8a1 1 0 0 1-1-1V2.8a1 1 0 0 1 1-1z"
                        fill="none"
                        stroke="currentColor"
                        stroke-width="1.1"
                        stroke-linejoin="round"
                      />
                    </svg>
                    <span class="row-name">{{ file.name }}</span>
                    <span class="row-tag">外部地址</span>
                  </a>
                  <button
                    v-else
                    type="button"
                    class="row-main"
                    :title="file.path"
                    @click="openPath(file.path)"
                  >
                    <svg
                      class="row-icon"
                      viewBox="0 0 16 16"
                      width="12"
                      height="12"
                      aria-hidden="true"
                    >
                      <path
                        v-if="file.dir"
                        d="M1.9 3.5h3.9l1.2 1.6h7.1v7.2a.8.8 0 0 1-.8.8H2.7a.8.8 0 0 1-.8-.8z"
                        fill="none"
                        stroke="currentColor"
                        stroke-width="1.1"
                        stroke-linejoin="round"
                      />
                      <path
                        v-else
                        d="M6.4 1.8h3.2l2.6 2.6v7.8a1 1 0 0 1-1 1H4.8a1 1 0 0 1-1-1V2.8a1 1 0 0 1 1-1z"
                        fill="none"
                        stroke="currentColor"
                        stroke-width="1.1"
                        stroke-linejoin="round"
                      />
                    </svg>
                    <span class="row-name">{{ file.name }}</span>
                  </button>
                  <el-tooltip v-if="!file.url" content="在文件夹中显示" placement="top">
                    <button type="button" class="row-action" @click="revealPath(file.path)">
                      <svg viewBox="0 0 16 16" width="11" height="11" aria-hidden="true">
                        <path
                          d="M1.9 3.5h3.9l1.2 1.6h7.1v7.2a.8.8 0 0 1-.8.8H2.7a.8.8 0 0 1-.8-.8z"
                          fill="none"
                          stroke="currentColor"
                          stroke-width="1.1"
                          stroke-linejoin="round"
                        />
                      </svg>
                    </button>
                  </el-tooltip>
                </li>
              </ul>
            </section>
          </template>
        </el-scrollbar>
      </el-tab-pane>
    </el-tabs>

    <el-tooltip v-if="'workspace' === files.tab" content="刷新工作区" placement="top">
      <button type="button" class="files-action" :disabled="loading" @click="load">
        <svg viewBox="0 0 16 16" width="13" height="13" aria-hidden="true">
          <path
            d="M13 8a5 5 0 1 1-1.5-3.5"
            fill="none"
            stroke="currentColor"
            stroke-width="1.3"
            stroke-linecap="round"
          />
          <path
            d="M13.2 2.4v3.2h-3.2"
            fill="none"
            stroke="currentColor"
            stroke-width="1.3"
            stroke-linecap="round"
            stroke-linejoin="round"
          />
        </svg>
      </button>
    </el-tooltip>
  </div>
</template>

<style lang="scss" scoped>
/**
 * 右侧一栏：宽度由 splitter 拖着调（见 stores/files），这里只管纵向排版 ——
 * 页签条在上，页签内容撑满其余高度、各页签自己滚动；刷新按钮浮在页签条右上角。
 */
.conversation-files {
  position: relative;
  flex: none;
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
}

.files-tabs {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  /* 页签条压到 34px：这一栏本来就窄，纵向空间留给目录本身 */
  --el-tabs-header-height: 34px;

  :deep(> .el-tabs__header) {
    flex: none;
    /* 右侧留出刷新按钮的位置，页签与它不重叠 */
    padding: 0 36px 0 8px;
    margin: 0;
    border-bottom: none;
  }

  /**
   * 页签条的下边线是 Element 画的（nav-wrap 的 ::after，默认 2px）：
   * 这一栏窄，2px 压得住视线，收成 1px 并换成更浅的一档。
   * 别在 header 上再加 border-bottom——那是第二条线，看着会重一倍。
   */
  :deep(> .el-tabs__header > .el-tabs__nav-wrap::after) {
    height: 1px;
    background-color: var(--el-border-color-lighter);
  }

  :deep(> .el-tabs__content) {
    flex: 1;
    min-height: 0;
  }

  :deep(.el-tab-pane) {
    height: 100%;
  }

  /* 页签上的计数：跟在标题后面的一枚小数字，没有内容时整个不出现 */
  .tab-count {
    margin-left: 4px;
    font-size: 11px;
    color: var(--el-text-color-placeholder);
  }
}

/* 刷新按钮：浮在页签条右上角（只有工作区那一页用得上） */
.files-action {
  position: absolute;
  top: 6px;
  right: 8px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  padding: 0;
  border: none;
  border-radius: 4px;
  background: transparent;
  color: var(--el-text-color-placeholder);
  cursor: pointer;

  &:hover:not(:disabled) {
    color: var(--el-color-primary);
    background: var(--el-fill-color-dark);
  }

  &:disabled {
    cursor: default;
    opacity: 0.5;
  }
}

.files-body {
  height: 100%;
  min-height: 0;

  :deep(> .el-scrollbar__wrap > .el-scrollbar__view) {
    padding: 8px 6px 12px;
  }
}

.files-hint {
  margin: 0;
  padding: 2px 6px 4px;
  font-size: 12px;
  line-height: 1.7;
  color: var(--el-text-color-placeholder);
  word-break: break-word;
}

/* 交付清单的两组：最终交付在上，过程文件在下 */
.files-group + .files-group {
  margin-top: 10px;
}

.group-head {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 0 6px 4px;

  .group-title {
    flex: none;
    font-size: 12px;
    color: var(--el-text-color-secondary);
  }

  .group-meta {
    flex: 1;
    min-width: 0;
    font-size: 11px;
    color: var(--el-text-color-placeholder);
  }

  /* 过程文件多了先折叠：这一枚按正文口径做，比按钮轻 */
  .group-toggle {
    flex: none;
    padding: 0;
    border: none;
    background: transparent;
    font-family: inherit;
    font-size: 11px;
    color: var(--el-color-primary);
    cursor: pointer;

    &:hover {
      text-decoration: underline;
    }
  }
}

.file-tree,
.file-list {
  margin: 0;
  padding: 0;
  list-style: none;
}

/* 一行文件 / 目录：缩进块 + 行身（点开）+ 定位按钮 */
.file-row {
  display: flex;
  align-items: center;
  border-radius: 4px;

  &:hover {
    /* 页面底色是浅灰：悬浮反馈要比它深一档才看得出来 */
    background: var(--el-fill-color-dark);

    .row-action {
      opacity: 1;
    }
  }

  .row-indent {
    flex: none;
  }

  .row-main {
    display: flex;
    align-items: center;
    gap: 5px;
    flex: 1;
    min-width: 0;
    padding: 3px 4px;
    border: none;
    border-radius: 4px;
    background: transparent;
    font-family: inherit;
    font-size: 12px;
    line-height: 1.6;
    color: var(--el-text-color-regular);
    text-align: left;
    text-decoration: none;
    cursor: pointer;

    &:hover .row-name {
      color: var(--el-color-primary);
    }

    .row-icon {
      flex: none;
      color: var(--el-text-color-placeholder);
    }

    /* 名字：占满剩余宽度，路径很长时从名字这一层收住，行高不跳 */
    .row-name {
      flex: 1;
      min-width: 0;
      overflow: hidden;
      white-space: nowrap;
      text-overflow: ellipsis;
    }

    .row-tag {
      flex: none;
      padding: 0 4px;
      border-radius: 3px;
      font-size: 11px;
      color: var(--el-text-color-placeholder);
      background: var(--el-fill-color-dark);
    }
  }

  .row-action {
    flex: none;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 18px;
    height: 18px;
    margin-right: 2px;
    padding: 0;
    border: none;
    border-radius: 3px;
    background: transparent;
    color: var(--el-text-color-placeholder);
    cursor: pointer;
    opacity: 0;
    transition: opacity 0.2s;

    &:hover {
      color: var(--el-color-primary);
    }
  }
}
</style>
