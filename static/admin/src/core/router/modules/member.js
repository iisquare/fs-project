import { layout } from '../config'

export const blanks = []

export const layouts = [{
  path: '/member',
  meta: { title: '用户中心' },
  component: layout.route,
  children: [{
    path: '/member/index/index',
    meta: { title: '工作面板' },
    component: layout.default
  }, {
    path: '/member/user/list',
    meta: { title: '用户列表' },
    component: () => import(/* webpackChunkName: 'member' */ '@/views/member/user/list')
  }, {
    path: '/member/role/list',
    meta: { title: '角色列表' },
    component: () => import(/* webpackChunkName: 'member' */ '@/views/member/role/list')
  }, {
    path: '/member/role/application',
    meta: { title: '权限管理' },
    component: () => import(/* webpackChunkName: 'member' */ '@/views/member/role/application')
  }, {
    path: '/member/setting/list',
    meta: { title: '配置列表' },
    component: () => import(/* webpackChunkName: 'member' */ '@/views/member/setting/list')
  }, {
    path: '/member/resource/list',
    meta: { title: '资源列表' },
    component: () => import(/* webpackChunkName: 'member' */ '@/views/member/resource/list')
  }, {
    path: '/member/menu/list',
    meta: { title: '菜单列表' },
    component: () => import(/* webpackChunkName: 'member' */ '@/views/member/menu/list')
  }, {
    path: '/member/dictionary/list',
    meta: { title: '字典列表' },
    component: () => import(/* webpackChunkName: 'member' */ '@/views/member/dictionary/list')
  }, {
    path: '/member/dictionary/tree',
    meta: { title: '树形字典' },
    component: () => import(/* webpackChunkName: 'member' */ '@/views/member/dictionary/tree')
  }, {
    path: '/member/application/list',
    meta: { title: '应用列表' },
    component: () => import(/* webpackChunkName: 'member' */ '@/views/member/application/list')
  }, {
    path: '/member/application/resource',
    meta: { title: '应用资源' },
    component: () => import(/* webpackChunkName: 'member' */ '@/views/member/application/resource')
  }, {
    path: '/member/application/menu',
    meta: { title: '应用菜单' },
    component: () => import(/* webpackChunkName: 'member' */ '@/views/member/application/menu')
  }]
}]

export default { blanks, layouts }
