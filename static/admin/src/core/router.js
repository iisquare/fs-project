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
  e500: () => import(/* webpackChunkName: 'main' */ '@/views/frame/page/500'),
  success: () => import(/* webpackChunkName: 'main' */ '@/views/frame/page/success'),
  fail: () => import(/* webpackChunkName: 'main' */ '@/views/frame/page/fail')
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
    component: () => import(/* webpackChunkName: 'main' */ '@/views/frame/user/login')
  }]
}, {
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
  }, {
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
      path: '/member/setting/list',
      meta: { title: '配置列表' },
      component: () => import(/* webpackChunkName: 'member' */ '@/views/member/setting/list')
    }, {
      path: '/member/resource/list',
      meta: { title: '资源列表' },
      component: () => import(/* webpackChunkName: 'member' */ '@/views/member/resource/list')
    }, {
      path: '/member/resource/tree',
      meta: { title: '树形资源' },
      component: () => import(/* webpackChunkName: 'member' */ '@/views/member/resource/tree')
    }, {
      path: '/member/menu/list',
      meta: { title: '菜单列表' },
      component: () => import(/* webpackChunkName: 'member' */ '@/views/member/menu/list')
    }, {
      path: '/member/menu/tree',
      meta: { title: '树形菜单' },
      component: () => import(/* webpackChunkName: 'member' */ '@/views/member/menu/tree')
    }]
  }, {
    path: '/spider',
    meta: { title: '网页爬虫' },
    component: layout.route,
    children: [{
      path: '/spider/index/index',
      meta: { title: '工作面板' },
      component: layout.default
    }, {
      path: '/spider/crawler/dashboard',
      meta: { title: '工作节点' },
      component: () => import(/* webpackChunkName: 'spider' */ '@/views/spider/crawler/dashboard')
    }, {
      path: '/spider/template/list',
      meta: { title: '模板列表' },
      component: () => import(/* webpackChunkName: 'spider' */ '@/views/spider/template/list')
    }, {
      path: '/spider/template/model',
      meta: { title: '爬虫模型' },
      component: () => import(/* webpackChunkName: 'spider' */ '@/views/spider/template/model')
    }]
  }, {
    path: '/spark',
    meta: { title: '数据计算' },
    component: layout.route,
    children: [{
      path: '/spark/index/index',
      meta: { title: '工作面板' },
      component: layout.default
    }, {
      path: '/spark/plugin/list',
      meta: { title: '插件列表' },
      component: () => import(/* webpackChunkName: 'spark' */ '@/views/spark/plugin/list')
    }, {
      path: '/spark/node/list',
      meta: { title: '流程节点列表', parents: ['/spark/flow'] },
      component: () => import(/* webpackChunkName: 'spark' */ '@/views/spark/node/list')
    }, {
      path: '/spark/node/tree',
      meta: { title: '树形流程节点', parents: ['/spark/flow'] },
      component: () => import(/* webpackChunkName: 'spark' */ '@/views/spark/node/tree')
    }, {
      path: '/spark/flow/list',
      meta: { title: '流程列表' },
      component: () => import(/* webpackChunkName: 'spark' */ '@/views/spark/flow/list')
    }, {
      path: '/spark/tool/property',
      meta: { title: '属性编辑器' },
      component: () => import(/* webpackChunkName: 'spark' */ '@/views/spark/tool/property')
    }, {
      path: '/spark/tool/field',
      meta: { title: '字段编辑器' },
      component: () => import(/* webpackChunkName: 'spark' */ '@/views/spark/tool/field')
    }]
  }, {
    path: '/face',
    meta: { title: '人脸识别', to: '/face/index/index' },
    component: layout.route,
    children: [{
      path: '/face/index/index',
      meta: { title: '工作面板' },
      component: () => import(/* webpackChunkName: 'face' */ '@/views/face/dashboard/index')
    }, {
      path: '/face/user/list',
      meta: { title: '用户列表' },
      component: () => import(/* webpackChunkName: 'face' */ '@/views/face/user/list')
    }, {
      path: '/face/group/list',
      meta: { title: '分组列表' },
      component: () => import(/* webpackChunkName: 'face' */ '@/views/face/group/list')
    }, {
      path: '/face/photo/list',
      meta: { title: '人像列表' },
      component: () => import(/* webpackChunkName: 'face' */ '@/views/face/photo/list')
    }, {
      path: '/face/dashboard/detect',
      meta: { title: '人脸检测' },
      component: () => import(/* webpackChunkName: 'face' */ '@/views/face/dashboard/detect')
    }, {
      path: '/face/dashboard/compare',
      meta: { title: '人脸对比' },
      component: () => import(/* webpackChunkName: 'face' */ '@/views/face/dashboard/compare')
    }, {
      path: '/face/dashboard/search',
      meta: { title: '人脸检索' },
      component: () => import(/* webpackChunkName: 'face' */ '@/views/face/dashboard/search')
    }]
  }, {
    path: '/lucene',
    meta: { title: '搜索引擎' },
    component: layout.route,
    children: [{
      path: '/lucene/index/index',
      meta: { title: '工作面板' },
      component: layout.default
    }, {
      path: '/lucene/elasticsearch/demo',
      meta: { title: '索引示例' },
      component: () => import(/* webpackChunkName: 'lucene' */ '@/views/lucene/elasticsearch/demo')
    }, {
      path: '/lucene/elasticsearch/reload',
      meta: { title: '服务重载' },
      component: () => import(/* webpackChunkName: 'lucene' */ '@/views/lucene/elasticsearch/reload')
    }, {
      path: '/lucene/dictionary/list',
      meta: { title: '词库列表' },
      component: () => import(/* webpackChunkName: 'lucene' */ '@/views/lucene/dictionary/list')
    }]
  }, {
    path: '/server',
    meta: { title: '服务管理' },
    component: layout.route,
    children: [{
      path: '/server/index/index',
      meta: { title: '工作面板' },
      component: layout.default
    }, {
      path: '/server/rabbit/dashboard',
      meta: { title: '消息队列' },
      component: () => import(/* webpackChunkName: 'server' */ '@/views/server/rabbit/dashboard')
    }]
  }]
}, {
  path: '*', redirect: page.e404
}]

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
