<script setup lang="ts">
/**
 * 回复正文的 Markdown 渲染 - 与编排的对话页一致，模型输出按 Markdown 展示。
 * 转换与清洗都收在 core/Markdown 里（见那里的说明）；
 * 代码块单独取出来交给 ChatCode 渲染（语言、行号、复制）。
 *
 * 正文里的知识库图片（形如 ![说明](kb:图片标识)）按当前授权实时签发地址后上屏：
 * 每条正文各自解析（本组件内部按标识去重），不做跨消息缓存，撤权后历史回复里的图片同样只剩默认图；
 * 地址没拿到（无权限 / 已删除 / 请求失败）一律回落默认图，解析期间先用浅色占位图，不出现破图。
 * 图片点一下看大图，同一段正文里的图片按出现顺序左右切换。
 *
 * 本机图片（形如 ![说明](local:相对路径)）走 `workspace:image`：主进程按项目工作区权限读取后
 * 签发 chat-image:// 地址，这里同样先扫描、异步解析，解析期间占位、失败回落默认图。
 * 只处理 Markdown 图片节点；正文里的纯路径不会自动转成图片。
 *
 * 链接分两种处理：本机路径（`[报告](D:\x\报告.md)` 这类，模型交代产物时很常见）点一下用系统默认程序打开；
 * 其余仍是外部地址，交给主进程的窗口打开策略走浏览器（见 core/Markdown 里补 target="_blank" 的说明）。
 */
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { knowledgeImageIds, localImagePaths, markdownParts } from '@renderer/core/Markdown'
import KnowledgeImageUtil from '@renderer/utils/KnowledgeImageUtil'
import LocalImageUtil from '@renderer/utils/LocalImageUtil'
import FileActionUtil from '@renderer/utils/FileActionUtil'
import loadingImage from '@renderer/assets/images/knowledge-image-loading.svg?url'
import fallbackImage from '@renderer/assets/images/knowledge-image-fallback.svg?url'
import ChatCode from './ChatCode.vue'

const props = defineProps<{
  content: string
  /** 会话所属项目：正文里的相对路径按它的工作区目录解析（打开文件时用） */
  projectId?: string
  /** 会话标识：本机图片越界读取时，审批申请交回这段对话 */
  conversationId?: string
}>()

/**
 * 已解析的图片标识 → 地址
 * 空串表示「服务端没给地址」（无权限 / 已删除），与「还没解析」区分开：前者不再重试，后者用占位图
 */
const urls = ref<Record<string, string>>({})

/** 已解析的本机图片路径 → 展示地址（空串表示没拿到，统一回落默认图） */
const localUrls = ref<Record<string, string>>({})

/** 渲染接口：正文里的每个知识库图片标识都要换成一个地址，一次渲染里可能被问多次 */
function imageUrl(id: string): string {
  return id in urls.value ? urls.value[id] || fallbackImage : loadingImage
}

/** 本机图片：解析中先占位，拿不到地址回落默认图 */
function localImageUrl(path: string): string {
  return path in localUrls.value ? localUrls.value[path] || fallbackImage : loadingImage
}

const parts = computed(() =>
  markdownParts(props.content, { knowledgeImage: imageUrl, localImage: localImageUrl })
)

/** 这段正文里还没解析过的图片标识（已经问过的按结论算，不再重复请求） */
function pendingIds(text: string): string[] {
  return knowledgeImageIds(text).filter((id) => !(id in urls.value))
}

/** 按标识批量换取展示地址：拿不到地址的记成空串，由默认图兜底 */
async function resolve(text: string): Promise<void> {
  const ids = pendingIds(text)
  if (!ids.length) return
  const result = await KnowledgeImageUtil.resolve(ids)
  const next = { ...urls.value }
  ids.forEach((id) => {
    next[id] = result[id] || ''
  })
  urls.value = next
}

/** 这段正文里还没解析过的本机图片路径 */
function pendingLocalPaths(text: string): string[] {
  return localImagePaths(text).filter((path) => !(path in localUrls.value))
}

/** 逐张换取本机图片的展示地址：拿不到地址的记成空串，由默认图兜底 */
async function resolveLocal(text: string): Promise<void> {
  const list = pendingLocalPaths(text)
  if (!list.length) return
  const projectId = String(props.projectId ?? '')
  const conversationId = String(props.conversationId ?? '')
  const resolved = await Promise.all(
    list.map((path) => LocalImageUtil.resolve(projectId, path, conversationId))
  )
  // 解析期间切了项目：这一批结果按旧项目的权限算，丢掉，等新的一轮重新解析
  if (projectId !== String(props.projectId ?? '')) return
  const next = { ...localUrls.value }
  list.forEach((path, index) => {
    next[path] = resolved[index] || ''
  })
  localUrls.value = next
}

/**
 * 解析的节流窗口：流式输出逐字变化，这里把同一阵子新出现的图片合成一次请求，
 * 到点了就拿当前正文再取一遍待解析的标识（中途新增的下一轮再说）
 */
const RESOLVE_WINDOW = 240
let timer: ReturnType<typeof setTimeout> | null = null

/** 这段正文里有没有还没解析的图片（知识库或本机） */
function needsResolve(text: string): boolean {
  return pendingIds(text).length > 0 || pendingLocalPaths(text).length > 0
}

/** 排队解析：同一阵子的变化只排一次，到点再按当时的正文取待解析项 */
function queueResolve(text: string): void {
  if (timer || !needsResolve(text)) return
  timer = setTimeout(() => {
    timer = null
    void resolve(props.content)
    void resolveLocal(props.content)
  }, RESOLVE_WINDOW)
}

watch(
  () => [props.content, props.projectId, props.conversationId] as const,
  ([, projectId, conversationId], previous) => {
    // 换了项目或会话：旧上下文签发的地址与解析结论都不再适用，清掉重新来
    if (previous && (previous[1] !== projectId || previous[2] !== conversationId)) {
      urls.value = {}
      localUrls.value = {}
    }
    queueResolve(props.content)
  },
  { immediate: true }
)

onBeforeUnmount(() => {
  if (timer) clearTimeout(timer)
})

/** 大图查看：占位图与默认图没有可看的内容，不参与查看器 */
const rootRef = ref<HTMLDivElement>()
const preview = ref<{ list: string[]; index: number } | null>(null)

function openPreview(event: MouseEvent): void {
  const image = (event.target as Element | null)?.closest?.('img')
  if (!image || !rootRef.value) return
  const images = Array.from(rootRef.value.querySelectorAll('img')).filter(
    (item) => ![loadingImage, fallbackImage].includes(item.getAttribute('src') ?? '')
  )
  const index = images.indexOf(image as HTMLImageElement)
  if (index < 0) return
  preview.value = {
    list: images.map((item) => item.getAttribute('src') ?? ''),
    index
  }
}

/**
 * 点击：正文里的本机路径链接拦下来交给「打开本机文件」，其余仍按大图查看处理。
 * 不拦的话这些链接会被主进程当成外部地址丢给浏览器（core/Markdown 给链接统一补了 target="_blank"）。
 */
function onClick(event: MouseEvent): void {
  const anchor = (event.target as Element | null)?.closest?.('a')
  const path = anchor ? FileActionUtil.localPath(anchor.getAttribute('href') ?? '') : ''
  if (path) {
    event.preventDefault()
    FileActionUtil.open(String(props.projectId ?? ''), path)
    return
  }
  openPreview(event)
}

/**
 * 图片加载失败（签发地址过期、网络异常等）：换成默认图
 * 正文是 v-html 插进来的，这里只改这一张图的 src；下次正文变化重新渲染时就回到解析结果
 */
function fallbackImageOnError(event: Event): void {
  const image = event.target as HTMLImageElement
  if (!image || 'IMG' !== image.tagName || fallbackImage === image.getAttribute('src')) return
  image.src = fallbackImage
}
</script>

<template>
  <div ref="rootRef" class="chat-markdown" @click="onClick" @error.capture="fallbackImageOnError">
    <template v-for="(part, index) in parts" :key="index">
      <ChatCode v-if="'code' === part.type" :code="part.code" :lang="part.lang" />
      <div v-else class="chat-markdown-text" v-html="part.html" />
    </template>
    <el-image-viewer
      v-if="preview"
      :url-list="preview.list"
      :initial-index="preview.index"
      teleported
      @close="preview = null"
    />
  </div>
</template>

<style lang="scss" scoped>
/* 气泡里的 Markdown 排版：字号与正文一致，块级间距收紧 */
.chat-markdown {
  font-size: 13px;
  line-height: 1.7;
  word-break: break-word;
  color: var(--el-text-color-primary);

  /* 正文片段：首尾元素不留外边距，避免与代码块、气泡边缘之间出现多余空隙 */
  .chat-markdown-text {
    :deep(> :first-child) {
      margin-top: 0;
    }

    :deep(> :last-child) {
      margin-bottom: 0;
    }
  }

  :deep(p) {
    margin: 0 0 8px;
  }

  :deep(h1),
  :deep(h2),
  :deep(h3),
  :deep(h4),
  :deep(h5),
  :deep(h6) {
    margin: 12px 0 6px;
    font-weight: 600;
    line-height: 1.4;
  }

  :deep(h1) {
    font-size: 16px;
  }

  :deep(h2) {
    font-size: 15px;
  }

  :deep(h3),
  :deep(h4),
  :deep(h5),
  :deep(h6) {
    font-size: 14px;
  }

  :deep(ul),
  :deep(ol) {
    margin: 0 0 8px;
    padding-left: 20px;
  }

  :deep(ul) {
    list-style: disc;
  }

  :deep(ol) {
    list-style: decimal;
  }

  :deep(li) {
    margin: 2px 0;
  }

  :deep(li > ul),
  :deep(li > ol) {
    margin: 2px 0 0;
  }

  /* 行内代码与代码块：与工具返回块同一套底。
     只认正文片段里的：顶层的代码块走 ChatCode，它自己的 pre 不能吃这里的版式 */
  .chat-markdown-text {
    :deep(code) {
      padding: 1px 4px;
      border-radius: 4px;
      background: var(--el-fill-color);
      font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
      font-size: 12px;
    }

    :deep(pre) {
      margin: 0 0 8px;
      padding: 8px 10px;
      border-radius: 8px;
      background: var(--el-fill-color-light);
      overflow-x: auto;
      overflow-y: auto;
      max-height: 420px;

      /* 代码块是原生滚动容器（内容由 v-html 插入，放不了组件），滚动条按全局口径收细 */
      &::-webkit-scrollbar {
        width: 5px;
        height: 5px;
      }

      &::-webkit-scrollbar-thumb {
        border-radius: 2px;
        background: var(--el-border-color);
      }

      &::-webkit-scrollbar-thumb:hover {
        background: var(--el-border-color-darker);
      }
    }

    :deep(pre code) {
      padding: 0;
      background: transparent;
      font-size: 12px;
      line-height: 1.6;
      white-space: pre;
    }
  }

  :deep(blockquote) {
    margin: 0 0 8px;
    padding: 2px 0 2px 10px;
    border-left: solid 3px var(--el-border-color);
    color: var(--el-text-color-secondary);
  }

  /* 表格：窄窗口里整体横向滚动，不把气泡撑破 */
  :deep(table) {
    display: block;
    max-width: 100%;
    margin: 0 0 8px;
    border-collapse: collapse;
    overflow-x: auto;
  }

  :deep(th),
  :deep(td) {
    padding: 4px 8px;
    border: solid 1px var(--el-border-color-lighter);
    font-size: 12px;
    text-align: left;
  }

  :deep(th) {
    font-weight: 600;
    background: var(--el-fill-color-light);
  }

  :deep(a) {
    color: var(--el-color-primary);
    text-decoration: none;

    &:hover {
      text-decoration: underline;
    }
  }

  :deep(hr) {
    margin: 10px 0;
    border: none;
    border-top: solid 1px var(--el-border-color-lighter);
  }

  :deep(img) {
    max-width: 100%;
    /* 图片自带宽高（占位图是定尺的 SVG）：收窄宽度时高度跟着走，别把图压扁 */
    height: auto;
    border-radius: 6px;
    cursor: zoom-in;
  }
}
</style>
