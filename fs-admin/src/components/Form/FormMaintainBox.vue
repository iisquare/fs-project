<script setup lang="ts">
/**
 * 维护任务盒子
 *
 * 左侧维护项导航（支持搜索），右侧展示当前维护任务的执行面板。
 * 适用于维护项会持续增加的场景，避免多个维护项平铺展开。
 */
import { computed, ref } from 'vue';
import * as ElementPlusIcons from '@element-plus/icons-vue';
import FormMaintain from './FormMaintain.vue';

const props = withDefaults(defineProps<{
  items: Array<{
    key: string
    title: string
    description?: string
    api: any
    params?: () => Record<string, any>
    headers?: () => Record<string, any>
  }>
  height?: string
}>(), {
  items: () => [],
  height: '320px',
})

const activeKey = ref('')
const keyword = ref('')

const activeItem = computed(() => props.items.find((item) => item.key === activeKey.value))
const filteredItems = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  if (!kw) return props.items
  return props.items.filter((item) => {
    return item.title.toLowerCase().includes(kw) || (item.description || '').toLowerCase().includes(kw)
  })
})

const handleSelect = (key: string) => {
  activeKey.value = key
}

// 默认选中第一项
if (props.items.length && !activeItem.value) {
  activeKey.value = props.items[0].key
}
</script>

<template>
  <div class="form-maintain-box">
    <el-container class="form-maintain-box__container">
      <el-aside width="240px" class="form-maintain-box__aside">
        <el-input
          v-model="keyword"
          placeholder="搜索维护项"
          clearable
          :prefix-icon="ElementPlusIcons.Search"
          class="form-maintain-box__search" />
        <el-scrollbar class="form-maintain-box__menu-scroll">
          <el-menu
            :default-active="activeKey"
            class="form-maintain-box__menu"
            @select="handleSelect">
            <el-menu-item v-for="item in filteredItems" :key="item.key" :index="item.key">
              <span>{{ item.title }}</span>
            </el-menu-item>
          </el-menu>
          <el-empty
            v-if="!filteredItems.length"
            description="未找到维护项"
            :image-size="60"
            class="form-maintain-box__empty-menu" />
        </el-scrollbar>
      </el-aside>
      <el-main class="form-maintain-box__main">
        <FormMaintain
          v-if="activeItem"
          :key="activeItem.key"
          :title="activeItem.title"
          :description="activeItem.description"
          :api="activeItem.api"
          :params="activeItem.params"
          :headers="activeItem.headers"
          :height="height" />
        <el-empty v-else description="暂无维护项" :image-size="60" />
      </el-main>
    </el-container>
  </div>
</template>

<style lang="scss" scoped>
.form-maintain-box {
  &__container {
    min-height: 380px;
  }

  &__aside {
    padding-right: 12px;
    border-right: 1px solid var(--el-border-color-light);
  }

  &__search {
    margin-bottom: 12px;
  }

  &__menu-scroll {
    height: calc(100% - 44px);
  }

  &__menu {
    border-right: none;
  }

  &__empty-menu {
    padding: 40px 0;
  }

  &__main {
    padding: 0 0 0 16px;
  }
}
</style>
