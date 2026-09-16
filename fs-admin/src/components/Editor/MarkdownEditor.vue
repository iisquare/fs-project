<script setup lang="ts">
/**
 * Markdown 编辑器 - 基于 Vditor 的所见即所得 Markdown 编辑/预览组件（通用组件）
 *
 * 图片能力通过可选属性注入，组件本身不依赖任何业务接口：
 * - imageScheme   正文中图片标识的协议名，默认 kb，形如 ![说明](kb:标识)
 * - resolveImages 批量解析图片标识为可展示地址，缺省时不做处理
 * - uploadImage   上传图片并返回 { id, url, alt }，缺省时禁用图片上传
 * - loadingImage  地址解析完成前的占位图片，需为可访问的静态资源
 * - fallbackImage 解析不到地址时的兜底图片
 *
 * 渲染态不直接使用标识作为 src：标识会先换成 loadingImage#协议:标识 这样的占位地址，
 * 避免浏览器按未知协议加载失败，保存时再原样还原，正文里始终只有标识
 *
 * @v-model  {String}   内容（双向绑定主值）
 * @prop     {Boolean}  readonly     - 是否只读（仅预览），默认 false
 * @prop     {Number}   height       - 编辑器高度(px)，默认 400
 * @prop     {String}   placeholder  - 占位提示文本
 * @prop     {Boolean}  resizable    - 是否允许拖拽调整高度，默认 false
 *
 * @example
 * <markdown-editor v-model="content" :resolve-images="resolve" :upload-image="upload" />
 */
import Vditor from 'vditor'
import 'vditor/dist/index.css'
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'

const model: any = defineModel()
const {
  readonly = false,
  height = 400,
  placeholder = '',
  resizable = false,
  imageScheme = 'kb',
  loadingImage = '/images/loading.svg',
  fallbackImage = '/images/no-permit.png',
  resolveImages,
  uploadImage,
} = defineProps({
  readonly: { type: Boolean, required: false },
  height: { type: Number, required: false },
  placeholder: { type: String, required: false },
  resizable: { type: Boolean, required: false },
  imageScheme: { type: String, required: false },
  loadingImage: { type: String, required: false },
  fallbackImage: { type: String, required: false },
  resolveImages: { type: Function, required: false },
  uploadImage: { type: Function, required: false },
})

const editorRef = ref<HTMLDivElement>()
const previewRef = ref<HTMLDivElement>()
let vditor: Vditor | null = null
let observer: MutationObserver | null = null
let refreshScheduled = false
const imageUrls: Record<string, string> = {}

const schemeName = () => (imageScheme || 'kb').replace(/[^A-Za-z0-9_-]/g, '')
const escapeRe = (text: string) => (text || '').replace(/[.*+?^${}()|[\]\\]/g, '\\$&')

/**
 * 图片引用匹配，每次返回新实例，避免正则的 lastIndex 相互影响
 */
const imagePattern = () => new RegExp(`\\]\\([^)]*${schemeName()}:([A-Za-z0-9_-]+)\\)`, 'g')
const placeholderPrefix = () => `${loadingImage}#${schemeName()}:`
const placeholderPattern = () => new RegExp(`\\]\\(${escapeRe(loadingImage)}#${schemeName()}:([A-Za-z0-9_-]+)\\)`, 'g')

/**
 * 进入编辑器：图片标识替换为可解析的占位地址
 */
const normalizeIn = (content: string) => {
  if (!content || !loadingImage) return content ?? ''
  return (content ?? '').replace(imagePattern(), (all: string, id: string) => {
    return `](${loadingImage}#${schemeName()}:${id})`
  })
}

/**
 * 出编辑器：占位地址还原为图片标识
 * 取值出口必须统一走这里，否则占位地址会被写回正文
 */
const normalizeOut = (content: string) => {
  if (!content) return content ?? ''
  let result = content
  // 兜底：图片弹层里若被改成解析后的真实地址，还原为占位引用，避免绝对地址写入正文
  for (const [id, url] of Object.entries(imageUrls)) {
    if (!url) continue
    result = result.split(`](${url})`).join(`](${placeholderPrefix()}${id})`)
  }
  return result
}

const getContent = () => {
  return vditor ? normalizeOut(vditor.getValue()) : (model.value ?? '')
}

const setContent = (content: string) => {
  vditor?.setValue(normalizeIn(content ?? ''))
}

/**
 * 收集正文中尚未取得展示地址的图片标识
 */
const unresolvedIds = (content: string) => {
  const ids: string[] = []
  for (const matched of (content ?? '').matchAll(imagePattern())) {
    if (!imageUrls[matched[1]]) ids.push(matched[1])
  }
  return Array.from(new Set(ids))
}

/**
 * 批量解析图片展示地址，未返回的图片回落到兜底图
 */
const resolve = async (content: string) => {
  const ids = unresolvedIds(content)
  if (typeof resolveImages !== 'function' || ids.length === 0) return
  try {
    const data = await (resolveImages as any)(ids) || {}
    ids.forEach((id: string) => { imageUrls[id] = data[id] || fallbackImage })
  } catch {
    ids.forEach((id: string) => { imageUrls[id] = fallbackImage })
  }
}

/**
 * 为编辑区中的占位图片补充展示地址
 * 只动附加类名与内联自定义属性，不修改 src；这些内容在保存时会被 Markdown 转换丢弃，标识不受影响
 */
const decorate = () => {
  const element = editorRef.value
  if (!element) return
  const prefix = placeholderPrefix()
  element.querySelectorAll<HTMLImageElement>('img[src*="#"]').forEach(item => {
    const src = item.getAttribute('src') || ''
    if (!src.startsWith(prefix)) return
    const url = imageUrls[src.slice(prefix.length)] || fallbackImage
    item.classList.add('md-image')
    item.style.setProperty('--md-image', `url("${url}")`)
  })
}

/**
 * 只读预览：渲染前把图片标识替换为实时地址，预览结果不回流正文，可安全改写
 */
const loadPreview = (content: string) => {
  if (!previewRef.value) return
  if (!content) {
    previewRef.value.innerHTML = '<span style="color: var(--el-text-color-placeholder)">暂无内容</span>'
    return
  }
  // 兼容内容里残留占位地址的情况，先统一还原为标识再解析
  const html = normalizeOut(content).replace(imagePattern(), (all: string, id: string) => {
    return `](${imageUrls[id] || fallbackImage})`
  })
  Vditor.preview(previewRef.value, html, {
    mode: 'light',
    hljs: { lineNumber: true },
  })
}


const refresh = async (content?: string) => {
  const text = content ?? getContent()
  await resolve(text)
  if (readonly) {
    loadPreview(text)
  } else {
    nextTick(() => decorate())
  }
}

/**
 * 编辑区 DOM 变化时重新装饰图片
 * 切换编辑模式、撤销重做等操作会重建 DOM，此时需要重新套用已解析的地址
 */
const scheduleRefresh = () => {
  if (refreshScheduled) return
  refreshScheduled = true
  requestAnimationFrame(() => {
    refreshScheduled = false
    refresh()
  })
}


const previewVisible = ref(false)
const previewUrlList = ref<string[]>([])
const previewIndex = ref(0)

/**
 * 自定义图片预览
 * Vditor 自带预览层用的是 img 的 src 属性（编辑器里是占位地址），且点击即关闭；
 * 这里改用解析后的真实地址，并交给 Element Plus 的查看器承载层级与关闭行为
 */
const openPreview = (element: HTMLElement) => {
  const image = element as HTMLImageElement
  const source = image.getAttribute('src') || ''
  const prefix = placeholderPrefix()
  const id = source.startsWith(prefix) ? source.slice(prefix.length) : ''
  const current = id ? imageUrls[id] : imageUrls[source]
  if (!current) return
  const urls = Object.values(imageUrls).filter(Boolean) as string[]
  previewUrlList.value = urls.length > 0 ? urls : [current]
  previewIndex.value = Math.max(0, previewUrlList.value.indexOf(current))
  previewVisible.value = true
}

const observeEditor = () => {
  const element = editorRef.value
  if (!element || observer) return
  observer = new MutationObserver(() => scheduleRefresh())
  observer.observe(element, { childList: true, subtree: true, attributes: true, attributeFilter: ['src'] })
}

const loadEditor = () => {
  if (!editorRef.value) return
  vditor = new Vditor(editorRef.value, {
    height,
    mode: 'wysiwyg',
    lang: 'zh_CN',
    value: normalizeIn(model.value ?? ''),
    placeholder: placeholder,
    cache: { enable: false },
    image: { preview: openPreview } as any,
    // Vditor 未对 customWysiwygToolbar 作空值保护，未提供时点击图片等元素会抛异常
    customWysiwygToolbar: (type: string, element: HTMLElement) => {
      // 图片标识由正文维护，不允许在弹层里改写 src
      if (type !== 'image' || !element) return
      const input = element.querySelector('input')
      if (input) input.setAttribute('readonly', 'readonly')
    },
    toolbar: [
      'headings', 'bold', 'italic', 'strike', '|',
      'list', 'ordered-list', 'check', '|',
      'quote', 'code', 'inline-code', 'table', 'line', '|',
      'link', 'upload', '|',
      'undo', 'redo', '|',
      'edit-mode', 'outline', 'fullscreen',
    ],
    counter: {
      enable: true,
    },
    resize: {
      enable: resizable,
      position: 'bottom',
    },
    upload: {
      accept: 'image/*',
      multiple: false,
      handler: async (files: File[]) => {
        const file = files && files.length > 0 ? files[0] : null
        if (!file || !vditor) return null
        if (typeof uploadImage !== 'function') {
          vditor.tip('当前场景不支持上传图片')
          return null
        }
        const data: any = await (uploadImage as any)(file)
        if (!data || !data.id) return null
        imageUrls[data.id] = data.url || fallbackImage
        vditor.insertMD(`![${data.alt || file.name}](${placeholderPrefix()}${data.id})`)
        await refresh()
        return null
      },
    },
    input(value: string) {
      const content = normalizeOut(value)
      model.value = content
      refresh(content)
    },
    after() {
      if (model.value) {
        vditor!.setValue(normalizeIn(model.value))
      }
      refresh()
    },
  })
}

watch(() => model.value, () => {
  if (readonly) {
    nextTick(() => refresh())
  } else if (vditor && model.value !== normalizeOut(vditor.getValue())) {
    vditor.setValue(normalizeIn(model.value ?? ''))
  }
})

onMounted(() => {
  nextTick(() => {
    if (readonly) {
      refresh()
    } else {
      loadEditor()
      observeEditor()
    }
  })
})

onBeforeUnmount(() => {
  if (observer) {
    observer.disconnect()
    observer = null
  }
  if (vditor) {
    model.value = normalizeOut(vditor.getValue())
    vditor.destroy()
    vditor = null
  }
})

defineExpose({ getContent, setContent })
</script>

<template>
  <div class="fs-markdown-editor">
    <div ref="previewRef" class="fs-markdown-preview vditor-reset" v-if="readonly"></div>
    <div ref="editorRef" v-else></div>
    <el-image-viewer
      v-if="previewVisible"
      :url-list="previewUrlList"
      :initial-index="previewIndex"
      :teleported="true"
      @close="previewVisible = false"
    />
  </div>
</template>

<style lang="scss" scoped>
.fs-markdown-editor {
  width: 100%;
  line-height: normal;
}
.fs-markdown-preview {
  padding: 8px 0;
  word-break: break-word;
  :deep(p:last-child) {
    margin-bottom: 0;
  }
}
// 编辑区图片：src 是占位地址（保存时会还原为标识），解析完成后通过 content 换成真实地址
:deep(img.md-image) {
  content: var(--md-image);
  max-width: 100%;
}
</style>

<style lang="scss">
// Vditor 的图片预览层插入在 body 上，需要压过 Element Plus 抽屉的层级，否则被抽屉遮挡
.vditor-img {
  z-index: 4000;
}
</style>