import { layout } from '../config'

export const blanks = [{
  path: '/bi/diagram/model',
  meta: { title: '计算规则设计器' },
  component: () => import(/* webpackChunkName: 'bi' */ '@/views/bi/diagram/model')
}]

export const layouts = [{
  path: '/bi',
  meta: { title: '商业智能' },
  component: layout.route,
  children: [{
    path: '/bi/index/index',
    meta: { title: '工作面板' },
    component: layout.default
  }, {
    path: '/bi/diagram/list',
    meta: { title: '规则列表' },
    component: () => import(/* webpackChunkName: 'bi' */ '@/views/bi/diagram/list')
  }, {
    path: '/bi/data/source',
    meta: { title: '数据源' },
    component: () => import(/* webpackChunkName: 'bi' */ '@/views/bi/source/list')
  }, {
    path: '/bi/data/dataset',
    meta: { title: '数据集' },
    component: () => import(/* webpackChunkName: 'bi' */ '@/views/bi/dataset/list')
  }, {
    path: '/bi/data/model',
    meta: { title: '数据集', hiddenGlobalFooter: true },
    component: () => import(/* webpackChunkName: 'bi' */ '@/views/bi/dataset/model')
  }]
}]

export default { blanks, layouts }
