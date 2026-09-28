<script setup lang="ts">
/**
 * 变量选择器 - 选择画布中上游节点的输出变量或系统变量，支持按类型过滤与手工输入。
 *
 * @v-model  {String}   选中值：变量引用按统一规范写成占位符 `{{#节点标识.变量名#}}`（如 `{{#n1.query#}}`），
 *                      手工输入的会话变量（`conversation.xxx`）同样转成占位符，其它文本按原文写入
 * @prop     {*}        instance   - 画布实例（X6Container 暴露的 flow）
 * @prop     {*}        activeItem - 当前激活的节点，用于排除自身
 * @prop     {String}   types      - 允许的变量类型，多个以英文逗号分隔；
 *                                   容器元素变量的类型由容器输入决定（静态未知），不参与该过滤
 * @prop     {Boolean}  inner      - 只列出当前节点内部的节点变量（容器收集输出时用），默认 false
 * @prop     {Boolean}  allowCreate - 是否允许输入自定义内容，默认 false
 * @prop     {String}   icon       - Element Plus 图标名称，作为下拉框的前缀图标
 * @prop     {Boolean}  writable   - 只列出可写入变量（容器内的元素/索引/循环变量），默认 false；
 *                                   用于变量赋值的目标变量，会话变量仍由手工输入
 * @prop     {Boolean}  outer      - 只取容器外部变量（容器自身的元素/索引/循环变量不作为取值来源），默认 false；
 *                                   用于循环变量初始值这类必须取自容器外部的取值
 */
import { computed, ref, watch } from 'vue'
import LayoutIcon from '@/components/Layout/LayoutIcon.vue'
import { detectReference, referenceOfToken, variableGroups, variableLabel, variableToken } from './variable'

const model: any = defineModel<string>()
const {
  instance,
  activeItem = {},
  placeholder = '请选择变量',
  clearable = true,
  allowCreate = false,
  inner = false,
  types = '',
  icon = '',
  writable = false,
  outer = false,
} = defineProps<{
  instance?: any,
  activeItem?: any,
  placeholder?: string,
  clearable?: boolean,
  allowCreate?: boolean,
  inner?: boolean,
  types?: string,
  icon?: string,
  writable?: boolean,
  outer?: boolean,
}>()

const version = ref(0)
// 展开下拉时重新收集变量，兼容画布中节点的增删
const all = computed(() => {
  version.value
  return variableGroups(instance, activeItem, inner, outer)
})
// 画布中全部可引用的变量引用：用于把历史数据里的裸引用升级为占位符
const references = computed(() => {
  const result = new Set<string>()
  all.value.forEach((group: any) => group.variables.forEach((item: any) => result.add(item.value)))
  return result
})
const groups = computed(() => {
  let list: any[] = all.value
  if (types) {
    const allowed = types.split(',')
    list = list.map((group: any) => Object.assign({}, group, {
      // 容器元素变量（动态类型）放行，否则迭代/循环内部的节点选不到「当前元素」
      variables: group.variables.filter((item: any) => item.dynamic || allowed.indexOf(item.type) >= 0),
    }))
  }
  if (writable) {
    list = list.map((group: any) => Object.assign({}, group, {
      variables: group.variables.filter((item: any) => true === item.writable),
    }))
  }
  return list.filter((group: any) => group.variables.length > 0)
})

const handleVisible = (visible: boolean) => {
  if (!visible) return
  version.value++
  upgrade()
}

/** 选项写入值：统一写入占位符，运行时按 `{{#节点标识.变量名#}}` 解析 */
const valueOf = (item: any) => variableToken(item.value)

/**
 * 兼容历史数据与手工输入 - 取值是裸引用（画布变量、`sys.xxx`、`conversation.xxx`）时升级为占位符。
 * 否则下拉框认不出这一项、会显示原始标识，运行时也解析不到取值
 */
const upgrade = () => {
  const reference = detectReference(model.value, references.value)
  if (reference) model.value = variableToken(reference)
}
watch(model, upgrade, { immediate: true })

/**
 * 已选但不在清单里的引用（手工输入的会话变量、被删节点的变量）：补一条同名选项，
 * 选中框展示引用本身，而不是占位符原文
 */
const selected = computed<any>(() => {
  const reference = referenceOfToken(model.value)
  if (!reference || references.value.has(reference)) return null
  return { value: reference, name: reference, label: reference, type: '', optionLabel: reference }
})
const list = computed(() => {
  if (!selected.value) return groups.value
  return [{ label: '已选变量', variables: [selected.value] }].concat(groups.value)
})
/** 选项展示名称：清单内的变量按「节点名称.变量名称」，已选变量直接展示引用本身 */
const labelOf = (group: any, item: any) => item.optionLabel || variableLabel(group.label, item)
</script>

<template>
  <el-select
    v-model="model"
    :placeholder="placeholder"
    :clearable="clearable"
    :filterable="true"
    :allow-create="allowCreate"
    default-first-option
    @visible-change="handleVisible">
    <template #prefix>
      <LayoutIcon v-if="icon" :name="icon" />
    </template>
    <el-option-group :key="group.label" :label="group.label" v-for="group in list">
      <!-- 选中后展示「节点名称.变量名称」：同名的变量来自不同节点时也能分辨来源 -->
      <el-option :key="item.value" :value="valueOf(item)" :label="labelOf(group, item)" v-for="item in group.variables">
        <span class="variable-name">{{ item.label || item.name }}</span>
        <span class="variable-alias" v-if="item.label && item.label !== item.name">{{ item.name }}</span>
        <span class="variable-type">{{ item.type }}</span>
      </el-option>
    </el-option-group>
  </el-select>
</template>

<style lang="scss" scoped>
.variable-alias {
  margin-left: 6px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
.variable-type {
  float: right;
  color: var(--el-text-color-placeholder);
  font-size: 12px;
}
</style>
