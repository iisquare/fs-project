import { layout } from '../config'

export const blanks = [{
  path: '/bi/diagram/model',
  meta: { title: '清洗规则设计器' },
  component: () => import(/* webpackChunkName: 'bi' */ '@/views/bi/diagram/model')
}, {
  path: '/bi/report/screen',
  meta: { title: '大屏设计器' },
  component: () => import(/* webpackChunkName: 'bi' */ '@/views/bi/dashboard/model')
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
    meta: { title: '清洗规则' },
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
  }, {
    path: '/bi/report/visualize',
    meta: { title: '数据报表' },
    component: () => import(/* webpackChunkName: 'bi' */ '@/views/bi/visualize/list')
  }, {
    path: '/bi/report/chart',
    meta: { title: '报表设计', hiddenGlobalFooter: true },
    component: () => import(/* webpackChunkName: 'bi' */ '@/views/bi/visualize/model')
  }, {
    path: '/bi/report/matrix',
    meta: { title: '数据矩阵' },
    component: () => import(/* webpackChunkName: 'bi' */ '@/views/bi/matrix/list')
  }, {
    path: '/bi/report/aggregate',
    meta: { title: '矩阵设计', hiddenGlobalFooter: true },
    component: () => import(/* webpackChunkName: 'bi' */ '@/views/bi/matrix/model')
  }, {
    path: '/bi/report/dashboard',
    meta: { title: '数据大屏' },
    component: () => import(/* webpackChunkName: 'bi' */ '@/views/bi/dashboard/list')
  }]
}]

export default { blanks, layouts }
