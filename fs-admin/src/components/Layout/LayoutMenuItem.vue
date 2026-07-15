<script setup lang="ts">
/**
 * 菜单项渲染组件 - 递归渲染菜单树，自动区分子菜单（el-sub-menu）和叶子菜单项（el-menu-item）。
 * 支持内部路由（RouterLink）和外部链接（a 标签）。
 *
 * @prop {MenuItem[]} menus - 菜单节点数组
 *
 * 菜单项结构 (MenuItem):
 *   { id: any, name: string, url: string, icon?: string, target?: string, children?: MenuItem[] }
 *
 * @example
 * <layout-menu-item :menus="menuTree" />
 */
defineProps<{
  menus: any
}>()
</script>

<template>
  <template v-for="menu in menus">
    <el-sub-menu :index="menu.id + ''" v-if="menu.children && menu.children.length > 0">
      <template #title>
        <LayoutIcon :name="menu.icon" />
        <span>{{ menu.name }}</span>
      </template>
      <LayoutMenuItem :menus="menu.children" />
    </el-sub-menu>
    <el-menu-item :index="menu.id + ''" v-else>
      <template #title>
        <LayoutIcon :name="menu.icon" />
        <a :href="menu.url" v-if="menu.url.startsWith('http')" :target="menu.target">{{ menu.name }}</a>
        <RouterLink :to="menu.url" v-else :target="menu.target">{{ menu.name }}</RouterLink>
      </template>
    </el-menu-item>
  </template>
</template>

<style lang="scss" scoped>
</style>
