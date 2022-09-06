import { layout } from '../config'

export const blanks = [{
  path: '/auto/layout/model',
  meta: { title: '页面设计器' },
  component: () => import(/* webpackChunkName: 'auto' */ '@/views/auto/layout/model')
}]

export const layouts = [{
  path: '/auto',
  meta: { title: '项目管理' },
  component: layout.route,
  children: [{
    path: '/auto/index/index',
    meta: { title: '工作面板' },
    component: layout.default
  }, {
    path: '/auto/layout/list',
    meta: { title: '页面设计' },
    component: () => import(/* webpackChunkName: 'auto' */ '@/views/auto/layout/list')
  }]
}]

export default { blanks, layouts }
