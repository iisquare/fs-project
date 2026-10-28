<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ModelApi from '@/api/govern/ModelApi'
import ApiUtil from '@/utils/ApiUtil'
import LayoutHeading from '@/components/Layout/LayoutHeading.vue'
import GovernPageShell from '@/views/govern/components/GovernPageShell.vue'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const form = ref<any>({ columns: [], pk: [] })
const catalog = String(route.query.catalog || '/')
const code = String(route.query.code || '')
const matchedColumn = String(route.query.column || '')

const pkMap = computed(() => {
  const result: Record<string, string> = {}
  ;(form.value.pk ?? []).forEach((item: string, index: number) => result[item] = `PK-${index}`)
  return result
})

const load = () => {
  if (!code) return
  loading.value = true
  ModelApi.info({ catalog, code }, { warning: false }).then((result: any) => {
    form.value = ApiUtil.data(result) ?? form.value
  }).catch(() => {}).finally(() => {
    loading.value = false
  })
}

const go = (path: string, extra: any = {}) => {
  router.push({ path, query: { catalog, code, column: matchedColumn, ...extra } }).catch(() => {})
}

onMounted(load)
</script>

<template>
  <GovernPageShell
    :title="code ? `${catalog}${code}` : '元数据详情'"
    description="查看元模型基础信息、字段结构和上下游分析入口"
    back="/govern/meta/list"
  >
    <template #actions>
      <el-button @click="go('/govern/meta/blood')">血缘分析</el-button>
      <el-button @click="go('/govern/meta/influence', { model: code })">影响分析</el-button>
      <el-button @click="router.push({ path: '/govern/meta/modelRelation', query: { catalog, model: code } })">关联关系</el-button>
    </template>

  <el-card shadow="never" v-loading="loading">
    <LayoutHeading title="基础信息" />
    <el-descriptions :column="3" border>
      <el-descriptions-item label="所属包">{{ form.catalog }}</el-descriptions-item>
      <el-descriptions-item label="模型编码">{{ form.code }}</el-descriptions-item>
      <el-descriptions-item label="模型名称">{{ form.name }}</el-descriptions-item>
      <el-descriptions-item label="模型类型">{{ form.type === 'catalog' ? '模型包' : '元数据模型' }}</el-descriptions-item>
      <el-descriptions-item label="模型主键">{{ (form.pk ?? []).join(', ') || '暂无' }}</el-descriptions-item>
      <el-descriptions-item label="排序">{{ form.sort }}</el-descriptions-item>
      <el-descriptions-item label="描述" :span="3">{{ form.description || '暂无' }}</el-descriptions-item>
    </el-descriptions>
  </el-card>

  <el-card shadow="never" class="mt-15" v-if="form.type !== 'catalog'">
    <LayoutHeading title="字段信息" :description="`共 ${form.columns?.length ?? 0} 个字段`" />
    <el-table :data="form.columns" row-key="code" border>
      <el-table-column label="字段编码" min-width="150">
        <template #default="{ row }">
          <span :class="{ matched: row.code === matchedColumn }">{{ row.code }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="name" label="字段名称" min-width="140" />
      <el-table-column prop="type" label="类型" min-width="110" />
      <el-table-column prop="size" label="长度" width="80" />
      <el-table-column prop="digit" label="小数位" width="80" />
      <el-table-column label="允许为空" width="95">
        <template #default="{ row }">{{ row.nullable ? '是' : '否' }}</template>
      </el-table-column>
      <el-table-column label="主键" width="90">
        <template #default="{ row }">{{ pkMap[row.code] || '' }}</template>
      </el-table-column>
      <el-table-column prop="description" label="描述" min-width="180" show-overflow-tooltip />
      <el-table-column label="操作" width="150">
        <template #default="{ row }">
          <el-button link @click="router.push({ path: '/govern/meta/blood', query: { catalog, code, column: row.code } })">血缘</el-button>
          <el-button link @click="router.push({ path: '/govern/meta/influence', query: { catalog, model: code, code: row.code } })">影响</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
  </GovernPageShell>
</template>

<style scoped>
.matched { color: var(--el-color-danger); font-weight: 600; }
</style>
