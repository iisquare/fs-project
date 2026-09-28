<script setup lang="ts">
/**
 * 输出变量 - 展示节点执行后可供下游引用的变量清单。
 * 迭代/循环容器的容器内变量（元素、索引、循环变量）单独分组：它们只对容器自身与其内部的节点可见。
 *
 * @prop {*}      data  - 节点 data
 * @prop {*}      instance - 画布实例，用于把引用型输出（变量赋值写入的变量）还原为「节点名称.变量名称」
 * @prop {String} title - 清单分组标题，默认「输出变量」；节点内已有同名可编辑分组时由调用方改名，避免两处同名
 */
import { computed } from 'vue'
import LayoutHelp from '@/components/Layout/LayoutHelp.vue'
import config from './config'
import SectionSlice from './SectionSlice.vue'
import { variableReference } from './variable'

const props = withDefaults(defineProps<{
  data?: any,
  instance?: any,
  title?: string,
}>(), {
  title: '输出变量',
})

const list = (scope: boolean) => {
  const items: any[] = config.outputs?.[props.data?.type]?.(props.data) ?? []
  return items.filter((item: any) => Boolean(item?.scope) === scope).map((item: any) => {
    if (!item?.reference) return item
    // 被写入的变量属于容器或会话作用域：按它在画布上的名称展示，取不到时回落到引用本身
    const source = variableReference(props.instance, String(item.name))
    return source ? Object.assign({}, item, {
      name: source.name, label: source.label, type: source.type,
    }) : item
  })
}

const groups = computed<any[]>(() => [{
  label: props.title,
  items: list(false),
}, {
  // 标题保持简短，可见性规则放在标题后的帮助图标里
  label: '容器内变量',
  help: '只在容器内生效：容器自身与容器内部的节点可以引用，容器外的节点取不到',
  items: list(true),
}].filter((group: any) => group.items.length))
</script>

<template>
  <template v-for="group in groups" :key="group.label">
    <SectionSlice :title="group.label">
      <template #title>
        {{ group.label }}
        <LayoutHelp v-if="group.help" :text="group.help" />
      </template>
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
    </SectionSlice>
  </template>
  <!-- 空态也走分组标题，避免出现一个没有标题的孤立提示行 -->
  <SectionSlice :title="props.title" v-if="!groups.length">
    <el-form-item label="">
      <el-text type="info" size="small">该节点无输出变量</el-text>
    </el-form-item>
  </SectionSlice>
</template>

<style lang="scss" scoped>
.output-slice {
  width: 100%;
  .output-item {
    padding: 6px 10px;
    /* 只读变量清单：与可编辑项（CollapseItem）同一套卡片语言——灰底上的白块，不带描边 */
    border-radius: 4px;
    background: var(--fs-panel-surface);
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
