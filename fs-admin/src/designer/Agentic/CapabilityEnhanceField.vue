<script setup lang="ts">
/**
 * 能力增强 - 在系统提示词下方快速注入平台支持的提示词片段。
 * 注入的是可继续编辑的普通文本，不新增节点或隐藏运行时配置。
 */
import { ArrowDown } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { PROMPT_INJECTIONS, appendPromptInjection } from './promptInjection'

const model = defineModel<string>({ default: '' })

const handleInject = (id: string) => {
  const injection = PROMPT_INJECTIONS.find((item) => item.id === id)
  if (!injection) return
  const next = appendPromptInjection(model.value, injection)
  if (next === model.value) {
    ElMessage.info(`系统提示词中已包含「${injection.label}」`)
    return
  }
  model.value = next
  ElMessage.success(`已注入「${injection.label}」`)
}
</script>

<template>
  <div class="capability-enhance">
    <div class="capability-enhance__copy">
      <span class="capability-enhance__title">能力增强</span>
      <span class="capability-enhance__desc">快速补充日期、图表等能力说明</span>
    </div>
    <el-dropdown trigger="click" @command="handleInject">
      <el-button class="capability-enhance__action" size="small" type="primary" plain>
        注入提示词
        <el-icon><ArrowDown /></el-icon>
      </el-button>
      <template #dropdown>
        <el-dropdown-menu>
          <el-dropdown-item
            :key="item.id"
            :command="item.id"
            v-for="item in PROMPT_INJECTIONS">
            {{ item.label }}
          </el-dropdown-item>
        </el-dropdown-menu>
      </template>
    </el-dropdown>
  </div>
</template>

<style lang="scss" scoped>
.capability-enhance {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 10px;
  padding: 10px 12px;
  border: 1px solid var(--el-color-primary-light-8);
  border-radius: 8px;
  background: var(--el-color-primary-light-9);
}
.capability-enhance__copy {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 2px;
}
.capability-enhance__title {
  font-size: 12.5px;
  font-weight: 650;
  color: var(--el-text-color-primary);
}
.capability-enhance__desc {
  font-size: 11px;
  line-height: 1.45;
  color: var(--el-text-color-secondary);
}
.capability-enhance__action {
  flex: none;
  border-radius: 999px;
}
</style>
