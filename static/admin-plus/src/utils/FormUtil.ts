const FormUtil = {
  layoutFlex (layout: any = {}) {
    layout = Object.assign({}, {
      labelWidth: 100,
      labelAlign: 'right'
    }, layout)
    const result = {
      layout: 'horizontal',
      labelAlign: layout.labelAlign,
      class: 'ui-form-flex',
      labelCol: { flex: 0, style: { width: layout.labelWidth + 'px' } },
      wrapperCol: { flex: 0, style: { width: `calc(100% - ${layout.labelWidth}px)` } }
    }
    return result
  },
}

export default FormUtil
