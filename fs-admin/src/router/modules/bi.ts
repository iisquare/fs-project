import { layout } from '../config'

export const blanks = []

export const layouts = [{
  path: '/bi',
  meta: { title: '商业智能' },
  children: [{
    path: '/bi/index/index',
    meta: { title: '工作面板' },
    component: layout.default
  }, {
    path: '/bi/data/datasource',
    meta: { title: '数据源管理', permit: ['bi:datasource:'] },
    component: () => import('@/views/bi/data/datasource.vue')
  }, {
    path: '/bi/data/dataset',
    meta: { title: '数据集管理', permit: ['bi:dataset:'] },
    component: () => import('@/views/bi/data/dataset.vue')
  }]
}]

export default { blanks, layouts }
