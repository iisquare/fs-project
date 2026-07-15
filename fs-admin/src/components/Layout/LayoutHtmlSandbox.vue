<script setup lang="ts">
/**
 * HTML 沙箱渲染器 - 在 iframe 中安全地渲染 HTML 内容，默认禁用脚本和同源访问。
 *
 * @prop {String}   html            - HTML 内容字符串（必填）
 * @prop {Boolean}  allowScripts    - 是否允许脚本执行，默认 false（为 false 时 sandbox 含 allow-scripts）
 * @prop {Boolean}  allowSameOrigin - 是否允许同源访问，默认 false
 *
 * @example
 * <layout-html-sandbox :html="'<h1>Hello</h1>'" />
 * <layout-html-sandbox :html="htmlCode" :allow-scripts="true" />
 */
import { computed } from 'vue';

const {
  html,
  allowScripts = false,
  allowSameOrigin = false,
} = defineProps({
  html: { type: String, required: true },
  allowScripts: { type: Boolean, required: false },
  allowSameOrigin: { type: Boolean, required: false },
})

const sandbox = computed(() => {
  let value = ''
  if (!allowScripts) value += ' allow-scripts'
  if (!allowSameOrigin) value += ' allow-same-origin'
  return value.trim()
})
</script>

<template>
  <iframe :srcdoc="html" class="layout-html-sandbox" :sandbox="sandbox"></iframe>
</template>

<style lang="scss" scoped>
.layout-html-sandbox {
  width: 100%;
  height: 100%;
  border: none;
}
</style>
