import { layout } from '../config'

export const blanks = [{
  path: '/govern/meta/draw',
  meta: { title: '模型维护' },
  component: () => import(/* webpackChunkName: 'govern' */ '@/views/govern/meta/draw')
}, {
  path: '/govern/meta/modelCompare',
  meta: { title: '模型对比' },
  component: () => import(/* webpackChunkName: 'govern' */ '@/views/govern/meta/modelCompare')
}, {
  path: '/govern/system/sourceModel',
  meta: { title: '数据源配置' },
  component: () => import(/* webpackChunkName: 'govern' */ '@/views/govern/system/sourceModel')
}, {
  path: '/govern/standard/draw',
  meta: { title: '数据标准' },
  component: () => import(/* webpackChunkName: 'govern' */ '@/views/govern/standard/draw')
}, {
  path: '/govern/standard/model',
  meta: { title: '评估方案' },
  component: () => import(/* webpackChunkName: 'govern' */ '@/views/govern/standard/model')
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
  }, {
    path: '/govern/system/source',
    meta: { title: '数据源' },
    component: () => import(/* webpackChunkName: 'govern' */ '@/views/govern/system/source')
  }, {
    path: '/govern/standard/list',
    meta: { title: '数据标准' },
    component: () => import(/* webpackChunkName: 'govern' */ '@/views/govern/standard/list')
  }, {
    path: '/govern/standard/assess',
    meta: { title: '落地评估' },
    component: () => import(/* webpackChunkName: 'govern' */ '@/views/govern/standard/assess')
  }, {
    path: '/govern/standard/log',
    meta: { title: '评估结果' },
    component: () => import(/* webpackChunkName: 'govern' */ '@/views/govern/standard/log')
  }]
}]

export default { blanks, layouts }
