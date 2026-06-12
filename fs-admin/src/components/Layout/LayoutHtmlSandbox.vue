<script setup lang="ts">
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
