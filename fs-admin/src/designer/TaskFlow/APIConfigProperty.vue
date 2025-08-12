<script setup lang="ts">
import { ref } from 'vue';
import NodeSlice from './NodeSlice.vue';

const active = ref('property')
const model: any = defineModel()
const tips: any = defineModel('tips', { type: null })
const props = defineProps<{
  config: any,
  instance: any,
}>()

const methods:any = [{ value: 'GET', label: 'GET' }]
</script>

<template>
  <el-tabs v-model="active" class="tab-property">
    <el-tab-pane label="节点属性" name="property">
      <el-form :model="model">
        <NodeSlice v-model="model" :instance="instance" :config="config" :tips="tips" />
        <el-form-item label="" class="title">接口配置</el-form-item>
        <el-form-item label="接口地址"><el-input v-model="model.data.url" placeholder="HTTP/HTTPS链接地址" /></el-form-item>
        <el-form-item label="请求方式">
          <el-radio-group v-model="model.data.method">
            <el-radio-button v-for="item in methods" :key="item.model" :value="item.value" :label="item.label" />
          </el-radio-group>
        </el-form-item>
        <el-form-item label="校验字段"><el-input v-model="model.data.checkField" placeholder="接口状态字段，为空时不校验" /></el-form-item>
        <el-form-item label="校验值"><el-input v-model="model.data.checkmodel" placeholder="请求成功的返回值" /></el-form-item>
        <el-form-item label="数据字段"><el-input v-model="model.data.dataField" placeholder="数据项所在路径" /></el-form-item>
      </el-form>
    </el-tab-pane>
  </el-tabs>
</template>

<style lang="scss" scoped>
</style>
