/**
 * 分支节点（条件分支 switch-case-default / 问题分类器）的几何布局与锚点定义。
 *
 * 节点内部按 header + N 行分支进行等高排布，右侧每个分支行对应一个输出锚点，
 * 因此布局常量同时被节点组件（SwitchNode.vue）与锚点计算复用，保证锚点与分支行对齐。
 * 两者仅行来源不同：条件分支为 cases + 默认分支，问题分类器为 classes（没有默认分支）。
 */

export interface SwitchRow {
  id: string
  name: string
  summary: string
}

const width = 240
const header = 36
const padding = 6
const row = 28

export const defaultPortId = 'default'

export const casePortId = (item: any) => `case-${item?.id ?? ''}`

export const classPortId = (item: any) => `class-${item?.id ?? ''}`

// 分支锚点与普通节点保持一致：默认隐藏，鼠标移入节点时才显示
const AnchorAttr = {
  circle: {
    r: 4,
    magnet: true,
    stroke: '#5F95FF',
    strokeWidth: 1,
    fill: '#fff',
    style: { visibility: 'hidden' },
  },
}

const rows = (data: any = {}): SwitchRow[] => {
  // 问题分类器：每个分类一行、一行一个输出锚点，分类增删即锚点增删
  if (Array.isArray(data.classes)) {
    return data.classes.map((item: any) => ({
      id: classPortId(item),
      name: item.name || '未命名分类',
      summary: '',
    }))
  }
  const cases: any[] = Array.isArray(data.cases) ? data.cases : []
  const result: SwitchRow[] = cases.map((item: any) => ({
    id: casePortId(item),
    name: item.name || '未命名分支',
    summary: `${(item.conditions ?? []).length} 个条件`,
  }))
  result.push({
    id: defaultPortId,
    name: data.defaultName || '否则（默认）',
    summary: '其他情况',
  })
  return result
}

const height = (data: any = {}) => header + padding * 2 + rows(data).length * row

const portY = (index: number) => header + padding + index * row + row / 2

const ports = (data: any = {}) => {
  const items: any[] = [{ id: 'in', group: 'in' }]
  rows(data).forEach((item: SwitchRow, index: number) => {
    items.push({
      id: item.id,
      group: 'out',
      args: { x: width, y: portY(index) },
    })
  })
  return {
    groups: {
      in: { position: 'left', attrs: AnchorAttr },
      out: { position: { name: 'absolute', args: {} }, attrs: AnchorAttr },
    },
    items,
  }
}

export const SwitchLayout = {
  width,
  header,
  padding,
  row,
  rows,
  height,
  portY,
  ports,
}

export default SwitchLayout
