import { layout } from '../config'

export const blanks = [{
  path: '/govern/meta/draw',
  meta: { title: '模型维护' },
  component: () => import(/* webpackChunkName: 'govern' */ '@/views/govern/meta/draw')
}, {
  path: '/govern/meta/modelCompare',
  meta: { title: '模型对比' },
  component: () => import(/* webpackChunkName: 'govern' */ '@/views/govern/meta/modelCompare')
}]

export const layouts = [{
  path: '/govern',
  meta: { title: '数据治理', to: '/govern/index/index' },
  component: layout.route,
  children: [{
    path: '/govern/index/index',
    meta: { title: '工作面板' },
    component: layout.default
  }, {
    path: '/govern/meta/map',
    meta: { title: '数据地图' },
    component: () => import(/* webpackChunkName: 'govern' */ '@/views/govern/meta/map')
  }, {
    path: '/govern/meta/model',
    meta: { title: '模型管理' },
    component: () => import(/* webpackChunkName: 'govern' */ '@/views/govern/meta/model')
  }, {
    path: '/govern/meta/modelRelation',
    meta: { title: '模型关系' },
    component: () => import(/* webpackChunkName: 'govern' */ '@/views/govern/meta/modelRelation')
  }, {
    path: '/govern/meta/list',
    meta: { title: '元数据检索' },
    component: () => import(/* webpackChunkName: 'govern' */ '@/views/govern/meta/list')
  }, {
    path: '/govern/meta/detail',
    meta: { title: '元数据信息' },
    component: () => import(/* webpackChunkName: 'govern' */ '@/views/govern/meta/detail')
  }, {
    path: '/govern/meta/blood',
    meta: { title: '血缘分析' },
    component: () => import(/* webpackChunkName: 'govern' */ '@/views/govern/meta/blood')
  }]
}]

export default { blanks, layouts }
