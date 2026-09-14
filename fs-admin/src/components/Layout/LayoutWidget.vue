<script setup lang="ts">
/**
 * 组件库面板 - 展示可拖拽的组件分组列表，支持原生拖拽和 vuedraggable 两种方式。
 *
 * @prop {WidgetGroup[]} widgets - 组件分组配置数组
 * @prop {Boolean}       native  - 是否使用原生 HTML5 拖拽，默认 true
 *
 * @emits {Function} dragStart - 拖拽开始时触发，参数：(event, widget)
 *
 * 组件分组结构 (WidgetGroup):
 *   { id: any, name: string, children: WidgetItem[] }
 *
 * 组件项结构 (WidgetItem):
 *   { id: any, type: string, label: string, icon: string, title?: string }
 *
 * @example
 * <layout-widget :widgets="[
 *   { id: 1, name: '基础组件', children: [
 *     { id: 'btn', type: 'Button', label: '按钮', icon: 'Button' },
 *     { id: 'input', type: 'Input', label: '输入框', icon: 'Edit' },
 *   ]},
 * ]" @drag-start="handleDragStart" />
 */
import { computed, onMounted, ref } from 'vue';
import LayoutIcon from './LayoutIcon.vue';
import draggable from 'vuedraggable'
import DesignUtil from '@/utils/DesignUtil';

const {
  widgets = [],
  native = true,
} = defineProps({
  widgets: { type: Array<any>, required: false },
  native: { type: Boolean, required: false },
})

const widgetMap: any = computed(() => DesignUtil.widgetMap(widgets))

const emit = defineEmits<{
  dragStart: [event: any, widget: any]
}>()

const handleDragStart = (event: any) => {
  emit('dragStart', event, widgetMap.value[event.clone.dataset.id])
}

const handleNativeDragStart = (event: any, widget: any) => {
  emit('dragStart', event, widget)
}

const active: any = ref([])
onMounted(() => {
  active.value = widgets.map((group: any) => group.id)
})
</script>

<template>
  <el-collapse v-model="active" expand-icon-position="left" class="widget">
    <el-collapse-item :title="group.name" :name="group.id" v-for="group in widgets">
      <ul v-if="native">
        <li
          draggable="true"
          class="widget-item"
          :data-id="item.type"
          :title="item.title"
          :key="item.id"
          v-for="item in group.children"
          @dragstart="(event: any) => handleNativeDragStart(event, item)">
          <LayoutIcon :name="item.icon" />
          <span>{{ item.label }}</span>
        </li>
      </ul>
      <draggable
        v-else
        :list="group.children"
        item-key="id"
        @start="handleDragStart"
        v-bind="{ group: { name:'widgets', pull:'clone', put:false }, sort:false, ghostClass: 'ghost' }"
        tag="ul">
        <template #item="{ element }">
          <li class="widget-item" :data-id="element.id" :title="element.title">
            <LayoutIcon :name="element.icon" />
            <span>{{ element.label }}</span>
          </li>
        </template>
      </draggable>
    </el-collapse-item>
  </el-collapse>
</template>

<style lang="scss" scoped>
.widget {
  margin-top: -1px;
  --el-collapse-header-height: 45px;
  --el-collapse-header-bg-color: var(--fs-layout-background-color);
  --el-collapse-content-bg-color: var(--fs-layout-background-color);
  :deep(.el-collapse-item__header) {
    padding: 0 12px;
    box-sizing: border-box;
  }
  :deep(.el-collapse-item__content) {
    padding: 0 12px 12px 12px;
  }
  ul {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(92px, 1fr));
    gap: 8px;
    margin: 0;
    padding: 0;
    list-style: none;
  }
  .widget-item {
    min-width: 0;
    height: 30px;
    padding: 0 8px;
    box-sizing: border-box;
    font-size: 12px;
    color: #333;
    cursor: move;
    background: #f4f6fc;
    border: 1px solid #f4f6fc;
    border-radius: 4px;
    @include flex-start();
    gap: 6px;
    transition: color 0.2s, border-color 0.2s, background-color 0.2s;
    > .el-icon {
      flex: none;
    }
    > span {
      flex: 1;
      min-width: 0;
      @include text-wrap();
    }
    &:hover {
      color: var(--el-color-primary);
      background: var(--el-color-primary-light-9);
      border: 1px dashed var(--el-color-primary);
    }
  }
}

// vuedraggable 拖拽占位元素会脱离组件作用域，需使用全局样式
:global(li.widget-item.ghost) {
  width: 92px;
  height: 30px;
  padding: 0 8px;
  box-sizing: border-box;
  list-style: none;
  font-size: 12px;
  color: var(--el-color-primary);
  background: #f4f6fc;
  border: 1px dashed var(--el-color-primary);
  border-radius: 4px;
  @include flex-start();
  gap: 6px;
}
</style>
