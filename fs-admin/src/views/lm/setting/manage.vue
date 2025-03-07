<script setup lang="ts">
import ManageApi from '@/api/lm/ManageApi';
import SensitiveApi from '@/api/lm/SensitiveApi';
import { ElMessageBox } from 'element-plus';
import { ref } from 'vue';

const loading = ref(false)
const form: any = ref({})
const handleProxy = () => {
  ElMessageBox.confirm('确定重新构建模型代理吗', '操作提示', { type: 'warning', }).then(() => {
    ManageApi.notice({ type: 'model' }, { success: true })
  }).catch(() => {})
}
const handleSensitive = () => {
  ElMessageBox.confirm('确定重新构建敏感词字典吗', '操作提示', { type: 'warning', }).then(() => {
    ManageApi.notice({ type: 'sensitive' }, { success: true })
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
    <el-form :mode="form" label-width="auto">
      <el-form-item label="管理">
        <el-space>
          <el-button type="danger" @click="handleSensitive">重建敏感词字典</el-button>
          <el-button type="danger" @click="handleProxy">重建模型代理</el-button>
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
