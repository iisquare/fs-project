<script setup lang="ts">
/**
 * 变量操作按钮 - 「插入变量」弹出面板与「复制」按钮，供变量编辑字段（标题行、卡片内）复用。
 *
 * @prop {*} instance   - 画布实例（X6Container 暴露的 flow）
 * @prop {*} activeItem - 当前激活的节点，用于排除自身
 * @prop {Function} copy - 复制回调，返回 Promise<Boolean>；未传时不展示复制按钮，成功后按钮短暂显示对号
 * @emits insert - 选中变量后抛出变量引用（`节点ID.变量名` 或 `sys.变量名`）
 */
import { computed, ref } from 'vue'
import { CollectionTag } from '@element-plus/icons-vue'
import ButtonCopy from '@/components/Button/ButtonCopy.vue'
import LayoutIcon from '@/components/Layout/LayoutIcon.vue'
import DataUtil from '@/utils/DataUtil'
import icons from '@/assets/icons'
import VariablePicker from './VariablePicker.vue'

const { instance, activeItem = {}, copy } = defineProps<{
  instance?: any,
  activeItem?: any,
  copy?: Function,
}>()
const emit = defineEmits(['insert'])

const pickerRef = ref()
const insertVisible = ref(false)

// 优先使用项目自定义的变量图标（assets/icons/form/variable.svg），文件为空时回落到 Element Plus 图标
const customIcon = computed(() => String(DataUtil.value(icons, 'form.variable') ?? '').indexOf('<svg') >= 0)

const handleInsert = (value: string) => {
  insertVisible.value = false
  emit('insert', value)
}
</script>

<template>
  <el-space :size="4">
    <el-popover
      v-model:visible="insertVisible"
      :width="280"
      trigger="click"
      placement="bottom-end"
      @show="pickerRef?.focus()">
      <template #reference>
        <!-- mousedown 阻止默认行为，避免点击按钮时编辑器失焦丢失光标；点击仍冒泡给弹出框触发 -->
        <span class="insert-trigger" @mousedown.prevent>
          <!-- 提示用原生 title，不额外挂 tooltip -->
          <el-button link size="small" title="插入变量">
            <LayoutIcon v-if="customIcon" name="form.variable" />
            <el-icon v-else><CollectionTag /></el-icon>
          </el-button>
        </span>
      </template>
      <VariablePicker
        ref="pickerRef"
        :instance="instance"
        :active-item="activeItem"
        @select="handleInsert" />
      <div class="insert-tips">
        占位符展示为「节点名称.变量中文名称」，实际值为
        <em v-pre>{{#节点标识.变量英文名称#}}</em>
        ；也可直接输入
        <em>/</em>
        唤起变量提示
      </div>
    </el-popover>
    <!-- 复制走统一组件：未传回调时不展示（如 HTTP 地址只保留插入变量）；成功后 3.5 秒内显示对号 -->
    <ButtonCopy v-if="'function' === typeof copy" :copy="copy" />
  </el-space>
</template>

<style lang="scss" scoped>
.insert-trigger {
  display: inline-flex;
}
.insert-tips {
  margin-top: 6px;
  font-size: 12px;
  line-height: 1.6;
  color: var(--el-text-color-placeholder);
  em {
    font-style: normal;
    color: var(--el-color-primary);
  }
}
</style>
