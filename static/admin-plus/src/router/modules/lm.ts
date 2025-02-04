import { layout } from '../config'

export const blanks = []

export const layouts = [{
  path: '/lm',
  meta: { title: '大模型', to: '/lm/index/index' },
  children: [{
    path: '/lm/index/index',
    meta: { title: '工作面板' },
    component: layout.default
  }, {
    path: '/lm/setting/manage',
    meta: { title: '状态管理', permit: ['lm:manage:'] },
    component: () => import('@/views/lm/setting/manage.vue')
  }, {
    path: '/lm/setting/sensitive',
    meta: { title: '敏感词管理', permit: ['lm:sensitive:'] },
    component: () => import('@/views/lm/setting/sensitive.vue')
  }, {
    path: '/lm/setting/server',
    meta: { title: '服务端管理', permit: ['lm:server:'] },
    component: () => import('@/views/lm/setting/server.vue')
  }, {
    path: '/lm/setting/serverEndpoint',
    meta: { title: '服务端端点', permit: ['lm:serverEndpoint:'] },
    component: () => import('@/views/lm/setting/serverEndpoint.vue')
  }, {
    path: '/lm/setting/client',
    meta: { title: '客户端管理', permit: ['lm:client:'] },
    component: () => import('@/views/lm/setting/client.vue')
  }, {
    path: '/lm/setting/clientEndpoint',
    meta: { title: '客户端端点', permit: ['lm:clientEndpoint:'] },
    component: () => import('@/views/lm/setting/clientEndpoint.vue')
  }]
}]

export default { blanks, layouts }
