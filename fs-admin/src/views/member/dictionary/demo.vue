<script setup lang="ts">
import RouteUtil from '@/utils/RouteUtil';
import type { FormInstance } from 'element-plus';
import { onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';

const route = useRoute()
const router = useRouter()
const filters: any = ref(RouteUtil.query2filter(route, {
  level: 0
}, false))
const formRef = ref<FormInstance>()

const handleSubmit = () => {
  RouteUtil.filter2query(route, router, filters.value)
}

onMounted(() => {
})
</script>

<template>
  <el-card :bordered="false">
    <el-form ref="formRef" :model="filters" label-width="auto">
      <el-form-item label="性别" prop="single">
        <dictionary-select v-model="filters.single" dictionary="gender" />
      </el-form-item>
      <el-form-item label="标签" prop="multiple">
        <dictionary-select v-model="filters.multiple" dictionary="audit-tag" multiple />
      </el-form-item>
      <el-form-item label="分组" prop="group">
        <dictionary-group v-model="filters.group" dictionary="ai-risk" filterable />
      </el-form-item>
      <el-form-item label="级联单选" prop="cascadeSingle">
        <dictionary-cascader v-model="filters.cascadeSingle" dictionary="cascade" />
      </el-form-item>
      <el-form-item label="级联多选" prop="cascadeMultiple">
        <dictionary-cascader v-model="filters.cascadeMultiple" dictionary="cascade" multiple />
      </el-form-item>
      <el-form-item label="级联加载层级" prop="level">
        <el-input-number v-model="filters.level" :min="0" />
      </el-form-item>
      <el-form-item label="级联动态加载" prop="cascadeLazy">
        <dictionary-cascader v-model="filters.cascadeLazy" dictionary="area" multiple lazy :level="filters.level" />
      </el-form-item>
      <el-form-item label="操作">
        <el-space>
          <el-button type="primary" @click="handleSubmit">确定</el-button>
          <el-button @click="formRef?.resetFields()">重置</el-button>
        </el-space>
      </el-form-item>
      <el-form-item>
        <el-alert title="级联加载层级默认为不限制，更改后请点击确定按钮并刷新页面！" type="success" show-icon />
      </el-form-item>
      <el-form-item label="结果">{{ JSON.stringify(filters, null, 2) }}</el-form-item>
    </el-form>
  </el-card>
</template>

<style lang="scss" scoped>
</style>
