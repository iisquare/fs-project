import { layout } from '../config'

export const blanks = []

export const layouts = [{
  path: '/file',
  meta: { title: '文件存储' },
  component: layout.route,
  children: [{
    path: '/file/index/index',
    meta: { title: '工作面板' },
    component: layout.default
  }, {
    path: '/file/archive/list',
    meta: { title: '文件列表' },
    component: () => import(/* webpackChunkName: 'file' */ '@/views/file/archive/list')
  }]
}]

export default { blanks, layouts }
