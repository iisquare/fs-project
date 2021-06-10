import validator from './validator'
import exhibition from './exhibition'

const config = {
  validator,
  exhibition,
  uuid () { return new Date().getTime() + ('' + Math.random()).slice(-6) },
  uuidRegular () { return 'regular-' + this.uuid() },
  uuidWidget () { return 'widget-' + this.uuid() },
  uuidGrid () { return 'grid-' + this.uuid() },
  uuidFilter () { return 'filter-' + this.uuid() },
  uuidForm () { return 'form-' + this.uuid() },
  uuidRecord () { return 'record-' + this.uuid() },
  generateGridItem () {
    return { id: this.uuidGrid(), span: 12, widgets: [] }
  },
  generateFilterRelation () {
    return { id: this.uuidFilter(), enabled: true, type: 'RELATION', value: 'AND', children: [] }
  },
  generateFilterOperation () {
    return { id: this.uuidFilter(), enabled: true, type: 'FILTER', operation: 'EQUAL', field: '', value: '' }
  }
}

validator.config = config
exhibition.config = config

const textOptions = () => {
  return { field: '', value: '', placeholder: '' }
}

const textareaOptions = () => {
  return { field: '', value: '', placeholder: '' }
}

const passwordOptions = () => {
  return { field: '', value: '', placeholder: '' }
}

const numberOptions = () => {
  return { field: '', value: 0, placeholder: '' }
}

const radioOptions = () => {
  return { field: '', value: '', items: [], display: 'inline' }
}

const checkboxOptions = () => {
  return { field: '', value: '', items: [], display: 'inline' }
}

const switchOptions = () => {
  return { field: '', value: false, txtChecked: '', txtUnChecked: '' }
}

const selectOptions = () => {
  return { field: '', value: '', mode: 'default', items: [], allowClear: false }
}

const txtOptions = () => {
  return { txt: '' }
}

const htmlOptions = () => {
  return { html: '' }
}

const subformOptions = () => {
  return { formId: '', column: '' }
}

const gridOptions = () => {
  return { items: [ config.generateGridItem(), config.generateGridItem() ] }
}

const dividerOptions = () => {
  return { dashed: false, orientation: 'left', type: 'horizontal' }
}

const formOptions = () => {
  return { align: 'left', labelType: 'grid', labelWidth: 100, labelSpan: 5, pageSize: 0, column: '', sort: '' }
}

export default Object.assign(config, {
  form: {
    options: formOptions, property: () => import('./FormProperty')
  },
  selectorItemMap (items) {
    const result = {}
    for (const index in items) {
      const item = items[index]
      result[item.value] = item
    }
    return result
  },
  widgetTransientMap: null,
  widgetByType (type) {
    if (this.widgetTransientMap === null) {
      const map = {}
      this.widgets.forEach(widget => {
        widget.children.forEach(item => {
          map[item.type] = item
        })
      })
      this.widgetTransientMap = map
    }
    return this.widgetTransientMap[type]
  },
  widgetDefaults (type) {
    return this.widgetByType(type).options()
  },
  widgets: [{
    name: '基础字段',
    children: [{
      type: 'text', label: '单行文本', icon: 'formInput', filterable: true, viewable: true, sortable: true, options: textOptions, property: () => import('./TextProperty')
    }, {
      type: 'textarea', label: '多行文本', icon: 'formTextarea', filterable: true, viewable: true, sortable: false, options: textareaOptions, property: () => import('./TextareaProperty')
    }, {
      type: 'password', label: '密码', icon: 'formPassword', filterable: true, viewable: true, sortable: true, options: passwordOptions, property: () => import('./PasswordProperty')
    }, {
      type: 'number', label: '数值', icon: 'formNumber', filterable: true, viewable: true, sortable: true, options: numberOptions, property: () => import('./NumberProperty')
    }, {
      type: 'radio', label: '单选', icon: 'formRadio', filterable: true, viewable: true, sortable: true, options: radioOptions, property: () => import('./RadioProperty')
    }, {
      type: 'checkbox', label: '多选', icon: 'formCheckbox', filterable: true, viewable: true, sortable: true, options: checkboxOptions, property: () => import('./CheckboxProperty')
    }, {
      type: 'switch', label: '开关', icon: 'formSwitch', filterable: true, viewable: true, sortable: true, options: switchOptions, property: () => import('./SwitchProperty')
    }, {
      type: 'select', label: '选择器', icon: 'formSelect', filterable: true, viewable: true, sortable: true, options: selectOptions, property: () => import('./SelectProperty')
    }, {
      type: 'txt', label: '文本', icon: 'formTxt', filterable: false, viewable: false, sortable: false, options: txtOptions, property: () => import('./TxtProperty')
    }, {
      type: 'html', label: 'HTML', icon: 'formHtml', filterable: false, viewable: false, sortable: false, options: htmlOptions, property: () => import('./HtmlProperty')
    }]
  }, {
    name: '高级字段',
    children: [{
      type: 'subform', label: '子表单', icon: 'formWrite', filterable: false, viewable: false, sortable: false, options: subformOptions, property: () => import('./SubformProperty')
    }]
  }, {
    name: '布局字段',
    children: [{
      type: 'grid', label: '栅格布局', icon: 'formRow', filterable: true, viewable: true, sortable: true, options: gridOptions, property: () => import('./GridProperty')
    }, {
      type: 'divider', label: '分割线', icon: 'formDivider', filterable: false, viewable: false, sortable: false, options: dividerOptions, property: () => import('./DividerProperty')
    }]
  }],
  reservedFields: [{
    type: 'number', label: '提交者', icon: 'reserved', filterable: true, viewable: false, sortable: false, options: { field: 'createdUid', value: '' }
  }, {
    type: 'number', label: '提交人', icon: 'reserved', filterable: false, viewable: true, sortable: false, options: { field: 'createdUidName', value: '' }
  }, {
    type: 'number', label: '提交时间', icon: 'reserved', filterable: true, viewable: true, sortable: true, options: { field: 'createdTime', value: '' }
  }, {
    type: 'number', label: '更新者', icon: 'reserved', filterable: true, viewable: false, sortable: false, options: { field: 'updatedUid', value: '' }
  }, {
    type: 'number', label: '更新人', icon: 'reserved', filterable: false, viewable: true, sortable: false, options: { field: 'updatedUidName', value: '' }
  }, {
    type: 'number', label: '更新时间', icon: 'reserved', filterable: true, viewable: true, sortable: true, options: { field: 'updatedTime', value: '' }
  }, {
    type: 'text', label: '流程实例', icon: 'reserved', filterable: false, viewable: true, sortable: false, options: { field: 'bpmInstance', value: '' }
  }, {
    type: 'text', label: '流程状态', icon: 'reserved', filterable: true, viewable: true, sortable: false, options: { field: 'bpmStatus', value: '' }
  }, {
    type: 'text', label: '当前节点', icon: 'reserved', filterable: true, viewable: true, sortable: false, options: { field: 'bpmTask', value: '' }
  }, {
    type: 'text', label: '当前负责人', icon: 'reserved', filterable: false, viewable: true, sortable: false, options: { field: 'bpmIdentity', value: '' }
  }],
  idField: { type: 'text', label: '主键', icon: 'reserved', filterable: true, viewable: true, sortable: true, options: { field: '_id', value: '' } },
  devices: [{
    type: 'pc', label: '电脑', icon: 'devicePc', width: '100%'
  }, {
    type: 'tablet', label: '平板', icon: 'deviceTablet', width: '770px'
  }, {
    type: 'mobile', label: '手机', icon: 'deviceMobile', width: '375px'
  }],
  relations: [{
    label: '并且（AND）', value: 'AND'
  }, {
    label: '或者（OR）', value: 'OR'
  }],
  filters: [{
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
  }, {
    label: '为空（is null）', value: 'IS_NULL'
  }, {
    label: '不为空（is not null）', value: 'IS_NOT_NULL'
  }, {
    label: '包含（like）', value: 'LIKE'
  }, {
    label: '不包含（not like）', value: 'NOT_LIKE'
  }, {
    label: '在列表中（in）', value: 'IN'
  }, {
    label: '不在列表中（not in）', value: 'NOT_IN'
  }]
})
