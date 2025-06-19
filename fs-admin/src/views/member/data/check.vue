<script setup lang="ts">
import DataLogApi from '@/api/member/DataLogApi';
import { ref } from 'vue';

const loading = ref(false)
const form: any = ref({})

const handleCheck = () => {
  loading.value = true
  DataLogApi.check({ permits: form.value.permits }).then(result => {
    form.value.result = JSON.stringify(result, null, 4)
  }).catch(() => { }).finally(() => {
    loading.value = false
  })
}
</script>

<template>
  <el-card :bordered="false" shadow="never">
    <el-form :model="form" label-width="auto">
      <el-form-item label="权限标识">
        <el-input v-model="form.permits" />
      </el-form-item>
      <el-form-item label="执行操作">
        <el-space>
          <el-button @click="handleCheck" :loading="loading" type="primary">检测</el-button>
        </el-space>
      </el-form-item>
      <el-form-item label="权限配置">
        <el-input type="textarea" v-model="form.result" :rows="12" />
      </el-form-item>
    </el-form>
  </el-card>
</template>

<style lang="scss" scoped>
</style>
