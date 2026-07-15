<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import ApiUtil from '@/utils/ApiUtil'
import PrintApi from '@/api/oa/PrintApi'

const route = useRoute()
const loading = ref(false)
const info = ref<any>({ id: 0, name: '', content: '' })

onMounted(() => {
  const id = route.query.id
  if (!id) return
  loading.value = true
  PrintApi.info({ id }).then(result => {
    Object.assign(info.value, ApiUtil.data(result))
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
})

const handlePrint = () => {
  window.print()
}
</script>

<template>
  <el-card :bordered="false" shadow="never" v-loading="loading">
    <template #header>
      <el-space>
        <span>打印预览 - {{ info.name }}</span>
        <el-button type="primary" @click="handlePrint">打印</el-button>
      </el-space>
    </template>
    <div class="print-content" v-html="info.content"></div>
  </el-card>
</template>

<style lang="scss" scoped>
.print-content {
  background: #fff;
  padding: 20px;
  min-height: 600px;
}
@media print {
  .print-content {
    padding: 0;
  }
}
</style>
