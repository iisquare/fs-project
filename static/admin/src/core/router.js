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

const routes = [{
  path: '/',
  component: layout.user,
  children: [{
    path: '/login',
    component: () => import(/* webpackChunkName: 'main' */ '@/views/frame/page/login')
  }]
}, {
  path: '/startup',
  name: '工作面板',
  component: () => import(/* webpackChunkName: 'main' */ '@/views/frame/page/startup')
}, {
  path: '*', name: 'Others', redirect: '/404'
}]

const router = new Router({ routes })

router.beforeEach((to, from, next) => {
  const user = store.state.user
  if (!user.ready) { // 用户状态未同步
    if (to.path === '/startup') {
      next()
    } else {
      next({
        path: '/startup',
        query: { redirect: to.fullPath }
      })
    }
  } else if (DataUtil.empty(user.data) || DataUtil.empty(user.data.info)) { // 用户未登陆
    if (to.path === '/login') {
      next()
    } else {
      next({
        path: '/login',
        query: { redirect: to.fullPath }
      })
    }
  } else { // 用户已登陆
    if (['/startup', '/login'].indexOf(to.path) === -1) {
      next()
    } else {
      let url = to.params.redirect
      if (DataUtil.empty(url)) url = '/'
      next(url)
    }
  }
})

export default router
