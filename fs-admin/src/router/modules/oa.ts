import { layout } from '../config'

export const blanks = [{
  path: '/oa/workflow/model',
  meta: { title: '流程设计', fit: true, permit: ['oa:workflow:'] },
  component: () => import('@/views/oa/workflow/model.vue')
}, {
  path: '/oa/print/model',
  meta: { title: '打印设计', fit: true, permit: ['oa:print:'] },
  component: () => import('@/views/oa/print/model.vue')
}]

export const layouts = [{
  path: '/oa',
  meta: { title: '在线办公' },
  children: [{
    path: '/oa/index/index',
    meta: { title: '工作面板' },
    component: layout.default
  }, {
    path: '/oa/form/frame',
    meta: { title: '表单模型', permit: ['oa:formFrame:'] },
    component: () => import('@/views/oa/form/frame.vue')
  }, {
    path: '/oa/form/model',
    meta: { title: '表单设计', fit: true, permit: ['oa:formFrame:'] },
    component: () => import('@/views/oa/form/model.vue')
  }, {
    path: '/oa/form/data',
    meta: { title: '托管数据', permit: ['oa:formData:'] },
    component: () => import('@/views/oa/form/data.vue')
  }, {
    path: '/oa/form/list',
    meta: { title: '表单数据', permit: ['oa:formData:'] },
    component: () => import('@/views/oa/form/list.vue')
  }, {
    path: '/oa/form/regular',
    meta: { title: '校验规则', permit: ['oa:formRegular:'] },
    component: () => import('@/views/oa/form/regular.vue')
  }, {
    path: '/oa/workflow/list',
    meta: { title: '流程模型', permit: ['oa:workflow:'] },
    component: () => import('@/views/oa/workflow/list.vue')
  }, {
    path: '/oa/workflow/deployment',
    meta: { title: '流程部署', permit: ['oa:workflow:'] },
    component: () => import('@/views/oa/workflow/deployment.vue')
  }, {
    path: '/oa/workflow/history',
    meta: { title: '流程管理', permit: ['oa:workflow:'] },
    component: () => import('@/views/oa/workflow/history.vue')
  }, {
    path: '/oa/approve/workflow',
    meta: { title: '流程单据', permit: ['oa:approve:'] },
    component: () => import('@/views/oa/approve/workflow.vue')
  }, {
    path: '/oa/approve/form',
    meta: { title: '单据填报', permit: ['oa:approve:'] },
    component: () => import('@/views/oa/approve/form.vue')
  }, {
    path: '/oa/approve/candidate',
    meta: { title: '待签任务', permit: ['oa:approve:'] },
    component: () => import('@/views/oa/approve/candidate.vue')
  }, {
    path: '/oa/approve/assignee',
    meta: { title: '待办任务', permit: ['oa:approve:'] },
    component: () => import('@/views/oa/approve/assignee.vue')
  }, {
    path: '/oa/approve/transact',
    meta: { title: '任务办理', permit: ['oa:approve:'] },
    component: () => import('@/views/oa/approve/transact.vue')
  }, {
    path: '/oa/approve/history',
    meta: { title: '历史任务', permit: ['oa:approve:'] },
    component: () => import('@/views/oa/approve/history.vue')
  }, {
    path: '/oa/approve/process',
    meta: { title: '流程详情', permit: ['oa:approve:'] },
    component: () => import('@/views/oa/approve/process.vue')
  }, {
    path: '/oa/print/list',
    meta: { title: '在线打印', permit: ['oa:print:'] },
    component: () => import('@/views/oa/print/list.vue')
  }, {
    path: '/oa/print/pdf',
    meta: { title: '打印预览', permit: ['oa:print:'] },
    component: () => import('@/views/oa/print/pdf.vue')
  }]
}]

export default { blanks, layouts }
