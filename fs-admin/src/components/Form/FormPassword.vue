<script setup lang="ts">
/**
 * 密码展示组件 - 显示脱敏密码，支持切换显示/隐藏、复制到剪贴板。
 *
 * @v-model  {String}   密码原文（双向绑定主值）
 * @prop     {String}   level - 脱敏级别：'high'=保留首尾2位，'medium'=保留首尾4位，默认=全星号
 *
 * @example
 * <form-password v-model="password" />
 * <form-password v-model="password" level="high" />
 */
import { computed, ref } from 'vue';
import * as ElementPlusIcons from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import FormUtil from '@/utils/FormUtil';

const model: any = defineModel()
const {
  level = '',
} = defineProps({
  level: { type: String, required: false },
})

const hide = ref(true)
const copied = ref(false)
const mask = (text: string, keep: number) => {
  if (!text || text.length <= keep * 2) return '*'.repeat(text?.length || 6)
  return text.slice(0, keep) + '*'.repeat(text.length - keep * 2) + text.slice(-keep)
}
const password = computed(() => {
  if (!hide.value) return model.value
  const val = model.value || ''
  switch (level) {
    case 'high': return mask(val, 2)
    case 'medium': return mask(val, 4)
    default: return '*'.repeat(val.length || 6)
  }
})
const handleCopy = () => {
  FormUtil.copyToClipboard(model.value).then(() => {
    ElMessage.success('已复制到剪贴板')
    copied.value = true
    window.setTimeout(() => {
      copied.value = false
    }, 3000)
  }).catch(() => {
    ElMessage.error('复制失败')
  });
}
</script>

<template>
  <div class="password">
    <div v-text="password"></div>
    <el-space>
      <el-icon @click="hide = !hide"><ElementPlusIcons.Hide v-if="hide" /><ElementPlusIcons.View v-else /></el-icon>
      <el-icon @click="handleCopy" title="复制"><ElementPlusIcons.Check v-if="copied" /><ElementPlusIcons.CopyDocument v-else /></el-icon>
    </el-space>
  </div>
</template>

<style lang="scss" scoped>
.password {
  width: 100%;
  @include flex-between();
  .el-icon {
    cursor: pointer;
  }
}
</style>
