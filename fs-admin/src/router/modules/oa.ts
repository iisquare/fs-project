import { layout } from '../config'

export const blanks = []

export const layouts = [{
  path: '/oa',
  meta: { title: '在线办公' },
  children: [{
    path: '/oa/index/index',
    meta: { title: '工作面板' },
    component: layout.default
  }]
}]

export default { blanks, layouts }
