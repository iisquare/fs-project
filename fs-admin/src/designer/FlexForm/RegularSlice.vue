<script setup lang="ts">
import draggable from 'vuedraggable'
import * as ElementPlusIcons from '@element-plus/icons-vue';
import { onMounted } from 'vue';
import DataUtil from '@/utils/DataUtil';

const model: any = defineModel()
const tips: any = defineModel('tips', { type: null })
const props = defineProps<{
  config: any,
  instance: any,
}>()

const handleAdd = () => {
  model.value.options.regulars.push({ id: props.config.uuidRegular(), regular: '', tooltip: '' })
}
const handleRemove = (element: any, index: any) => {
  model.value.options.regulars.splice(index, 1)
}

onMounted(() => {
  if (!DataUtil.isArray(model.value.options.regulars)) {
    Object.assign(model.value.options, { regulars: [] })
  }
})
</script>

<template>
  <el-divider content-position="center">正则校验</el-divider>
  <draggable
    :list="model.options.regulars"
    item-key="id"
    group="regulars"
    animation="340"
    chosenClass="regular-chosen"
    class="regular-container">
    <template #item="{ element, index }">
      <div class="regular-item">
        <LayoutIcon name="Close" @click="handleRemove(element, index)" class="close-btn" />
        <el-form-item label="表达式"><el-input v-model="element.regular" /></el-form-item>
        <el-form-item label="错误提示"><el-input v-model="element.tooltip" /></el-form-item>
      </div>
    </template>
    <template #footer>
      <el-button :icon="ElementPlusIcons.CirclePlus" link type="primary" @click="handleAdd">添加规则</el-button>
    </template>
  </draggable>
</template>

<style lang="scss" scoped>
.regular-item {
  padding: 18px 10px 0px 10px;
  background: #f8f8f8;
  position: relative;
  border-radius: 4px;
  border: 1px dashed #ccc;
  margin-bottom: 10px;
  .close-btn {
    position: absolute;
    right: -7px;
    top: -7px;
    display: block;
    width: 16px;
    height: 16px;
    line-height: 16px;
    background: rgba(0,0,0,.2);
    border-radius: 50%;
    color: #fff;
    text-align: center;
    z-index: 1;
    cursor: pointer;
    font-size: 12px;
  }
}
.regular-chosen {
  border: 1px dashed #409eff;
}
.regular-btn {
  padding: 5px 0px;
}
</style>
