<script setup lang="ts">
import icons from '@renderer/assets/icons'
import DataUtil from '@renderer/utils/DataUtil';

const props = defineProps<{
  name: any
  filled?: boolean
}>()
</script>

<template>
  <el-icon v-if="name && name.includes('.')" v-html="DataUtil.value(icons, name)"></el-icon>
  <el-icon v-else>
    <component :is="name" :filled="filled" />
  </el-icon>
</template>
