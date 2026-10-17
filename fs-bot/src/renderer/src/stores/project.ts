import { ref } from 'vue'
import { defineStore } from 'pinia'
import ApiUtil from '@renderer/utils/ApiUtil'
import ProjectDb from '@renderer/db/ProjectDb'
import type { Project } from '@renderer/db/ProjectDb'

/**
 * 项目：一个项目 = 图标 / 名称 / 描述 / 环境变量 / 技能 / 工具 / 工作区 + 绑定的服务端应用。
 *
 * 会话按项目创建：每个会话用自己的项目口径（模型 / 工具 / 工作区权限），
 * 多个项目可以同时跑各自的会话，所以这里没有「当前项目」——需要项目的地方都按会话归属去取。
 */
export const useProjectStore = defineStore('project', () => {
  const projects = ref<Project[]>([])
  const loading = ref(false)

  async function fetchProjects(): Promise<Project[]> {
    loading.value = true
    try {
      const result: any = await ProjectDb.list()
      if (!ApiUtil.succeed(result)) {
        throw new Error(String(ApiUtil.message(result) || '项目列表读取失败'))
      }
      projects.value = ApiUtil.data(result) as Project[]
    } finally {
      loading.value = false
    }
    return projects.value
  }

  async function create(data: Partial<Project>) {
    const result: any = await ProjectDb.create(data)
    await fetchProjects()
    return result
  }

  async function update(id: string, data: Partial<Project>) {
    const result: any = await ProjectDb.update(id, data)
    await fetchProjects()
    return result
  }

  async function remove(id: string) {
    const result: any = await ProjectDb.delete(id)
    await fetchProjects()
    return result
  }

  function find(id: string): Project | null {
    return projects.value.find((item) => item.id === id) ?? null
  }

  return {
    projects,
    loading,
    fetchProjects,
    create,
    update,
    remove,
    find
  }
})
