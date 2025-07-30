<script setup lang="ts">
import { onMounted, ref } from 'vue';
import NodeSlice from './NodeSlice.vue';
import { useCacheStore } from '@/stores/cache';
import DagApi from '@/api/bi/DagApi';
import ApiUtil from '@/utils/ApiUtil';

const active = ref('property')
const model: any = defineModel()
const tips: any = defineModel('tips', { type: null })
const props = defineProps<{
  config: any,
  instance: any,
}>()
const dagConfig: any = ref({
  timezones: [],
  locales: [],
})
const cache = useCacheStore()
onMounted(() => {
  cache.load(null, () => DagApi.config(), 0).then((result: any) => {
    dagConfig.value = ApiUtil.data(result)
  }).catch(() => {})
})
</script>

<template>
  <el-tabs v-model="active" class="tab-property">
    <el-tab-pane label="节点属性" name="property">
      <el-form :model="model">
        <NodeSlice v-model="model" :instance="instance" :config="config" :tips="tips" />
      </el-form>
      <el-form-item label="" class="title">参数配置</el-form-item>
      <el-form-item  label="变量名称"><el-input v-model="model.data.arg" placeholder="date" /></el-form-item >
      <el-form-item  label="固定日期"><el-input v-model="model.data.datetime" placeholder="默认为服务执行时间" /></el-form-item >
      <el-form-item  label="日期格式"><el-input v-model="model.data.pattern" placeholder="仅在设定固定日期时有效" /></el-form-item >
      <el-form-item  label="所在时区">
        <el-select v-model="model.data.timezone" placeholder="请选择所在时区" clearable filterable>
          <el-option v-for="(item, key) in dagConfig.timezones" :key="key" :value="item.value" :label="item.label" />
        </el-select>
      </el-form-item >
      <el-form-item  label="所在地区">
        <el-select v-model="model.data.locale" placeholder="请选择所在地区" clearable filterable>
          <el-option v-for="(item, key) in dagConfig.locales" :key="key" :value="item.value" :label="item.label" />
        </el-select>
      </el-form-item >
    </el-tab-pane>
  </el-tabs>
</template>

<style lang="scss" scoped>
</style>
