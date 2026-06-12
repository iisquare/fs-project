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
    path: '/lm/plugin/tool',
    meta: { title: '工具配置', permit: ['lm:tool:'], fit: true },
    component: () => import('@/views/lm/plugin/tool.vue')
  }, {
    path: '/lm/plugin/mcp',
    meta: { title: 'MCP服务', permit: ['lm:mcp:'] },
    component: () => import('@/views/lm/plugin/mcp.vue')
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
  }, {
    path: '/lm/setting/agent',
    meta: { title: '智能体管理', permit: ['lm:agent:'] },
    component: () => import('@/views/lm/setting/agent.vue')
  }, {
    path: '/lm/chat/demo',
    meta: { title: '模型调试', fit: true, permit: ['lm:chat:demo'] },
    component: () => import('@/views/lm/chat/demo.vue')
  }, {
    path: '/lm/chat/compare',
    meta: { title: '模型对比', permit: ['lm:chat:compare'] },
    component: () => import('@/views/lm/chat/compare.vue')
  }, {
    path: '/lm/chat/dialog',
    meta: { title: '模型对话', fit: true, permit: ['lm:chat:dialog'] },
    component: () => import('@/views/lm/chat/dialog.vue')
  }, {
    path: '/lm/agentic/list',
    meta: { title: '智能体', permit: ['lm:agentic:'] },
    component: () => import('@/views/lm/agentic/list.vue')
  }, {
    path: '/lm/knowledge/list',
    meta: { title: '知识库', permit: ['lm:knowledge:'] },
    component: () => import('@/views/lm/knowledge/list.vue')
  }, {
    path: '/lm/knowledge/document',
    meta: { title: '文档管理', permit: ['lm:knowledge:'] },
    component: () => import('@/views/lm/knowledge/document.vue')
  }, {
    path: '/lm/knowledge/segment',
    meta: { title: '分段管理', permit: ['lm:knowledge:'] },
    component: () => import('@/views/lm/knowledge/segment.vue')
  }, {
    path: '/lm/knowledge/chunk',
    meta: { title: '分块管理', permit: ['lm:knowledge:'] },
    component: () => import('@/views/lm/knowledge/chunk.vue')
  }]
}]

export default { blanks, layouts }
