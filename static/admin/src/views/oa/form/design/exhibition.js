
const exhibition = {
  config: null,
  formLayout (frame) {
    const result = {}
    if (!frame) return result
    result.layout = ['top'].indexOf(frame.options.align) === -1 ? 'horizontal' : 'vertical'
    result.labelAlign = ['left', 'right'].indexOf(frame.options.align) === -1 ? 'left' : frame.options.align
    if (result.layout !== 'horizontal') return result
    if (frame.options.labelType === 'grid') {
      result.labelCol = { span: frame.options.labelSpan }
      result.wrapperCol = { span: 24 - frame.options.labelSpan }
    } else {
      result.class = ['fs-form-flex']
      result.labelCol = { flex: 0, style: { width: frame.options.labelWidth + 'px' } }
      result.wrapperCol = { flex: 0, style: { width: `calc(100% - ${frame.options.labelWidth}px)` } }
    }
    return result
  },
  authorityDefaults: {
    viewable: false, // 可查看
    editable: false, // 可编辑
    variable: false, // 作为流程变量
    addable: false, // 子表单可添加新纪录
    removable: false, // 子表单可删除已有记录
    changeable: false // 子表单可编辑已有记录
  },
  authority (fields, refer, defaults) {
    const result = {}
    if (!Array.isArray(fields)) return result
    if (!refer) refer = {}
    defaults = Object.assign({}, this.authorityDefaults, defaults || {})
    fields.forEach(widget => {
      const id = widget.id
      const item = {
        viewable: refer[id] ? refer[id].viewable : defaults.viewable,
        editable: refer[id] ? refer[id].editable : defaults.editable,
        variable: refer[id] ? refer[id].variable : defaults.variable
      }
      if (widget.type === 'subform') {
        Object.assign(item, {
          addable: refer[id] ? refer[id].addable : defaults.addable,
          removable: refer[id] ? refer[id].removable : defaults.removable,
          changeable: refer[id] ? refer[id].changeable : defaults.changeable
        })
        Object.assign(result, this.authority(widget.children, refer, defaults))
      }
      result[id] = item
    })
    return result
  },
  authorityFields (widgets, expandedRowKeys = []) {
    const result = []
    if (!Array.isArray(widgets)) return result
    widgets.forEach(widget => {
      if (widget.type === 'grid') {
        widget.options.items.forEach(item => {
          result.push(...this.authorityFields(item.widgets, expandedRowKeys))
        })
        return
      }
      const field = widget.options.field
      const label = widget.label
      const wc = this.config.widgetByType(widget.type)
      if (widget.type === 'subform') {
        if (!widget.options.formInfo) return
        expandedRowKeys.push(widget.id)
        const children = this.authorityFields(widget.options.formInfo.widgets, expandedRowKeys)
        result.push(Object.assign({}, widget, { id: widget.id, field, label, editable: wc.editable, children }))
        return
      }
      if (!wc.editable) return
      result.push(Object.assign({}, widget, { id: widget.id, field, label, editable: wc.editable }))
    })
    return result
  },
  operateFields (widgets, useField, withChildren, prefixField = '', prefixLabel = '') {
    const result = []
    if (!Array.isArray(widgets)) return result
    widgets.forEach(widget => {
      if (Object.keys(widget).indexOf(useField) === -1) {
        if (!this.config.widgetByType(widget.type)[useField]) return
      } else {
        if (!widget[useField]) return
      }
      if (widget.type === 'grid') {
        widget.options.items.forEach(item => {
          result.push(...this.operateFields(item.widgets, useField, withChildren, prefixField, prefixLabel))
        })
        return
      }
      if (!widget.options.field) return
      const field = prefixField + widget.options.field
      const label = prefixLabel + widget.label
      if (widget.type === 'subform') {
        if (!widget.options.formInfo) return
        const children = this.operateFields(widget.options.formInfo.widgets, useField, withChildren, field + '.', label + '.')
        if (withChildren) {
          result.push(Object.assign({}, widget, { field, label, children }))
        } else {
          result.push(...children)
        }
        return
      }
      result.push(Object.assign({}, widget, { field, label }))
    })
    return result
  },
  generateColumnItem (source, sorted, defaultEnabled) {
    const item = { field: source.field, label: source.label, enabled: sorted ? !!sorted.enabled : defaultEnabled }
    if (source.type === 'subform') {
      item.children = this.mergeColumnItem(source.children, sorted ? sorted.children || [] : [])
    }
    return item
  },
  mergeColumnNextSort (inserted, destination, sortedMap) {
    if (inserted >= destination.length) return null // 步骤五、已寻找至末尾，未找到下个排序项，则将b移动到a之后
    if (sortedMap[destination[inserted].field]) return inserted // 步骤四、找到下个排序项，则将b移动到该项之前
    return this.mergeColumnNextSort(inserted + 1, destination, sortedMap)
  },
  mergeColumnItem (source, sorted) {
    const destination = [] // 目标结果
    const sortedMap = {} // 已排序项索引，field => item
    const defaultEnabled = sorted.length < 1 // 是否默认展示
    sorted.forEach(item => { sortedMap[item.field] = item })
    const indexMap = {} // 目标结果索引，field => { item, index }
    source.forEach((item, index) => {
      item = this.generateColumnItem(item, sortedMap[item.field], defaultEnabled)
      indexMap[item.field] = { item, index }
      destination.push(item) // 步骤一、按源数据生成待排序数组
    })
    for (let index = sorted.length - 1; index >= 0; index--) { // 步骤二、移除已排序数组的缺失项
      if (indexMap[sorted[index].field]) continue
      sorted.splice(index, 1)
    }
    for (let index = 1; index < sorted.length; index++) { // 执行排序
      const a = sorted[index - 1]
      const b = sorted[index]
      if (indexMap[a.field].index + 1 === indexMap[b.field].index) continue // 顺序一致
      if (indexMap[a.field].index > indexMap[b.field].index) { // 步骤三、b在a之前，直接将b移动到a之后
        destination.splice(indexMap[a.field].index + 1, 0, indexMap[b.field].item)
        destination.splice(indexMap[b.field].index, 1)
      } else { // b在a之后
        const will = indexMap[a.field].index + 1 // 在a之后插入元素
        let real = this.mergeColumnNextSort(will, destination, sortedMap) // 跳过新增项，寻找合适的插入点
        if (real === null) real = will
        destination.splice(real, 0, indexMap[b.field].item)
        destination.splice(indexMap[b.field].index + 1, 1)
      }
      destination.forEach((v, k) => { indexMap[v.field] = { item: v, index: k } }) // 重建目标结果索引
    }
    return destination
  },
  parseColumnSorted (column) {
    const result = []
    if (!column) return result
    const columns = column.split(',')
    for (let item of columns) {
      item = item.trim()
      if (!item) continue
      result.push({ field: item, label: item, enabled: true })
    }
    return result
  },
  tableColumns (sorted) {
    const result = []
    for (const item of sorted) {
      if (!item.enabled) continue
      result.push({ title: item.label, dataIndex: item.field })
    }
    return result
  },
  parseSortor (sort) {
    const result = []
    if (!sort) return result
    const columns = sort.split(',')
    for (const item of columns) {
      const sorts = item.split('.')
      const field = sorts[0].trim()
      if (!field) continue
      const direction = sorts.length > 1 ? sorts[1].trim() : 'asc'
      result.push({ field, direction })
    }
    return result
  }
}

export default exhibition
