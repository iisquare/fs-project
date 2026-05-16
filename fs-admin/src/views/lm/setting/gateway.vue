<script setup lang="ts">
import GatewayApi from '@/api/lm/GatewayApi';
import SensitiveApi from '@/api/lm/SensitiveApi';
import ElementUtil from '@/utils/ElementUtil';
import { ref } from 'vue';

const loading = ref(false)
const form: any = ref({})
const handleProxy = () => {
  ElementUtil.confirm('确定重新构建网关模型吗').then(() => {
    GatewayApi.notice({ type: 'model' }, { success: true })
  }).catch(() => {})
}
const handleSensitive = () => {
  ElementUtil.confirm('确定重新构建安全围栏吗').then(() => {
    GatewayApi.notice({ type: 'sensitive' }, { success: true })
  }).catch(() => {})
}

const handleWindow = () => {
  loading.value = true
  SensitiveApi.window({ sentence: form.value.sentence }).then(result => {
    form.value.result = JSON.stringify(result, null, 4)
  }).catch(() => { }).finally(() => {
    loading.value = false
  })
}
const handleCheck = () => {
  loading.value = true
  SensitiveApi.check({ sentence: form.value.sentence }).then(result => {
    form.value.result = JSON.stringify(result, null, 4)
  }).catch(() => { }).finally(() => {
    loading.value = false
  })
}
</script>

<template>
  <el-card :bordered="false" shadow="never">
    <el-form :model="form" label-width="auto">
      <el-form-item label="管理">
        <el-space>
          <el-button type="danger" @click="handleSensitive">重建安全围栏</el-button>
          <el-button type="danger" @click="handleProxy">重建网关模型</el-button>
        </el-space>
      </el-form-item>
      <el-form-item label="语句">
        <el-input type="textarea" v-model="form.sentence" />
      </el-form-item>
      <el-form-item label="敏感词">
        <el-space>
          <el-button @click="handleWindow" :loading="loading">窗口</el-button>
          <el-button @click="handleCheck" :loading="loading">检测</el-button>
        </el-space>
      </el-form-item>
      <el-form-item label="结果">
        <el-input type="textarea" v-model="form.result" :rows="12" />
      </el-form-item>
    </el-form>
  </el-card>
</template>

<style lang="scss" scoped>
</style>
