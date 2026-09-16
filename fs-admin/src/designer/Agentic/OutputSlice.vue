<script setup lang="ts">
/**
 * 输出变量 - 展示节点执行后可供下游引用的变量清单。
 * 迭代/循环容器的容器内变量（元素、索引、循环变量）单独分组：它们只对容器内部的节点可见。
 */
import { computed } from 'vue'
import config from './config'

const props = defineProps<{ data?: any }>()

const list = (scope: boolean) => {
  const items: any[] = config.outputs?.[props.data?.type]?.(props.data) ?? []
  return items.filter((item: any) => Boolean(item?.scope) === scope)
}

const groups = computed<any[]>(() => [{
  label: '输出变量',
  items: list(false),
}, {
  label: '容器内变量（容器自身与其内部的节点可引用）',
  items: list(true),
}].filter((group: any) => group.items.length))
</script>

<template>
  <template v-for="group in groups" :key="group.label">
    <el-form-item label="" class="title">{{ group.label }}</el-form-item>
    <el-form-item label="">
      <div class="output-slice">
        <div class="output-item" :key="item.name" v-for="item in group.items">
          <div class="line">
            <span class="name">{{ item.label || item.name }}</span>
            <span class="alias" v-if="item.label && item.label !== item.name">{{ item.name }}</span>
            <span class="type">{{ item.type }}</span>
          </div>
          <div class="description" v-if="item.description">{{ item.description }}</div>
        </div>
      </div>
    </el-form-item>
  </template>
  <el-form-item label="" v-if="!groups.length">
    <el-text type="info" size="small">该节点无输出变量</el-text>
  </el-form-item>
</template>

<style lang="scss" scoped>
.output-slice {
  width: 100%;
  .output-item {
    padding: 6px 8px;
    border-radius: 4px;
    background: var(--el-fill-color-lighter);
    & + .output-item {
      margin-top: 6px;
    }
    .line {
      @include flex-start();
      .name {
        font-size: 12px;
        color: var(--el-text-color-primary);
      }
      .alias {
        margin-left: 6px;
        font-size: 12px;
        color: var(--el-text-color-secondary);
      }
      .type {
        margin-left: auto;
        font-size: 12px;
        color: var(--el-text-color-placeholder);
      }
    }
    .description {
      margin-top: 2px;
      font-size: 12px;
      color: var(--el-text-color-secondary);
    }
  }
}
</style>
