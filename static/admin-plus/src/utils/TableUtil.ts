import { ElMessageBox, type TableInstance } from 'element-plus';
import DataUtil from './DataUtil';

const TableUtil = {
  expandedRowKeys (tree: any, deep = -1, keyField = 'id', childrenField = 'children') {
    const result: any = [];
    (function walk (data, level) {
      if (!data) return
      data.forEach((element: any) => {
        result.push(element[keyField] + '')
        if (deep !== -1 && level < deep) {
          walk(element[childrenField], level + 1)
        }
      })
    })(tree, 1)
    return result
  },
  async selection (selection: any, idField: string = 'id') {
    return ElMessageBox.confirm('确认删除所选记录吗？', '操作提示', { type: 'warning', }).then(() => {
      return DataUtil.values(selection, idField)
    })
  },
  toggleRowSelection (table: TableInstance | undefined, selection: any, selected = true, idField = 'id', childrenField = 'children') {
    this.toggleRow(table, selection, idField, childrenField, (row: any) => {
      table?.toggleRowSelection(row, selected)
    })
  },
  toggleRowExpansion (table: TableInstance | undefined, selection: any, expanded = true, idField = 'id', childrenField = 'children') {
    this.toggleRow(table, selection, idField, childrenField, (row: any) => {
      table?.toggleRowExpansion(row, expanded)
    })
  },
  toggleRow (table: TableInstance | undefined, selection: any, idField: string, childrenField: string, callback: Function) {
    table && table.$nextTick(() => {
      (function walk (data) {
        data && data.forEach((row: any) => {
          walk(row[childrenField])
          if (selection.indexOf(row[idField]) !== -1) {
            callback(row)
          }
        })
      })(table.data)
    })
  },
  tree (rows: any, parentId: any = 0, idField = 'id', parentField = 'parentId', childField = 'children') {
    const result = []
    for (const item of rows) {
      if (item[parentField] !== parentId) continue
      const children: any = this.tree(rows, item[idField], idField, parentField, childField)
      if (children.length > 0) {
        result.push(Object.assign({}, item, { [childField]: children }))
      } else {
        result.push(Object.assign({}, item))
      }
    }
    return result
  },
  matrix (tree: any, colMinCount = 0, idField = 'id', childField = 'children') {
    let rowCount = 0 // 矩阵行数
    let colCount = colMinCount // 矩阵列数
    const items: any = {} // 元素索引
    const cells: any = {} // 实心元素
    const leaves: any = [] // 叶子节点
    ;(function generation (general: any, parents: any) {
      let count = 0 // 叶子节点个数（孙子节点所占行数）
      for (const siblingIndex in general) {
        const item: any = general[siblingIndex]
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
        const cell: any = { item, parents, rowIndex, columnIndex, siblingIndex, leafCount }
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
        const rowspan = cell ? Math.max(1, cell.leafCount) : 0
        const colspan = cell ? (next ? 1 : colCount - columnIndex) : 0
        const attrs = { rowIndex, columnIndex, rowspan, colspan }
        columns.push(Object.assign(attrs, cell || {}))
      }
      matrix.push(columns)
    }
    return { tree, matrix, rowCount, colCount, items, cells, leaves, idField, childField }
  },
  pretty (table: any) {
    console.log('*TableUtil.pretty')
    for (let rowIndex = 0; rowIndex < table.rowCount; rowIndex++) {
      const columns = []
      for (let columnIndex = 0; columnIndex < table.colCount; columnIndex++) {
        const cell = this.span(table, rowIndex, columnIndex)
        columns.push(`${cell.rowspan},${cell.colspan}`)
      }
      console.log(rowIndex, columns)
    }
  },
  span (table: any, rowIndex: any, columnIndex: any, columnOffset: number = 0) {
    const attrs = { rowspan: 1, colspan: 1 }
    columnIndex -= columnOffset
    if (rowIndex >= table.rowCount) return attrs
    if (columnIndex < 0 || columnIndex >= table.colCount) return attrs
    return table.matrix[rowIndex][columnIndex]
  },
  cellValue (table: any, rowIndex: any, columnIndex: any, labelField = 'name') {
    const cell = this.span(table, rowIndex, columnIndex)
    return cell.item ? cell.item[labelField] : ''
  },
}

export default TableUtil
