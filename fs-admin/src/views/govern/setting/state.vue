<script setup lang="ts">
import MaintainApi from '@/api/govern/MaintainApi'
import FormMaintainBox from '@/components/Form/FormMaintainBox.vue'
import GovernPageShell from '@/views/govern/components/GovernPageShell.vue'

const items = [
  {
    key: 'elasticsearch',
    title: '元数据检索索引重建',
    description: '读取元模型和字段数据，重建 Elasticsearch 检索索引并清理历史版本。',
    api: MaintainApi.reindexES,
    params: () => ({ time: Date.now() }),
  },
  {
    key: 'neo4j',
    title: '元数据关系图重建',
    description: '重建 Neo4j 模型节点、字段节点和关系，用于元数据血缘及影响分析。',
    api: MaintainApi.reindexNeo4j,
    params: () => ({ time: Date.now() }),
  },
]
</script>

<template>
  <GovernPageShell
    title="运行维护"
    description="同步重建元数据检索索引和关系图，修复检索缺失、血缘不一致等运行问题"
  >
    <el-alert
      type="warning"
      :closable="false"
      show-icon
      title="维护任务会读取当前元模型数据并重新生成检索索引"
      description="任务执行期间可停止任务，但已写入的新版本索引会保留。建议在元模型批量维护完成后执行。"
    />
    <el-card shadow="never" class="maintain-panel">
      <template #header>
        <div class="maintain-panel__header">
          <div>
            <strong>索引与关系图维护</strong>
            <span>选择左侧维护项，执行进度和日志将在右侧实时显示。</span>
          </div>
          <el-tag effect="plain" type="info">管理员操作</el-tag>
        </div>
      </template>
      <FormMaintainBox :items="items" height="360px" />
    </el-card>
  </GovernPageShell>
</template>

<style scoped>
.maintain-panel__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}
.maintain-panel__header > div {
  display: flex;
  flex-direction: column;
  gap: 3px;
}
.maintain-panel__header strong {
  color: var(--el-text-color-primary);
  font-size: 15px;
}
.maintain-panel__header span {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  font-weight: 400;
}
.maintain-panel :deep(.el-card__body) {
  padding: 0;
}
.maintain-panel :deep(.form-maintain-box__container) {
  min-height: 440px;
}
.maintain-panel :deep(.form-maintain-box__aside) {
  padding: 18px 16px;
}
@media (max-width: 760px) {
  .maintain-panel__header {
    align-items: flex-start;
  }
  .maintain-panel :deep(.form-maintain-box__container) {
    display: block;
  }
  .maintain-panel :deep(.form-maintain-box__aside) {
    width: 100% !important;
    padding: 12px 16px;
    border-right: none;
    border-bottom: 1px solid var(--el-border-color-light);
  }
  .maintain-panel :deep(.form-maintain-box__main) {
    padding: 12px 0 0;
  }
}
</style>
