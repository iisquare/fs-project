/**
 * 工具方法的参数展示与固定参数整理：项目引用工具方法时都用这一套口径。
 *
 * 方法行的参数形如 `{ name, type, required, description, schema }`：
 * 对象参数（OpenAPI 的 body、MCP 的嵌套对象）的字段在 `schema.properties` 里，可以按字段单独固定。
 */
import type { ProjectToolArgs } from '@renderer/db/ProjectDb'

/** 对象参数的字段清单（只取一层）：字段名、类型、是否必填与说明 */
export function paramFields(
  parameter: any
): Array<{ name: string; type: string; required: boolean; description: string }> {
  const properties: any = parameter?.schema?.properties
  if (!properties) return []
  const required: string[] = parameter?.schema?.required ?? []
  return Object.keys(properties).map((name: string) => ({
    name,
    type: String(properties[name]?.type ?? 'string'),
    required: required.indexOf(name) >= 0,
    description: String(properties[name]?.description ?? '')
  }))
}

/** 参数的类型文案：`string · 必填`、`object{id、name} · 必填` */
export function paramTypeText(parameter: any): string {
  const keys = paramFields(parameter).map((field) => field.name)
  const type = String(parameter?.type ?? 'string')
  const text = keys.length ? `${type}{${keys.slice(0, 6).join('、')}}` : type
  return `${text}${parameter?.required ? ' · 必填' : ''}`
}

/** 参数的悬浮说明：类型 + 描述 */
export function paramHint(parameter: any): string {
  return [paramTypeText(parameter), String(parameter?.description ?? '')].filter(Boolean).join('\n')
}

/** 取值是否算填了：空白按未填处理（固定值留空等于不固定） */
const hasText = (value: any) => '' !== String(value ?? '').trim()

/** 已固定参数的数量：对象参数按已固定的字段数算 */
export function fixedCount(args?: ProjectToolArgs): number {
  let count = 0
  Object.values(args ?? {}).forEach((arg) => {
    const fields = Object.keys(arg?.fields ?? {}).filter(
      (field) => false === arg.fields?.[field]?.auto && hasText(arg.fields?.[field]?.value)
    )
    if (fields.length) {
      count += fields.length
      return
    }
    if (false === arg?.auto && hasText(arg?.value)) count += 1
  })
  return count
}

/**
 * 固定参数整理：只保留「关掉自动且填了取值」的项，空项与自动项都不落库，
 * 换过方法或改回自动后不在项目里留下残留；一个固定项都没有时返回 undefined。
 */
export function pruneArgs(
  args: ProjectToolArgs | undefined,
  params: any[]
): ProjectToolArgs | undefined {
  const result: ProjectToolArgs = {}
  params.forEach((parameter: any) => {
    const binding: any = args?.[parameter?.name] ?? {}
    const fields = paramFields(parameter)
    if (fields.length) {
      const fixed: Record<string, any> = {}
      fields.forEach((field: any) => {
        const value: any = binding.fields?.[field.name]
        if (false === value?.auto && hasText(value?.value)) {
          fixed[field.name] = { auto: false, value: String(value.value) }
        }
      })
      if (Object.keys(fixed).length) result[parameter.name] = { fields: fixed }
      return
    }
    if (false === binding?.auto && hasText(binding?.value)) {
      result[parameter.name] = { auto: false, value: String(binding.value) }
    }
  })
  return Object.keys(result).length ? result : undefined
}
