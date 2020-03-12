import Vue from 'vue'
import Router from 'vue-router'
import store from '@/core/store'
import DataUtil from '@/utils/data'

Vue.use(Router)

const layout = {
  basic: () => import(/* webpackChunkName: 'main' */ '@/views/frame/layout/basic'),
  blank: () => import(/* webpackChunkName: 'main' */ '@/views/frame/layout/blank'),
  page: () => import(/* webpackChunkName: 'main' */ '@/views/frame/layout/page'),
  route: () => import(/* webpackChunkName: 'main' */ '@/views/frame/layout/route'),
  user: () => import(/* webpackChunkName: 'main' */ '@/views/frame/layout/user')
}

const page = {
  root: '/',
  home: '/dashboard/workplace',
  login: '/user/login',
  startup: '/startup'
}

const routes = [{
  path: '/startup',
  name: '启动页面',
  component: () => import(/* webpackChunkName: 'main' */ '@/views/frame/page/startup')
}, {
  path: '/user',
  component: layout.user,
  children: [{
    path: '/user/login',
    name: '登录页面',
    component: () => import(/* webpackChunkName: 'main' */ '@/views/frame/page/login')
  }]
}, {
  path: '/',
  name: '首页',
  component: layout.basic,
  redirect: page.home,
  children: [{
    path: '/dashboard',
    name: '仪表盘',
    component: layout.route,
    children: [{
      path: '/dashboard/workplace',
      name: '工作台',
      component: () => import(/* webpackChunkName: 'dashboard' */ '@/views/frame/dashboard/workplace')
    }]
  }]
}, {
  path: '*', name: 'Others', redirect: '/404'
}]

const router = new Router({ routes })

router.beforeEach((to, from, next) => {
  const user = store.state.user
  if (!user.ready) { // 用户状态未同步
    if (to.path === page.startup) {
      next()
    } else {
      next({
        path: page.startup,
        query: { redirect: to.fullPath }
      })
    }
  } else if (DataUtil.empty(user.data) || DataUtil.empty(user.data.info)) { // 用户未登陆
    if (to.path === page.login) {
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
