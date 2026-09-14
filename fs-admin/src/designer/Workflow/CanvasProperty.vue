<script setup lang="ts">
import { computed, onMounted, ref, shallowRef } from 'vue'
import ApiUtil from '@/utils/ApiUtil'
import FormApi from '@/api/oa/FormApi'
import FormFrameApi from '@/api/oa/FormFrameApi'

const active = ref('property')
const props = defineProps<{
  bpmn: any,
  workflow: any,
}>()

const statuses = [{
  label: '启用', value: 1
}, {
  label: '禁用', value: 2
}]

// 流程定义在 importXML 完成后才可用，需在导入结束时刷新，否则流程标识列表为空
const definitions = shallowRef<any>(null)
const refresh = () => {
  const modeler = props.bpmn?.modeler
  definitions.value = modeler?.getDefinitions ? modeler.getDefinitions() : null
}

const rootElements = computed(() => {
  return definitions.value ? definitions.value.rootElements : []
})

onMounted(() => {
  refresh()
  props.bpmn?.modeler?.on('import.done', refresh)
})

// 选择关联表单后即时载入表单定义，便于配置用户任务的表单权限
const handleFormChange = (value: any) => {
  props.workflow.formInfo = null
  if (!value) return false
  FormApi.frame({ id: value }).then((result: any) => {
    if (!ApiUtil.succeed(result)) return false
    props.workflow.formInfo = ApiUtil.data(result)
    return true
  }).catch(() => {})
  return true
}
</script>

<template>
  <el-tabs v-model="active" class="tab-property">
    <el-tab-pane label="流程属性" name="property">
      <el-form :model="workflow">
        <el-form-item label="主键">{{ workflow?.id || '新建' }}</el-form-item>
        <el-form-item label="名称">
          <el-input v-model="workflow.name" placeholder="请输入流程名称" autocomplete="off" />
        </el-form-item>
        <el-form-item label="关联表单">
          <form-select
            v-model="workflow.formId"
            :callback="FormFrameApi.list"
            clearable
            placeholder="输入名称进行检索"
            @change="handleFormChange" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="workflow.sort" :min="0" :max="200" :controls="false" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="workflow.status" placeholder="请选择">
            <el-option :key="item.value" :value="item.value" :label="item.label" v-for="item in statuses" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input type="textarea" v-model="workflow.description" placeholder="请输入流程描述" />
        </el-form-item>
        <el-divider>流程标识</el-divider>
        <el-form-item label="标识" :key="element.id" v-for="element in rootElements">
          <el-input v-model="element.id" placeholder="流程定义标识，需全局唯一" />
          <div class="fs-form-tip">流程发布后作为流程定义标识（key）使用，不同流程请勿重复</div>
        </el-form-item>
        <el-empty description="流程定义尚未载入" :image-size="60" v-if="rootElements.length === 0" />
      </el-form>
    </el-tab-pane>
  </el-tabs>
</template>

<style lang="scss" scoped>
.el-input-number {
  width: 100%;
}
.fs-form-tip {
  width: 100%;
  line-height: 18px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
</style>
