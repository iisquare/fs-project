<script setup lang="ts">
import { computed, inject, onMounted, ref } from 'vue'
import * as X6 from '@antv/x6'
import DesignUtil from '@/utils/DesignUtil'

const node = (inject('getNode') as any)() as X6.Node
const data: any = ref(Object.assign({ fields: [] }, node.getData()))

const color = computed(() => {
  return data.value.color || '#4299e1'
})

onMounted(() => {
  node.on('change:data', DesignUtil.fixedFlowChangeData(({ current } = {} as any) => {
    data.value = current
    node.setProp({
      size: {
        height: data.value.fields.length * 30 + 88,
      },
    } as any)
  }))
})
</script>

<template>
  <div class="entity-box">
    <div class="entity-title">{{ `${data.name || '未命名'} (${data.label || '未定义'})` }}</div>
    <ul class="attribute-list">
      <li class="attribute-item" v-for="(item, index) in data.fields" :key="index">
        <span class="attribute-name"><strong>{{ item.name || 'unknow' }}</strong></span>
        <span class="attribute-value">{{ item.type }}</span>
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
  border: 1px solid #3182ce;
  background-color: #ebf8ff;
  box-shadow: 0 10px 20px -5px rgba(49, 130, 206, 0.3), 0 4px 8px -2px rgba(49, 130, 206, 0.2);
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
}

.attribute-name {
  flex-shrink: 0;
  margin-right: 0.5rem;
}

.attribute-name strong {
  color: #2d3748;
}

.attribute-value {
  display: flex;
  align-items: center;
  gap: 0.4rem;
  flex-wrap: wrap;
  justify-content: flex-end;
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
