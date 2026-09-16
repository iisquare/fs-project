<script setup lang="ts">
/**
 * 提示词编辑器 - 文本域与变量插入的组合，插入的变量占位符为 `{{#节点标识.变量英文名称#}}`，
 * 如 `{{#n1.query#}}`，下拉展示为 `节点名称.变量中文名称`。
 *
 * @v-model  {String} 文本内容
 * @prop     {*}      instance   - 画布实例（X6Container 暴露的 flow）
 * @prop     {*}      activeItem - 当前激活的节点，用于排除自身
 */
import { computed, ref } from 'vue'
import { variableGroups, variableTokens, variableTitle } from './variable'

const model: any = defineModel<string>()
const {
  instance,
  activeItem = {},
  rows = 6,
  placeholder = '',
} = defineProps<{
  instance?: any,
  activeItem?: any,
  rows?: number,
  placeholder?: string,
}>()

const version = ref(0)
const insertValue = ref('')
const groups = computed(() => {
  version.value
  return variableGroups(instance, activeItem)
})
const tokens = computed(() => variableTokens(groups.value))

const handleVisible = (visible: boolean) => {
  if (visible) version.value++
}

const handleInsert = () => {
  if (!insertValue.value) return
  model.value = (model.value ?? '') + (tokens.value[insertValue.value]?.token ?? '')
  insertValue.value = ''
}
</script>

<template>
  <div class="prompt-editor">
    <el-input v-model="model" type="textarea" :rows="rows" :placeholder="placeholder" />
    <div class="footer">
      <el-select v-model="insertValue" size="small" filterable clearable placeholder="插入变量" @visible-change="handleVisible">
        <el-option-group :key="group.label" :label="group.label" v-for="group in groups">
          <el-option :key="item.value" :value="item.value" :label="variableTitle(item)" v-for="item in group.variables" />
        </el-option-group>
      </el-select>
      <el-button size="small" @click="handleInsert" :disabled="!insertValue">插入</el-button>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.prompt-editor {
  width: 100%;
  .footer {
    margin-top: 6px;
    @include flex-start();
    gap: 6px;
    .el-select {
      flex: 1;
    }
  }
}
</style>
