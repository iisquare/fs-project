<script setup lang="ts">
import { computed, onMounted, provide, ref, watch } from 'vue'
import FlexFormDraggable from './FlexFormDraggable.vue';

const model: any = defineModel()
const tips: any = defineModel('tips', { type: null })
const emit = defineEmits(['update:activeItem'])
const { // 子组件中，无法整个替换defineModel的value值
  config = {} as any,
  activeItem = {} as any,
} = defineProps<{
  config: { type: null, required: false },
  activeItem: { type: null, required: true },
}>()

const width = ref('770px')
const handleClick = () => {
  emit('update:activeItem', {})
  tips.value.text = '选中表单'
}

const formLayout = computed(() => {
  return config.exhibition.formLayout(model.value.content)
})

const removeItem = (item: any) => {
  for (const index in model.value.content.widgets) {
    const widget = model.value.content.widgets[index]
    if (item.id === widget.id) {
      model.value.content.widgets.splice(index, 1)
      tips.value.text = `移除组件 - ${item.id}`
      break
    }
  }
  handleClick()
  return this
}

onMounted(() => {
})

provide('removeItem', removeItem)
defineExpose({ width, removeItem })
</script>

<template>
  <div class="viewport" @click="handleClick">
    <el-form v-bind="formLayout">
      <FlexFormDraggable v-model="model" :active-item="activeItem" :config="config" :tips="tips" @update:active-item="v => emit('update:activeItem', v)" />
      <el-empty description="从左侧拖入组件进行表单设计" class="empty" v-if="model.content.widgets.length === 0" />
    </el-form>
  </div>
</template>

<style lang="scss" scoped>
.viewport {
  width: v-bind(width);
  height: 100%;
  overflow-x: auto;
  background: white;
  margin: 0 auto;
  .el-form {
    width: 100%;
    height: 100%;
  }
}
.empty {
  position: absolute;
  top: calc(50% - 150px);
  left: calc(50% - 125px);
  width: 250px;
}
</style>
