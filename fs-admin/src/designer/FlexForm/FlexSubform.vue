<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import DesignUtil from '@/utils/DesignUtil'
import FlexForm from './FlexForm.vue'

const model: any = defineModel()
const {
  config = {} as any,
  subform = {} as any,
  authority = {} as any,
} = defineProps<{
  config: any,
  subform: any,
  authority: any,
}>()

const info = computed(() => subform.options.formInfo || {})
const widgets = computed(() => DesignUtil.frameWidgets(info.value))

const columns = computed(() => {
  const sorted = config.exhibition.mergeColumnItem(
    config.exhibition.operateFields(widgets.value, 'viewable', true),
    config.exhibition.parseColumnSorted(subform.options.column || DesignUtil.frameOptions(info.value).column)
  )
  return config.exhibition.tableColumns(sorted)
})

const rows = computed(() => {
  const all = [config.idField].concat(widgets.value)
  return config.validator.pretty(all, model.value || [])
})

const subformAuthority = computed(() => {
  const result = Object.assign({}, config.exhibition.authorityDefaults, authority[subform.id] || {})
  result.addable = result.addable && result.editable
  result.removable = result.removable && result.editable
  result.changeable = result.changeable && result.editable
  return result
})

const formAuthority = computed(() => {
  const fields = config.exhibition.authorityFields(widgets.value)
  const view: any = Object.assign({}, authority)
  fields.forEach((widget: any) => {
    view[widget.id] = { viewable: authority[widget.id]?.viewable }
  })
  return { fields, view, edit: Object.assign({}, authority) }
})

const infoVisible = ref(false)
const formVisible = ref(false)
const formLoading = ref(false)
const formRef = ref<any>()
const form = ref<any>({})

const handleAdd = () => {
  form.value = {}
  formVisible.value = true
}

const handleEdit = (scope: any) => {
  form.value = (model.value || [])[scope.$index]
  formVisible.value = true
}

const handleShow = (scope: any) => {
  form.value = (model.value || [])[scope.$index]
  infoVisible.value = true
}

const handleRemove = (scope: any) => {
  const records = model.value || []
  for (let index = records.length - 1; index >= 0; index--) {
    if (records[index]._id === scope.row._id) {
      records.splice(index, 1)
      return true
    }
  }
  return false
}

const handleSubmit = (force: boolean) => {
  formRef.value?.validate((valid: boolean) => {
    if (!valid) {
      ElMessage.warning('数据校验不通过')
      if (!force) return
    }
    formVisible.value = false
    const records = model.value || []
    if (form.value._id) {
      for (const index in records) {
        if (records[index]._id === form.value._id) {
          records.splice(Number(index), 1, form.value)
          return
        }
      }
      return
    }
    records.push(Object.assign({ _id: config.uuidRecord() }, form.value))
  })
}
</script>

<template>
  <section class="fs-subform">
    <el-table :data="rows" :row-key="(record: any) => record._id" :border="true" size="small">
      <TableColumn :columns="columns" />
      <el-table-column label="操作" width="180">
        <template #default="scope">
          <el-button link @click="() => handleShow(scope)">查看</el-button>
          <el-button link v-if="subformAuthority.changeable" @click="() => handleEdit(scope)">编辑</el-button>
          <el-button link type="danger" v-if="subformAuthority.removable" @click="() => handleRemove(scope)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-button link type="primary" v-if="subformAuthority.addable" @click="handleAdd">添加</el-button>

    <el-dialog v-model="infoVisible" :title="`信息查看 - ${form._id}`" :destroy-on-close="true" width="818">
      <FlexForm v-model="form" :config="config" :frame="info" :authority="formAuthority.view" />
      <template #footer>
        <el-button @click="infoVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="formVisible"
      :title="form._id ? `信息修改 - ${form._id}` : '信息添加'"
      :close-on-click-modal="false"
      :destroy-on-close="true"
      width="818">
      <FlexForm ref="formRef" v-model="form" :config="config" :frame="info" :authority="formAuthority.edit" />
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="formLoading" @click="() => handleSubmit(false)">确定</el-button>
        <el-button type="danger" :loading="formLoading" @click="() => handleSubmit(true)">强制提交</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style lang="scss" scoped>
</style>
