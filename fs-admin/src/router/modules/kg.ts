import { layout } from '../config'

export const blanks = []

/**
 * 知识图谱路由
 *
 * 菜单分为本体建模、知识抽取、知识融合、知识评估、知识检索五个模块，
 * 旧的 /kg/manage/* 路径保留重定向，避免书签与外部链接失效。
 */
export const layouts = [{
  path: '/kg',
  meta: { title: '知识图谱', to: '/kg/index/index' },
  children: [{
    path: '/kg/index/index',
    meta: { title: '工作面板' },
    component: layout.default
  }, {
    path: '/kg/modeling/ontology',
    meta: { title: '本体管理', permit: ['kg:ontology:'] },
    component: () => import('@/views/kg/modeling/ontology.vue')
  }, {
    path: '/kg/modeling/er',
    meta: { title: '实体关系', fit: true, permit: ['kg:ontology:add', 'kg:ontology:modify'] },
    component: () => import('@/views/kg/modeling/er.vue')
  }, {
    path: '/kg/modeling/indexs',
    meta: { title: '索引管理', permit: ['kg:ontology:schema'] },
    component: () => import('@/views/kg/modeling/indexs.vue')
  }, {
    path: '/kg/modeling/constraints',
    meta: { title: '约束管理', permit: ['kg:ontology:schema'] },
    component: () => import('@/views/kg/modeling/constraints.vue')
  }, {
    path: '/kg/modeling/schema',
    meta: { title: '结构对账', permit: ['kg:ontology:schema'] },
    component: () => import('@/views/kg/modeling/schema.vue')
  }, {
    path: '/kg/extraction/data',
    meta: { title: '图谱数据', permit: ['kg:graph:'] },
    component: () => import('@/views/kg/extraction/data.vue')
  }, {
    path: '/kg/extraction/workbench',
    meta: { title: '抽取工作台', permit: ['kg:extract:'] },
    component: () => import('@/views/kg/extraction/workbench.vue')
  }, {
    path: '/kg/fusion/rule',
    meta: { title: '融合规则', permit: ['kg:fusion:'] },
    component: () => import('@/views/kg/fusion/rule.vue')
  }, {
    path: '/kg/fusion/candidate',
    meta: { title: '候选审核', permit: ['kg:fusion:'] },
    component: () => import('@/views/kg/fusion/candidate.vue')
  }, {
    path: '/kg/fusion/record',
    meta: { title: '融合记录', permit: ['kg:fusion:'] },
    component: () => import('@/views/kg/fusion/record.vue')
  }, {
    path: '/kg/assess/run',
    meta: { title: '执行评估', permit: ['kg:assess:run'] },
    component: () => import('@/views/kg/assess/run.vue')
  }, {
    path: '/kg/assess/history',
    meta: { title: '评估历史', permit: ['kg:assess:history'] },
    component: () => import('@/views/kg/assess/history.vue')
  }, {
    path: '/kg/retrieval/traverse',
    meta: { title: '图谱探索', permit: ['kg:graph:'] },
    component: () => import('@/views/kg/retrieval/traverse.vue')
  }, {
    // 旧路径兼容
    path: '/kg/manage',
    redirect: '/kg/modeling/ontology'
  }, {
    path: '/kg/manage/ontology',
    redirect: '/kg/modeling/ontology'
  }, {
    path: '/kg/manage/er',
    redirect: '/kg/modeling/er'
  }, {
    path: '/kg/manage/indexs',
    redirect: '/kg/modeling/indexs'
  }, {
    path: '/kg/manage/constraints',
    redirect: '/kg/modeling/constraints'
  }, {
    path: '/kg/manage/schema',
    redirect: '/kg/modeling/schema'
  }, {
    path: '/kg/manage/data',
    redirect: '/kg/extraction/data'
  }, {
    path: '/kg/manage/graph',
    redirect: '/kg/retrieval/traverse'
  }, {
    path: '/kg/manage/fusion',
    redirect: '/kg/fusion/rule'
  }, {
    path: '/kg/manage/assess',
    redirect: '/kg/assess/run'
  }]
}]

export default { blanks, layouts }
