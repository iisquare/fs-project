import { layout } from '../config'

export const blanks = []

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
  }]
}]

export default { blanks, layouts }
