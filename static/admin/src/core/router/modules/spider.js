import { layout } from '../config'

export const blanks = []

export const layouts = [{
  path: '/spider',
  meta: { title: '网页爬虫' },
  component: layout.route,
  children: [{
    path: '/spider/index/index',
    meta: { title: '工作面板' },
    component: layout.default
  }, {
    path: '/spider/crawler/dashboard',
    meta: { title: '工作节点' },
    component: () => import(/* webpackChunkName: 'spider' */ '@/views/spider/crawler/dashboard')
  }, {
    path: '/spider/template/list',
    meta: { title: '模板列表' },
    component: () => import(/* webpackChunkName: 'spider' */ '@/views/spider/template/list')
  }, {
    path: '/spider/template/model',
    meta: { title: '爬虫模型' },
    component: () => import(/* webpackChunkName: 'spider' */ '@/views/spider/template/model')
  }]
}]

export default { blanks, layouts }
