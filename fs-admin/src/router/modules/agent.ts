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
    meta: { title: '流程管理', permit: ['agent:agentic:'] },
    component: () => import('@/views/agent/agentic/list.vue')
    }, {
      path: '/agent/agentic/model',
      meta: { title: '流程编排', fit: true, permit: ['agent:agentic:'] },
      component: () => import('@/views/agent/agentic/model.vue')
    }, {
      path: '/agent/agentic/log',
      meta: { title: '运行日志', permit: ['agent:agentic:'] },
      component: () => import('@/views/agent/agentic/log.vue')
    }, {
      path: '/agent/agentic/chat',
      meta: { title: '对话历史', permit: ['agent:agentic:'] },
      component: () => import('@/views/agent/agentic/chat.vue')
    }, {
      path: '/agent/agentic/dialog',
      meta: { title: '流程对话', fit: true, permit: ['agent:agentic:'] },
      component: () => import('@/views/agent/agentic/dialog.vue')
    }, {
      path: '/agent/agentic/statistic',
      meta: { title: '流程统计', permit: ['agent:agentic:'] },
      component: () => import('@/views/agent/agentic/statistic.vue')
    }, {
    path: '/agent/chat/compare',
    meta: { title: '模型对比', fit: true, permit: ['agent:compare:'] },
    component: () => import('@/views/agent/chat/compare.vue')
  }, {
    path: '/agent/chat/statistic',
    meta: { title: '对比统计', permit: ['agent:compare:'] },
    component: () => import('@/views/agent/chat/statistic.vue')
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
  }, {
    path: '/agent/setting/state',
    meta: { title: '运行状态', permit: ['agent:maintain:reindexChunk'] },
    component: () => import('@/views/agent/setting/state.vue')
  }]
}]

export default { blanks, layouts }
