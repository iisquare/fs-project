import Vue from 'vue'
import Router from 'vue-router'
import store from '@/core/store'
import DataUtil from '@/utils/data'

Vue.use(Router)

const routes = [{
  path: '/startup',
  name: '工作面板',
  component: () => import(/* webpackChunkName: 'main' */ '@/view/layout/page/startup')
}, {
  path: '/login',
  name: '用户登录',
  component: () => import(/* webpackChunkName: 'main' */ '@/view/layout/page/login')
}, {
  path: '/403',
  name: 'Error403',
  component: () => import(/* webpackChunkName: 'error' */ '@/view/layout/page/403')
}, {
  path: '/404',
  name: 'Error404',
  component: () => import(/* webpackChunkName: 'error' */ '@/view/layout/page/404')
}, {
  path: '/500',
  name: 'Error500',
  component: () => import(/* webpackChunkName: 'error' */ '@/view/layout/page/500')
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
