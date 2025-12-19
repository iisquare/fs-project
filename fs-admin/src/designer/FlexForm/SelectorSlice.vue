<script setup lang="ts">
import draggable from 'vuedraggable'
import * as ElementPlusIcons from '@element-plus/icons-vue';
import { onMounted } from 'vue';
import DictionaryApi from '@/api/member/DictionaryApi';

const model: any = defineModel()
const tips: any = defineModel('tips', { type: null })
const props = defineProps<{
  config: any,
  instance: any,
}>()

const types = [{ value: 'static', label: '静态数据' }, { value: 'dictionary', label: '数据字典' }, { value: 'api', label: '远端数据' }]

const handleAdd = () => {
  model.value.options.items.push({ value: '', label: '' })
}
const handleRemove = (element: any, index: any) => {
  model.value.options.items.splice(index, 1)
}

onMounted(() => {
})
</script>

<template>
  <el-form-item label="数据来源">
    <el-radio-group v-model="model.options.type" size="small">
      <el-radio-button :label="item.label" :value="item.value" v-for="item in types" :key="item.value" />
    </el-radio-group>
  </el-form-item>
  <el-divider content-position="center">配置项</el-divider>
  <template v-if="model.options.type === 'static'">
    <draggable
      :list="model.options.items"
      item-key="id"
      group="selector"
      animation="340"
      chosenClass="selector-chosen"
      class="selector-container">
      <template #item="{ element, index }">
        <div class="selector-item">
          <el-button :icon="ElementPlusIcons.Rank" link />
          <el-input v-model="element.value"><template #prepend>值</template></el-input>
          <el-input v-model="element.label"><template #prepend>标签</template></el-input>
          <el-button :icon="ElementPlusIcons.Delete" link type="danger" @click="handleRemove(element, index)" />
        </div>
      </template>
      <template #footer>
        <el-button :icon="ElementPlusIcons.CirclePlus" link type="primary" @click="handleAdd">添加选项</el-button>
      </template>
    </draggable>
  </template>
  <template v-else-if="model.options.type === 'dictionary'">
    <el-form-item label="数据字典">
      <form-select v-model="model.options.dictionary" :callback="DictionaryApi.list" clearable placeholder="输入名称进行检索" />
    </el-form-item>
  </template>
  <template v-else-if="model.options.type === 'api'">
    <el-form-item label="接口地址"><el-input type="textarea" v-model="model.apiUrl" placeholder="数据接口地址" /></el-form-item>
    <el-form-item label="数据字段"><el-input v-model="model.options.fieldData" placeholder="接口数据所在字段路径" /></el-form-item>
    <el-form-item label="取值字段"><el-input v-model="model.options.fieldValue" placeholder="接口Value字段名称" /></el-form-item>
    <el-form-item label="标签字段"><el-input v-model="model.options.fieldLabel" placeholder="接口Lable字段名称" /></el-form-item>
  </template>
  <template v-else="model.options.type === 'api'">
    <el-empty description="请选择数据来源方式" />
  </template>
</template>

<style lang="scss" scoped>
.selector-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 5px;
  border:dashed 1px white;
  :deep(.el-input-group__prepend) {
    padding: 0px 8px;
  }
}
.selector-chosen {
  border:dashed 1px lightblue;
}
</style>
