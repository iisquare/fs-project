import { layout } from '../config'

export const blanks = []

export const layouts = [{
  path: '/server',
  meta: { title: '服务管理' },
  children: [{
    path: '/server/index/index',
    meta: { title: '工作面板' },
    component: layout.default
  }, {
    path: '/server/cron/node',
    meta: { title: '工作节点', permit: ['cron::'] },
    component: () => import('@/views/server/cron/node.vue')
  }, {
    path: '/server/cron/job',
    meta: { title: '作业管理', permit: ['cron:job:'] },
    component: () => import('@/views/server/cron/job.vue')
  }, {
    path: '/server/cron/trigger',
    meta: { title: '作业调度', permit: ['cron::'] },
    component: () => import('@/views/server/cron/trigger.vue')
  }, {
    path: '/server/cron/flow',
    meta: { title: '任务编排', permit: ['cron:flow:'] },
    component: () => import('@/views/server/cron/flow.vue')
  }, {
    path: '/server/cron/flowLog',
    meta: { title: '调度日志', permit: ['cron:flow:'] },
    component: () => import('@/views/server/cron/flowLog.vue')
  }, {
    path: '/server/cron/flowStage',
    meta: { title: '调度明细', permit: ['cron:flow:'] },
    component: () => import('@/views/server/cron/flowStage.vue')
  }, {
    path: '/server/cron/diagram',
    meta: { title: '任务编排', permit: ['cron:flow:add', 'cron:flow:modify'], fit: true },
    component: () => import('@/views/server/cron/diagram.vue')
  }, {
    path: '/server/file/archive',
    meta: { title: '文件管理', permit: ['file:archive:'] },
    component: () => import('@/views/server/file/archive.vue')
  }]
}]

export default { blanks, layouts }
