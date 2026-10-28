export const qualityRuleTypes = [
  { type: 'EmptyRule', label: '空值检查', description: '检查字段值是否为 NULL 或空字符串' },
  { type: 'RangeRule', label: '范围检查', description: '检查字段值是否在给定范围内' },
  { type: 'EnumRule', label: '枚举检查', description: '检查字段值是否在字典中' },
  { type: 'RegularRule', label: '正则校验', description: '检查字段值是否匹配正则表达式' },
  { type: 'FormulaRule', label: '公式校验', description: '检查记录是否满足自定义 SQL 表达式' },
  { type: 'ReferRule', label: '引用检查', description: '检查字段值是否存在于关联表' },
  { type: 'ConstCompareRule', label: '常量比较', description: '按分组字段对比计算值和常量' },
  { type: 'GroupCompareRule', label: '分组对比', description: '比较两张表的计算值是否一致' },
]

export const qualityOperators = [
  { label: '等于（=）', value: 'EQUAL' },
  { label: '不等于（!=）', value: 'NOT_EQUAL' },
  { label: '小于（<）', value: 'LESS_THAN' },
  { label: '小于等于（<=）', value: 'LESS_THAN_OR_EQUAL' },
  { label: '大于（>）', value: 'GREATER_THAN' },
  { label: '大于等于（>=）', value: 'GREATER_THAN_OR_EQUAL' },
]

export const qualityMetrics = [
  { label: '不聚合', value: '' },
  { label: '求和（SUM）', value: 'SUM' },
  { label: '计数（COUNT）', value: 'COUNT' },
  { label: '去重计数（COUNT_DISTINCT）', value: 'COUNT_DISTINCT' },
  { label: '最大（MAX）', value: 'MAX' },
  { label: '最小（MIN）', value: 'MIN' },
  { label: '平均（AVG）', value: 'AVG' },
]

export const ruleDefaultContent = (type: string) => {
  switch (type) {
    case 'EmptyRule':
      return { isNull: true, isBlank: false }
    case 'RangeRule':
      return { min: '', minEnable: true, minInclude: true, max: '', maxEnable: true, maxInclude: true }
    case 'EnumRule':
      return { json: '{"A":"示例值"}' }
    case 'RegularRule':
    case 'FormulaRule':
      return { expression: '' }
    case 'ConstCompareRule':
      return { operator: 'EQUAL', value: '' }
    default:
      return {}
  }
}
