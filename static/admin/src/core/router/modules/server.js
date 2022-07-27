import { layout } from '../config'

export const blanks = [{
  path: '/server/cron/model',
  meta: { title: '定时任务' },
  component: () => import(/* webpackChunkName: 'server' */ '@/views/server/cron/model')
}]

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
  }, {
    path: '/server/cron/trigger',
    meta: { title: '作业调度' },
    component: () => import(/* webpackChunkName: 'server' */ '@/views/server/cron/trigger')
  }, {
    path: '/server/cron/flow',
    meta: { title: '流程管理' },
    component: () => import(/* webpackChunkName: 'server' */ '@/views/server/cron/flow')
  }, {
    path: '/server/cron/flowLog',
    meta: { title: '调度日志' },
    component: () => import(/* webpackChunkName: 'server' */ '@/views/server/cron/flowLog')
  }, {
    path: '/server/cron/flowStage',
    meta: { title: '调度明细' },
    component: () => import(/* webpackChunkName: 'server' */ '@/views/server/cron/flowStage')
  }]
}]

export default { blanks, layouts }
