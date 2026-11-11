import { layout } from '../config'

export const blanks: any[] = []

export const layouts = [{
  path: '/siteBackend',
  meta: { title: '网站资产' },
  children: [{
    path: '/siteBackend/setting/site',
    meta: { title: '入口与名单' },
    component: () => import('@/views/siteBackend/setting/site.vue')
  }]
}]

export default { blanks, layouts }
