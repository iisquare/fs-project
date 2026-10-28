<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessage, type FormInstance } from 'element-plus'
import { Plus, Top, Edit, Delete, Back } from '@element-plus/icons-vue'

const columns = defineModel<any[]>({ default: () => [] })
const props = defineProps<{
  columnTypes?: any
}>()

const selectedIndex = ref(-1)
const formVisible = ref(false)
const form = ref<any>({})
const formRef = ref<FormInstance>()
const mode = ref<'add' | 'insert' | 'edit'>('add')
const rules = {
  code: [{ required: true, message: '请输入字段编码', trigger: 'blur' }],
  type: [{ required: true, message: '请选择字段类型', trigger: 'change' }],
}

const typeOptions = computed(() => Object.values(props.columnTypes ?? {}).map(String))

const resetForm = (source: any = {}) => {
  form.value = Object.assign({
    code: '',
    name: '',
    type: '',
    size: 0,
    digit: 0,
    nullable: false,
    description: '',
  }, JSON.parse(JSON.stringify(source)))
}

const open = (nextMode: 'add' | 'insert' | 'edit') => {
  if (nextMode === 'edit' && selectedIndex.value < 0) {
    return ElMessage.warning('请先选择字段')
  }
  mode.value = nextMode
  resetForm(nextMode === 'edit' ? columns.value[selectedIndex.value] : {})
  formVisible.value = true
}

const submit = () => {
  formRef.value?.validate((valid: boolean) => {
    if (!valid) return
    const duplicate = columns.value.some((item: any, index: number) =>
      item.code === form.value.code && (mode.value !== 'edit' || index !== selectedIndex.value))
    if (duplicate) {
      ElMessage.warning('字段编码不能重复')
      return
    }
    const value = JSON.parse(JSON.stringify(form.value))
    if (mode.value === 'edit') {
      columns.value.splice(selectedIndex.value, 1, value)
    } else if (mode.value === 'insert') {
      columns.value.splice(Math.max(0, selectedIndex.value), 0, value)
      selectedIndex.value = Math.max(0, selectedIndex.value)
    } else {
      columns.value.push(value)
      selectedIndex.value = columns.value.length - 1
    }
    formVisible.value = false
  })
}

const move = (offset: number) => {
  if (selectedIndex.value < 0) {
    ElMessage.warning('请先选择字段')
    return
  }
  const next = selectedIndex.value + offset
  if (next < 0 || next >= columns.value.length) return
  const [item] = columns.value.splice(selectedIndex.value, 1)
  columns.value.splice(next, 0, item)
  selectedIndex.value = next
}

const remove = () => {
  if (selectedIndex.value < 0) {
    ElMessage.warning('请先选择字段')
    return
  }
  columns.value.splice(selectedIndex.value, 1)
  selectedIndex.value = Math.min(selectedIndex.value, columns.value.length - 1)
}
</script>

<template>
  <div class="column-editor">
    <div class="column-editor__actions">
      <el-button :icon="Plus" @click="open('add')">添加</el-button>
      <el-button :icon="Top" @click="open('insert')">插入</el-button>
      <el-button :icon="Edit" @click="open('edit')">编辑</el-button>
      <el-button :icon="Back" @click="move(-1)">上移</el-button>
      <el-button :icon="Back" @click="move(1)">下移</el-button>
      <el-button :icon="Delete" type="danger" plain @click="remove">删除</el-button>
      <el-button type="danger" text @click="columns = []">清空</el-button>
    </div>
    <el-table
      :data="columns"
      border
      highlight-current-row
      row-key="code"
      empty-text="暂无字段，请添加字段属性"
      @current-change="(row: any) => selectedIndex = columns.indexOf(row)"
    >
      <el-table-column prop="code" label="字段编码" min-width="140" />
      <el-table-column prop="name" label="字段名称" min-width="140" />
      <el-table-column prop="type" label="字段类型" min-width="120" />
      <el-table-column prop="size" label="字段长度" width="100" />
      <el-table-column prop="digit" label="小数位数" width="100" />
      <el-table-column label="允许为空" width="100">
        <template #default="{ row }">
          <el-tag :type="row.nullable ? 'success' : 'info'" effect="plain">{{ row.nullable ? '是' : '否' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
    </el-table>

    <el-dialog v-model="formVisible" :title="mode === 'edit' ? '编辑字段' : '添加字段'" width="680px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-row :gutter="18">
          <el-col :span="12">
            <el-form-item label="字段编码" prop="code">
              <el-input v-model="form.code" placeholder="字母、数字、下划线" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="字段名称">
              <el-input v-model="form.name" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="字段类型" prop="type">
              <el-select v-model="form.type" filterable allow-create default-first-option>
                <el-option v-for="item in typeOptions" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="字段长度">
              <el-input-number v-model="form.size" :min="0" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="小数位数">
              <el-input-number v-model="form.digit" :min="0" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="允许为空">
              <el-switch v-model="form.nullable" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="描述">
              <el-input v-model="form.description" type="textarea" :rows="3" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-space>
          <el-button type="primary" @click="submit">确定</el-button>
          <el-button @click="formVisible = false">取消</el-button>
        </el-space>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.column-editor__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 14px;
}
.column-editor__actions :deep(.el-button + .el-button) {
  margin-left: 0;
}
</style>
