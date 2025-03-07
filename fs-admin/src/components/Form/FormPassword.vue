<script setup lang="ts">
import { computed, ref } from 'vue';
import * as ElementPlusIcons from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';

const model: any = defineModel()

const hide = ref(true)
const copied = ref(false)
const password = computed(() => {
  return hide.value ? '******' : model.value
})
const handleCopy = () => {
  navigator.clipboard.writeText(model.value).then(() => {
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
