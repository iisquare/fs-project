<script setup lang="ts">
/**
 * 工具栏组件 - 渲染一组图标按钮和分隔线，支持单选/多选高亮，点击执行对应回调。
 *
 * @v-model  {ToolbarItem[]} 选中状态（双向绑定主值），组件会自动管理 selected 状态
 * @prop     {ToolbarItem[]}  toolbars    - 工具栏配置数组
 * @prop     {*}              instance    - 画布实例，在回调中传递
 * @prop     {Boolean}        multiSelect  - 是否允许多选，默认 false
 *
 * 工具栏项结构 (ToolbarItem):
 *   { type: 'divider'|'button', label: string, icon: string, selectable?: boolean, selected?: boolean, callback?: (toolbar, instance, event) => void }
 *
 * @example
 * <layout-toolbar v-model="activeTools" :toolbars="[
 *   { type: 'button', label: '指针', icon: 'Cursor', selectable: true, selected: true, callback: onSelect },
 *   { type: 'divider' },
 *   { type: 'button', label: '矩形', icon: 'Rectangle', selectable: true, callback: onSelect },
 * ]" />
 */
import { ref } from 'vue';
import LayoutIcon from './LayoutIcon.vue';
import DataUtil from '@/utils/DataUtil';

const {
  toolbars,
  instance,
  multiSelect = false,
} = defineProps({
  toolbars: { type: Array<any> },
  instance: { type: null, required: false },
  multiSelect: { type: Boolean, required: false },
})

// v-model值必须通过ref()声明，否则无法监听数据变更
const model = defineModel<Array<any>>({ required: false })
if (DataUtil.empty(model.value)) model.value = ref(toolbars).value

const handleClick = (event: any, toolbar: any) => {
  if (toolbar.selectable) {
    if (model.value && !multiSelect) {
      for (const toolbar of model.value) {
        toolbar.selected = false
      }
    }
    toolbar.selected = true
  }
  toolbar.callback && toolbar.callback(toolbar, instance, event)
}
</script>

<template>
  <el-space>
    <template :key="index" v-for="(toolbar, index) in model">
      <el-divider direction="vertical" v-if="toolbar.type === 'divider'" :title="toolbar.label" />
      <LayoutIcon
        :name="toolbar.icon"
         :title="toolbar.label"
        :class="[toolbar.selected && 'selected']"
        @click="(event: any) => handleClick(event, toolbar)"
        v-else />
    </template>
  </el-space>
</template>

<style lang="scss" scoped>
.el-icon {
  cursor: pointer;
}
.selected {
  color: #409eff;
}
.el-divider {
  margin: 0 auto;
}
</style>
