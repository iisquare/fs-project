<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { FormInstance } from 'element-plus'
import { ElMessage } from 'element-plus'
import ModelApi from '@/api/govern/ModelApi'
import ModelCompareApi from '@/api/govern/ModelCompareApi'
import ApiUtil from '@/utils/ApiUtil'
import LayoutHeading from '@/components/Layout/LayoutHeading.vue'
import ModelColumnEditor from '@/views/govern/components/ModelColumnEditor.vue'
import GovernPageShell from '@/views/govern/components/GovernPageShell.vue'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const activeTab = ref('csv')
const sourceColumns = ref<any[]>([])
const selectedSource = ref<any[]>([])
const config = ref<any>({ columnTypes: [] })
const formRef = ref<FormInstance>()
const form = ref<any>({
  type: 'table',
  catalog: String(route.query.catalog || '/'),
  code: route.query.code,
  name: '',
  pk: [],
  sort: 0,
  description: '',
  columns: [],
})
const rules = {
  catalog: [{ required: true, message: '所属包不能为空', trigger: 'blur' }],
  code: [{ required: true, message: '请输入模型编码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入模型名称', trigger: 'blur' }],
}

const csv = ref<any>({
  glue: '\t',
  charset: 'UTF-8',
})
const jdbc = ref({
  url: '',
  username: '',
  password: '',
})
const tables = ref<any[]>([])
const selectedTable = ref<any>(null)
const useRemarkAsName = ref(true)

const targetMap = computed(() => new Map(form.value.columns.map((item: any) => [item.code, item])))
const comparedSource = computed(() => sourceColumns.value.map((item: any) => {
  const target: any = targetMap.value.get(item.code)
  let state = 'add'
  if (target) {
    state = ['type', 'size', 'digit', 'nullable'].every(key => String(target[key] ?? '') === String(item[key] ?? ''))
      ? 'same'
      : 'differ'
  }
  return Object.assign({}, item, { state })
}))

const load = () => {
  if (!form.value.code) return
  loading.value = true
  ModelApi.info({ catalog: form.value.catalog, code: form.value.code }, { warning: false }).then((result: any) => {
    const data = ApiUtil.data(result) ?? {}
    form.value = Object.assign({}, form.value, data, {
      pk: Array.isArray(data.pk) ? data.pk : String(data.pk || '').split(',').filter(Boolean),
      columns: (data.columns ?? []).map((item: any) => Object.assign({}, item, { nullable: Boolean(item.nullable) })),
    })
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}

const parseDelimited = (text: string, glue: string) => {
  const rows: string[][] = []
  let row: string[] = []
  let cell = ''
  let quoted = false
  for (let index = 0; index < text.length; index++) {
    const char = text[index]
    if (char === '"') {
      if (quoted && text[index + 1] === '"') {
        cell += '"'
        index++
      } else {
        quoted = !quoted
      }
    } else if (!quoted && text.startsWith(glue, index)) {
      row.push(cell)
      cell = ''
      index += glue.length - 1
    } else if (!quoted && (char === '\n' || char === '\r')) {
      if (char === '\r' && text[index + 1] === '\n') index++
      row.push(cell)
      rows.push(row)
      row = []
      cell = ''
    } else {
      cell += char
    }
  }
  if (cell || row.length) {
    row.push(cell)
    rows.push(row)
  }
  return rows.filter(item => item.some(value => value !== ''))
}

const parseCsv = async (file: any) => {
  if (!file.raw) return
  try {
    const buffer = await file.raw.arrayBuffer()
    const text = new TextDecoder(csv.value.charset || 'utf-8').decode(buffer)
    const glue = String(csv.value.glue || '\t').replace(/\\t/g, '\t')
    const rows = parseDelimited(text, glue)
    if (rows.length < 2) return ElMessage.warning('文件没有字段数据')
    const headers = rows[0].map(item => item.trim())
    const required = ['code', 'name', 'type', 'size', 'digit', 'nullable', 'description']
    if (required.some(key => !headers.includes(key))) {
      return ElMessage.warning(`表头必须包含：${required.join(',')}`)
    }
    const indexes: Record<string, number> = {}
    headers.forEach((key, index) => indexes[key] = index)
    const result = rows.slice(1).map((row, index) => {
      const code = String(row[indexes.code] ?? '').trim()
      if (!code) throw new Error(`第 ${index + 2} 行字段编码为空`)
      return {
        code,
        name: String(row[indexes.name] ?? '').trim(),
        type: String(row[indexes.type] ?? '').trim(),
        size: Number(row[indexes.size] || 0),
        digit: Number(row[indexes.digit] || 0),
        nullable: Number(row[indexes.nullable] || 0) !== 0,
        description: String(row[indexes.description] ?? ''),
      }
    })
    if (new Set(result.map(item => item.code)).size !== result.length) {
      return ElMessage.warning('文件中存在重复字段编码')
    }
    sourceColumns.value = result
    ElMessage.success(`已解析 ${result.length} 个字段`)
  } catch (error: any) {
    ElMessage.error(error?.message || '文件解析失败')
  }
}

const loadJdbc = () => {
  if (!jdbc.value.url) return ElMessage.warning('请输入 JDBC 连接串')
  loading.value = true
  ModelCompareApi.jdbc(jdbc.value, { warning: false }).then((result: any) => {
    const data = ApiUtil.data(result) ?? {}
    tables.value = Object.values(data).map((table: any) => ({
      key: table.name,
      name: table.name,
      columns: Object.values(table.columns ?? {}),
    }))
  }).catch((result: any) => {
    ElMessage.warning(ApiUtil.message(result) || '载入数据库结构失败')
  }).finally(() => {
    loading.value = false
  })
}

const useTable = () => {
  if (!selectedTable.value) return ElMessage.warning('请选择数据表')
  sourceColumns.value = (selectedTable.value.columns ?? []).map((item: any) => ({
    code: item.name,
    name: useRemarkAsName.value ? (item.remark || '') : '',
    type: item.type,
    size: item.size,
    digit: item.digit,
    nullable: Boolean(item.nullable),
    description: item.remark || '',
  }))
  ElMessage.success(`已载入 ${sourceColumns.value.length} 个字段`)
}

const copySource = (replace: boolean) => {
  if (selectedSource.value.length === 0) return ElMessage.warning('请选择待导入字段')
  if (replace) form.value.columns = []
  const map = new Map(form.value.columns.map((item: any) => [item.code, item]))
  selectedSource.value.forEach((item: any) => {
    map.set(item.code, Object.assign({}, item, { state: undefined }))
  })
  form.value.columns = Array.from(map.values())
  ElMessage.success('已加入模型字段')
}

const save = () => {
  formRef.value?.validate((valid: boolean) => {
    if (!valid || loading.value) return
    loading.value = true
    ModelApi.save(form.value, { success: true }).then(() => {
      router.replace({
        path: '/govern/meta/draw',
        query: { catalog: form.value.catalog, code: form.value.code, type: form.value.type },
      }).catch(() => {})
    }).catch(() => {}).finally(() => {
      loading.value = false
    })
  })
}

onMounted(() => {
  load()
  ModelApi.config({ warning: false }).then((result: any) => {
    config.value = ApiUtil.data(result) ?? config.value
  }).catch(() => {})
})
</script>

<template>
  <GovernPageShell
    title="导入并对比模型字段"
    description="从文件或数据库读取字段结构，对比差异后加入目标元模型"
    back="/govern/meta/model"
    sticky
  >
    <template #actions>
      <el-button type="primary" :loading="loading" @click="save">保存模型</el-button>
    </template>

  <el-card shadow="never">
    <el-tabs>
      <el-tab-pane label="模型信息">
        <el-form ref="formRef" :model="form" :rules="rules" label-width="96px" class="basic-form">
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="所属包" prop="catalog">
                <el-input v-model="form.catalog" disabled />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="模型编码" prop="code">
                <el-input v-model="form.code" :disabled="!!route.query.code" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="模型名称" prop="name">
                <el-input v-model="form.name" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="模型主键">
                <el-select v-model="form.pk" multiple filterable allow-create>
                  <el-option v-for="item in form.columns" :key="item.code" :label="item.code" :value="item.code" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="24">
              <el-form-item label="描述">
                <el-input v-model="form.description" type="textarea" :rows="3" />
              </el-form-item>
            </el-col>
          </el-row>
        </el-form>
      </el-tab-pane>

      <el-tab-pane label="来源字段">
        <LayoutHeading title="选择导入来源" description="支持从 CSV/TXT 解析，或连接 MySQL 读取数据表结构" />
        <el-tabs v-model="activeTab" type="border-card">
          <el-tab-pane label="CSV / TXT" name="csv">
            <div class="source-form">
              <el-form :inline="true">
                <el-form-item label="分隔符">
                  <el-input v-model="csv.glue" style="width: 120px" placeholder="\\t 或 ," />
                </el-form-item>
                <el-form-item label="编码">
                  <el-select v-model="csv.charset" style="width: 130px">
                    <el-option label="UTF-8" value="UTF-8" />
                    <el-option label="GBK" value="GBK" />
                    <el-option label="GB2312" value="GB2312" />
                  </el-select>
                </el-form-item>
                <el-form-item>
                  <el-upload :auto-upload="false" :show-file-list="false" accept=".csv,.txt" :on-change="parseCsv">
                    <el-button type="primary">解析文件</el-button>
                  </el-upload>
                </el-form-item>
              </el-form>
              <el-alert
                type="info"
                :closable="false"
                show-icon
                title="表头格式"
                description="code,name,type,size,digit,nullable,description；nullable 使用 0 或 1。"
              />
            </div>
          </el-tab-pane>
          <el-tab-pane label="MySQL" name="mysql">
            <el-form label-width="80px" class="jdbc-form">
              <el-form-item label="连接串">
                <el-input v-model="jdbc.url" placeholder="jdbc:mysql://host:3306/database" />
              </el-form-item>
              <el-form-item label="用户">
                <el-input v-model="jdbc.username" />
              </el-form-item>
              <el-form-item label="密码">
                <el-input v-model="jdbc.password" type="password" show-password />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" :loading="loading" @click="loadJdbc">载入数据表</el-button>
                <el-checkbox v-model="useRemarkAsName">将字段备注作为名称</el-checkbox>
              </el-form-item>
            </el-form>
            <el-table :data="tables" border highlight-current-row @current-change="(row: any) => selectedTable = row">
              <el-table-column prop="name" label="数据表" />
              <el-table-column label="字段数" width="100">
                <template #default="{ row }">{{ row.columns.length }}</template>
              </el-table-column>
            </el-table>
            <el-button class="mt-10" type="primary" :disabled="!selectedTable" @click="useTable">使用所选表字段</el-button>
          </el-tab-pane>
        </el-tabs>

        <LayoutHeading title="来源字段对比" description="同名编码会与模型现有字段比较类型、长度、精度和可空属性" class="mt-20">
          <template #extra>
            <el-button :disabled="selectedSource.length === 0" @click="copySource(false)">加入模型</el-button>
            <el-button type="primary" :disabled="selectedSource.length === 0" @click="copySource(true)">替换模型字段</el-button>
          </template>
        </LayoutHeading>
        <el-table :data="comparedSource" border @selection-change="(value: any[]) => selectedSource = value">
          <el-table-column type="selection" width="45" />
          <el-table-column prop="code" label="字段编码" min-width="150" />
          <el-table-column prop="name" label="字段名称" min-width="140" />
          <el-table-column prop="type" label="类型" min-width="110" />
          <el-table-column prop="size" label="长度" width="80" />
          <el-table-column prop="digit" label="精度" width="80" />
          <el-table-column label="字符" width="90">
            <template #default="{ row }">{{ row.nullable ? '可空' : '非空' }}</template>
          </el-table-column>
          <el-table-column label="对比" width="95">
            <template #default="{ row }">
              <el-tag :type="row.state === 'same' ? 'success' : row.state === 'differ' ? 'warning' : 'primary'" effect="plain">
                {{ row.state === 'same' ? '一致' : row.state === 'differ' ? '不同' : '新增' }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="模型字段">
        <LayoutHeading title="模型字段" description="导入完成后可继续编辑、排序或补充字段" />
        <ModelColumnEditor v-model="form.columns" :column-types="config.columnTypes" />
      </el-tab-pane>
    </el-tabs>
  </el-card>
  </GovernPageShell>
</template>

<style scoped>
.basic-form, .jdbc-form { max-width: 980px; }
.source-form { margin-bottom: 18px; }
</style>
