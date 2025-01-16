import { layout } from '../config'

export const blanks = []

export const layouts = [{
  path: '/demo',
  meta: { title: '演示实例', to: '/demo/index/index' },
  children: [{
    path: '/demo/index/index',
    meta: { title: '工作面板' },
    component: layout.default
  }, {
    path: '/demo/table/merge',
    meta: { title: '合并单元格' },
    component: () => import('@/views/demo/table/merge.vue')
  }, {
    path: '/demo/table/column',
    meta: { title: '表头设置' },
    component: () => import('@/views/demo/table/column.vue')
  }, {
    path: '/demo/data/sse',
    meta: { title: 'EventSource' },
    component: () => import('@/views/demo/data/sse.vue')
  }]
}]

export default { blanks, layouts }
