import { layout } from '../config'

export const blanks = []

export const layouts = [{
  path: '/member',
  meta: { title: '用户中心' },
  children: [{
    path: '/member/index/index',
    meta: { title: '工作面板' },
    component: layout.default
  }, {
    path: '/member/user/list',
    meta: { title: '用户列表', permit: ['member:user:'] },
    component: () => import('@/views/member/user/list.vue')
  }, {
    path: '/member/role/list',
    meta: { title: '角色列表', permit: ['member:role:'] },
    component: () => import('@/views/member/role/list.vue')
  }, {
    path: '/member/role/application',
    meta: { title: '权限管理', permit: ['member:role:application'] },
    component: () => import('@/views/member/role/application.vue')
  }, {
    path: '/member/setting/list',
    meta: { title: '配置列表', permit: ['member:setting:'] },
    component: () => import('@/views/member/setting/list.vue')
  }, {
    path: '/member/resource/list',
    meta: { title: '资源列表', permit: ['member:resource:'] },
    component: () => import('@/views/member/resource/list.vue')
  }, {
    path: '/member/menu/list',
    meta: { title: '菜单列表', permit: ['member:menu:'] },
    component: () => import('@/views/member/menu/list.vue')
  }, {
    path: '/member/dictionary/list',
    meta: { title: '字典列表', permit: ['member:dictionary:'] },
    component: () => import('@/views/member/dictionary/list.vue')
  }, {
    path: '/member/dictionary/tree',
    meta: { title: '树形字典', permit: ['member:dictionary:'] },
    component: () => import('@/views/member/dictionary/tree.vue')
  }, {
    path: '/member/dictionary/demo',
    meta: { title: '字典示例', permit: ['member:dictionary:'] },
    component: () => import('@/views/member/dictionary/demo.vue')
  }, {
    path: '/member/application/list',
    meta: { title: '应用列表', permit: ['member:application:'] },
    component: () => import('@/views/member/application/list.vue')
  }, {
    path: '/member/application/resource',
    meta: { title: '应用资源', permit: ['member:application:resource'] },
    component: () => import('@/views/member/application/resource.vue')
  }, {
    path: '/member/application/menu',
    meta: { title: '应用菜单', permit: ['member:application:menu'] },
    component: () => import('@/views/member/application/menu.vue')
  }, {
    path: '/member/data/list',
    meta: { title: '数据模型', permit: ['member:data:'] },
    component: () => import('@/views/member/data/list.vue')
  }, {
    path: '/member/data/permit',
    meta: { title: '授权管理', permit: ['member:dataPermit:'] },
    component: () => import('@/views/member/data/permit.vue')
  }, {
    path: '/member/data/check',
    meta: { title: '权限检测', permit: ['member:dataLog:check'] },
    component: () => import('@/views/member/data/check.vue')
  }, {
    path: '/member/data/log',
    meta: { title: '鉴权日志', permit: ['member:dataLog:'] },
    component: () => import('@/views/member/data/log.vue')
  }]
}]

export default { blanks, layouts }
