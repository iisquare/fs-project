import { ref } from 'vue'
import { defineStore } from 'pinia'
import ApiUtil from '@renderer/utils/ApiUtil'
import ProjectApi from '@renderer/api/agent/ProjectApi'
import ProjectDb from '@renderer/db/ProjectDb'
import { configOf, publishConfig } from '@renderer/utils/ProjectUtil'
import type { Project } from '@renderer/db/ProjectDb'

/**
 * 项目分享：服务端上的分享项目与版本记录。
 *
 * 服务端才是分享内容的权威存储（本项目只把「分享到哪个项目、最新版本号」记在本地项目的
 * share_id / share_name / share_version 上做回显），所以列表与版本每次都按需拉取，不做本地缓存。
 */

/** 分享的项目（服务端出参） */
export interface ShareProject {
  id: number
  name: string
  icon: string
  description: string
  sort: number
  status: number
  statusText: string
  versionCount: number
  latestVersion: ShareVersion | null
  /** 是不是我分享的：浏览列表里据此区分自己的项目 */
  mine: boolean
  createdUid: number
  createdUserInfo?: { id?: number; name?: string }
  createdTime: number
  updatedTime: number
}

/** 分享项目的版本记录（列表出参，不含配置原文） */
export interface ShareVersion {
  id: number
  projectId: number
  name: string
  status: number
  statusText: string
  description: string
  /** 配置原文的大小（UTF-8 字节） */
  contentSize: number
  createdUid: number
  createdUserInfo?: { id?: number; name?: string }
  createdTime: number
  updatedTime: number
}

/** 发布入参：本地项目 + 分享目标（0 表示新建）+ 版本号与版本说明 */
export interface PublishPayload {
  project: Project
  projectId: number
  name: string
  version: string
  versionDescription: string
}

/** 版本记录的一页：页面按 total 决定要不要给「加载更多」 */
export interface ShareVersionPage {
  rows: ShareVersion[]
  total: number
  page: number
}

/** 版本清单的查询条件：页码、页大小与版本号检索词 */
export interface ShareVersionQuery {
  page?: number
  pageSize?: number
  name?: string
}

/** 分享项目列表每页条数：首页只铺这些，其余按「加载更多」续取 */
const LIST_PAGE_SIZE = 30
/** 版本记录每页条数：与服务端默认值一致 */
export const VERSION_PAGE_SIZE = 20

export const useShareStore = defineStore('share', () => {
  const mine = ref<ShareProject[]>([])
  const mineTotal = ref(0)
  const minePage = ref(1)
  const browse = ref<ShareProject[]>([])
  const browseTotal = ref(0)
  const browsePage = ref(1)
  const loadingMine = ref(false)
  const loadingBrowse = ref(false)

  /**
   * 我分享过的项目：停用的项目与停用的版本在这里都能看到（本人视角）。
   * page 大于 1 表示续取（追加到已有列表后面），page 为 1 是重新拉一份。
   */
  async function fetchMine(name = '', page = 1): Promise<ShareProject[]> {
    loadingMine.value = true
    try {
      const result: any = await ProjectApi.mine({ name, page, pageSize: LIST_PAGE_SIZE })
      const data = ApiUtil.data(result) ?? {}
      const rows = data.rows ?? []
      mine.value = page > 1 ? [...mine.value, ...rows] : rows
      mineTotal.value = data.total ?? 0
      minePage.value = page
    } finally {
      loadingMine.value = false
    }
    return mine.value
  }

  /** 浏览别人分享的项目：可见范围由服务端判定（已启用 + 授权命中） */
  async function fetchBrowse(name = '', page = 1): Promise<ShareProject[]> {
    loadingBrowse.value = true
    try {
      const result: any = await ProjectApi.browse({ name, page, pageSize: LIST_PAGE_SIZE })
      const data = ApiUtil.data(result) ?? {}
      const rows = data.rows ?? []
      browse.value = page > 1 ? [...browse.value, ...rows] : rows
      browseTotal.value = data.total ?? 0
      browsePage.value = page
    } finally {
      loadingBrowse.value = false
    }
    return browse.value
  }

  /** 续取下一页（列表已经铺满一页时用） */
  async function loadMoreMine(name = ''): Promise<ShareProject[]> {
    if (mine.value.length >= mineTotal.value) return mine.value
    return fetchMine(name, minePage.value + 1)
  }

  async function loadMoreBrowse(name = ''): Promise<ShareProject[]> {
    if (browse.value.length >= browseTotal.value) return browse.value
    return fetchBrowse(name, browsePage.value + 1)
  }

  /** 某个分享项目的版本记录（一页）：列表不含配置原文，导出 / 创建本地项目时再按版本取详情 */
  async function fetchVersions(
    projectId: number,
    query: ShareVersionQuery = {}
  ): Promise<ShareVersionPage> {
    const page = query.page ?? 1
    const result: any = await ProjectApi.versions(projectId, {
      page,
      pageSize: query.pageSize ?? VERSION_PAGE_SIZE,
      name: query.name ?? ''
    })
    const data = ApiUtil.data(result) ?? {}
    return {
      rows: data.rows ?? [],
      total: Number(data.total ?? 0),
      page: Number(data.page ?? page)
    }
  }

  /** 版本详情：带 `{ project, version: { ..., content } }`，content 就是项目配置 */
  async function fetchVersion(id: number): Promise<any | null> {
    const result: any = await ProjectApi.version(id)
    return ApiUtil.succeed(result) ? ApiUtil.data(result) : null
  }

  /**
   * 发布一个版本：把项目配置（模型、提示词、工具、工作区等）提交到服务端，
   * 成功后把分享去向记回本地项目（卡片回显 + 下次分享默认选中同一个分享项目）。
   *
   * 环境变量按 `publishConfig` 过一遍再发：标了「密钥」的行整行留在本机——
   * 那些值是用来在本机跑命令的凭据（见 `main/project-env.ts`），不进服务端。
   */
  async function publish(payload: PublishPayload): Promise<any> {
    const project = payload.project
    const result: any = await ProjectApi.publish({
      projectId: payload.projectId,
      name: payload.name,
      icon: project.icon,
      description: project.description,
      version: payload.version,
      versionDescription: payload.versionDescription,
      content: publishConfig(configOf(project))
    })
    const data = ApiUtil.data(result) ?? {}
    await ProjectDb.update(project.id, {
      shareId: Number(data?.project?.id ?? 0),
      shareName: String(data?.project?.name ?? ''),
      shareVersion: String(data?.version?.name ?? '')
    })
    return data
  }

  function findMine(id: number): ShareProject | null {
    return mine.value.find((item) => Number(item.id) === Number(id)) ?? null
  }

  return {
    mine,
    mineTotal,
    browse,
    browseTotal,
    loadingMine,
    loadingBrowse,
    fetchMine,
    fetchBrowse,
    loadMoreMine,
    loadMoreBrowse,
    fetchVersions,
    fetchVersion,
    publish,
    findMine
  }
})
