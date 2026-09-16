<script setup lang="ts">
/**
 * 连线属性 - 维护连线名称（条件分支的分支名称）与说明。
 */
import { computed, ref } from 'vue'

const active = ref('property')
const model: any = defineModel()
const tips: any = defineModel('tips', { type: null })
const props = defineProps<{
  config?: any,
  instance?: any,
}>()

if (!model.value.data) model.value.data = { name: '', description: '' }

const handleDelete = () => {
  props.instance?.flow?.remove(model.value)
}

const nodeName = (id: string) => {
  const cell: any = props.instance?.flow?.graph?.getCellById(id)
  return cell?.getData?.()?.name ?? id
}

const source = computed(() => nodeName(model.value?.source?.cell))
const target = computed(() => nodeName(model.value?.target?.cell))
</script>

<template>
  <el-tabs v-model="active" class="tab-property">
    <el-tab-pane label="连线属性" name="property">
      <el-form :model="model" label-position="top">
        <el-form-item label="" class="title">
          <span>基础信息</span>
          <el-popconfirm title="确认删除该连线？" width="180" @confirm="handleDelete">
            <template #reference>
              <LayoutIcon name="Delete" class="delete" />
            </template>
          </el-popconfirm>
        </el-form-item>
        <el-form-item label="起点节点">{{ source }}</el-form-item>
        <el-form-item label="终点节点">{{ target }}</el-form-item>
        <el-form-item label="连线名称">
          <el-input v-model="model.data.name" placeholder="条件分支的输出名称" />
        </el-form-item>
        <el-form-item label="连线说明">
          <el-input v-model="model.data.description" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
    </el-tab-pane>
  </el-tabs>
</template>

<style lang="scss" scoped>
</style>
