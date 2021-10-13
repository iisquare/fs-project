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

const TextOptions = () => {
  return { field: '', value: '', placeholder: '' }
}

const TextareaOptions = () => {
  return { field: '', value: '', placeholder: '' }
}

const PasswordOptions = () => {
  return { field: '', value: '', placeholder: '' }
}

const NumberOptions = () => {
  return { field: '', value: 0, placeholder: '' }
}

const RadioOptions = () => {
  return { field: '', value: '', items: [], display: 'inline' }
}

const CheckboxOptions = () => {
  return { field: '', value: '', items: [], display: 'inline' }
}

const SwitchOptions = () => {
  return { field: '', value: false, txtChecked: '', txtUnChecked: '' }
}

const SelectOptions = () => {
  return { field: '', value: '', mode: 'default', items: [], allowClear: false }
}

const TxtOptions = () => {
  return { txt: '' }
}

const HtmlOptions = () => {
  return { html: '' }
}

const SubFormOptions = () => {
  return { formId: '', column: '' }
}

const GridOptions = () => {
  return { items: [ config.generateGridItem(), config.generateGridItem() ] }
}

const DividerOptions = () => {
  return { dashed: false, orientation: 'left', type: 'horizontal' }
}

const FormOptions = () => {
  return { align: 'left', labelType: 'grid', labelWidth: 100, labelSpan: 5, pageSize: 0, column: '', sort: '' }
}

export default Object.assign(config, {
  form: {
    options: FormOptions, property: () => import('./FormProperty')
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
      type: 'text', label: '单行文本', icon: 'formInput', filterable: true, viewable: true, sortable: true, editable: true, options: TextOptions, property: () => import('./TextProperty')
    }, {
      type: 'textarea', label: '多行文本', icon: 'formTextarea', filterable: true, viewable: true, sortable: false, editable: true, options: TextareaOptions, property: () => import('./TextareaProperty')
    }, {
      type: 'password', label: '密码', icon: 'formPassword', filterable: true, viewable: true, sortable: true, editable: true, options: PasswordOptions, property: () => import('./PasswordProperty')
    }, {
      type: 'number', label: '数值', icon: 'formNumber', filterable: true, viewable: true, sortable: true, editable: true, options: NumberOptions, property: () => import('./NumberProperty')
    }, {
      type: 'radio', label: '单选', icon: 'formRadio', filterable: true, viewable: true, sortable: true, editable: true, options: RadioOptions, property: () => import('./RadioProperty')
    }, {
      type: 'checkbox', label: '多选', icon: 'formCheckbox', filterable: true, viewable: true, sortable: true, editable: true, options: CheckboxOptions, property: () => import('./CheckboxProperty')
    }, {
      type: 'switch', label: '开关', icon: 'formSwitch', filterable: true, viewable: true, sortable: true, editable: true, options: SwitchOptions, property: () => import('./SwitchProperty')
    }, {
      type: 'select', label: '选择器', icon: 'formSelect', filterable: true, viewable: true, sortable: true, editable: true, options: SelectOptions, property: () => import('./SelectProperty')
    }, {
      type: 'txt', label: '文本', icon: 'formTxt', filterable: false, viewable: false, sortable: false, editable: false, options: TxtOptions, property: () => import('./TxtProperty')
    }, {
      type: 'html', label: 'HTML', icon: 'formHtml', filterable: false, viewable: false, sortable: false, editable: false, options: HtmlOptions, property: () => import('./HtmlProperty')
    }]
  }, {
    name: '高级字段',
    children: [{
      type: 'subform', label: '子表单', icon: 'formWrite', filterable: false, viewable: false, sortable: false, editable: true, options: SubFormOptions, property: () => import('./SubformProperty')
    }]
  }, {
    name: '布局字段',
    children: [{
      type: 'grid', label: '栅格布局', icon: 'formRow', filterable: true, viewable: true, sortable: true, editable: false, options: GridOptions, property: () => import('./GridProperty')
    }, {
      type: 'divider', label: '分割线', icon: 'formDivider', filterable: false, viewable: false, sortable: false, editable: false, options: DividerOptions, property: () => import('./DividerProperty')
    }]
  }],
  reservedFields: [{
    type: 'number', label: '提交者', icon: 'reserved', filterable: true, viewable: false, sortable: false, editable: false, options: { field: 'createdUid', value: '' }
  }, {
    type: 'number', label: '提交人', icon: 'reserved', filterable: false, viewable: true, sortable: false, editable: false, options: { field: 'createdUidName', value: '' }
  }, {
    type: 'number', label: '提交时间', icon: 'reserved', filterable: true, viewable: true, sortable: true, editable: false, options: { field: 'createdTime', value: '' }
  }, {
    type: 'number', label: '更新者', icon: 'reserved', filterable: true, viewable: false, sortable: false, editable: false, options: { field: 'updatedUid', value: '' }
  }, {
    type: 'number', label: '更新人', icon: 'reserved', filterable: false, viewable: true, sortable: false, editable: false, options: { field: 'updatedUidName', value: '' }
  }, {
    type: 'number', label: '更新时间', icon: 'reserved', filterable: true, viewable: true, sortable: true, editable: false, options: { field: 'updatedTime', value: '' }
  }, {
    type: 'text', label: '流程模型标识', icon: 'reserved', filterable: false, viewable: true, sortable: false, editable: false, options: { field: 'bpmWorkflowId', value: '' }
  }, {
    type: 'text', label: '流程模型名称', icon: 'reserved', filterable: false, viewable: true, sortable: false, editable: false, options: { field: 'bpmWorkflowName', value: '' }
  }, {
    type: 'text', label: '流程实例标识', icon: 'reserved', filterable: true, viewable: true, sortable: false, editable: false, options: { field: 'bpmInstanceId', value: '' }
  }, {
    type: 'text', label: '发起人标识', icon: 'reserved', filterable: false, viewable: true, sortable: false, editable: false, options: { field: 'bpmStartUserId', value: '' }
  }, {
    type: 'text', label: '发起人名称', icon: 'reserved', filterable: false, viewable: true, sortable: false, editable: false, options: { field: 'bpmStartUserName', value: '' }
  }],
  idField: { type: 'text', label: '主键', icon: 'reserved', filterable: true, viewable: true, sortable: true, editable: false, options: { field: '_id', value: '' } },
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
