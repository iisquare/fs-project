import { createRouter, createWebHashHistory } from 'vue-router'
import member from './modules/member'
import demo from './modules/demo'
import { layout, page } from './config'
import { useUserStore } from '@/stores/user'
import DataUtil from '@/utils/DataUtil'

const blanks: any = [] // 独立页面
const layouts: any = [] // 布局页面
/**
 * {
 *   meta: {
 *     title: '页面题',
 *     fit: true, // 去除内边距
 *     to: '/path/to/router', // 面包屑导航链接
 *     permit: ['权限声明', 'module:controller:action'], // 满足任意一个即可
 *   }
 * }
 */
const routes: any = []

;([member, demo] as any).forEach((module: any) => {
  module.blanks && blanks.push(...module.blanks)
  module.layouts && layouts.push(...module.layouts)
})

// blank pages
routes.push({
  path: page.e403,
  meta: { title: '403' },
  component: layout.e403
}, {
  path: page.e404,
  meta: { title: '404' },
  component: layout.e404
}, {
  path: '/startup',
  meta: { title: '启动页面' },
  component: () => import('@/views/frame/page/startup.vue')
}, {
  path: '/redirect',
  meta: { title: '正在前往' },
  component: () => import('@/views/frame/page/redirect.vue')
}, {
  path: '/login',
  meta: { title: '登录页面' },
  component: () => import('@/views/frame/account/login.vue')
}, ...blanks)

// layout pages
routes.push({
  path: '/',
  meta: { title: '首页', to: '/' },
  component: layout.basic,
  redirect: page.home,
  children: [{
    path: '/account',
    meta: { title: '个人中心' },
    children: [{
      path: '/account/profile',
      meta: { title: '个人信息' },
      component: () => import('@/views/frame/account/profile.vue')
    }, {
      path: '/account/password',
      meta: { title: '修改密码' },
      component: () => import('@/views/frame/account/password.vue')
    }]
  }, {
    path: '/dashboard',
    meta: { title: '仪表盘' },
    children: [{
      path: '/dashboard/workplace',
      meta: { title: '工作台', fit: true },
      component: () => import('@/views/frame/dashboard/workplace.vue')
    }]
  }].concat(layouts)
})

// default page
routes.push({
  path: '/:catchAll(.*)', redirect: page.e404
})

const router = createRouter({
  history: createWebHashHistory(import.meta.env.BASE_URL),
  routes,
})

const title = document.title

router.beforeEach((to: any, from, next) => {
  document.title = (to.meta && to.meta.title) ? title.replace('FS Project', to.meta.title) : title
  const user = useUserStore()
  if (!user.ready) { // 用户状态未同步
    if ([page.startup, page.e403, page.e404].includes(to.path)) {
      next()
    } else {
      next({
        path: page.startup,
        query: { redirect: to.query.redirect ?? to.fullPath }
      })
    }
  } else if (user.info.id < 1) { // 用户未登陆
    if ([page.login, page.e403, page.e404].includes(to.path)) {
      next()
    } else {
      next({
        path: page.login,
        query: { redirect: to.query.redirect ?? to.fullPath }
      })
    }
  } else { // 用户已登陆
    if ([page.startup, page.login].includes(to.path)) {
      let url = to.query.redirect
      if (DataUtil.empty(url)) url = page.root
      next(url)
    } else if (to.meta.permit && !user.hasPermit(to.meta.permit)) {
      next(page.e403)
    } else {
      next()
    }
  }
})

export default router
