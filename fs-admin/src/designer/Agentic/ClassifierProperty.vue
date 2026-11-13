<script setup lang="ts">
/**
 * 问题分类器节点属性 - 模型配置（模型名称、温度、思考模式与强度、系统提示词）、
 * 输入变量、问题分类列表、记忆配置。
 */
import { ref } from 'vue'
import { Plus, Rank } from '@element-plus/icons-vue'
import draggable from 'vuedraggable'
import DesignUtil from '@/utils/DesignUtil'
import CollapseItem from './CollapseItem.vue'
import MemoryField from './MemoryField.vue'
import ModelParamsField from './ModelParamsField.vue'
import NodeSlice from './NodeSlice.vue'
import OutputSlice from './OutputSlice.vue'
import SectionSlice from './SectionSlice.vue'
import VariableField from './VariableField.vue'
import VariableSelect from './VariableSelect.vue'

const active = ref('property')
const model: any = defineModel()
const tips: any = defineModel('tips', { type: null })
defineProps<{
  config?: any,
  instance?: any,
}>()

// 分类列表：默认收起，仅一个分类时默认展开；展开状态按分类 id 记录，拖拽排序后不错位
const expandedIds = ref<string[]>([])
const collapsedIds = ref<string[]>([])
const isOpen = (item: any) => {
  if (expandedIds.value.indexOf(item.id) >= 0) return true
  if (collapsedIds.value.indexOf(item.id) >= 0) return false
  return 1 === (model.value?.data?.classes ?? []).length
}
const toggle = (item: any) => {
  if (isOpen(item)) {
    expandedIds.value = expandedIds.value.filter((id: string) => id !== item.id)
    collapsedIds.value = collapsedIds.value.concat(item.id)
    return
  }
  collapsedIds.value = collapsedIds.value.filter((id: string) => id !== item.id)
  expandedIds.value = expandedIds.value.concat(item.id)
}

const handleAdd = () => {
  const item: any = {
    id: DesignUtil.uuid(),
    name: `分类${model.value.data.classes.length + 1}`,
    description: '',
  }
  model.value.data.classes.push(item)
  expandedIds.value = expandedIds.value.concat(item.id) // 新增项默认展开
}

const handleRemove = (index: number) => {
  const item: any = model.value.data.classes[index]
  if (item) {
    expandedIds.value = expandedIds.value.filter((id: string) => id !== item.id)
    collapsedIds.value = collapsedIds.value.filter((id: string) => id !== item.id)
  }
  model.value.data.classes.splice(index, 1)
}

// 收起时的摘要：分类描述
const summaryTags = (item: any) => {
  const description = String(item?.description ?? '').trim()
  if (!description) return []
  return [description.length > 14 ? description.slice(0, 14) + '…' : description]
}
</script>

<template>
  <el-tabs v-model="active" class="tab-property">
    <el-tab-pane label="节点属性" name="property">
      <el-form :model="model" label-position="top">
        <NodeSlice v-model="model" :instance="$props.instance" :config="$props.config" :tips="tips" />
        <ModelParamsField
          v-model="model.data"
          :config="$props.config"
          :instance="$props.instance"
          :active-item="model"
          system-placeholder="补充说明分类的判断依据，可插入上游变量" />
        <SectionSlice title="输入与指令">
          <el-form-item label="输入变量">
            <VariableSelect
              v-model="model.data.query"
              :instance="$props.instance"
              :active-item="model"
              allow-create
              placeholder="请选择待分类的文本" />
          </el-form-item>
        </SectionSlice>
        <SectionSlice title="问题分类">
          <el-form-item label="">
          <div class="class-slice">
            <draggable
              v-model="model.data.classes"
              class="class-list"
              handle=".class-drag"
              ghost-class="class-ghost"
              item-key="id"
              :animation="200">
              <template #item="{ element: item, index }">
                <CollapseItem
                  :title="item.name || '未命名分类'"
                  :expanded="isOpen(item)"
                  @toggle="toggle(item)"
                  @delete="handleRemove(index)">
                  <template #head>
                    <!-- 拖拽手柄 + 收起时的描述摘要 -->
                    <el-icon class="class-drag" title="按住拖拽调整判定顺序"><Rank /></el-icon>
                    <span class="class-preview">{{ summaryTags(item)[0] || '' }}</span>
                  </template>
                  <VariableField
                    v-model="item.description"
                    title="分类描述"
                    :compact="true"
                    :instance="$props.instance"
                    :active-item="model"
                    :height="90"
                    placeholder="说明满足什么条件时归入该分类，可插入变量">
                    <!-- 分类名称与插入变量/复制按钮同一行 -->
                    <template #head>
                      <el-input v-model="item.name" placeholder="分类名称，如：咨询价格" />
                    </template>
                  </VariableField>
                </CollapseItem>
              </template>
            </draggable>
            <el-button class="class-add" :icon="Plus" @click="handleAdd">添加分类</el-button>
          </div>
          </el-form-item>
        </SectionSlice>
        <MemoryField v-model="model.data" />
        <OutputSlice :data="model.data" />
      </el-form>
    </el-tab-pane>
  </el-tabs>
</template>

<style lang="scss" scoped>
/**
 * 问题分类列表：卡片式折叠项 + 描述摘要，展开态的主色描边与浅色底由 CollapseItem 统一提供，
 * 这里只补分类项自己的间距与拖拽手柄；底部是全宽的虚线「添加分类」，窄面板里也整齐
 */
.class-slice {
  width: 100%;
  :deep(.collapse-item) {
    padding: 10px;
    & + .collapse-item {
      margin-top: 8px;
    }
    .collapse-head {
      margin-bottom: 0;
      /* 描述摘要靠右显示，删除按钮紧跟其后 */
      .delete {
        margin-left: 0;
      }
    }
    &.is-expanded .collapse-head {
      margin-bottom: 8px;
    }
    .collapse-body {
      @include flex-column();
      gap: 6px;
    }
    .el-input {
      width: 100%;
    }
  }
  .class-preview {
    max-width: 110px;
    margin-left: auto;
    margin-right: 8px;
    font-size: 12px;
    color: var(--el-text-color-placeholder);
    @include text-wrap();
  }
  /* 拖拽手柄：紧挨标题，按住可调整分类判定顺序 */
  .class-drag {
    flex: none;
    margin-left: 6px;
    color: var(--el-text-color-placeholder);
    cursor: move;
    &:hover {
      color: var(--el-color-primary);
    }
  }
  .class-ghost {
    opacity: 0.5;
  }
  .class-add {
    width: 100%;
    margin-top: 8px;
    border-style: dashed;
    &:hover {
      border-color: var(--el-color-primary);
      background: var(--el-color-primary-light-9);
    }
  }
}
</style>
