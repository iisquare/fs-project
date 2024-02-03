import { layout } from '../config'

export const blanks = []

export const layouts = [{
  path: '/demo',
  meta: { title: '演示实例' },
  component: layout.route,
  children: [{
    path: '/demo/index/index',
    meta: { title: '工作面板' },
    component: layout.default
  }, {
    path: '/demo/table/merge',
    meta: { title: '合并单元格' },
    component: () => import(/* webpackChunkName: 'demo' */ '@/views/demo/table/merge')
  }, {
    path: '/demo/data/sse',
    meta: { title: 'EventSource' },
    component: () => import(/* webpackChunkName: 'demo' */ '@/views/demo/data/sse')
  }]
}]

export default { blanks, layouts }
