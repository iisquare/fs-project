import { layout } from '../config'

export const blanks = []

export const layouts = [{
  path: '/server',
  meta: { title: '服务管理' },
  component: layout.route,
  children: [{
    path: '/server/index/index',
    meta: { title: '工作面板' },
    component: layout.default
  }, {
    path: '/server/rabbit/dashboard',
    meta: { title: '消息队列' },
    component: () => import(/* webpackChunkName: 'server' */ '@/views/server/rabbit/dashboard')
  }]
}]

export default { blanks, layouts }
