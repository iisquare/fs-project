<script setup lang="ts">
import { ref } from 'vue';

const active = ref('property')
const model: any = defineModel()
const tips: any = defineModel('tips', { type: null })
defineProps<{
  config: any,
  instance: any,
}>()
</script>

<template>
  <el-tabs v-model="active" class="tab-property">
    <el-tab-pane label="流程属性" name="property">
      <el-form :model="model">
        <el-form-item label="" class="title">基础信息</el-form-item>
        <el-form-item label="流程标识" v-if="model.id">{{ model.id }}</el-form-item>
        <el-form-item label="流程名称">
          <el-input v-model="model.name" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="model.sort" :controls="false" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="model.status" placeholder="请选择">
            <el-option v-for="(value, key) in config.status" :key="key" :value="key" :label="value" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input type="textarea" v-model="model.description" />
        </el-form-item>
        <el-form-item label="" class="title">流程配置</el-form-item>
        <el-form-item label="并发度">
          <el-input-number v-model="model.concurrent" :controls="false" />
        </el-form-item>
        <el-form-item label="并发策略">
          <el-select v-model="model.concurrency" placeholder="请选择">
            <el-option v-for="item in config.concurrences" :key="item.value" :value="item.value" :label="item.label" />
          </el-select>
        </el-form-item>
        <el-form-item label="失败策略">
          <el-select v-model="model.failure" placeholder="请选择">
            <el-option v-for="item in config.failures" :key="item.value" :value="item.value" :label="item.label" />
          </el-select>
        </el-form-item>
        <el-form-item label="" class="title">定时调度</el-form-item>
        <el-form-item label="表达式">
          <form-cron v-model="model.expression" />
        </el-form-item>
        <el-form-item label="" class="title">默认参数</el-form-item>
        <el-form-item label="">
          <el-input type="textarea" v-model="model.data" :rows="5" />
        </el-form-item>
      </el-form>
    </el-tab-pane>
    <el-tab-pane label="消息通知" name="notify">
      <el-form :model="model">
        <el-form-item label="" class="title">通知配置</el-form-item>
        <el-form-item label="触发时机">
          <el-checkbox-group v-model="model.notify.stage">
            <el-checkbox :label="item.label" :value="item.value" v-for="item in config.stages" :key="item.value" />
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="失败通知">
          <el-input type="textarea" v-model="model.notify.failure" placeholder="采用英文逗号分隔" :rows="5" />
        </el-form-item>
        <el-form-item label="成功通知">
          <el-input type="textarea" v-model="model.notify.success" placeholder="采用英文逗号分隔" :rows="5" />
        </el-form-item>
      </el-form>
    </el-tab-pane>
  </el-tabs>
</template>

<style lang="scss" scoped>
</style>
