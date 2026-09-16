import { layout } from '../config'

export const blanks = []

export const layouts = [{
  path: '/agent',
  meta: { title: '智能体', to: '/agent/index/index' },
  children: [{
    path: '/agent/index/index',
    meta: { title: '工作面板' },
    component: layout.default
  }, {
    path: '/agent/agentic/list',
    meta: { title: '应用管理', permit: ['agent:agentic:'] },
    component: () => import('@/views/agent/agentic/list.vue')
  }, {
    path: '/agent/agentic/model',
    meta: { title: '应用编排', fit: true, permit: ['agent:agentic:'] },
    component: () => import('@/views/agent/agentic/model.vue')
  }, {
    path: '/agent/setting/agent',
    meta: { title: '智能体管理', permit: ['agent:agent:'] },
    component: () => import('@/views/agent/setting/agent.vue')
  }, {
    path: '/agent/chat/demo',
    meta: { title: '模型调试', fit: true, permit: ['agent:chat:demo'] },
    component: () => import('@/views/agent/chat/demo.vue')
  }, {
    path: '/agent/chat/compare',
    meta: { title: '模型对比', permit: ['agent:chat:compare'] },
    component: () => import('@/views/agent/chat/compare.vue')
  }, {
    path: '/agent/chat/dialog',
    meta: { title: '模型对话', fit: true, permit: ['agent:chat:dialog'] },
    component: () => import('@/views/agent/chat/dialog.vue')
  }, {
    path: '/agent/knowledge/list',
    meta: { title: '知识库', permit: ['agent:knowledge:'] },
    component: () => import('@/views/agent/knowledge/list.vue')
  }, {
    path: '/agent/knowledge/document',
    meta: { title: '文档管理', permit: ['agent:knowledge:'] },
    component: () => import('@/views/agent/knowledge/document.vue')
  }, {
    path: '/agent/knowledge/segment',
    meta: { title: '分段管理', permit: ['agent:knowledge:'] },
    component: () => import('@/views/agent/knowledge/segment.vue')
  }, {
    path: '/agent/knowledge/recall',
    meta: { title: '知识召回', permit: ['agent:knowledge:'] },
    component: () => import('@/views/agent/knowledge/recall.vue')
  }, {
    path: '/agent/plugin/tool',
    meta: { title: '工具管理', permit: ['agent:tool:'] },
    component: () => import('@/views/agent/plugin/tool.vue')
  }, {
    path: '/agent/plugin/mcp',
    meta: { title: 'MCP服务', permit: ['agent:tool:'] },
    component: () => import('@/views/agent/plugin/mcp.vue')
  }, {
    path: '/agent/plugin/skill',
    meta: { title: '技能管理', permit: ['agent:skill:'] },
    component: () => import('@/views/agent/plugin/skill.vue')
  }, {
    path: '/agent/plugin/skillVersion',
    meta: { title: '版本管理', permit: ['agent:skill:'] },
    component: () => import('@/views/agent/plugin/skillVersion.vue')
  }]
}]

export default { blanks, layouts }
