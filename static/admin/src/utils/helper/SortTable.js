import Vue from 'vue'

class SortTable {
  constructor (rows, selection = {}) {
    this.rows = Array.isArray(rows) ? rows : []
    this.selection = Object.assign({}, selection, {
      type: 'radio',
      selectedRowKeys: [],
      onChange (selectedRowKeys, selectedRows) {
        this.selectedRowKeys = selectedRowKeys
      }
    })
  }

  reset (rows) {
    if (Array.isArray(rows)) this.rows = rows
    this.selection.selectedRowKeys = []
  }

  selectedIndex () {
    if (this.selection.selectedRowKeys.length > 0) {
      return this.selection.selectedRowKeys[0]
    }
    return -1
  }

  add (item) {
    this.rows.push(item)
    return item
  }

  insert (item) {
    const index = this.selectedIndex()
    this.rows.splice(index === -1 ? index + 1 : index, 0, item)
    if (index !== -1) {
      this.selection.selectedRowKeys = [index + 1]
    }
    return item
  }

  up () {
    const index = this.selectedIndex()
    if (index < 1) {
      return false
    }
    const row = this.rows[index]
    Vue.set(this.rows, index, this.rows[index - 1])
    Vue.set(this.rows, index - 1, row)
    this.selection.selectedRowKeys = [index - 1]
    return true
  }

  down () {
    const index = this.selectedIndex()
    if (index < 0 || index >= this.rows.length - 1) {
      return false
    }
    const row = this.rows[index]
    Vue.set(this.rows, index, this.rows[index + 1])
    Vue.set(this.rows, index + 1, row)
    this.selection.selectedRowKeys = [index + 1]
    return true
  }

  remove () {
    const index = this.selectedIndex()
    if (index === -1) {
      return false
    }
    this.rows.splice(index, 1)
    this.selection.selectedRowKeys = []
    return true
  }
}

export default SortTable
