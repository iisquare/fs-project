<script setup lang="ts">
/**
 * 流程状态标签
 *
 * @prop {Object} historic - 历史流程实例（endTime、deleteReason），有值时优先使用
 * @prop {Object} runtime  - 运行中流程实例（id、isSuspended），流程已结束时不存在
 * @prop {String} status   - 简写状态，兼容仅有关键字的场景
 */
defineProps<{
  historic?: any,
  runtime?: any,
  status?: string,
}>()

const statusMap: Record<string, { label: string, type: string }> = {
  running: { label: '运行中', type: 'success' },
  suspended: { label: '已挂起', type: 'warning' },
  ended: { label: '已结束', type: 'info' },
  revoked: { label: '已撤销', type: 'danger' },
  rejected: { label: '已驳回', type: 'danger' },
}
</script>

<template>
  <template v-if="historic">
    <el-tag type="info" size="small" v-if="historic.endTime && !historic.deleteReason">已结束</el-tag>
    <el-popover trigger="hover" placement="top" title="撤销原因" :width="200" v-else-if="historic.deleteReason">
      <template #reference><el-tag type="danger" size="small">已撤销</el-tag></template>
      {{ historic.deleteReason }}
    </el-popover>
    <el-tag type="warning" size="small" v-else-if="runtime?.isSuspended">已挂起</el-tag>
    <!-- 未结束且未被撤销即视为进行中：审批侧的历史接口不返回运行时实例，无法进一步区分是否挂起 -->
    <el-tag type="success" size="small" v-else>运行中</el-tag>
  </template>
  <template v-else-if="status">
    <el-tag v-if="statusMap[status]" :type="statusMap[status].type as any" size="small">{{ statusMap[status].label }}</el-tag>
    <el-tag v-else type="info" size="small">{{ status }}</el-tag>
  </template>
</template>

<style lang="scss" scoped>
</style>
