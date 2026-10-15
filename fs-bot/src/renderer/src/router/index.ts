import { createRouter, createWebHashHistory } from 'vue-router'
import { page } from './config'
import { useUserStore } from '@renderer/stores/user'

const routes = [
  {
    path: page.login,
    meta: { title: '用户登录' },
    component: () => import('@renderer/views/login.vue')
  },
  {
    path: page.root,
    component: () => import('@renderer/views/index.vue'),
    redirect: page.home,
    children: [
      {
        path: page.home,
        meta: { title: '首页' },
        component: () => import('@renderer/views/home/HomePage.vue')
      },
      {
        path: page.search,
        meta: { title: '对话检索' },
        component: () => import('@renderer/views/history/SearchPage.vue')
      },
      {
        path: page.profile,
        meta: { title: '个人信息' },
        component: () => import('@renderer/views/profile/PersonalInfo.vue')
      },
      {
        path: page.settings,
        meta: { title: '系统设置' },
        component: () => import('@renderer/views/settings/SystemSettings.vue')
      },
      {
        path: page.project,
        meta: { title: '项目管理' },
        component: () => import('@renderer/views/project/ProjectManagement.vue')
      },
      {
        path: page.schedule,
        meta: { title: '定时任务' },
        component: () => import('@renderer/views/schedule/ScheduledTasks.vue')
      },
      {
        path: page.skill,
        meta: { title: '技能管理' },
        component: () => import('@renderer/views/config/SkillManagement.vue')
      }
    ]
  },
  {
    path: page.e404,
    meta: { title: '404' },
    component: () => import('@renderer/views/404.vue')
  },
  {
    path: '/:catchAll(.*)',
    redirect: page.e404
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

const title = document.title

router.beforeEach((to, _from) => {
  document.title = to.meta?.title ? `${to.meta.title} - fs-bot` : title
  if (to.path === page.login) {
    const user = useUserStore()
    if (user.isLogined()) {
      return { path: page.home }
    }
    return true
  }

  const user = useUserStore()
  if (!user.isLogined()) {
    return { path: page.login, query: { redirect: to.fullPath } }
  }

  return true
})

export default router
