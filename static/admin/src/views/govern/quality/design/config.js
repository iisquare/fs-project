const config = {

}

const DefaultOptions = () => {
  return {}
}

const EmptyOptions = () => {
  return { isNull: true, isBlank: false }
}

const RangeOptions = () => {
  return { min: '', minEnable: true, minInclude: true, max: '', maxEnable: true, maxInclude: true }
}

const EnumOptions = () => {
  const exmaple = { code: 'label' }
  return { json: JSON.stringify(exmaple) }
}

const RegularOptions = () => {
  return { expression: '' }
}

const FormulaOptions = () => {
  return { expression: '' }
}

const ConstCompareOptions = () => {
  return { operator: 'EQUAL', value: '' }
}

export default Object.assign(config, {
  ruleTransientMap: null,
  ruleByType (type) {
    if (this.ruleTransientMap === null) {
      const map = {}
      this.rules.forEach(rule => {
        map[rule.type] = rule
      })
      this.ruleTransientMap = map
    }
    return this.ruleTransientMap[type]
  },
  ruleDefaults (type) {
    return this.ruleByType(type).options()
  },
  rules: [{
    type: 'EmptyRule', label: '空值检查', description: '检查字段值是否为NULL或空字符串', icon: 'dagConfig', options: EmptyOptions, property: () => import('./EmptyRuleProperty')
  }, {
    type: 'RangeRule', label: '范围检查', description: '检查字段值是否在给定的范围中', icon: 'dagConfig', options: RangeOptions, property: () => import('./RangeRuleProperty')
  }, {
    type: 'EnumRule', label: '枚举检查', description: '检查字段值是否在给定的枚举值中', icon: 'dagConfig', options: EnumOptions, property: () => import('./EnumRuleProperty')
  }, {
    type: 'RegularRule', label: '正则校验', description: '检查字段值是否匹配正则表达式', icon: 'dagConfig', options: RegularOptions, property: () => import('./RegularRuleProperty')
  }, {
    type: 'FormulaRule', label: '公式校验', description: '检查行记录是否满足自定义表达式', icon: 'dagConfig', options: FormulaOptions, property: () => import('./FormulaRuleProperty')
  }, {
    type: 'ReferRule', label: '引用检查', description: '检查字段值是否在给定的关联表中', icon: 'dagConfig', options: DefaultOptions, property: () => import('./ReferRuleProperty')
  }, {
    type: 'ConstCompareRule', label: '常量比较', description: '按分组字段对比计算值和常量', icon: 'dagConfig', options: ConstCompareOptions, property: () => import('./ConstCompareRuleProperty')
  }, {
    type: 'GroupCompareRule', label: '分组对比', description: '按分组字段比较两张表的计算值是否一致', icon: 'dagConfig', options: DefaultOptions, property: () => import('./GroupCompareRuleProperty')
  }],
  operators: [{
    label: '等于（=）', value: 'EQUAL'
  }, {
    label: '不等于（!=）', value: 'NOT_EQUAL'
  }, {
    label: '小于（<）', value: 'LESS_THAN'
  }, {
    label: '小于等于（<=）', value: 'LESS_THAN_OR_EQUAL'
  }, {
    label: '大于（>）', value: 'GREATER_THAN'
  }, {
    label: '大于等于（>=）', value: 'GREATER_THAN_OR_EQUAL'
  }],
  metrics: [
    { label: '不聚合', value: '' },
    { label: '求和（SUM）', value: 'SUM' },
    { label: '计数（COUNT）', value: 'COUNT' },
    { label: '去重计数（COUNT_DISTINCT）', value: 'COUNT_DISTINCT' },
    { label: '最大（MAX）', value: 'MAX' },
    { label: '最小（MIN）', value: 'MIN' },
    { label: '平均（AVG）', value: 'AVG' }
  ]
})
