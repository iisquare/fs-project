import { layout } from '../config'

export const blanks = []

export const layouts = [{
  path: '/govern',
  meta: { title: '数据治理', to: '/govern/index/index' },
  children: [{
    path: '/govern/index/index',
    meta: { title: '工作面板' },
    component: () => import('@/views/govern/index/index.vue')
  }, {
    path: '/govern/meta/map',
    meta: { title: '数据地图', permit: ['govern:model:'] },
    component: () => import('@/views/govern/meta/map.vue')
  }, {
    path: '/govern/meta/model',
    meta: { title: '模型管理', permit: ['govern:model:'] },
    component: () => import('@/views/govern/meta/model.vue')
  }, {
    path: '/govern/meta/draw',
    meta: { title: '模型维护', permit: ['govern:model:'] },
    component: () => import('@/views/govern/meta/draw.vue')
  }, {
    path: '/govern/meta/modelCompare',
    meta: { title: '模型导入', permit: ['govern:model:'] },
    component: () => import('@/views/govern/meta/modelCompare.vue')
  }, {
    path: '/govern/meta/modelRelation',
    meta: { title: '模型关系', permit: ['govern:modelRelation:'] },
    component: () => import('@/views/govern/meta/modelRelation.vue')
  }, {
    path: '/govern/meta/list',
    meta: { title: '元数据检索', permit: ['govern:model:'] },
    component: () => import('@/views/govern/meta/list.vue')
  }, {
    path: '/govern/meta/detail',
    meta: { title: '元数据信息', permit: ['govern:model:'] },
    component: () => import('@/views/govern/meta/detail.vue')
  }, {
    path: '/govern/meta/blood',
    meta: { title: '血缘分析', permit: ['govern:model:'] },
    component: () => import('@/views/govern/meta/blood.vue')
  }, {
    path: '/govern/meta/influence',
    meta: { title: '影响分析', permit: ['govern:model:'] },
    component: () => import('@/views/govern/meta/influence.vue')
  }, {
    path: '/govern/system/source',
    meta: { title: '数据源', permit: ['govern:source:'] },
    component: () => import('@/views/govern/system/source.vue')
  }, {
    path: '/govern/system/sourceModel',
    meta: { title: '数据源配置', permit: ['govern:source:'] },
    component: () => import('@/views/govern/system/sourceModel.vue')
  }, {
    path: '/govern/setting/state',
    meta: { title: '运行维护', permit: ['govern:model:'] },
    component: () => import('@/views/govern/setting/state.vue')
  }, {
    path: '/govern/standard/list',
    meta: { title: '数据标准', permit: ['govern:standard:'] },
    component: () => import('@/views/govern/standard/list.vue')
  }, {
    path: '/govern/standard/draw',
    meta: { title: '标准维护', permit: ['govern:standard:'] },
    component: () => import('@/views/govern/standard/draw.vue')
  }, {
    path: '/govern/standard/assess',
    meta: { title: '落地评估', permit: ['govern:assess:'] },
    component: () => import('@/views/govern/standard/assess.vue')
  }, {
    path: '/govern/standard/model',
    meta: { title: '评估方案', permit: ['govern:assess:'] },
    component: () => import('@/views/govern/standard/model.vue')
  }, {
    path: '/govern/standard/log',
    meta: { title: '评估结果', permit: ['govern:assess:'] },
    component: () => import('@/views/govern/standard/log.vue')
  }, {
    path: '/govern/quality/logic',
    meta: { title: '质检分类', permit: ['govern:qualityLogic:'] },
    component: () => import('@/views/govern/quality/logic.vue')
  }, {
    path: '/govern/quality/rule',
    meta: { title: '质检规则', permit: ['govern:qualityRule:'] },
    component: () => import('@/views/govern/quality/rule.vue')
  }, {
    path: '/govern/quality/plan',
    meta: { title: '质检方案', permit: ['govern:qualityPlan:'] },
    component: () => import('@/views/govern/quality/plan.vue')
  }, {
    path: '/govern/quality/log',
    meta: { title: '质检报告', permit: ['govern:qualityPlan:log'] },
    component: () => import('@/views/govern/quality/log.vue')
  }, {
    path: '/govern/quality/draw',
    redirect: '/govern/quality/rule'
  }]
}]

export default { blanks, layouts }
