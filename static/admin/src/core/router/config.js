export const layout = {
  basic: () => import(/* webpackChunkName: 'main' */ '@/views/frame/layout/basic'),
  blank: () => import(/* webpackChunkName: 'main' */ '@/views/frame/layout/blank'),
  page: () => import(/* webpackChunkName: 'main' */ '@/views/frame/layout/page'),
  route: () => import(/* webpackChunkName: 'main' */ '@/views/frame/layout/route'),
  user: () => import(/* webpackChunkName: 'main' */ '@/views/frame/layout/user'),
  default: () => import(/* webpackChunkName: 'main' */ '@/views/frame/page/default'),
  e403: () => import(/* webpackChunkName: 'main' */ '@/views/frame/page/403'),
  e404: () => import(/* webpackChunkName: 'main' */ '@/views/frame/page/404'),
  e500: () => import(/* webpackChunkName: 'main' */ '@/views/frame/page/500'),
  success: () => import(/* webpackChunkName: 'main' */ '@/views/frame/page/success'),
  fail: () => import(/* webpackChunkName: 'main' */ '@/views/frame/page/fail')
}

export const page = {
  root: '/',
  e404: '/404',
  home: '/dashboard/workplace',
  login: '/user/login',
  startup: '/startup'
}

export default { layout, page }
