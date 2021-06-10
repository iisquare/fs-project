import store from '@/core/store'

const DataUtil = {
  trim (str, charlist = ' ') {
    if (str == null) return ''
    if (Object.prototype.toString.call(str) !== '[object String]') return ''
    str = str.replace(new RegExp('^[' + charlist + ']+'), '')
    str = str.replace(new RegExp('[' + charlist + ']+$'), '')
    return str
  },
  empty (data) {
    if (data == null) return true
    if (data instanceof Array && data.length < 1) return true
    if (data instanceof Object && Object.getOwnPropertyNames(data).length < 1) return true
    if (Object.prototype.toString.call(data) === '[object String]' && this.trim(data).length < 1) return true
    return false
  },
  cache: store.getters['cache/load'],
  tableTree (rows, parentId, idField = 'id', parentField = 'parentId', childField = 'children') {
    const result = []
    for (const item of rows) {
      if (item[parentField] !== parentId) continue
      const children = this.tableTree(rows, item[idField], idField, parentField, childField)
      result.push(Object.assign({}, item, { [childField]: children }))
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
  }
}
export default DataUtil
