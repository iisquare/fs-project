<script setup lang="ts">
import { computed, inject, nextTick, onMounted, ref } from 'vue'
import * as X6 from '@antv/x6'
import DesignUtil from '@/utils/DesignUtil'

const node = (inject('getNode') as any)() as X6.Node
const data: any = ref(Object.assign({ fields: [] }, node.getData()))
const boxRef = ref<any>()

const color = computed(() => {
  return data.value.color || '#4299e1'
})

/**
 * 节点卡片只展示勾选了"画布展示"的字段；一个都没勾选时不展示任何字段。
 */
const displayFields = computed(() => {
  const fields: any[] = data.value.fields ?? []
  return fields.filter((item: any) => item.display)
})
const hiddenCount = computed(() => (data.value.fields ?? []).length - displayFields.value.length)
const rowCount = computed(() => displayFields.value.length + (hiddenCount.value > 0 ? 1 : 0))

/**
 * 节点标签：优先使用完整标签集合，兼容只有 label 字符串的场景；首个为主标签
 */
const labels = computed(() => {
  const list = data.value.labels
  if (Array.isArray(list) && list.length) return list.filter((item: any) => !!item)
  return data.value.label ? String(data.value.label).split(':').filter((item: string) => !!item) : []
})
const primaryLabel = computed(() => labels.value[0] ?? '')
const extraLabels = computed(() => labels.value.slice(1))

/**
 * 精简模式：只展示名称，并以本体配置的颜色作为背景色
 */
const compact = computed(() => !!data.value.compact)
const textColor = computed(() => {
  const hex = String(color.value || '').replace('#', '')
  if (6 !== hex.length) return '#ffffff'
  const r = parseInt(hex.slice(0, 2), 16)
  const g = parseInt(hex.slice(2, 4), 16)
  const b = parseInt(hex.slice(4, 6), 16)
  const luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255
  return luminance > 0.62 ? '#1f2937' : '#ffffff'
})

/**
 * 卡片模式
 *
 * edit：本体设计器，展示字段名与数据类型，便于建模核对；
 * view：图探索等展示场景，展示字段显示名（中文）与属性值。
 */
const mode = computed(() => 'view' === data.value.mode ? 'view' : 'edit')
const fieldLabel = (field: any) => 'view' === mode.value
  ? (field.title || field.name || '')
  : (field.name || '')
const fieldValue = (field: any) => 'view' === mode.value
  ? (field.value ?? field.type ?? '')
  : (field.type || '')

/**
 * 高度按实际内容测量，保证标签、字段完整展示不被裁剪；无法测量时按行数兜底
 */
const applySize = () => {
  const box = boxRef.value as HTMLElement | undefined
  const height = box?.offsetHeight || rowCount.value * 30 + 88
  if (!height) return
  const size: any = node.getSize()
  if (Math.abs((size?.height ?? 0) - height) < 1) return
  node.setProp({ size: { height } } as any)
}

onMounted(() => {
  node.on('change:data', DesignUtil.fixedFlowChangeData(({ current } = {} as any) => {
    data.value = current
    nextTick(applySize)
  }))
  // 载入已有本体时按展示字段一次性校正高度
  nextTick(applySize)
})
</script>

<template>
  <div class="entity-box" :class="{ 'entity-box--compact': compact }" ref="boxRef">
    <div class="entity-title" :title="`${data.name || '未命名'}（${labels.join(' / ') || '未定义'}）`">
      <span class="entity-name">{{ data.name || '未命名' }}</span>
      <span class="entity-label" v-if="!compact && primaryLabel">（{{ primaryLabel }}）</span>
    </div>
    <div v-if="!compact && extraLabels.length" class="entity-labels">
      <span class="entity-label-item" v-for="label in extraLabels" :key="label">{{ label }}</span>
    </div>
    <ul class="attribute-list" v-if="!compact">
      <li
        class="attribute-item"
        v-for="(item, index) in displayFields"
        :key="index"
        :title="`${fieldLabel(item)}：${fieldValue(item)}`"
      >
        <span class="attribute-name"><strong>{{ fieldLabel(item) || 'unknow' }}</strong></span>
        <span class="attribute-value">{{ fieldValue(item) }}</span>
      </li>
      <li class="attribute-item attribute-more" v-if="hiddenCount > 0">
        <span class="attribute-name">+{{ hiddenCount }} 个字段未展示</span>
      </li>
    </ul>
  </div>
</template>

<style lang="scss" scoped>
.entity-box {
  background-color: #ffffff;
  border-radius: 0.75rem;
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05);
  padding: 1.25rem;
  border: 1px solid #e2e8f0;
  transition: transform 0.2s ease-in-out, border-color 0.2s ease-in-out, background-color 0.2s ease-in-out;
  cursor: pointer;
}

.x6-node-selected .entity-box {
  // 选中态用阴影体现，不加高亮边框
  border-color: #e2e8f0;
  background-color: #ebf8ff;
  box-shadow: 0 12px 26px -8px rgba(49, 130, 206, 0.5), 0 4px 10px -4px rgba(49, 130, 206, 0.3);
}

// 精简模式：本体配置的颜色作为卡片背景，只展示名称
.entity-box--compact {
  padding: 0.5rem 0.75rem;
  background-color: v-bind(color);
  border-color: transparent;
  border-radius: 0.5rem;
  color: v-bind(textColor);
  .entity-title {
    margin-bottom: 0;
    padding-bottom: 0;
    border-bottom: none;
    color: v-bind(textColor);
    font-size: 0.875rem;
    // 精简模式只展示名称，居中显示
    justify-content: center;
    text-align: center;
    &::before { display: none; }
    .entity-name {
      flex: 0 1 auto;
      max-width: 100%;
      color: v-bind(textColor);
    }
  }
}
.x6-node-selected .entity-box--compact {
  // 选中时保持本体颜色，仅通过阴影体现选中，避免浅色底盖掉文字对比度
  background-color: v-bind(color);
  color: v-bind(textColor);
  border-color: transparent;
  box-shadow: 0 12px 26px -8px rgba(15, 23, 42, 0.55), 0 4px 10px -4px rgba(15, 23, 42, 0.35);
  .entity-title {
    color: v-bind(textColor);
    .entity-name { color: v-bind(textColor); }
  }
}
.attribute-more {
  color: #94a3b8;
  font-size: 0.75rem;
}

.entity-title {
  font-size: 1.0rem;
  font-weight: 600;
  color: #2d3748;
  margin-bottom: 0.75rem;
  padding-bottom: 0.5rem;
  border-bottom: 2px solid #4299e1;
  display: flex;
  align-items: center;
  // 标签较多时不换行，超长部分省略，完整内容通过 title 提示查看
  white-space: nowrap;
  overflow: hidden;
}

.entity-title .entity-name {
  flex: 0 1 auto;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
}

.entity-title .entity-label {
  flex: 0 1 auto;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  color: #718096;
  font-weight: 500;
}

.entity-labels {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-bottom: 0.75rem;
}

.entity-label-item {
  padding: 0 0.4rem;
  font-size: 0.7rem;
  line-height: 1.5;
  color: #718096;
  background-color: #edf2f7;
  border-radius: 0.625rem;
}

.entity-title::before {
  content: '';
  display: block;
  width: 0.25rem;
  height: 1.2rem;
  background-color: v-bind(color);
  border-radius: 0.2rem;
  margin-right: 0.75rem;
  flex-shrink: 0;
}

.attribute-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.attribute-item {
  font-size: 1rem;
  color: #4a5568;
  margin-bottom: 0.4rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  min-width: 0;
  white-space: nowrap;
}

.attribute-name {
  flex: 0 1 auto;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-right: 0.5rem;
}

.attribute-name strong {
  color: #2d3748;
}

.attribute-value {
  flex: 0 1 auto;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  text-align: right;
}

.pk-label {
  background-color: #4299e1;
  color: #ffffff;
  font-size: 0.7rem;
  font-weight: 600;
  padding: 0.15rem 0.4rem;
  border-radius: 0.375rem;
}

.fk-label {
  background-color: #f6ad55;
  color: #ffffff;
  font-size: 0.7rem;
  font-weight: 600;
  padding: 0.15rem 0.4rem;
  border-radius: 0.375rem;
}
</style>
