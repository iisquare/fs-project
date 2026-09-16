<script setup lang="ts">
/**
 * 变量选择面板 - 插入变量弹出框的内容：顶部搜索框，下方按节点分组展示变量，点击即选中。
 * 面板内不再嵌套下拉，避免弹出框里再弹一层导致的「点两次」问题。
 * 列表支持鼠标上下选择（悬停即高亮为当前项，点击插入），键盘上下与回车同步作用于当前项。
 *
 * @prop {*} instance   - 画布实例（X6Container 暴露的 flow）
 * @prop {*} activeItem - 当前激活的节点，用于排除自身
 * @prop {Boolean} searchable - 是否展示顶部搜索框，斜线唤起时由编辑器内已输入的查询词过滤，默认 true
 * @emits select - 选中变量，参数为变量引用（`节点ID.变量名` 或 `sys.变量名`）
 */
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import LayoutIcon from '@/components/Layout/LayoutIcon.vue'
import { filterVariableGroups, variableGroups, variableTitle } from './variable'

const props = withDefaults(defineProps<{
  instance?: any,
  activeItem?: any,
  searchable?: boolean,
}>(), {
  // 布尔 prop 未传时会被 Vue 转成 false，这里显式声明默认值
  searchable: true,
})
const emit = defineEmits(['select'])

// 搜索词可双向绑定：斜线唤起时由编辑器内已输入的查询词驱动过滤
const keyword = defineModel<string>('keyword', { default: '' })
const searchRef = ref()
const listRef = ref()
// 当前选中项在拍平列表中的下标
const active = ref(0)

// 分组内变量拍平并记录全局下标，供鼠标悬停与键盘上下选择定位
const sections = computed(() => {
  let index = 0
  return filterVariableGroups(variableGroups(props.instance, props.activeItem), keyword.value).map((group) => ({
    label: group.label,
    items: group.variables.map((item: any) => ({ item, index: index++ })),
  }))
})
const items = computed(() => sections.value.flatMap((section) => section.items))

// 过滤结果变化后回到首项
watch([keyword, () => items.value.length], () => {
  active.value = 0
})

const handleSelect = (value: string) => {
  emit('select', value)
}

/** 上下移动当前项（循环），并保证当前项在可视区域内 */
const move = (step: number) => {
  if (!items.value.length) return
  active.value = (active.value + step + items.value.length) % items.value.length
  nextTick(() => {
    listRef.value?.querySelector?.('.is-active')?.scrollIntoView({ block: 'nearest' })
  })
}

/** 插入当前选中项 */
const pickActive = () => {
  const current: any = items.value[active.value]
  if (current) handleSelect(current.item.value)
}

/** 展开弹出框时聚焦搜索框并清空上次的搜索词；无搜索框时不处理 */
const focus = () => {
  if (false === props.searchable) return
  keyword.value = ''
  nextTick(() => searchRef.value?.focus())
}
// 首次展开时弹出框内容才创建，这里主动聚焦搜索框（后续展开由父级 show 事件触发）
onMounted(() => focus())
defineExpose({ focus, move, pickActive })
</script>

<template>
  <div class="variable-picker">
    <el-input
      v-if="searchable"
      ref="searchRef"
      v-model="keyword"
      size="small"
      clearable
      placeholder="搜索变量名称"
      @keydown.up.prevent="move(-1)"
      @keydown.down.prevent="move(1)"
      @keyup.enter="pickActive">
      <template #prefix>
        <LayoutIcon name="Search" />
      </template>
    </el-input>
    <div class="variable-picker__list" ref="listRef">
      <template :key="section.label" v-for="section in sections">
        <div class="variable-picker__group">{{ section.label }}</div>
        <div
          class="variable-picker__item"
          :class="{ 'is-active': item.index === active }"
          :key="item.item.value"
          v-for="item in section.items"
          @mouseenter="active = item.index"
          @click="handleSelect(item.item.value)">
          <span class="name">{{ variableTitle(item.item) }}</span>
          <span class="type">{{ item.item.type }}</span>
        </div>
      </template>
      <div class="variable-picker__empty" v-if="!sections.length">未找到匹配的变量</div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.variable-picker {
  width: 100%;
  .variable-picker__list {
    max-height: 240px;
    margin-top: 6px;
    overflow: auto;
  }
  .variable-picker__group {
    padding: 6px 0 2px;
    font-size: 12px;
    color: var(--el-text-color-placeholder);
  }
  .variable-picker__item {
    padding: 4px 8px;
    border-radius: 4px;
    cursor: pointer;
    font-size: 13px;
    @include flex-between();
    &:hover, &.is-active {
      background: var(--el-fill-color-light);
      color: var(--el-color-primary);
    }
    .name {
      @include text-wrap();
    }
    .type {
      margin-left: 8px;
      flex: none;
      font-size: 12px;
      color: var(--el-text-color-placeholder);
    }
  }
  .variable-picker__empty {
    padding: 12px 0;
    text-align: center;
    font-size: 12px;
    color: var(--el-text-color-placeholder);
  }
}
</style>
