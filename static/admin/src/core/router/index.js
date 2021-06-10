import Vue from 'vue'
import Router from 'vue-router'
import store from '@/core/store'
import DataUtil from '@/utils/data'
import { layout, page } from './config'

Vue.use(Router)

const blanks = []
const layouts = []
const routes = []

const context = require.context('./modules', false, /\.js$/)
context.keys().forEach(key => {
  const module = context(key)
  module.blanks && blanks.push(...module.blanks)
  module.layouts && layouts.push(...module.layouts)
})

// blank pages
routes.push({
  path: page.e404,
  component: layout.e404
}, {
  path: '/startup',
  meta: { title: '启动页面' },
  component: () => import(/* webpackChunkName: 'main' */ '@/views/frame/page/startup')
}, {
  path: '/user',
  component: layout.user,
  children: [{
    path: '/user/login',
    meta: { title: '登录页面' },
    component: () => import(/* webpackChunkName: 'main' */ '@/views/frame/user/login')
  }]
}, ...blanks)

// layout pages
routes.push({
  name: '/',
  path: '/',
  meta: { title: '首页' },
  component: layout.basic,
  redirect: page.home,
  children: [{
    path: '/account',
    meta: { title: '个人中心' },
    component: () => import(/* webpackChunkName: 'main' */ '@/views/frame/account/left.inc'),
    children: [{
      path: '/account/profile',
      meta: { title: '个人信息' },
      component: () => import(/* webpackChunkName: 'main' */ '@/views/frame/account/profile')
    }, {
      path: '/account/password',
      meta: { title: '修改密码' },
      component: () => import(/* webpackChunkName: 'main' */ '@/views/frame/account/password')
    }]
  }, {
    name: 'dashboard',
    path: '/dashboard',
    meta: { title: '仪表盘' },
    component: layout.route,
    children: [{
      path: '/dashboard/workplace',
      meta: { title: '工作台' },
      component: () => import(/* webpackChunkName: 'dashboard' */ '@/views/frame/dashboard/workplace')
    }]
  }, {
    path: '/exception',
    meta: { title: '异常页面' },
    component: layout.route,
    children: [{
      path: '/exception/403',
      meta: { title: '403' },
      component: layout.e403
    }, {
      path: '/exception/404',
      meta: { title: '404' },
      component: layout.e404
    }, {
      path: '/exception/500',
      meta: { title: '500' },
      component: layout.e500
    }]
  }, {
    path: '/result',
    name: 'result',
    component: layout.route,
    meta: { title: '结果页' },
    children: [
      {
        path: '/result/success',
        meta: { title: '操作成功' },
        component: () => import(/* webpackChunkName: "main" */ '@/views/frame/page/success')
      },
      {
        path: '/result/fail',
        meta: { title: '操作失败' },
        component: () => import(/* webpackChunkName: "main" */ '@/views/frame/page/fail')
      }
    ]
  }].concat(layouts)
})

// default page
routes.push({
  path: '*', redirect: page.e404
})

const router = new Router({ routes })
// const router = new Router({ mode: 'history', routes })

router.beforeEach((to, from, next) => {
  const user = store.state.user
  if (!user.ready) { // 用户状态未同步
    if (to.path === page.startup || to.path === page.e404) {
      next()
    } else {
      next({
        path: page.startup,
        query: { redirect: to.fullPath }
      })
    }
  } else if (DataUtil.empty(user.data) || DataUtil.empty(user.data.info)) { // 用户未登陆
    if (to.path === page.login || to.path === page.e404) {
      next()
    } else {
      next({
        path: page.login,
        query: { redirect: to.fullPath }
      })
    }
  } else { // 用户已登陆
    if ([page.startup, page.login].indexOf(to.path) === -1) {
      next()
    } else {
      let url = to.params.redirect
      if (DataUtil.empty(url)) url = page.root
      next(url)
    }
  }
})

export default router
