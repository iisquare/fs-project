<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import LayoutIcon from './LayoutIcon.vue';
import draggable from 'vuedraggable'
import DesignUtil from '@/utils/DesignUtil';

const {
  widgets = [],
  native = true,
} = defineProps({
  widgets: { type: Array<any>, required: false },
  native: { type: Boolean, required: false }, // 是否采用原生DOM方式
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
    padding: 0 10px 0 10px;
    box-sizing: border-box;
  }
  :deep(.el-collapse-item__content) {
    padding: 0px 16px 16px 16px;
  }
  ul {
    position: relative;
    overflow: hidden;
    padding: 0;
    margin: 0;
    @include flex-between();
    flex-wrap: wrap;
    row-gap: 10px;
  }
  .widget-item {
    flex: 0 0 36%;
    font-size: 12px;
    line-height: 26px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    color: #333;
    cursor: move;
    background: #f4f6fc;
    border: 1px solid #f4f6fc;
    padding: 3px 10px;
    @include flex-start();
    gap: 8px;
    &:hover {
      color: #409eff;
      border: 1px dashed #409eff;
    }
  }
}
</style>
