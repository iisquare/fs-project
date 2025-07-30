<script setup lang="ts">
import { inject, onMounted, ref } from 'vue'
import * as X6 from '@antv/x6'

const node = (inject('getNode') as any)() as X6.Node
const data = ref()

onMounted(() => {
  node.on('change:data', ({ current } = {} as any) => {
    data.value = current
  })
})
</script>

<template>
  <el-button>BTN:{{ JSON.stringify(data) }}</el-button>
</template>

<style lang="scss" scoped>

</style>
