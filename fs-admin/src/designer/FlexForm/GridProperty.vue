<script setup lang="ts">
import { reactive, ref } from 'vue';
import draggable from 'vuedraggable'
import NodeSlice from './NodeSlice.vue';
import * as ElementPlusIcons from '@element-plus/icons-vue';

const active = ref('property')
const model: any = defineModel()
const tips: any = defineModel('tips', { type: null })
const props = defineProps<{
  config: any,
  instance: any,
}>()

const flexJustifies = [
  { value: 'start', label: '开始位置对齐' },
  { value: 'end', label: '结束位置对齐' },
  { value: 'center', label: '容器中心对齐' },
  { value: 'space-around', label: '空间周围分布' },
  { value: 'space-between', label: '空间之间分布' },
  { value: 'space-evenly', label: '空间均匀分布' },
]
const flexAligns = [
  { value: 'top', label: '顶部' },
  { value: 'middle', label: '居中' },
  { value: 'bottom', label: '底部' },
]

const marks: any = reactive(Object.fromEntries([0, 6, 12, 18, 24].map(item => [item + '', item + ''])))

const handleAdd = () => {
  model.value.options.items.push(props.config.generateGridItem())
}
const handleRemove = (element: any, index: any) => {
  model.value.options.items.splice(index, 1)
}
</script>

<template>
  <el-tabs v-model="active" class="tab-property">
    <el-tab-pane label="组件属性" name="property">
      <el-form :model="model">
        <NodeSlice v-model="model" :instance="instance" :config="config" :tips="tips" />
        <el-form-item label="名称"><el-input v-model="model.label" /></el-form-item>
        <el-form-item label="栅格间隔">
          <el-input-number v-model="model.options.gutter" :controls="false" :min="0" />
        </el-form-item>
        <el-form-item label="水平排列">
          <el-select v-model="model.options.justify" placeholder="请选择">
            <el-option :label="`${item.label}(${item.value})`" :value="item.value" v-for="item in flexJustifies" :key="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="垂直排列">
          <el-radio-group v-model="model.options.align">
            <el-radio-button :label="item.label" :value="item.value" v-for="item in flexAligns" :key="item.value" />
          </el-radio-group>
        </el-form-item>
        <el-divider content-position="center">栅格配置</el-divider>
        <draggable
          :list="model.options.items"
          item-key="id"
          group="grids"
          animation="340"
          chosenClass="grid-chosen"
          class="grid-container">
          <template #item="{ element, index }">
            <div class="grid-item">
              <el-button :icon="ElementPlusIcons.Rank" link>{{ index }}</el-button>
              <el-slider v-model="element.span" :step="1" :min="1" :max="24" :marks="marks" />
              <el-button :icon="ElementPlusIcons.Delete" link type="danger" @click="handleRemove(element, index)" />
            </div>
          </template>
          <template #footer>
            <el-button :icon="ElementPlusIcons.CirclePlus" link type="primary" @click="handleAdd">添加栅格</el-button>
          </template>
        </draggable>
      </el-form>
    </el-tab-pane>
  </el-tabs>
</template>

<style lang="scss" scoped>
.grid-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 0px 5px 15px 5px;
  margin-bottom: 5px;
}
</style>
