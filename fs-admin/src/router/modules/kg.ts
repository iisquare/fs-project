import { layout } from '../config'

export const blanks = []

export const layouts = [{
  path: '/kg',
  meta: { title: '知识图谱', to: '/kg/index/index' },
  children: [{
    path: '/kg/index/index',
    meta: { title: '工作面板' },
    component: layout.default
  }, {
    path: '/kg/manage/ontology',
    meta: { title: '本体管理', permit: ['kg:ontology:'] },
    component: () => import('@/views/kg/manage/ontology.vue')
  }, {
    path: '/kg/manage/er',
    meta: { title: '实体关系', fit: true, permit: ['kg:ontology:add', 'kg:ontology:modify'] },
    component: () => import('@/views/kg/manage/er.vue')
  }, {
    path: '/kg/manage/indexs',
    meta: { title: '索引管理', permit: ['kg:neo4j:'] },
    component: () => import('@/views/kg/manage/indexs.vue')
  }, {
    path: '/kg/manage/constraints',
    meta: { title: '约束管理', permit: ['kg:neo4j:'] },
    component: () => import('@/views/kg/manage/constraints.vue')
  }]
}]

export default { blanks, layouts }
