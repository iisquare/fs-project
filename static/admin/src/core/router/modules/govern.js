import { layout } from '../config'

export const blanks = []

export const layouts = [{
  path: '/govern',
  meta: { title: '数据治理' },
  component: layout.route,
  children: [{
    path: '/govern/index/index',
    meta: { title: '工作面板' },
    component: layout.default
  }]
}]

export default { blanks, layouts }
