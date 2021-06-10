import { layout } from '../config'

export const blanks = []

export const layouts = [{
  path: '/lucene',
  meta: { title: '搜索引擎' },
  component: layout.route,
  children: [{
    path: '/lucene/index/index',
    meta: { title: '工作面板' },
    component: layout.default
  }, {
    path: '/lucene/elasticsearch/demo',
    meta: { title: '索引示例' },
    component: () => import(/* webpackChunkName: 'lucene' */ '@/views/lucene/elasticsearch/demo')
  }, {
    path: '/lucene/elasticsearch/reload',
    meta: { title: '服务重载' },
    component: () => import(/* webpackChunkName: 'lucene' */ '@/views/lucene/elasticsearch/reload')
  }, {
    path: '/lucene/dictionary/list',
    meta: { title: '词库列表' },
    component: () => import(/* webpackChunkName: 'lucene' */ '@/views/lucene/dictionary/list')
  }]
}]

export default { blanks, layouts }
