<script setup lang="ts">
import { computed, inject, onMounted, ref } from 'vue'
import * as X6 from '@antv/x6'
import DesignUtil from '@/utils/DesignUtil'

const node = (inject('getNode') as any)() as X6.Node
const data: any = ref(node.getData())
const role = computed(() => {
  if (data.value.direction) return data.value.direction
  const type = data.value.type === 'ScriptTransform'
    ? (data.value.options?.pkgClass ?? '')
    : (data.value.type ?? '')
  if (type.endsWith('Source') || type.endsWith('Capture')) return 'source'
  if (type.endsWith('Sink')) return 'sink'
  if (type.endsWith('Config')) return 'config'
  return 'transform'
})
const roleLabels: Record<string, string> = {
  source: '读取',
  transform: '加工',
  sink: '写入',
  config: '配置',
}
const roleLabel = computed(() => roleLabels[role.value] ?? '节点')

onMounted(() => {
  node.on('change:data', DesignUtil.fixedFlowChangeData(({ current } = {} as any) => {
    data.value = current
  }))
})
</script>

<template>
  <div class="node">
    <div class="box">
      <LayoutIcon :name="data.icon" color="#4A93FF" size="24" />
      <span
        :class="['role', `is-${role}`]"
        :title="`${roleLabel}节点`"
        :aria-label="`${roleLabel}节点`"
      ></span>
    </div>
    <div class="title">{{ data.name }}</div>
  </div>
</template>

<style lang="scss" scoped>
.node {
  width: 100%;
  height: 100%;
  overflow: visible;
  position: relative;
}
.role {
  position: absolute;
  top: 6px;
  right: 6px;
  width: 5px;
  height: 5px;
  box-sizing: content-box;
  border: 1px solid #fff;
  border-radius: 50%;
  background: currentColor;
  opacity: 0.9;
  &.is-source {
    color: #409eff;
  }
  &.is-transform {
    color: #67c23a;
  }
  &.is-sink {
    color: #e6a23c;
  }
  &.is-config {
    color: #909399;
  }
}
.box {
  width: 100%;
  height: 100%;
  border: 1px solid #e3eaf2;
  box-sizing: border-box;
  border-radius: 10px;
  background: linear-gradient(180deg, #fff 0%, #fafcff 100%);
  box-shadow: 0 2px 6px rgb(31 45 61 / 4%);
  @include flex-center();
  transition: border-color 0.18s, background-color 0.18s, box-shadow 0.18s;
  &:hover {
    border-color: #c8dcf5;
    background: #fff;
    box-shadow: 0 3px 9px rgb(31 45 61 / 7%);
  }
}
.title {
  position: absolute;
  top: 100%;
  left: 50%;
  width: max-content;
  max-width: 120px;
  height: 30px;
  line-height: 30px;
  transform: translateX(-50%);
  font-size: 12px;
  font-weight: 500;
  color: var(--el-text-color-primary);
  text-align: center;
  white-space: nowrap; /* 保持文本在一行内 */
  overflow: hidden; /* 隐藏溢出的文本 */
  text-overflow: ellipsis; /* 使用省略号表示文本溢出 */
}
.x6-node-selected {
  .box {
    border-color: #409eff;
    background: #fff;
    box-shadow: 0 0 0 2px rgb(64 158 255 / 12%);
  }
}
</style>
