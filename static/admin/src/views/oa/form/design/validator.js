
const validator = {
  config: null,
  fields (widgets, reduceSubform) {
    const result = {}
    widgets.forEach(widget => {
      if (widget.type === 'grid') {
        widget.options.items.forEach(item => {
          Object.assign(result, this.fields(item.widgets, reduceSubform))
        })
        return
      }
      if (!widget.options.field) return
      const field = widget.options.field
      if (widget.type === 'subform') {
        if (!widget.options.formInfo) return
        if (reduceSubform) { // 迭代处理子表单
          result[field] = this.fields(widget.options.formInfo.widgets, reduceSubform)
        } else {
          result[field] = widget
        }
        return
      }
      result[field] = widget
    })
    return result
  },
  prettySelector (items, value, mode) {
    items = this.config.selectorItemMap(items)
    switch (mode) {
      case 'default':
        if (items[value] && items[value].label) {
          value = items[value].label
        }
        break
      case 'multiple':
        if (!Array.isArray(value) || value.length < 1) {
          value = ''
          break
        }
        const array = []
        for (let v of value) {
          if (items[v] && items[v].label) v = items[v].label
          array.push(v)
        }
        value = array.join(',')
        break
      case 'tags':
        if (!Array.isArray(value) || value.length < 1) {
          value = ''
        } else {
          value = value.join(',')
        }
        break
    }
    return value
  },
  prettySwitch (options, value) {
    return value ? (options.txtChecked || '开') : (options.txtUnChecked || '关')
  },
  prettyWidget (widget, value) {
    const options = widget.options
    switch (widget.type) {
      case 'radio':
        return this.prettySelector(options.items, value, 'default')
      case 'checkbox':
        return this.prettySelector(options.items, value, 'multiple')
      case 'select':
        return this.prettySelector(options.items, value, options.mode)
      case 'switch':
        return this.prettySwitch(options, value)
      default:
        return value
    }
  },
  pretty (widgets, rows) {
    const result = []
    if (rows.length < 1) return result
    const fields = this.fields(widgets, false)
    for (const row of rows) {
      const data = {}
      for (const key in row) {
        const widget = fields[key]
        if (!widget) continue
        data[key] = this.prettyWidget(widget, row[key])
      }
      result.push(data)
    }
    return result
  },
  format (widgets, obj) {
    const result = {}
    widgets.forEach(widget => {
      const options = widget.options
      switch (widget.type) {
        case 'grid':
          widget.options.items.forEach(item => {
            Object.assign(result, this.format(item.widgets, obj))
          })
          break
        case 'text':
        case 'textarea':
        case 'password':
        case 'radio':
          if (!options.field) return
          if (typeof obj[options.field] === 'undefined') {
            result[options.field] = options.value
          } else {
            result[options.field] = obj[options.field]
          }
          break
        case 'number':
          if (!options.field) return
          if (typeof obj[options.field] === 'undefined') {
            result[options.field] = options.value
          } else {
            result[options.field] = Number.parseFloat(obj[options.field])
          }
          break
        case 'checkbox':
          if (!options.field) return
          if (Array.isArray(obj[options.field])) {
            result[options.field] = obj[options.field]
          } else if (options.value) {
            result[options.field] = options.value.split(',')
          } else {
            result[options.field] = []
          }
          break
        case 'select':
          if (!options.field) return
          if (['default', 'combobox'].indexOf(options.mode) !== -1) {
            if (typeof obj[options.field] === 'undefined') {
              result[options.field] = options.value
            } else {
              result[options.field] = obj[options.field]
            }
          } else if (['multiple', 'tags'].indexOf(options.mode) !== -1) {
            if (Array.isArray(obj[options.field])) {
              result[options.field] = obj[options.field]
            } else if (options.value) {
              result[options.field] = options.value.split(',')
            } else {
              result[options.field] = []
            }
          }
          break
        case 'switch':
          if (!options.field) return
          if (typeof obj[options.field] === 'undefined') {
            result[options.field] = options.value
          } else {
            result[options.field] = !!obj[options.field]
          }
          break
        case 'subform':
          if (!options.field) return
          if (Array.isArray(obj[options.field])) {
            result[options.field] = obj[options.field]
          } else {
            result[options.field] = []
          }
          break
      }
    })
    return result
  },
  generate (widgets, authority) {
    const result = {}
    widgets.forEach(widget => {
      const options = widget.options
      const rules = []
      switch (widget.type) {
        case 'grid':
          widget.options.items.forEach(item => {
            Object.assign(result, this.generate(item.widgets))
          })
          return
        case 'text':
        case 'textarea':
        case 'password':
          if (!options.field || !options.ruleEnabled) return
          if (options.required) {
            rules.push({ type: 'string', required: true, trigger: 'blur', message: options.requiredTooltip || `字段值不能为空` })
          }
          if (options.minLength > 0) {
            rules.push({ type: 'string', min: options.minLength, trigger: 'blur', message: options.minTooltip || `字段长度不能少于${options.minLength}` })
          }
          if (options.maxLength > 0) {
            rules.push({ type: 'string', max: options.maxLength, trigger: 'blur', message: options.maxTooltip || `字段长度不能多于${options.maxLength}` })
          }
          break
        case 'number':
          if (!options.field || !options.ruleEnabled) return
          if (options.minEnabled) {
            rules.push({
              message: options.minTooltip || `字段值不能小于${options.min}`,
              validator (rule, value, callback) {
                return value >= options.min
              }
            })
          }
          if (options.maxEnabled) {
            rules.push({
              message: options.maxTooltip || `字段值不能大于${options.max}`,
              validator (rule, value, callback) {
                return value <= options.max
              }
            })
          }
          break
        case 'radio':
          if (!options.field || !options.ruleEnabled) return
          if (options.required) {
            rules.push({ type: 'string', required: true, trigger: 'change', message: options.requiredTooltip || `选项不能为空` })
          }
          break
        case 'checkbox':
          if (!options.field || !options.ruleEnabled) return
          if (options.minLength > 0) {
            rules.push({
              message: options.minTooltip || `至少选择${options.minLength}项`,
              validator (rule, value, callback) {
                return value.length >= options.minLength
              }
            })
          }
          if (options.maxLength > 0) {
            rules.push({
              message: options.maxTooltip || `最多选择${options.maxLength}项`,
              validator (rule, value, callback) {
                return value.length <= options.maxLength
              }
            })
          }
          break
        case 'select':
          if (!options.field || !options.ruleEnabled) return
          if (['default', 'combobox'].indexOf(options.mode) !== -1) {
            if (options.minLength > 0) {
              rules.push({ type: 'string', required: true, trigger: 'change', message: options.minTooltip || `选项不能为空` })
            }
          } else if (['multiple', 'tags'].indexOf(options.mode) !== -1) {
            if (options.minLength > 0) {
              rules.push({
                message: options.minTooltip || `至少选择${options.minLength}项`,
                validator (rule, value, callback) {
                  return value.length >= options.minLength
                }
              })
            }
            if (options.maxLength > 0) {
              rules.push({
                message: options.maxTooltip || `最多选择${options.maxLength}项`,
                validator (rule, value, callback) {
                  return value.length <= options.maxLength
                }
              })
            }
          }
          break
        default:
          return
      }
      result[options.field] = authority[widget.id]?.editable ? rules : []
    })
    return result
  }
}

export default validator
