<script setup lang="ts">
import LayoutDesigner from '@/components/Layout/LayoutDesigner.vue';
import LayoutProperty from '@/components/Layout/LayoutProperty.vue';
import LayoutWidget from '@/components/Layout/LayoutWidget.vue';
import X6Container from '@/components/X6/X6Container.vue';
import { onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import config from '@/components/KnowledgeGraph/config'

const route = useRoute()
const router = useRouter()
const flowRef = ref()

const loading = ref(false)
const handleSubmit = () => {
  console.log('x6', flowRef.value.toJSON())
}

const handleDragStart = (event: any, widget: any) => {
   flowRef.value.startDrag(event, widget)
}

onMounted(() => {
})
</script>

<template>
  <LayoutDesigner>
    <template #left>
      <LayoutWidget :widgets="config.widgets" @drag-start="handleDragStart" />
    </template>
    <template #top>
      <el-space>

      </el-space>
      <el-space>
        <el-button type="primary" @click="handleSubmit" :loading="loading">保存</el-button>
        <el-button @click="router.go(-1)">返回</el-button>
      </el-space>
    </template>
    <template #default>
      <X6Container ref="flowRef" />
    </template>
    <template #right>
      <LayoutProperty />
    </template>
  </LayoutDesigner>
</template>

<style lang="scss" scoped>
</style>
