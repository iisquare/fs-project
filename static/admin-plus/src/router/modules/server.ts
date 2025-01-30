import { layout } from '../config'

export const blanks = []

export const layouts = [{
  path: '/server',
  meta: { title: '服务管理' },
  children: [{
    path: '/server/index/index',
    meta: { title: '工作面板' },
    component: layout.default
  }]
}]

export default { blanks, layouts }
