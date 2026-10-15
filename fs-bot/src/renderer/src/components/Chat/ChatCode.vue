<script setup lang="ts">
/**
 * 回复里的代码块：左上角标语言、右上角一键复制，正文带行号与语法着色，过长时块内滚动。
 *
 * 版式是「行号列 + 代码列」两块：行号整块一个元素，宽度按最长的行号撑开，右边框因此是一条
 * 直线、数字靠右；代码整块交给 highlight.js 着色，跨行的 token（块注释、模板字符串）不会被
 * 行号切断。行号列 sticky 在左侧，代码横向滚动时不跟着跑。
 */
import { computed, ref } from 'vue'
import { highlightCode } from '@renderer/core/Highlight'

const props = defineProps<{
  code: string
  lang?: string
}>()

const copied = ref(false)

const language = computed(() => String(props.lang ?? '').trim())
/** 正文去尾换行：行号与代码按同一份文本对齐，末尾那个换行不算一行 */
const body = computed(() => String(props.code ?? '').replace(/\n$/, ''))
/** 行号列：一行一个数字，行高与代码一致即逐行对齐 */
const numbers = computed(() =>
  Array.from({ length: body.value.split('\n').length }, (_, index) => index + 1).join('\n')
)
const highlighted = computed(() => highlightCode(body.value, language.value))

async function copy(): Promise<void> {
  try {
    await navigator.clipboard.writeText(String(props.code ?? ''))
    copied.value = true
    setTimeout(() => (copied.value = false), 1500)
  } catch {
    copied.value = false
  }
}
</script>

<template>
  <div class="chat-code">
    <div class="code-head">
      <span class="code-lang">{{ language || '代码' }}</span>
      <button
        type="button"
        class="code-copy"
        :title="copied ? '已复制' : '复制代码'"
        @click="copy"
      >
        <svg v-if="copied" viewBox="0 0 16 16" width="12" height="12" aria-hidden="true">
          <path
            d="M3.6 8.4 6.4 11.2 12.4 5"
            fill="none"
            stroke="currentColor"
            stroke-width="1.5"
            stroke-linecap="round"
            stroke-linejoin="round" />
        </svg>
        <svg v-else viewBox="0 0 16 16" width="12" height="12" aria-hidden="true">
          <rect
            x="5.9"
            y="5.9"
            width="7.6"
            height="7.6"
            rx="1.7"
            fill="none"
            stroke="currentColor"
            stroke-width="1.2" />
          <path
            d="M10.4 3.9H4.8a1.7 1.7 0 0 0-1.7 1.7v5.6"
            fill="none"
            stroke="currentColor"
            stroke-width="1.2"
            stroke-linecap="round" />
        </svg>
        <span class="code-copy-text">{{ copied ? '已复制' : '复制' }}</span>
      </button>
    </div>
    <el-scrollbar class="code-body" max-height="420px">
      <div class="code-lines">
        <pre class="code-gutter" aria-hidden="true">{{ numbers }}</pre>
        <pre class="code-text"><code class="hljs" v-html="highlighted"></code></pre>
      </div>
    </el-scrollbar>
  </div>
</template>

<style lang="scss" scoped>
.chat-code {
  margin: 0 0 8px;
  border: solid 1px var(--el-border-color-lighter);
  border-radius: 8px;
  overflow: hidden;
  background: var(--el-fill-color-light);
}

/* 顶部条：语言在左、复制在右 */
.code-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  height: 28px;
  padding: 0 8px 0 10px;
  border-bottom: solid 1px var(--el-border-color-lighter);
  background: var(--el-fill-color);

  .code-lang {
    min-width: 0;
    overflow: hidden;
    white-space: nowrap;
    text-overflow: ellipsis;
    font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
    font-size: 11px;
    letter-spacing: 0.02em;
    color: var(--el-text-color-secondary);
  }

  .code-copy {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    flex: none;
    height: 20px;
    padding: 0 6px;
    font-size: 12px;
    line-height: 20px;
    color: var(--el-text-color-secondary);
    background: transparent;
    border: none;
    border-radius: 6px;
    cursor: pointer;
    transition: color 0.2s, background-color 0.2s;

    &:hover {
      color: var(--el-color-primary);
      background: var(--el-color-primary-light-9);
    }

    svg {
      display: block;
      flex: none;
    }
  }
}

.code-body {
  background: var(--el-fill-color-light);
}

/* 行号列 + 代码列并排：行高一致即逐行对齐；整块按内容撑宽，超出时交给外层滚动 */
.code-lines {
  display: flex;
  align-items: flex-start;
  min-width: 100%;
  width: max-content;
  /* 正文区不留内边距：首行紧贴顶部条、末行贴到块底 */
  padding: 0;
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 12px;
  line-height: 20px;
}

/* 两个 pre 都按同一套字体与行高走，别让浏览器给 pre 的默认等宽字体影响行高 */
.code-gutter,
.code-text {
  flex: none;
  font-family: inherit;
  font-size: inherit;
  line-height: inherit;
}

/* 行号列：整块一个元素，宽度由最长的行号撑开 → 逐行同宽，右边框是一条直线 */
.code-gutter {
  position: sticky;
  left: 0;
  /* 压住代码列：横向滚动时代码从行号下面穿过去，不能盖住数字 */
  z-index: 1;
  padding: 0 10px;
  text-align: right;
  white-space: pre;
  background: var(--el-fill-color-light);
  /* 行号与代码之间一条浅分隔：贴在行号列上，横向滚动时跟着走 */
  border-right: solid 1px var(--el-border-color-lighter);
  color: var(--el-text-color-placeholder);
  user-select: none;
}

/* 代码列：整块高亮，右侧留出滚动余量 */
.code-text {
  padding: 0 12px 0 10px;
  white-space: pre;

  /* github 主题只管 token 配色：底色与内边距让给代码块自己的面板 */
  :deep(code.hljs) {
    display: block;
    overflow: visible;
    padding: 0;
    background: transparent;
    font-family: inherit;
    font-size: inherit;
    line-height: inherit;
  }
}
</style>
