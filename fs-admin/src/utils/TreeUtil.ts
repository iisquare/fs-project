const TreeUtil = {
  ids (data: any, filter: Function | undefined = undefined, idField: string = 'id', childField: string = 'children') {
    const result: any = []
    data && data.map((item: any) => {
      if (!filter || !filter(item)) result.push(item[idField])
      result.push(...TreeUtil.ids(item[childField], filter, idField, childField))
    })
    return result
  },
  select (data: any, formatter: any = null) {
    const tree: any = []
    const expandedKeys: any = []
    if (!Array.isArray(data)) return { tree, expandedKeys }
    if (formatter === null) {
      formatter = (item: any) => { return { value: item.id, label: item.name } }
    }
    data.forEach(item => {
      const node = formatter(item)
      const result = this.select(item.children, formatter)
      node.children = result.tree
      expandedKeys.push(node.key, ...result.expandedKeys)
      tree.push(node)
    })
    return { tree, expandedKeys }
  },
  node (item: any, formatter: any = null) {
    if (formatter !== null) {
      item = formatter(item)
    }
    return { key: item.id, title: item.name || item.id, data: item, children: [] }
  },
  data (data: any, formatter = null) {
    const tree: any = []
    const expandedKeys: any = []
    if (!Array.isArray(data)) return { tree, expandedKeys }
    data.forEach(item => {
      const node = this.node(item, formatter)
      const result = this.data(item.children, formatter)
      node.children = result.tree
      expandedKeys.push(node.key, ...result.expandedKeys)
      tree.push(node)
    })
    return { tree, expandedKeys }
  },
  nodes (data: any, keyField = 'id', childField = 'children') {
    const result: any = {}
    if (!data) return result
    for (const index in data) {
      const item = data[index]
      result[item[keyField]] = item
      Object.assign(result, this.nodes(item[childField], keyField, childField))
    }
    return result
  },
  remove (tree: any, node: any) {
    if (!tree || !node) return false
    for (const index in tree) {
      const item = tree[index]
      if (item.key === node.key) {
        tree.splice(index, 1)
        return true
      }
      if (this.remove(item.children, node)) return true
    }
    return false
  },
  drop (tree: any, info: any) {
    const dropKey = info.node.eventKey
    const dragKey = info.dragNode.eventKey
    const dropPos = info.node.pos.split('-')
    const dropPosition = info.dropPosition - Number(dropPos[dropPos.length - 1])
    const loop = (data: any, key: any, callback: any) => {
      data.forEach((item: any, index: any, arr: any) => {
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
    let dragObj: any
    loop(data, dragKey, (item: any, index: any, arr: any) => {
      arr.splice(index, 1)
      dragObj = item
    })
    if (!info.dropToGap) {
      // Drop on the content
      loop(data, dropKey, (item: any) => {
        item.children = item.children || []
        // where to insert 示例添加到尾部，可以是随意位置
        item.children.push(dragObj)
      })
    } else if (
      (info.node.children || []).length > 0 && // Has children
      info.node.expanded && // Is expanded
      dropPosition === 1 // On the bottom gap
    ) {
      loop(data, dropKey, (item: any) => {
        item.children = item.children || []
        // where to insert 示例添加到尾部，可以是随意位置
        item.children.unshift(dragObj)
      })
    } else {
      let ar: any
      let i: any
      loop(data, dropKey, (item: any, index: any, arr: any) => {
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
}

export default TreeUtil
