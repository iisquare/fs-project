import { ref } from 'vue'
import { defineStore } from 'pinia'
import { useUserStore } from './user'

/**
 * 本地技能：服务端下发的技能包解压到 skills 目录后，由主进程扫描目录得到清单。
 * 页面只负责选择版本并触发安装，下载与解压都在主进程完成。
 */

export interface InstalledSkillMeta {
  skillId: number
  skillName: string
  versionId: number
  versionName: string
  installedAt: number
}

export interface InstalledSkill {
  name: string
  path: string
  fileCount: number
  size: number
  meta: InstalledSkillMeta | null
  /** 技能摘要：SKILL.md 里 YAML 的 description，没有就取第一段正文 */
  summary: string
}

export interface SkillInstallParams {
  skillId: number
  skillName: string
  versionId: number
  versionName: string
}

export const useSkillStore = defineStore('skill', () => {
  const installed = ref<InstalledSkill[]>([])
  const root = ref('')
  const loading = ref(false)
  const installing = ref<number>(0)

  async function fetchInstalled(): Promise<InstalledSkill[]> {
    loading.value = true
    try {
      root.value = await window.ipc.skill.path()
      installed.value = await window.ipc.skill.list()
    } finally {
      loading.value = false
    }
    return installed.value
  }

  /** 安装指定版本：服务端取包 → 解压到 skills/<技能名>（覆盖旧版本） */
  async function install(params: SkillInstallParams): Promise<InstalledSkill> {
    const user = useUserStore()
    installing.value = params.versionId
    try {
      const result = await window.ipc.skill.install({
        baseUrl: String(import.meta.env.VITE_APP_API_URL || ''),
        token: user.info.token,
        skillId: params.skillId,
        skillName: params.skillName,
        versionId: params.versionId,
        versionName: params.versionName
      })
      await fetchInstalled()
      return result
    } finally {
      installing.value = 0
    }
  }

  async function remove(name: string): Promise<void> {
    await window.ipc.skill.remove(name)
    await fetchInstalled()
  }

  function find(skillId: number): InstalledSkill | null {
    return installed.value.find((item) => Number(item.meta?.skillId) === Number(skillId)) ?? null
  }

  return { installed, root, loading, installing, fetchInstalled, install, remove, find }
})
