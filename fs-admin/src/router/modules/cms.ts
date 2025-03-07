import { layout } from '../config'

export const blanks = []

export const layouts = [{
  path: '/cms',
  meta: { title: '内容管理' },
  children: [{
    path: '/cms/index/index',
    meta: { title: '工作面板' },
    component: layout.default
  }]
}]

export default { blanks, layouts }
