<script setup lang="ts">
import DesignUtil from '@/utils/DesignUtil'
import MenuUtil from '@/utils/MenuUtil'
import { computed, inject, onMounted } from 'vue'
import draggable from 'vuedraggable'

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

const widgetMap: any = computed(() => DesignUtil.widgetMap(config.widgets))

const generateItem = (widget: any) => {
  return { id: config.uuidWidget(), type: widget.type, label: widget.label, icon: widget.icon, options: widget.options() }
}
const removeItem: any = inject('removeItem')

const handleDragAdd = (event: any) => {
  if (!event.clone.dataset.id) return true
  const item = generateItem(widgetMap.value[event.clone.dataset.id])
  model.value.content.widgets.splice(event.newIndex, 1, item)
  handleClick(item)
}

const handleClick = (element: any) => {
  emit('update:activeItem', element)
  tips.value.text = `选中组件 - ${element.id}`
}

const handleContextMenu = (event: any, element: any) => {
  MenuUtil.context(event, [
    { title: `${element.type} - ${element.label}`, disabled: true },
    { type: 'divider' },
    { key: 'delete', icon: 'Delete', title: '移除组件' },
    { type: 'divider' },
    { title: element.id, disabled: true }
  ], (menu: any) => {
    switch (menu.key) {
      case 'delete':
        return removeItem(element)
      default:
        return false
    }
  })
}

onMounted(() => {
})

</script>

<template>
  <draggable
    :list="model.content.widgets"
    item-key="id"
    group="widgets"
    animation="340"
    class="container"
    @add="handleDragAdd">
    <template #item="{ element }">
      <div class="form-item" @click.stop="handleClick(element)" @contextmenu="ev => handleContextMenu(ev, element)">
        <el-form-item :label="element.label" v-if="element.type === 'text'"><el-input :placeholder="element.options.placeholder" /></el-form-item>
        <el-form-item :label="element.label" v-else>{{ `异常组件 ${element.type} - ${element.id}` }}</el-form-item>
      </div>
    </template>
  </draggable>
</template>

<style lang="scss" scoped>
.container {
  width: 100%;
  height: 100%;
}
.sortable-ghost {
  border-top: solid 3px #5959df;
}
:deep(li.sortable-ghost) {
  list-style: none;
  width: 100%;
  height: 60px;
  background-color: #f6f7ff;
  border-top: solid 3px #5959df;
  .el-icon {
    margin: 0px 6px;
    font-size: 14px;
  }
}
.form-item {
  width: 100%;
  cursor: move;
  border-radius: 3px;
  > .el-form-item {
    padding: 12px 10px;
    margin: 0px;
  }
}
</style>
