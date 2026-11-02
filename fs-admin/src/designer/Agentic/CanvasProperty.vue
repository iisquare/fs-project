<script setup lang="ts">
/**
 * 画布属性 - 编排应用的基础信息配置（含授权角色）。
 */
import { computed, ref } from 'vue'
import RoleApi from '@/api/member/RoleApi'
import SectionSlice from './SectionSlice.vue'

const active = ref('property')
const model: any = defineModel()
const tips: any = defineModel('tips', { type: null })
defineProps<{
  config?: any,
  instance?: any,
}>()

const tags = computed({
  get: () => Array.isArray(model.value?.tags) ? model.value.tags : [],
  set: (value: string[]) => { model.value.tags = value ?? [] },
})
// 授权角色：为空表示所有登录用户都能运行/对话，配置后按角色放行（列表页与新用户对话页都按此过滤）
const roleIds = computed({
  get: () => Array.isArray(model.value?.roleIds) ? model.value.roleIds : [],
  set: (value: number[]) => { model.value.roleIds = value ?? [] },
})
</script>

<template>
  <el-tabs v-model="active" class="tab-property">
    <el-tab-pane label="应用属性" name="property">
      <el-form :model="model" label-position="top">
        <SectionSlice title="基础信息">
          <el-form-item label="应用标识" v-if="model.id">{{ model.id }}</el-form-item>
          <el-form-item label="应用名称">
            <el-input v-model="model.name" placeholder="请输入应用名称" />
          </el-form-item>
          <el-form-item label="应用类型">
            <el-select v-model="model.mode" placeholder="请选择">
              <el-option :key="item.value" :value="item.value" :label="item.label" v-for="item in $props.config.modes" />
            </el-select>
          </el-form-item>
          <el-form-item label="展示图标">
            <el-input v-model="model.icon" placeholder="Element Plus 图标名称，如 ai.robot" />
          </el-form-item>
          <el-form-item label="应用标签">
            <el-select v-model="tags" multiple filterable allow-create default-first-option placeholder="请输入标签" />
          </el-form-item>
          <el-form-item label="授权角色">
            <form-select v-model="roleIds" :callback="RoleApi.list" multiple clearable placeholder="不选择表示所有登录用户可用" />
          </el-form-item>
          <el-form-item label="排序">
            <el-input-number v-model="model.sort" :controls="false" />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="model.status" placeholder="请选择">
              <el-option :key="key" :value="key" :label="value" v-for="(value, key) in $props.config.status" />
            </el-select>
          </el-form-item>
          <el-form-item label="应用描述">
            <el-input v-model="model.description" type="textarea" :rows="3" />
            <tip-text>该应用作为工具时，描述会附加到常驻工具说明中，用于帮助模型判断何时调用；建议简洁写清适用场景和业务范围。</tip-text>
          </el-form-item>
        </SectionSlice>
        <SectionSlice title="使用说明">
          <el-form-item label="">
            <tip-text>
              <div>从左侧拖拽节点到画布，连线完成编排</div>
              <div>节点配置中的变量引用上游节点的输出变量</div>
              <div>保存得到草稿，仅用于调试运行；发布后对外提供的是发布内容</div>
            </tip-text>
          </el-form-item>
        </SectionSlice>
      </el-form>
    </el-tab-pane>
  </el-tabs>
</template>
