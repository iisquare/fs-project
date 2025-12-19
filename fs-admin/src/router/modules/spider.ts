import { layout } from '../config'

export const blanks = []

export const layouts = [{
  path: '/spider',
  meta: { title: '数据采集' },
  children: [{
    path: '/spider/index/index',
    meta: { title: '工作面板' },
    component: layout.default
  }, {
    path: '/spider/setting/template',
    meta: { title: '模板管理', permit: ['spider:template:'] },
    component: () => import('@/views/spider/setting/template.vue')
  }, {
    path: '/spider/setting/rate',
    meta: { title: '请求频率', permit: ['spider:rate:'] },
    component: () => import('@/views/spider/setting/rate.vue')
  }, {
    path: '/spider/schedule/node',
    meta: { title: '节点状态', permit: ['spider:node:'] },
    component: () => import('@/views/spider/schedule/node.vue')
  }, {
    path: '/spider/schedule/job',
    meta: { title: '作业管理', permit: ['spider:node:job'] },
    component: () => import('@/views/spider/schedule/job.vue')
  }, {
    path: '/spider/schedule/ack',
    meta: { title: '确认队列', permit: ['spider:node:job'] },
    component: () => import('@/views/spider/schedule/ack.vue')
  }]
}]

export default { blanks, layouts }
