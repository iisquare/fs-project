<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue';
import * as ElementPlusIcons from '@element-plus/icons-vue';
import MenuUtil from '@/utils/MenuUtil';

const props = defineProps<{
  tabs: any[];
  modelValue: any;
  label?: (item: any) => string;
}>();

const emit = defineEmits<{
  (e: 'update:modelValue', value: any): void;
}>();

const tabName = (item: any): string => {
  if (props.label) return props.label(item);
  return item?.title ?? String(item);
};

const viewportRef = ref<HTMLElement>();

const canScrollLeft = ref(false);
const canScrollRight = ref(false);

const updateScrollState = () => {
  const el = viewportRef.value;
  if (!el) return;
  canScrollLeft.value = el.scrollLeft > 0;
  canScrollRight.value = el.scrollLeft + el.clientWidth < el.scrollWidth - 1;
};

const scrollBy = (delta: number) => {
  viewportRef.value?.scrollBy({ left: delta, behavior: 'smooth' });
};

const scrollToIndex = (index: number) => {
  nextTick(() => {
    const el = viewportRef.value;
    if (!el) return;
    const item = el.querySelector(`[data-tab-index="${index}"]`) as HTMLElement | null;
    if (!item) return;
    const target = item.offsetLeft - (el.clientWidth - item.offsetWidth) / 2;
    el.scrollTo({ left: target, behavior: 'smooth' });
  });
};

const select = (item: any) => {
  emit('update:modelValue', item);
};

const removeAt = (indices: number[]) => {
  if (!indices.length) return;
  const activeIndex = props.modelValue == null ? -1 : props.tabs.indexOf(props.modelValue);
  const indexSet = new Set(indices);
  [...indices].sort((a, b) => b - a).forEach((i) => props.tabs.splice(i, 1));
  if (activeIndex > -1 && indexSet.has(activeIndex)) {
    emit('update:modelValue', props.tabs.length ? props.tabs[props.tabs.length - 1] : null);
  }
};

const closeSingle = (item: any) => {
  const index = props.tabs.indexOf(item);
  if (index > -1) removeAt([index]);
};

const handleContextMenu = (event: MouseEvent, item: any) => {
  const index = props.tabs.indexOf(item);
  if (index === -1) return;
  MenuUtil.context(event, [
    { key: 'close', icon: 'Close', title: '关闭当前' },
    { key: 'close-others', icon: 'CircleClose', title: '关闭其他', disabled: props.tabs.length <= 1 },
    { key: 'close-left', icon: 'Back', title: '关闭左侧', disabled: index === 0 },
    { key: 'close-right', icon: 'Right', title: '关闭右侧', disabled: index === props.tabs.length - 1 },
    { key: 'close-all', icon: 'FolderDelete', title: '关闭全部' },
  ], (menu: any) => {
    switch (menu.key) {
      case 'close':
        return removeAt([index]);
      case 'close-others':
        return removeAt(props.tabs.map((_: any, i: number) => i).filter((i: number) => i !== index));
      case 'close-left':
        return removeAt(props.tabs.map((_: any, i: number) => i).filter((i: number) => i < index));
      case 'close-right':
        return removeAt(props.tabs.map((_: any, i: number) => i).filter((i: number) => i > index));
      case 'close-all':
        return removeAt(props.tabs.map((_: any, i: number) => i));
      default:
        return false;
    }
  });
};

const searchVisible = ref(false);
const keyword = ref('');
const searchInputRef = ref();

const toggleSearch = () => {
  searchVisible.value = !searchVisible.value;
  if (searchVisible.value) {
    nextTick(() => searchInputRef.value?.focus());
  } else {
    keyword.value = '';
  }
};

const matches = computed(() => {
  const kw = keyword.value.trim().toLowerCase();
  if (!kw) return [] as number[];
  return props.tabs.reduce<number[]>((acc, item, index) => {
    if (tabName(item).toLowerCase().includes(kw)) acc.push(index);
    return acc;
  }, []);
});

const currentIndex = ref(0);

const locate = (index: number) => {
  if (index > -1) scrollToIndex(index);
};

watch(matches, (value) => {
  if (value.length) {
    currentIndex.value = 0;
    locate(value[0]);
  }
});

const handleSearchKeydown = (e: KeyboardEvent | Event) => {
  const event = e as KeyboardEvent;
  if (!matches.value.length) return;
  if (event.key === 'Enter') {
    const index = matches.value[currentIndex.value];
    if (index != null) emit('update:modelValue', props.tabs[index]);
  } else if (event.key === 'ArrowDown') {
    event.preventDefault();
    currentIndex.value = (currentIndex.value + 1) % matches.value.length;
    locate(matches.value[currentIndex.value]);
  } else if (event.key === 'ArrowUp') {
    event.preventDefault();
    currentIndex.value = (currentIndex.value - 1 + matches.value.length) % matches.value.length;
    locate(matches.value[currentIndex.value]);
  } else if (event.key === 'Escape') {
    keyword.value = '';
    searchVisible.value = false;
  }
};

const handleSearchBlur = () => {
  if (!keyword.value) searchVisible.value = false;
};

watch(() => props.modelValue, (item) => {
  if (item != null) {
    locate(props.tabs.indexOf(item));
  }
});

watch(() => props.tabs, () => {
  nextTick(updateScrollState);
}, { deep: true });

onMounted(() => {
  nextTick(updateScrollState);
});
</script>

<template>
  <div class="tab-bar">
    <div class="tab-bar__nav" :class="{ 'is-disabled': !canScrollLeft }" @click="scrollBy(-200)">
      <el-icon><ElementPlusIcons.ArrowLeft /></el-icon>
    </div>
    <div ref="viewportRef" class="tab-bar__viewport" @scroll="updateScrollState">
      <div class="tab-bar__strip">
        <div
          v-for="(tab, index) in tabs"
          :key="index"
          :data-tab-index="index"
          class="tab-bar__item"
          :class="{
            'is-active': tab === modelValue,
            'is-match': matches.includes(index),
            'is-match-current': matches[currentIndex] === index,
          }"
          @click="select(tab)"
          @contextmenu.prevent="handleContextMenu($event, tab)"
        >
          <span class="tab-bar__item-label">{{ tabName(tab) }}</span>
          <el-icon class="tab-bar__item-close" @click.stop="closeSingle(tab)"><ElementPlusIcons.Close /></el-icon>
        </div>
      </div>
    </div>
    <div class="tab-bar__nav" :class="{ 'is-disabled': !canScrollRight }" @click="scrollBy(200)">
      <el-icon><ElementPlusIcons.ArrowRight /></el-icon>
    </div>
    <div class="tab-bar__search">
      <el-input
        v-if="searchVisible"
        ref="searchInputRef"
        v-model="keyword"
        size="small"
        placeholder="检索标签"
        clearable
        :prefix-icon="ElementPlusIcons.Search"
        @keydown="handleSearchKeydown"
        @blur="handleSearchBlur"
      />
      <div v-else class="tab-bar__search-toggle" title="检索标签" @click="toggleSearch">
        <el-icon><ElementPlusIcons.Search /></el-icon>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.tab-bar {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 0 8px;
  border-bottom: 1px solid var(--el-border-color-light);
}

.tab-bar__nav {
  flex: none;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  border-radius: 3px;
  cursor: pointer;
  color: var(--el-text-color-regular);

  &:hover:not(.is-disabled) {
    background: var(--el-fill-color-light);
    color: var(--el-color-primary);
  }

  &.is-disabled {
    color: var(--el-text-color-placeholder);
    cursor: not-allowed;
  }
}

.tab-bar__viewport {
  flex: 1;
  min-width: 0;
  overflow-x: auto;
  overflow-y: hidden;
  scrollbar-width: none;

  &::-webkit-scrollbar {
    display: none;
  }
}

.tab-bar__strip {
  display: inline-flex;
  align-items: flex-end;
  gap: 4px;
  padding: 4px 0;
}

.tab-bar__item {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 10px;
  font-size: 13px;
  white-space: nowrap;
  cursor: pointer;
  color: var(--el-text-color-regular);
  border: 1px solid transparent;
  border-bottom: none;
  border-radius: 4px 4px 0 0;

  &.is-active {
    color: var(--el-color-primary);
    border-color: var(--el-border-color-light);
    background: #fff;
  }

  &.is-match .tab-bar__item-label {
    color: var(--el-color-warning);
    font-weight: 600;
  }

  &.is-match-current {
    background: var(--el-color-warning-light-9);
  }

  &:hover .tab-bar__item-close {
    opacity: 1;
  }
}

.tab-bar__item-close {
  opacity: 0;
  font-size: 12px;

  &:hover {
    color: var(--el-color-danger);
  }
}

.tab-bar__search {
  flex: none;
  display: flex;
  align-items: center;

  :deep(.el-input) {
    width: 160px;
  }
}

.tab-bar__search-toggle {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  border-radius: 3px;
  cursor: pointer;
  color: var(--el-text-color-regular);

  &:hover {
    background: var(--el-fill-color-light);
    color: var(--el-color-primary);
  }
}
</style>
