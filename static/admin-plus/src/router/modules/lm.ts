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
    path: '/lm/setting/sensitive',
    meta: { title: '敏感词管理', permit: ['lm:sensitive:'] },
    component: () => import('@/views/lm/setting/sensitive.vue')
  }]
}]

export default { blanks, layouts }
