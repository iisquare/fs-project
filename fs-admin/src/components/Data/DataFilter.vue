<script setup lang="ts">
import UIUtil from '@/utils/UIUtil'

/**
 * 根据数据模型字段，配置过滤条件
 */
const model: any = defineModel()
const fields = defineModel('fields', { type: Array<Object>, default: [] })
const editable = defineModel('editable', { type: Boolean, default: false })

const relations = [
  { label: '且', value: 'AND' },
  { label: '或', value: 'OR' },
]
const operators = [
  { label: '等于(=)', value: 'EQUAL' },
  { label: '不等于(!=)', value: 'NOT_EQUAL' },
  { label: '小于(<)', value: 'LESS_THAN' },
  { label: '小于等于(<=)', value: 'LESS_THAN_OR_EQUAL' },
  { label: '大于(>)', value: 'GREATER_THAN' },
  { label: '大于等于(>=)', value: 'GREATER_THAN_OR_EQUAL' },
  { label: '为空(is null)', value: 'IS_NULL' },
  { label: '不为空(is not null)', value: 'IS_NOT_NULL' },
  { label: '包含(like)', value: 'LIKE' },
  { label: '不包含(not like)', value: 'NOT_LIKE' },
  { label: '在列表中(in)', value: 'IN' },
  { label: '不在列表中(not in)', value: 'NOT_IN' },
]
const definitions = [
  { label: '字符', value: 'STRING' },
  { label: '数值', value: 'NUMBER' },
  { label: '字段', value: 'FIELD' },
  { label: '变量', value: 'VARIABLE' },
]
const variables = [
  { label: '用户主键', value: 'USER_ID' },
  { label: '用户名称', value: 'USER_NAME' },
]

const handleRelation = () => {
  model.value.push({
    type: 'RELATION',
    value: 'AND',
    children: [],
  })
}
const handleFilter = () => {
  model.value.push({
    type: 'FILTER',
    value: '',
    operator: 'EQUAL',
    definition: 'STRING',
  })
}
const handleDelete = (item: any, index: any) => {
  model.value.splice(index, 1)
}
</script>
<template>
  <div class="filter-box">
    <el-space class="filter-header"><LayoutIcon name="Sunny" /></el-space>
    <template v-for="(item, index) in model" :key="index">
      <div class="relation-box" v-if="item.type === 'RELATION'">
        <div class="relation">
          <el-select v-model="item.value" placeholder="" class="relation-selector" clearable @clear="handleDelete(item, index)" :disabled="!editable">
            <el-option v-for="item in relations" :key="item.value" :value="item.value" :label="item.label" />
          </el-select>
        </div>
        <data-filter v-model="item.children" :fields="fields" :editable="editable" />
      </div>
      <el-space v-if="item.type === 'FILTER'">
        <el-autocomplete v-model="item.field" placeholder="字段" :fetch-suggestions="query => UIUtil.fetchSuggestions(fields, query, 'name')" class="field-selector" clearable :disabled="!editable" />
        <el-select v-model="item.operator" placeholder="运算" class="operator-selector" filterable :disabled="!editable">
          <el-option v-for="item in operators" :key="item.value" :value="item.value" :label="item.label" />
        </el-select>
        <el-select v-model="item.definition" placeholder="" class="definition-selector" :disabled="!editable">
          <el-option v-for="item in definitions" :key="item.value" :value="item.value" :label="item.label" />
        </el-select>
        <el-autocomplete v-model="item.value" v-if="item.definition === 'VARIABLE'" placeholder="变量" :fetch-suggestions="query => UIUtil.filterSuggestions(variables, query)" class="value-selector" clearable :disabled="!editable">
          <template #default="{ item }">{{ item.value }} - {{ item.label }}</template>
        </el-autocomplete>
        <el-autocomplete v-model="item.value" v-else-if="item.definition === 'FIELD'" placeholder="字段" :fetch-suggestions="query => UIUtil.fetchSuggestions(fields, query, 'name')" class="value-selector" clearable :disabled="!editable" />
        <el-input v-model="item.value" v-else placeholder="值" class="value-selector" :disabled="!editable" />
        <LayoutIcon name="Delete" @click="handleDelete(item, index)" v-if="editable" />
      </el-space>
    </template>
    <el-space v-if="editable">
      <el-button @click="handleRelation">新增关系</el-button>
      <el-button @click="handleFilter">新增条件</el-button>
    </el-space>
    <el-space class="filter-footer"><LayoutIcon name="Sunny" /></el-space>
  </div>
</template>

<style lang="scss" scoped>
$line-color: var(--el-border-color);
.relation-box {
  display: flex;
  align-items: center;
  justify-content: start;
}
.relation {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 75px;
  .relation-selector {
    width: 60px;
  }
  &::after {
    content: "";
    width: 15px;
    height: 1px;
    background-color: $line-color;
  }
}
.filter-box {
  display: flex;
  flex-direction: column;
  position: relative;
  &::before {
    position: absolute;
    top: 23px;
    content: "";
    width: 1px;
    height: calc(100% - 45px);
    background-color: $line-color;
  }
  .el-space {
    height: 45px;
    &::before {
      margin-right: -8px;
    }
    .el-icon {
      cursor: pointer;
    }
  }
  >::before {
    align-self: center;
    content: "";
    width: 15px;
    height: 1px;
    background-color: $line-color;
  }
}
.filter-header, .filter-footer {
  color: $line-color;
}
:deep(.el-space) {
  .field-selector, .value-selector {
    width: 200px;
  }
  .operator-selector {
    width: 165px;
  }
  .definition-selector {
    width: 80px;
  }
}
</style>
