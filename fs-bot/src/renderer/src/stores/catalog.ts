import { ref } from 'vue'
import { defineStore } from 'pinia'
import ApiUtil from '@renderer/utils/ApiUtil'
import AgenticApi from '@renderer/api/agent/AgenticApi'
import BotApi from '@renderer/api/agent/BotApi'
import SkillApi from '@renderer/api/agent/SkillApi'
import ToolApi from '@renderer/api/agent/ToolApi'
import KnowledgeApi from '@renderer/api/agent/KnowledgeApi'
import DataThemeApi from '@renderer/api/bi/DataThemeApi'
import OntologyApi from '@renderer/api/kg/OntologyApi'

/**
 * 目录接口的失败文案：权限类失败是 RBAC 拦下来的（服务端只回一句 permit denied），
 * 这里翻译成「找谁开通什么权限」，其余错误原样带出，便于页面直接展示。
 */
function catalogError(e: any, label: string, permit: string): string {
  const message = String(ApiUtil.message(e) ?? '').trim()
  if (!message || /forbidden|permit denied/i.test(message)) {
    return `当前账号没有${label}的查看权限（需授予 ${permit}），请联系管理员开通`
  }
  return `${label}加载失败：${message}`
}

/**
 * 服务端目录数据：可用应用（编排）、技能与版本、知识库、数据主题、知识图谱本体、模型清单、工具方法。
 *
 * 项目引用工具时按类型取对应清单（见 `ProjectEditor.vue` 的「工具」段），
 * 这些数据都由服务端维护，桌面端只读不写：客户端只保存「选中了哪些」。
 */
export const useCatalogStore = defineStore('catalog', () => {
  const apps = ref<any[]>([])
  const skills = ref<any[]>([])
  const knowledges = ref<any[]>([])
  const themes = ref<any[]>([])
  const ontologies = ref<any[]>([])
  const models = ref<any[]>([])
  const methods = ref<any[]>([])
  const loadingApps = ref(false)
  const loadingSkills = ref(false)
  const loadingKnowledges = ref(false)
  const loadingThemes = ref(false)
  const loadingOntologies = ref(false)
  const loadingModels = ref(false)
  const loadingMethods = ref(false)
  /** 加载失败原因：页面据此给出可执行提示，而不是弹服务端原始报错 */
  const appError = ref('')
  const skillError = ref('')
  const knowledgeError = ref('')
  const themeError = ref('')
  const ontologyError = ref('')
  const modelError = ref('')
  const methodError = ref('')

  /** 可用应用：新建项目 / 新建会话时选择 */
  async function fetchApps(): Promise<any[]> {
    loadingApps.value = true
    try {
      const result: any = await AgenticApi.authorized({ error: false, warning: false })
      apps.value = ApiUtil.data(result)?.rows ?? []
      appError.value = ''
    } catch (e) {
      apps.value = []
      appError.value = catalogError(e, '编排应用', 'agent:agentic:')
    } finally {
      loadingApps.value = false
    }
    return apps.value
  }

  /** 可用技能：只返回基础信息（不含配置内容） */
  async function fetchSkills(): Promise<any[]> {
    loadingSkills.value = true
    try {
      const result: any = await SkillApi.available({ error: false, warning: false })
      skills.value = ApiUtil.data(result)?.rows ?? []
      skillError.value = ''
    } catch (e) {
      skills.value = []
      skillError.value = catalogError(e, '技能', 'agent:skill:')
    } finally {
      loadingSkills.value = false
    }
    return skills.value
  }

  /** 技能版本：安装 / 升级时选择版本 */
  async function fetchVersions(skillId: number): Promise<any[]> {
    try {
      const result: any = await SkillApi.versions(skillId, { error: false, warning: false })
      return ApiUtil.data(result)?.rows ?? []
    } catch (e) {
      skillError.value = catalogError(e, '技能版本', 'agent:skill:')
      return []
    }
  }

  /** 可用知识库：项目引用时选择，内容与检索配置都在服务端 */
  async function fetchKnowledges(keyword = ''): Promise<any[]> {
    loadingKnowledges.value = true
    try {
      const result: any = await KnowledgeApi.list(
        { name: keyword, page: 1, pageSize: 100, status: 1 },
        { error: false, warning: false }
      )
      knowledges.value = ApiUtil.data(result)?.rows ?? []
      knowledgeError.value = ''
    } catch (e) {
      knowledges.value = []
      knowledgeError.value = catalogError(e, '知识库', 'agent:knowledge:')
    } finally {
      loadingKnowledges.value = false
    }
    return knowledges.value
  }

  /** 可用数据主题：项目把它作为一种工具引用，主题的数据集与字段都在服务端 */
  async function fetchThemes(keyword = ''): Promise<any[]> {
    loadingThemes.value = true
    try {
      const result: any = await DataThemeApi.list(
        { name: keyword, page: 1, pageSize: 100, status: 1 },
        { error: false, warning: false }
      )
      themes.value = ApiUtil.data(result)?.rows ?? []
      themeError.value = ''
    } catch (e) {
      themes.value = []
      themeError.value = catalogError(e, '数据主题', 'bi:dataTheme:')
    } finally {
      loadingThemes.value = false
    }
    return themes.value
  }

  /** 可用知识图谱本体：项目把它作为一种工具引用，实体与关联检索都在服务端 */
  async function fetchOntologies(keyword = ''): Promise<any[]> {
    loadingOntologies.value = true
    try {
      const result: any = await OntologyApi.list(
        { name: keyword, page: 1, pageSize: 100, status: 1 },
        { error: false, warning: false }
      )
      ontologies.value = ApiUtil.data(result)?.rows ?? []
      ontologyError.value = ''
    } catch (e) {
      ontologies.value = []
      ontologyError.value = catalogError(e, '知识图谱本体', 'kg:ontology:')
    } finally {
      loadingOntologies.value = false
    }
    return ontologies.value
  }

  /** 可用模型：项目用它直接发起模型对话，清单取自模型网关 */
  async function fetchModels(): Promise<any[]> {
    loadingModels.value = true
    try {
      models.value = await modelRows()
      modelError.value = ''
    } catch (e) {
      models.value = []
      modelError.value = catalogError(e, '模型清单', 'agent:bot:')
    } finally {
      loadingModels.value = false
    }
    return models.value
  }

  /** 模型清单只走客户端专用 bot 接口，不借用模型对比权限。 */
  async function modelRows(): Promise<any[]> {
    const result: any = await BotApi.models({ error: false, warning: false })
    return ApiUtil.data(result)?.rows ?? []
  }

  /** 工具方法检索：选择器里按关键词分页取，行内含工具与方法名 */
  async function searchMethods(
    keyword = '',
    page = 1,
    pageSize = 50
  ): Promise<{ rows: any[]; total: number }> {
    loadingMethods.value = true
    try {
      const result: any = await ToolApi.methodList(
        { name: keyword, page, pageSize, status: 1, present: 1 },
        { error: false, warning: false }
      )
      const rows = ApiUtil.data(result)?.rows ?? []
      const total = ApiUtil.data(result)?.total ?? rows.length
      methods.value = rows
      methodError.value = ''
      return { rows, total }
    } catch (e) {
      methods.value = []
      methodError.value = catalogError(e, '工具方法', 'agent:tool:')
      return { rows: [], total: 0 }
    } finally {
      loadingMethods.value = false
    }
  }

  /**
   * 单个方法的参数明细：配置固定参数时按「工具标识 + 方法名」精确定位取一次。
   * 选中方法时缓存里的行本来就带明细，这里补的是「离开页面后再回来」那一次；
   * 返回 null 表示取不到（接口不通或方法已被删除），空数组表示这个方法确实没有参数。
   */
  async function fetchMethodParams(toolId: number, name: string): Promise<any[] | null> {
    if (!toolId || !name) return null
    try {
      const result: any = await ToolApi.methodList(
        { toolId, name, page: 1, pageSize: 20 },
        { error: false, warning: false }
      )
      const rows = ApiUtil.data(result)?.rows ?? []
      const row = rows.find((item: any) => String(item?.name ?? '') === name)
      return row ? (row.params ?? []) : null
    } catch {
      return null
    }
  }

  /**
   * 服务端声明的上下文窗口（词元数）：模型不在清单里（手工输入的模型）或服务端没配时为 0，
   * 表示未知——此时界面上的用量圆环只能由用户手工指定窗口后才有比例。
   */
  function modelContext(name: string): number {
    const row = models.value.find((item) => item?.name === name)
    const length = Math.floor(Number(row?.contextLength ?? 0))
    return Number.isFinite(length) && length > 0 ? length : 0
  }

  return {
    apps,
    skills,
    knowledges,
    themes,
    ontologies,
    models,
    methods,
    loadingApps,
    loadingSkills,
    loadingKnowledges,
    loadingThemes,
    loadingOntologies,
    loadingModels,
    loadingMethods,
    appError,
    skillError,
    knowledgeError,
    themeError,
    ontologyError,
    modelError,
    methodError,
    fetchApps,
    fetchSkills,
    fetchVersions,
    fetchKnowledges,
    fetchThemes,
    fetchOntologies,
    fetchModels,
    searchMethods,
    fetchMethodParams,
    modelContext
  }
})
