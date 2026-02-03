export const layout = {
  basic: () => import('@/views/frame/layout/basic.vue'),
  blank: () => import('@/views/frame/layout/blank.vue'),
  route: () => import('@/views/frame/layout/route.vue'),
  e403: () => import('@/views/frame/page/403.vue'),
  e404: () => import('@/views/frame/page/404.vue'),
  default: () => import('@/views/frame/page/default.vue'),
}

export const page = {
  root: '/',
  e403: '/403',
  e404: '/404',
  home: '/dashboard/workplace',
  login: '/login',
  signup: '/signup',
  forgot: '/forgot',
  startup: '/startup'
}

export default { layout, page }
