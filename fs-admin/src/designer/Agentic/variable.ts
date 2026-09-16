import DesignUtil from '@/utils/DesignUtil'
import config from './config'

export interface VariableItem {
  value: string
  name: string
  label?: string
  type: string
  description?: string
}

export interface VariableGroup {
  label: string
  variables: VariableItem[]
}

const sysVariables: VariableItem[] = [
  { value: 'sys.query', name: 'query', label: '用户输入', type: 'String', description: '用户输入内容' },
  { value: 'sys.files', name: 'files', label: '用户文件', type: 'Array<File>', description: '用户上传的文件' },
  { value: 'sys.appId', name: 'appId', label: '应用标识', type: 'String', description: '当前应用标识' },
  { value: 'sys.userId', name: 'userId', label: '用户标识', type: 'String', description: '当前用户标识' },
  { value: 'sys.userName', name: 'userName', label: '用户名称', type: 'String', description: '当前用户名称' },
  { value: 'sys.conversationId', name: 'conversationId', label: '会话标识', type: 'String', description: '当前会话标识' },
  { value: 'sys.time', name: 'time', label: '当前时间', type: 'String', description: '当前时间' },
]

/** 变量的展示名称：标题名称优先，未配置时回落到变量名称 */
export const variableTitle = (item: any) => String(item?.label || item?.name || '')

/**
 * 变量占位符实际值 - 采用 `{{#节点标识.变量英文名称#}}`，如 `{{#n1.query#}}`，用于保存与后端解析
 */
export const variableToken = (reference: string) => `{{#${reference}#}}`

/**
 * 变量占位符的展示名称 - `节点名称.变量中文名称`，如 `开始.用户输入`
 */
export const variableLabel = (group: string, item: any) => `${group}.${variableTitle(item)}`

/**
 * 变量的插入信息映射：变量引用（`节点ID.变量名` 或 `sys.变量名`）→ `{ token, label }`
 * token 为实际写入文本的占位符，label 为编辑器中展示的名称标签
 */
export const variableTokens = (groups: VariableGroup[]) => {
  const result: Record<string, { token: string, label: string }> = {}
  groups.forEach((group) => {
    group.variables.forEach((item: any) => {
      result[item.value] = {
        token: variableToken(item.value),
        label: variableLabel(group.label, item),
      }
    })
  })
  return result
}

/**
 * 按关键字过滤变量分组，匹配变量中文名称、变量名称与节点名称，便于插入变量时快速查找
 */
export const filterVariableGroups = (groups: VariableGroup[], keyword: string) => {
  const word = String(keyword ?? '').trim().toUpperCase()
  if (!word) return groups
  return groups.map((group) => ({
    label: group.label,
    variables: group.variables.filter((item: any) => [
      variableTitle(item), item.name, group.label,
    ].some((text: any) => String(text ?? '').toUpperCase().indexOf(word) >= 0)),
  })).filter((group) => group.variables.length)
}

/**
 * 解析斜线触发：从一行文本与光标位置解析出触发字符后的查询词
 * @returns {Object|null} `{ from, word }`，from 为触发字符所在下标，未触发时返回 null
 */
export const parseTriggerWord = (line: string, ch: number, trigger = '/') => {
  const text = String(line ?? '')
  const start = text.lastIndexOf(trigger, ch - 1)
  if (start < 0) return null
  const word = text.slice(start + trigger.length, ch)
  // 触发字符后出现空白视为普通输入，不再提示
  if (/\s/.test(word)) return null
  return { from: start, word }
}

/**
 * 汇总画布中可被引用的变量，用于下游节点选择上游节点的输出
 * 迭代/循环的容器内变量（元素、索引、循环变量）仅对容器自身与其内部的节点可见
 * @param instance 画布实例（X6Container 暴露的 flow）
 * @param activeItem 当前激活的节点，用于排除自身
 */
export const variableGroups = (instance: any, activeItem: any = {}): VariableGroup[] => {
  const result: VariableGroup[] = [{
    label: '系统变量',
    variables: sysVariables.map(item => Object.assign({}, item)),
  }]
  // 容器作用域链（由内到外），用于判断容器内变量的可见性；节点尚未进入画布时不做限制
  const scopes: any = (() => {
    const cell: any = instance?.flow?.graph?.getCellById?.(activeItem?.id)
    if (!cell) return null
    const ids: string[] = []
    let parent: any = cell.getParent?.()
    while (parent) {
      if ('flow-subprocess' === parent.shape) ids.push(parent.id)
      parent = parent.getParent?.()
    }
    return ids
  })()
  const nodes: any[] = instance?.flow?.graph?.getNodes?.() ?? []
  nodes.forEach((node: any) => {
    const data = node.getData() ?? {}
    if (!DesignUtil.widgetByType(data.type, config)) return
    const self = node.id === activeItem?.id
    const items: any[] = config.outputs?.[data.type]?.(data) ?? []
    const variables = items.filter((item: any) => {
      if (!item?.name) return false
      // 容器内变量（元素、索引、循环变量）只对容器自身与其内部的节点可见
      if (item.scope) return !scopes || self || scopes.indexOf(node.id) !== -1
      return !self
    }).map((item: any) => ({
      value: `${node.id}.${item.name}`,
      name: item.name,
      // 标题名称用于展示与占位符，为空时回落到变量名称
      label: item.label || item.name,
      type: item.type ?? 'String',
      description: item.description,
    }))
    if (!variables.length) return
    result.push({ label: data.name ?? node.id, variables })
  })
  return result
}

export default {
  variableGroups, variableTokens, variableToken, variableLabel, variableTitle,
  filterVariableGroups, parseTriggerWord,
}
