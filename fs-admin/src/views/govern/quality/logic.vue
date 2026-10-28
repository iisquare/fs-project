<script setup lang="ts">
import { nextTick, onMounted, ref } from 'vue'
import type { FormInstance, TableInstance } from 'element-plus'
import { Expand, Fold } from '@element-plus/icons-vue'
import QualityLogicApi from '@/api/govern/QualityLogicApi'
import ApiUtil from '@/utils/ApiUtil'
import TableUtil from '@/utils/TableUtil'
import GovernPageShell from '@/views/govern/components/GovernPageShell.vue'

const tableRef = ref<TableInstance>()
const loading = ref(false)
const rows = ref<any[]>([])
const selection = ref<any[]>([])
const config = ref<any>({ status: {} })
const expanded = ref<number[]>([])
const columns = ref([
  { prop: 'name', label: '分类名称', minWidth: 220, slot: 'name' },
  { prop: 'description', label: '描述', minWidth: 260, showOverflowTooltip: true },
  { prop: 'sort', label: '排序', width: 80 },
  { prop: 'statusText', label: '状态', width: 90, slot: 'status' },
])

const collectIds = (items: any[]): number[] => items.flatMap((item: any) => [item.id, ...collectIds(item.children ?? [])])

const toggleExpand = () => {
  expanded.value = expanded.value.length > 0 ? [] : collectIds(rows.value)
  nextTick(() => {
    const walk = (items: any[]) => items.forEach(item => {
      if (items.length > 0) tableRef.value?.toggleRowExpansion(item, expanded.value.length > 0)
      walk(item.children ?? [])
    })
    walk(rows.value)
  })
}

const refresh = () => {
  loading.value = true
  QualityLogicApi.tree({}, { warning: false }).then((result: any) => {
    rows.value = ApiUtil.data(result) ?? []
    if (expanded.value.length === 0) expanded.value = collectIds(rows.value)
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}

const formVisible = ref(false)
const formLoading = ref(false)
const formRef = ref<FormInstance>()
const form = ref<any>({})
const rules = {
  name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
}

const edit = (row?: any, parentId = 0) => {
  form.value = Object.assign({
    parentId,
    sort: 0,
    status: '1',
  }, row, row?.status == null ? {} : { status: String(row.status) })
  formVisible.value = true
}

const addChild = (row: any) => edit(undefined, row.id)

const save = () => {
  formRef.value?.validate((valid: boolean) => {
    if (!valid || formLoading.value) return
    formLoading.value = true
    QualityLogicApi.save(form.value, { success: true }).then(() => {
      formVisible.value = false
      refresh()
    }).catch(() => {}).finally(() => {
      formLoading.value = false
    })
  })
}

const remove = () => {
  if (selection.value.length === 0) return
  TableUtil.selection(selection.value).then((ids: any) => {
    loading.value = true
    QualityLogicApi.delete(ids, { success: true }).then(refresh).catch(() => {
      loading.value = false
    })
  }).catch(() => {})
}

onMounted(() => {
  refresh()
  QualityLogicApi.config({ warning: false }).then((result: any) => {
    config.value = ApiUtil.data(result) ?? config.value
  }).catch(() => {})
})
</script>

<template>
  <GovernPageShell title="质检分类" description="按业务维度组织质检规则，支持多级分类和启停管理">
  <el-card shadow="never" class="fs-table-card">
    <div class="fs-table-toolbar flex-between">
      <el-space>
        <el-button :icon="expanded.length ? Fold : Expand" @click="toggleExpand">{{ expanded.length ? '收起' : '展开' }}</el-button>
        <button-add v-permit="'govern:qualityLogic:add'" @click="edit()" />
        <button-delete v-permit="'govern:qualityLogic:delete'" :disabled="selection.length === 0" @click="remove" />
      </el-space>
      <el-space>
        <button-refresh :loading="loading" @click="refresh" />
        <TableColumnSetting v-model="columns" :table="tableRef" :loading="loading" />
      </el-space>
    </div>
    <el-table
      ref="tableRef"
      v-loading="loading"
      :data="rows"
      row-key="id"
      border
      default-expand-all
      @selection-change="(value: any[]) => selection = value"
    >
      <el-table-column type="selection" width="45" />
      <TableColumn :columns="columns">
        <template #name="scope">
          <span>{{ scope.row.name }}</span>
          <span class="category-code">#{{ scope.row.id }}</span>
        </template>
        <template #status="scope">
          <el-tag :type="scope.row.status === 1 ? 'success' : 'info'" effect="plain">
            {{ scope.row.statusText || config.status?.[scope.row.status] }}
          </el-tag>
        </template>
      </TableColumn>
      <el-table-column label="操作" width="170">
        <template #default="{ row }">
          <el-button link v-permit="'govern:qualityLogic:add'" @click="addChild(row)">子级</el-button>
          <el-button link v-permit="'govern:qualityLogic:modify'" @click="edit(row)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <el-dialog v-model="formVisible" :title="form.id ? '编辑质检分类' : '新增质检分类'" width="520px">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
      <el-form-item label="父级 ID"><el-input-number v-model="form.parentId" :min="0" /></el-form-item>
      <el-form-item label="分类名称" prop="name"><el-input v-model="form.name" /></el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="form.status">
          <el-option v-for="(value, key) in config.status" :key="key" :label="value" :value="key" />
        </el-select>
      </el-form-item>
      <el-form-item label="排序"><el-input-number v-model="form.sort" :min="0" /></el-form-item>
      <el-form-item label="描述"><el-input v-model="form.description" type="textarea" :rows="3" /></el-form-item>
    </el-form>
    <template #footer>
      <el-space>
        <el-button type="primary" :loading="formLoading" @click="save">保存</el-button>
        <el-button @click="formVisible = false">取消</el-button>
      </el-space>
    </template>
  </el-dialog>
  </GovernPageShell>
</template>

<style scoped>
.category-code { margin-left: 8px; color: #a8abb2; font-size: 12px; }
</style>
