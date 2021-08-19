import { layout } from '../config'

export const blanks = []

export const layouts = [{
  path: '/cms',
  meta: { title: '内容管理' },
  component: layout.route,
  children: [{
    path: '/cms/index/index',
    meta: { title: '工作面板' },
    component: layout.default
  }, {
    path: '/cms/setting/carousel',
    meta: { title: '首页轮播', hiddenGlobalFooter: true },
    component: () => import(/* webpackChunkName: 'cms' */ '@/views/cms/setting/tree')
  }, {
    path: '/cms/setting/link',
    meta: { title: '友情链接', hiddenGlobalFooter: true },
    component: () => import(/* webpackChunkName: 'cms' */ '@/views/cms/setting/tree')
  }, {
    path: '/cms/setting/menu',
    meta: { title: '导航菜单', hiddenGlobalFooter: true },
    component: () => import(/* webpackChunkName: 'cms' */ '@/views/cms/setting/tree')
  }, {
    path: '/cms/setting/notice',
    meta: { title: '通知公告', hiddenGlobalFooter: true },
    component: () => import(/* webpackChunkName: 'cms' */ '@/views/cms/setting/tree')
  }, {
    path: '/cms/setting/profile',
    meta: { title: '基础信息' },
    component: () => import(/* webpackChunkName: 'cms' */ '@/views/cms/setting/profile')
  }, {
    path: '/cms/site/article',
    meta: { title: '文章管理', hiddenGlobalFooter: true },
    component: () => import(/* webpackChunkName: 'cms' */ '@/views/cms/site/article')
  }, {
    path: '/cms/site/catalog',
    meta: { title: '栏目管理' },
    component: () => import(/* webpackChunkName: 'cms' */ '@/views/cms/site/catalog')
  }, {
    path: '/cms/site/comment',
    meta: { title: '评论管理' },
    component: () => import(/* webpackChunkName: 'cms' */ '@/views/cms/site/comment')
  }, {
    path: '/cms/site/feedback',
    meta: { title: '留言反馈' },
    component: () => import(/* webpackChunkName: 'cms' */ '@/views/cms/site/feedback')
  }, {
    path: '/cms/site/tag',
    meta: { title: '标签管理' },
    component: () => import(/* webpackChunkName: 'cms' */ '@/views/cms/site/tag')
  }, {
    path: '/cms/site/cite',
    meta: { title: '引用管理' },
    component: () => import(/* webpackChunkName: 'cms' */ '@/views/cms/site/cite')
  }, {
    path: '/cms/site/editor',
    meta: { title: '文章发布' },
    component: () => import(/* webpackChunkName: 'cms' */ '@/views/cms/site/editor')
  }]
}]

export default { blanks, layouts }
