<script setup lang="ts">
defineProps<{
  status: string
}>()

const statusMap: Record<string, { label: string; type: string }> = {
  running: { label: '运行中', type: 'primary' },
  suspended: { label: '已挂起', type: 'warning' },
  ended: { label: '已结束', type: 'success' },
  revoked: { label: '已撤销', type: 'danger' },
  rejected: { label: '已驳回', type: 'danger' },
}
</script>

<template>
  <el-tag v-if="statusMap[status]" :type="statusMap[status].type as any" size="small">
    {{ statusMap[status].label }}
  </el-tag>
  <el-tag v-else type="info" size="small">{{ status }}</el-tag>
</template>

<style lang="scss" scoped>
</style>
