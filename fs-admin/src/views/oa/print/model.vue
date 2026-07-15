<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ApiUtil from '@/utils/ApiUtil'
import PrintApi from '@/api/oa/PrintApi'

const route = useRoute()
const router = useRouter()
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

const handleSave = () => {
  loading.value = true
  PrintApi.save(info.value, { success: true }).then((result: any) => {
    info.value.id = result.data.id
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}
</script>

<template>
  <el-card :bordered="false" shadow="never" v-loading="loading">
    <template #header>
      <el-space>
        <span>打印模型设计 - {{ info.name || '新建' }}</span>
        <el-button type="primary" @click="handleSave" :loading="loading">保存</el-button>
        <el-button @click="router.go(-1)">返回</el-button>
      </el-space>
    </template>
    <el-form :model="info" label-width="80px">
      <el-form-item label="名称">
        <el-input v-model="info.name" />
      </el-form-item>
      <el-form-item label="模板内容">
        <el-input type="textarea" v-model="info.content" :rows="20" placeholder="HTML / 模板内容" />
      </el-form-item>
    </el-form>
  </el-card>
</template>

<style lang="scss" scoped>
</style>
