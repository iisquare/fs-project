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
    path: '/lm/manage/log',
    meta: { title: '调用日志', permit: ['lm:log:'] },
    component: () => import('@/views/lm/manage/log.vue')
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
  }, {
    path: '/lm/model/provider',
    meta: { title: '供应商配置', permit: ['lm:provider:'] },
    component: () => import('@/views/lm/model/provider.vue')
  }, {
    path: '/lm/model/list',
    meta: { title: '模型配置', permit: ['lm:model:'] },
    component: () => import('@/views/lm/model/list.vue')
  }, {
    path: '/lm/tool/list',
    meta: { title: '工具配置', permit: ['lm:tool:'], fit: true },
    component: () => import('@/views/lm/tool/list.vue')
  }, {
    path: '/lm/mcp/list',
    meta: { title: 'MCP服务', permit: ['lm:mcp:'] },
    component: () => import('@/views/lm/mcp/list.vue')
  }]
}]

export default { blanks, layouts }
