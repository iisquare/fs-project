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
    path: '/lm/setting/gateway',
    meta: { title: '网关状态', permit: ['lm:gateway:'] },
    component: () => import('@/views/lm/setting/gateway.vue')
  }, {
    path: '/lm/security/sensitive',
    meta: { title: '拦截关键词', permit: ['lm:sensitive:'] },
    component: () => import('@/views/lm/security/sensitive.vue')
  }, {
    path: '/lm/operation/usage',
    meta: { title: '调用日志', permit: ['lm:usage:'] },
    component: () => import('@/views/lm/operation/usage.vue')
  }, {
    path: '/lm/operation/statistic',
    meta: { title: '调用统计', permit: ['lm:usage:'] },
    component: () => import('@/views/lm/operation/statistic.vue')
  }, {
    path: '/lm/operation/rate',
    meta: { title: '速率限制', permit: ['lm:rate:'] },
    component: () => import('@/views/lm/operation/rate.vue')
  }, {
    path: '/lm/operation/credit',
    meta: { title: '用户积分', permit: ['lm:credit:'] },
    component: () => import('@/views/lm/operation/credit.vue')
  }, {
    path: '/lm/operation/auth',
    meta: { title: '授权密钥', permit: ['lm:auth:'] },
    component: () => import('@/views/lm/operation/auth.vue')
  }, {
    path: '/lm/setting/provider',
    meta: { title: '供应商配置', permit: ['lm:provider:'] },
    component: () => import('@/views/lm/setting/provider.vue')
  }, {
    path: '/lm/setting/model',
    meta: { title: '模型配置', permit: ['lm:model:'] },
    component: () => import('@/views/lm/setting/model.vue')
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
