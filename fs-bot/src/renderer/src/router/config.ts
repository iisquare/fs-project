export const page = {
  root: '/',
  login: '/login',
  home: '/home',
  search: '/search',
  profile: '/profile',
  settings: '/settings',
  project: '/project',
  schedule: '/schedule',
  skill: '/skill',
  e404: '/404'
}

/** 项目管理页「直接进新建表单」的查询参数值 */
export const projectCreateAction = 'create'

/**
 * 项目管理页的新建入口：带 action=create 进去直接展开新建表单。
 * 一个项目都没有时的入口（首页空状态、侧边栏「新对话」）都走这里，
 * 免得先落到空列表再让用户点一次「新建项目」。
 */
export function projectCreateRoute(): { path: string; query: { action: string } } {
  return { path: page.project, query: { action: projectCreateAction } }
}
