<script setup lang="ts">
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
