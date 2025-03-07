import { layout } from '../config'

export const blanks = []

export const layouts = [{
  path: '/spider',
  meta: { title: '数据采集' },
  children: [{
    path: '/spider/index/index',
    meta: { title: '工作面板' },
    component: layout.default
  }]
}]

export default { blanks, layouts }
