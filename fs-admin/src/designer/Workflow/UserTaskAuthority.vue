<script setup lang="ts">
import { ref, watch } from 'vue'
import flexFormConfig from '@/designer/FlexForm/config'
import DesignUtil from '@/utils/DesignUtil'

const model: any = defineModel()
const props = defineProps<{
  bpmn: any,
  element: any,
  workflow: any,
}>()

const rows = ref<any[]>([])
const authority = ref<any>({})
const expandedRowKeys = ref<any[]>([])
let syncing = false

const parseValue = (value: any) => {
  try {
    if (!value) return {}
    return JSON.parse(value)
  } catch (e) {
    return {}
  }
}

watch(() => props.element, () => {
  expandedRowKeys.value = []
  rows.value = flexFormConfig.exhibition.authorityFields(
    DesignUtil.frameWidgets(props.workflow?.formInfo), expandedRowKeys.value)
  syncing = true
  authority.value = flexFormConfig.exhibition.authority(rows.value, parseValue(model.value))
  syncing = false
}, { immediate: true })

watch(authority, (val) => {
  if (syncing) return
  model.value = JSON.stringify(val)
}, { deep: true, flush: 'sync' })

const checkedAll = (field: string) => {
  for (const key in authority.value) authority.value[key][field] = true
}

const checkedRevert = (field: string) => {
  for (const key in authority.value) authority.value[key][field] = !authority.value[key][field]
}
</script>

<template>
  <section>
    <el-table
      :data="rows"
      row-key="id"
      :tree-props="{ children: 'children' }"
      :expand-row-keys="expandedRowKeys"
      :border="true"
      size="small">
      <el-table-column prop="label" label="字段" />
      <el-table-column label="可见" width="60" align="center">
        <template #default="scope">
          <el-checkbox v-model="authority[scope.row.id].viewable" v-if="authority[scope.row.id]" />
        </template>
      </el-table-column>
      <el-table-column label="可编辑" width="70" align="center">
        <template #default="scope">
          <el-checkbox v-model="authority[scope.row.id].editable" v-if="scope.row.editable" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="60" align="center">
        <template #default="scope">
          <el-popover trigger="click" placement="left" :width="300">
            <template #reference>
              <LayoutIcon name="Setting" class="fs-authority-setting" />
            </template>
            <el-form label-width="60px" class="fs-authority-action">
              <el-form-item label="标识：">{{ scope.row.id }}</el-form-item>
              <el-form-item label="字段：">{{ scope.row.field }}</el-form-item>
              <el-form-item label="标签：">{{ scope.row.label }}</el-form-item>
              <el-form-item label="配置：" v-if="scope.row.editable">
                <el-checkbox v-model="authority[scope.row.id].variable">设为流程变量</el-checkbox>
                <el-checkbox v-model="authority[scope.row.id].addable" v-if="scope.row.type === 'subform'">可新增记录</el-checkbox>
                <el-checkbox v-model="authority[scope.row.id].changeable" v-if="scope.row.type === 'subform'">可编辑已有记录</el-checkbox>
                <el-checkbox v-model="authority[scope.row.id].removable" v-if="scope.row.type === 'subform'">可删除已有记录</el-checkbox>
              </el-form-item>
            </el-form>
          </el-popover>
        </template>
      </el-table-column>
    </el-table>
    <el-row class="fs-authority-ctr">
      <el-col :span="12">
        可见：<el-button link type="primary" @click="checkedAll('viewable')">全选</el-button>/<el-button link type="primary" @click="checkedRevert('viewable')">反选</el-button>
      </el-col>
      <el-col :span="12">
        可编辑：<el-button link type="primary" @click="checkedAll('editable')">全选</el-button>/<el-button link type="primary" @click="checkedRevert('editable')">反选</el-button>
      </el-col>
    </el-row>
  </section>
</template>

<style lang="scss" scoped>
.fs-authority-setting {
  cursor: pointer;
  &:hover {
    color: var(--el-color-primary);
  }
}
.fs-authority-action {
  :deep(.el-form-item) {
    margin-bottom: 0px;
  }
  :deep(.el-checkbox) {
    display: block;
    margin-right: 0px;
  }
}
.fs-authority-ctr {
  padding: 5px 2px;
  font-size: 12px;
  .el-button {
    padding: 0px 2px;
  }
}
</style>
