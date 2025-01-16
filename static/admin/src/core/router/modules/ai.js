import { layout } from '../config'

export const blanks = []

export const layouts = [{
  path: '/ai',
  meta: { title: '人工智能' },
  component: layout.route,
  children: [{
    path: '/ai/index/index',
    meta: { title: '工作面板' },
    component: layout.default
  }]
}]

export default { blanks, layouts }
