import DesignUtil from '@/utils/DesignUtil'
import validator from './validator'
import exhibition from './exhibition'

const config: any = {
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

const CanvasOptions = () => {
  return { labelPosition: 'left', labelWidth: 100, pageSize: 0, column: '', sort: '' }
}

export default Object.assign(config, {
  canvas: { options: CanvasOptions, property: () => import('./CanvasProperty.vue') },
  widgets: DesignUtil.widgets([{
    name: '基础字段',
    children: [{
      type: 'text', label: '单行文本', icon: 'form.input', filterable: true, viewable: true, sortable: true, editable: true, options: TextOptions, property: () => import('./TextProperty.vue')
    }, {
      type: 'textarea', label: '多行文本', icon: 'form.textarea', filterable: true, viewable: true, sortable: false, editable: true, options: TextareaOptions, property: () => import('./TextareaProperty.vue')
    }, {
      type: 'password', label: '密码', icon: 'form.password', filterable: true, viewable: true, sortable: true, editable: true, options: PasswordOptions, property: () => import('./TextProperty.vue')
    }, {
      type: 'number', label: '数值', icon: 'form.number', filterable: true, viewable: true, sortable: true, editable: true, options: NumberOptions, property: () => import('./NumberProperty.vue')
    }, {
      type: 'radio', label: '单选', icon: 'form.radio', filterable: true, viewable: true, sortable: true, editable: true, options: RadioOptions, property: () => import('./RadioProperty.vue')
    }, {
      type: 'checkbox', label: '多选', icon: 'form.checkbox', filterable: true, viewable: true, sortable: true, editable: true, options: CheckboxOptions, property: () => import('./CheckboxProperty.vue')
    }, {
      type: 'switch', label: '开关', icon: 'form.switcher', filterable: true, viewable: true, sortable: true, editable: true, options: SwitchOptions, property: () => import('./SwitchProperty.vue')
    }, {
      type: 'select', label: '选择器', icon: 'form.select', filterable: true, viewable: true, sortable: true, editable: true, options: SelectOptions, property: () => import('./SelectProperty.vue')
    }, {
      type: 'txt', label: '文本', icon: 'form.txt', filterable: false, viewable: false, sortable: false, editable: false, options: TxtOptions, property: () => import('./TxtProperty.vue')
    }, {
      type: 'html', label: 'HTML', icon: 'form.html', filterable: false, viewable: false, sortable: false, editable: false, options: HtmlOptions, property: () => import('./HtmlProperty.vue')
    }]
  }, {
    name: '高级字段',
    children: [{
      type: 'subform', label: '子表单', icon: 'form.write', filterable: false, viewable: false, sortable: false, editable: true, options: SubFormOptions, property: () => import('./SubformProperty.vue')
    }]
  }, {
    name: '布局字段',
    children: [{
      type: 'grid', label: '栅格布局', icon: 'form.row', filterable: true, viewable: true, sortable: true, editable: false, options: GridOptions, property: () => import('./GridProperty.vue')
    }, {
      type: 'divider', label: '分割线', icon: 'form.divider', filterable: false, viewable: false, sortable: false, editable: false, options: DividerOptions, property: () => import('./DividerProperty.vue')
    }]
  }]),
  toolbars: [{
    type: 'pc', label: '电脑', icon: 'device.pc', selectable: true, callback (toolbar: any, instance: any, event: any) { instance.width = '100%' }
  }, {
    type: 'tablet', label: '平板', icon: 'device.tablet', selectable: true, selected: true, callback (toolbar: any, instance: any, event: any) { instance.width = '770px' }
  }, {
    type: 'mobile', label: '手机', icon: 'device.mobile', selectable: true, callback (toolbar: any, instance: any, event: any) { instance.width = '375px' }
  }],
  labelPositions: [{ value: 'left', label: '左对齐' }, { value: 'right', label: '右对齐' }, { value: 'top', label: '顶对齐' }]
})
