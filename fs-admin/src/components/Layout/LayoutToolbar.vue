<script setup lang="ts">
import { ref } from 'vue';
import LayoutIcon from './LayoutIcon.vue';
import DataUtil from '@/utils/DataUtil';

const {
  toolbars,
  instance,
  multiSelect = false,
} = defineProps({
  toolbars: { type: Array<any> }, // [{ type: '类型', label: '标签', icon: '图标', selectable: true, selected: true, callback (toolbar: any, flow: any, event: any) {} }]
  instance: { type: null, required: false },
  multiSelect: { type: Boolean, required: false }, // 是否允许多选
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
  toolbar.callback && toolbar.callback(toolbar, instance.flow, event)
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
</style>
