import { layout } from '../config'

export const blanks = []

export const layouts = [{
  path: '/project',
  meta: { title: '项目管理' },
  component: layout.route,
  children: [{
    path: '/project/index/index',
    meta: { title: '工作面板' },
    component: layout.default
  }]
}]

export default { blanks, layouts }
