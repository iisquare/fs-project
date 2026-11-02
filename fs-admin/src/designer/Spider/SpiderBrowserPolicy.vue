<script setup lang="ts">
import { computed } from 'vue'

const model = defineModel<any>()
const props = defineProps<{
  waitModes: Record<string, string>
}>()

const waitDescription = computed(() => {
  switch (model.value?.browserWaitUntil) {
    case 'domcontentloaded':
      return '等待 HTML DOM 解析完成。速度最快，建议同时填写需要等待的元素。'
    case 'load':
      return '等待页面 load 事件完成，适合图片和多资源加载后才能生成内容的页面。'
    case 'networkidle':
    default:
      return '等待网络请求进入空闲状态，适合通过接口异步加载数据的页面。'
  }
})
</script>

<template>
  <div class="request-policy">
    <div class="browser-grid">
      <div class="setting-field">
        <div class="setting-label">页面就绪条件</div>
        <el-select v-model="model.browserWaitUntil" class="full-width">
          <el-option v-for="(value, key) in props.waitModes" :key="key" :value="key" :label="value" />
        </el-select>
        <div class="setting-help">{{ waitDescription }}</div>
      </div>
      <div class="setting-field">
        <div class="setting-label">等待元素</div>
        <el-input v-model="model.browserWaitSelector" placeholder="可选，例如 #app 或 .list-item" clearable />
        <div class="setting-help">填写 CSS 选择器后，元素出现在 DOM 中才会读取页面内容。</div>
      </div>
      <div class="setting-field">
        <div class="setting-label">执行超时</div>
        <el-input-number v-model="model.browserTimeout" :min="1000" :step="1000" class="full-width" />
        <div class="setting-help">页面加载和元素等待共用此超时时间，单位毫秒。</div>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.request-policy {
  padding: 14px 16px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  background: var(--el-fill-color-lighter);
}

.setting-help {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.6;
}

.browser-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.setting-field {
  min-width: 0;
}

.setting-label {
  margin-bottom: 6px;
  color: var(--el-text-color-regular);
  font-size: 13px;
  font-weight: 500;
}

.setting-help {
  margin-top: 5px;
}

.full-width {
  width: 100%;
}

@media (max-width: 900px) {
  .browser-grid {
    grid-template-columns: 1fr;
  }
}
</style>
