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
  }, {
    path: '/server/cron/node',
    meta: { title: '工作节点' },
    component: () => import(/* webpackChunkName: 'server' */ '@/views/server/cron/node')
  }, {
    path: '/server/cron/job',
    meta: { title: '作业管理' },
    component: () => import(/* webpackChunkName: 'server' */ '@/views/server/cron/job')
  }]
}]

export default { blanks, layouts }
