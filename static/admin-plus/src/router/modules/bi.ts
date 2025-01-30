import { layout } from '../config'

export const blanks = []

export const layouts = [{
  path: '/bi',
  meta: { title: '商业智能' },
  children: [{
    path: '/bi/index/index',
    meta: { title: '工作面板' },
    component: layout.default
  }]
}]

export default { blanks, layouts }
