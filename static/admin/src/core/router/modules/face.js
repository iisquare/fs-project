import { layout } from '../config'

export const blanks = []

export const layouts = [{
  path: '/face',
  meta: { title: '人脸识别', to: '/face/index/index' },
  component: layout.route,
  children: [{
    path: '/face/index/index',
    meta: { title: '工作面板' },
    component: () => import(/* webpackChunkName: 'face' */ '@/views/face/dashboard/index')
  }, {
    path: '/face/user/list',
    meta: { title: '用户列表' },
    component: () => import(/* webpackChunkName: 'face' */ '@/views/face/user/list')
  }, {
    path: '/face/group/list',
    meta: { title: '分组列表' },
    component: () => import(/* webpackChunkName: 'face' */ '@/views/face/group/list')
  }, {
    path: '/face/photo/list',
    meta: { title: '人像列表' },
    component: () => import(/* webpackChunkName: 'face' */ '@/views/face/photo/list')
  }, {
    path: '/face/dashboard/detect',
    meta: { title: '人脸检测' },
    component: () => import(/* webpackChunkName: 'face' */ '@/views/face/dashboard/detect')
  }, {
    path: '/face/dashboard/compare',
    meta: { title: '人脸对比' },
    component: () => import(/* webpackChunkName: 'face' */ '@/views/face/dashboard/compare')
  }, {
    path: '/face/dashboard/search',
    meta: { title: '人脸检索' },
    component: () => import(/* webpackChunkName: 'face' */ '@/views/face/dashboard/search')
  }]
}]

export default { blanks, layouts }
