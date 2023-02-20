import store from '@/core/store'

const UIUtil = {
  cache: store.getters['cache/load'],
  kv (key, value, ttl) {
    if (arguments.length === 1) {
      return store.getters['cache/get'](key)
    } else {
      return store.getters['cache/set'](key, value, ttl)
    }
  },
  tableTree (rows, parentId = 0, idField = 'id', parentField = 'parentId', childField = 'children') {
    const result = []
    for (const item of rows) {
      if (item[parentField] !== parentId) continue
      const children = this.tableTree(rows, item[idField], idField, parentField, childField)
      if (children.length > 0) {
        result.push(Object.assign({}, item, { [childField]: children }))
      } else {
        result.push(Object.assign({}, item))
      }
    }
    return result
  },
  tableMatrix (tree, colMinCount = 0, idField = 'id', childField = 'children') {
    let rowCount = 0 // 矩阵行数
    let colCount = colMinCount // 矩阵列数
    const items = {} // 元素索引
    const cells = {} // 实心元素
    const leaves = [] // 叶子节点
    ;(function generation (general, parents) {
      let count = 0 // 叶子节点个数（孙子节点所占行数）
      for (const siblingIndex in general) {
        const item = general[siblingIndex]
        const rowIndex = rowCount // 所在行
        const columnIndex = parents.length // 所在列
        colCount = Math.max(colCount, columnIndex + 1)
        const leafCount = generation(item[childField], parents.concat([item[idField]]))
        if (leafCount > 0) {
          count += leafCount
        } else { // 当前为叶子节点
          count += 1
          rowCount++ // 优先累计叶子节点
          leaves.push(item)
        }
        const cell = { item, parents, rowIndex, columnIndex, siblingIndex, leafCount }
        items[item[idField]] = cells[`${rowIndex}_${columnIndex}`] = cell
      }
      return count
    })(tree, [])
    const matrix = [] // 布局矩阵
    // 填充属性值
    for (let rowIndex = 0; rowIndex < rowCount; rowIndex++) {
      const columns = []
      for (let columnIndex = 0; columnIndex < colCount; columnIndex++) {
        const cell = cells[`${rowIndex}_${columnIndex}`]
        const next = cells[`${rowIndex}_${columnIndex + 1}`]
        const rowSpan = cell ? Math.max(1, cell.leafCount) : 0
        const colSpan = cell ? (next ? 1 : colCount - columnIndex) : 0
        const attrs = { rowIndex, columnIndex, rowSpan, colSpan }
        columns.push(Object.assign(attrs, cell || {}))
      }
      matrix.push(columns)
    }
    return { tree, matrix, rowCount, colCount, items, cells, leaves, idField, childField }
  },
  tablePretty (table) {
    console.log('*DataUtil.tablePretty')
    for (let rowIndex = 0; rowIndex < table.rowCount; rowIndex++) {
      const columns = []
      for (let columnIndex = 0; columnIndex < table.colCount; columnIndex++) {
        const cell = this.tableSpan(table, rowIndex, columnIndex)
        columns.push(`${cell.rowSpan},${cell.colSpan}`)
      }
      console.log(rowIndex, columns)
    }
  },
  tableSpan (table, rowIndex, columnIndex) {
    const attrs = { rowSpan: 1, colSpan: 1 }
    if (rowIndex >= table.rowCount) return attrs
    if (columnIndex >= table.colCount) return attrs
    return table.matrix[rowIndex][columnIndex]
  },
  tableSpanRender (table, rowIndex, columnIndex, labelField = 'name') {
    const cell = this.tableSpan(table, rowIndex, columnIndex)
    return { children: cell.item ? cell.item[labelField] : '', attrs: cell }
  },
  treeSelect (data, formatter = null) {
    const tree = []
    const expandedKeys = []
    if (!Array.isArray(data)) return { tree, expandedKeys }
    if (formatter === null) {
      formatter = item => { return { value: item.id, label: item.name } }
    }
    data.forEach(item => {
      const node = formatter(item)
      const result = this.treeSelect(item.children, formatter)
      node.children = result.tree
      expandedKeys.push(node.key, ...result.expandedKeys)
      tree.push(node)
    })
    return { tree, expandedKeys }
  },
  treeNode (item, formatter = null) {
    if (formatter !== null) {
      item = formatter(item)
    }
    return { key: item.id, title: item.name || item.id, data: item, children: [] }
  },
  treeData (data, formatter = null) {
    const tree = []
    const expandedKeys = []
    if (!Array.isArray(data)) return { tree, expandedKeys }
    data.forEach(item => {
      const node = this.treeNode(item, formatter)
      const result = this.treeData(item.children, formatter)
      node.children = result.tree
      expandedKeys.push(node.key, ...result.expandedKeys)
      tree.push(node)
    })
    return { tree, expandedKeys }
  },
  treeRemove (tree, node) {
    if (!tree || !node) return false
    for (const index in tree) {
      const item = tree[index]
      if (item.key === node.key) {
        tree.splice(index, 1)
        return true
      }
      if (this.treeRemove(item.children, node)) return true
    }
    return false
  },
  treeDrop (tree, info) {
    const dropKey = info.node.eventKey
    const dragKey = info.dragNode.eventKey
    const dropPos = info.node.pos.split('-')
    const dropPosition = info.dropPosition - Number(dropPos[dropPos.length - 1])
    const loop = (data, key, callback) => {
      data.forEach((item, index, arr) => {
        if (item.key === key) {
          return callback(item, index, arr)
        }
        if (item.children) {
          return loop(item.children, key, callback)
        }
      })
    }
    const data = [...tree]

    // Find dragObject
    let dragObj
    loop(data, dragKey, (item, index, arr) => {
      arr.splice(index, 1)
      dragObj = item
    })
    if (!info.dropToGap) {
      // Drop on the content
      loop(data, dropKey, (item) => {
        item.children = item.children || []
        // where to insert 示例添加到尾部，可以是随意位置
        item.children.push(dragObj)
      })
    } else if (
      (info.node.children || []).length > 0 && // Has children
      info.node.expanded && // Is expanded
      dropPosition === 1 // On the bottom gap
    ) {
      loop(data, dropKey, (item) => {
        item.children = item.children || []
        // where to insert 示例添加到尾部，可以是随意位置
        item.children.unshift(dragObj)
      })
    } else {
      let ar
      let i
      loop(data, dropKey, (item, index, arr) => {
        ar = arr
        i = index
      })
      if (dropPosition === -1) {
        ar.splice(i, 0, dragObj)
      } else {
        ar.splice(i + 1, 0, dragObj)
      }
    }
    return data
  },
  prettyFileSize (size) {
    if (Number.isNaN(size)) return ''
    const units = [{
      measure: 1, unit: 'B'
    }, {
      measure: 1024, unit: 'KB'
    }, {
      measure: 1024 * 1024, unit: 'MB'
    }, {
      measure: 1024 * 1024 * 1024, unit: 'GB'
    }, {
      measure: 1024 * 1024 * 1024 * 1024, unit: 'TB'
    }, {
      measure: 1024 * 1024 * 1024 * 1024 * 1024, unit: 'PB'
    }]
    for (let index = units.length - 1; index >= 0; index--) {
      const item = units[index]
      if (size >= item.measure) return Math.round(size / item.measure) + item.unit
    }
    return size + ''
  },
  formLayoutFlex (layout = {}) {
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
  uuid (prefix = '') {
    return prefix + new Date().getTime() + ('' + Math.random()).slice(-6)
  },
  filterOption (inputValue, option) {
    return option.componentOptions.children[0].text.toUpperCase().indexOf(inputValue.toUpperCase()) >= 0
  },
  highlight (highlight, glue = '') {
    const result = {}
    for (const key in highlight) {
      result[key] = highlight[key].join(glue)
    }
    return result
  }
}
export default UIUtil
