import { layout } from '../config'

export const blanks = []

export const layouts = [{
  path: '/bi',
  meta: { title: '商业智能' },
  children: [{
    path: '/bi/index/index',
    meta: { title: '工作面板' },
    component: layout.default
  }, {
    path: '/bi/data/datasource',
    meta: { title: '数据源管理', permit: ['bi:datasource:'] },
    component: () => import('@/views/bi/data/datasource.vue')
  }, {
    path: '/bi/data/dataset',
    meta: { title: '数据集管理', permit: ['bi:dataset:'] },
    component: () => import('@/views/bi/data/dataset.vue')
  }, {
    path: '/bi/data/theme',
    meta: { title: '数据主题', permit: ['bi:dataTheme:'] },
    component: () => import('@/views/bi/data/theme.vue')
  }, {
    path: '/bi/data/excel',
    meta: { title: 'Excel管理', permit: ['bi:dataExcel:'] },
    component: () => import('@/views/bi/data/excel.vue')
  }, {
    path: '/bi/data/api',
    meta: { title: '接口管理', permit: ['bi:dataApi:'] },
    component: () => import('@/views/bi/data/api.vue')
  }, {
    path: '/bi/setting/state',
    meta: { title: '运行状态', permit: ['bi:maintain:'] },
    component: () => import('@/views/bi/setting/state.vue')
  }, {
    path: '/bi/olap/sql',
    meta: { title: 'SQL查询', fit: true, permit: ['bi:olap:'] },
    component: () => import('@/views/bi/olap/sql.vue')
  }]
}]

export default { blanks, layouts }
