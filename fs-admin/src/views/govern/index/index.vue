<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import ApiUtil from '@/utils/ApiUtil'
import MetaApi from '@/api/govern/MetaApi'
import SourceApi from '@/api/govern/SourceApi'
import StandardApi from '@/api/govern/StandardApi'
import AssessApi from '@/api/govern/AssessApi'
import QualityPlanApi from '@/api/govern/QualityPlanApi'
import LayoutHeading from '@/components/Layout/LayoutHeading.vue'
import GovernPageShell from '@/views/govern/components/GovernPageShell.vue'

const router = useRouter()
const loading = ref(false)
const keyword = ref('')
const statistic = ref<any>({
  modelCount: 0,
  relationCount: 0,
  relationModelCount: 0,
  relationColumnCount: 0,
})
const counts = ref<any>({
  source: 0,
  standard: 0,
  assess: 0,
  plan: 0,
})

const total = (result: any) => Number(ApiUtil.data(result)?.total ?? 0)

const load = async () => {
  loading.value = true
  const tasks = await Promise.allSettled([
    MetaApi.statistic({}, { warning: false }),
    SourceApi.list({ page: 1, pageSize: 1 }, { warning: false }),
    StandardApi.list({ page: 1, pageSize: 1 }, { warning: false }),
    AssessApi.list({ page: 1, pageSize: 1 }, { warning: false }),
    QualityPlanApi.list({ page: 1, pageSize: 1 }, { warning: false }),
  ])
  if (tasks[0].status === 'fulfilled') statistic.value = ApiUtil.data(tasks[0].value) ?? statistic.value
  if (tasks[1].status === 'fulfilled') counts.value.source = total(tasks[1].value)
  if (tasks[2].status === 'fulfilled') counts.value.standard = total(tasks[2].value)
  if (tasks[3].status === 'fulfilled') counts.value.assess = total(tasks[3].value)
  if (tasks[4].status === 'fulfilled') counts.value.plan = total(tasks[4].value)
  loading.value = false
}

const metrics = computed(() => [
  { label: '元数据模型', value: statistic.value.modelCount ?? 0, hint: '已纳管的数据表与模型', path: '/govern/meta/model' },
  { label: '模型关系', value: statistic.value.relationCount ?? 0, hint: '模型与字段之间的关联', path: '/govern/meta/modelRelation' },
  { label: '数据标准', value: counts.value.standard, hint: '目录和字段标准', path: '/govern/standard/list' },
  { label: '数据源', value: counts.value.source, hint: '评估与质检连接对象', path: '/govern/system/source' },
])

const modules = [
  {
    title: '元数据管理',
    description: '建立模型、维护字段关系，并从元数据地图进入检索、血缘与影响分析。',
    items: [
      { label: '数据地图', path: '/govern/meta/map' },
      { label: '模型管理', path: '/govern/meta/model' },
      { label: '模型关系', path: '/govern/meta/modelRelation' },
      { label: '元数据检索', path: '/govern/meta/list' },
      { label: '血缘分析', path: '/govern/meta/blood' },
    ],
  },
  {
    title: '数据标准',
    description: '维护业务与技术标准，配置评估方案并追踪字段落地差异。',
    items: [
      { label: '标准管理', path: '/govern/standard/list' },
      { label: '落地评估', path: '/govern/standard/assess' },
      { label: '评估结果', path: '/govern/standard/log' },
    ],
  },
  {
    title: '数据质量',
    description: '按分类组织质检规则，组合质检方案并生成可追溯的检查报告。',
    items: [
      { label: '质检分类', path: '/govern/quality/logic' },
      { label: '质检规则', path: '/govern/quality/rule' },
      { label: '质检方案', path: '/govern/quality/plan' },
      { label: '质检报告', path: '/govern/quality/log' },
    ],
  },
  {
    title: '系统配置',
    description: '维护标准评估和质量检查所使用的数据源连接。',
    items: [
      { label: '数据源管理', path: '/govern/system/source' },
      { label: '运行维护', path: '/govern/setting/state' },
    ],
  },
]

const search = () => {
  router.push({ path: '/govern/meta/list', query: { keyword: keyword.value } }).catch(() => {})
}

onMounted(load)
</script>

<template>
  <GovernPageShell title="数据治理" description="围绕元数据、数据标准与数据质量建立可检索、可评估、可追踪的治理闭环">

  <el-card shadow="never" class="overview">
    <div class="overview__content">
      <div>
        <div class="overview__eyebrow">治理工作台</div>
        <h3>从资产目录到质量报告，统一进入治理流程</h3>
        <p>维护模型与关系，依据数据标准执行落地评估，再通过质检规则发现数据问题并留痕。</p>
      </div>
      <el-input v-model="keyword" size="large" clearable placeholder="检索模型或字段名称" @keyup.enter="search">
        <template #append><el-button @click="search">检索</el-button></template>
      </el-input>
    </div>
  </el-card>

  <div class="metric-grid" v-loading="loading">
    <el-card
      v-for="item in metrics"
      :key="item.label"
      shadow="hover"
      class="metric-card"
      @click="router.push(item.path)"
    >
      <div class="metric-card__label">{{ item.label }}</div>
      <div class="metric-card__value">{{ item.value }}</div>
      <div class="metric-card__hint">{{ item.hint }}</div>
    </el-card>
  </div>

  <div class="module-grid">
    <el-card v-for="module in modules" :key="module.title" shadow="never" class="module-card">
      <template #header>
        <div class="module-card__header">
          <span>{{ module.title }}</span>
          <el-tag effect="plain" type="info">{{ module.items.length }} 项</el-tag>
        </div>
      </template>
      <p>{{ module.description }}</p>
      <div class="module-card__links">
        <el-button
          v-for="item in module.items"
          :key="item.path"
          text
          @click="router.push(item.path)"
        >
          {{ item.label }}
        </el-button>
      </div>
    </el-card>
  </div>

  <el-card shadow="never" class="mt-20">
    <LayoutHeading title="治理运行信息" />
    <el-descriptions :column="4" border>
      <el-descriptions-item label="模型关系">{{ statistic.relationModelCount ?? 0 }}</el-descriptions-item>
      <el-descriptions-item label="字段关系">{{ statistic.relationColumnCount ?? 0 }}</el-descriptions-item>
      <el-descriptions-item label="落地评估方案">{{ counts.assess }}</el-descriptions-item>
      <el-descriptions-item label="质检方案">{{ counts.plan }}</el-descriptions-item>
    </el-descriptions>
  </el-card>
  </GovernPageShell>
</template>

<style scoped>
.overview {
  border: 0;
  background:
    radial-gradient(circle at 90% 10%, rgba(64, 158, 255, 0.18), transparent 34%),
    linear-gradient(135deg, #f8fbff, #eef5ff);
}
.overview__content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 40px;
  min-height: 150px;
}
.overview__content > div { flex: 1; }
.overview__content :deep(.el-input) { width: min(430px, 42vw); }
.overview__eyebrow { margin-bottom: 12px; color: var(--el-color-primary); font-size: 13px; font-weight: 600; }
.overview h3 { margin-bottom: 10px; font-size: 20px; color: #1f2d3d; }
.overview p { color: #606266; line-height: 1.8; }
.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 15px;
  margin-top: 15px;
}
.metric-card { cursor: pointer; }
.metric-card__label { color: #606266; font-size: 13px; }
.metric-card__value { margin: 10px 0 6px; font-size: 32px; font-weight: 700; color: #1f2d3d; }
.metric-card__hint { color: #909399; font-size: 12px; }
.module-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 15px;
  margin-top: 15px;
}
.module-card p { min-height: 46px; color: #606266; line-height: 1.7; }
.module-card__header { display: flex; align-items: center; justify-content: space-between; font-weight: 600; }
.module-card__links { display: flex; flex-wrap: wrap; gap: 4px; margin-top: 16px; }
.module-card__links :deep(.el-button + .el-button) { margin-left: 0; }
@media (max-width: 1100px) {
  .metric-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .module-grid { grid-template-columns: 1fr; }
  .overview__content { align-items: flex-start; flex-direction: column; gap: 20px; }
  .overview__content :deep(.el-input) { width: 100%; }
}
</style>
