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
  user: () => import(/* webpackChunkName: 'main' */ '@/views/frame/layout/user'),
  default: () => import(/* webpackChunkName: 'main' */ '@/views/frame/page/default'),
  e403: () => import(/* webpackChunkName: 'main' */ '@/views/frame/page/403'),
  e404: () => import(/* webpackChunkName: 'main' */ '@/views/frame/page/404'),
  e500: () => import(/* webpackChunkName: 'main' */ '@/views/frame/page/500')
}

const page = {
  root: '/',
  e404: '/404',
  home: '/dashboard/workplace',
  login: '/user/login',
  startup: '/startup'
}

const routes = [{
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
    component: () => import(/* webpackChunkName: 'main' */ '@/views/frame/page/login')
  }]
}, {
  name: '/',
  path: '/',
  meta: { title: '首页' },
  component: layout.basic,
  redirect: page.home,
  children: [{
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
    path: '/member',
    meta: { title: '用户中心' },
    component: layout.route,
    children: [{
      path: '/member/index/index',
      meta: { title: '工作面板' },
      component: layout.default
    }]
  }]
}, {
  path: '*', redirect: page.e404
}]

const router = new Router({ routes })

router.beforeEach((to, from, next) => {
  const user = store.state.user
  if (!user.ready) { // 用户状态未同步
    if (to.path === page.startup || to.path === page.e404) {
      next()
    } else {
      next({
        path: page.startup,
        query: { redirect: to.fullPath }
      }).catch(err => err)
    }
  } else if (DataUtil.empty(user.data) || DataUtil.empty(user.data.info)) { // 用户未登陆
    if (to.path === page.login || to.path === page.e404) {
      next()
    } else {
      next({
        path: page.login,
        query: { redirect: to.fullPath }
      }).catch(err => err)
    }
  } else { // 用户已登陆
    if ([page.startup, page.login].indexOf(to.path) === -1) {
      next()
    } else {
      let url = to.params.redirect
      if (DataUtil.empty(url)) url = page.root
      next(url).catch(err => err)
    }
  }
})

export default router
