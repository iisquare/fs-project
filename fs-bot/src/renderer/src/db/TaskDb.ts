import Db from '@renderer/core/Db'
import ApiUtil from '@renderer/utils/ApiUtil'

const TABLE = 'tasks'

export interface ScheduleTask {
  id: string
  projectId: string
  name: string
  cron: string
  prompt: string
  inputs: Record<string, any>
  enabled: boolean
  lastRunAt: string
  lastStatus: string
  lastMessage: string
  createdAt: string
  updatedAt: string
}

function parse(text: unknown): Record<string, any> {
  if (typeof text !== 'string' || !text) return {}
  try {
    return JSON.parse(text) as Record<string, any>
  } catch {
    return {}
  }
}

function format(row: Record<string, any>): ScheduleTask {
  return {
    id: String(row['id'] ?? ''),
    projectId: String(row['project_id'] ?? ''),
    name: String(row['name'] ?? ''),
    cron: String(row['cron'] ?? ''),
    prompt: String(row['prompt'] ?? ''),
    inputs: parse(row['inputs']),
    enabled: Number(row['enabled'] ?? 1) === 1,
    lastRunAt: String(row['last_run_at'] ?? ''),
    lastStatus: String(row['last_status'] ?? ''),
    lastMessage: String(row['last_message'] ?? ''),
    createdAt: String(row['created_at'] ?? ''),
    updatedAt: String(row['updated_at'] ?? '')
  }
}

export default {
  async list(): Promise<{ code: number; message: string; data: ScheduleTask[] }> {
    const result: any = await Db.select(TABLE, undefined, 'updated_at DESC')
    return ApiUtil.result(
      ApiUtil.code(result) ?? 0,
      ApiUtil.message(result),
      (ApiUtil.data(result) as any[]).map(format)
    )
  },

  async create(data: Partial<ScheduleTask>) {
    const now = new Date().toISOString()
    return Db.insert(TABLE, {
      id: crypto.randomUUID(),
      project_id: data.projectId ?? '',
      name: data.name ?? '未命名任务',
      cron: data.cron ?? '0 9 * * *',
      prompt: data.prompt ?? '',
      inputs: JSON.stringify(data.inputs ?? {}),
      enabled: data.enabled === false ? 0 : 1,
      last_run_at: '',
      last_status: '',
      last_message: '',
      created_at: now,
      updated_at: now
    })
  },

  async update(id: string, data: Partial<ScheduleTask>) {
    const row: Record<string, unknown> = { updated_at: new Date().toISOString() }
    if (data.projectId !== undefined) row['project_id'] = data.projectId
    if (data.name !== undefined) row['name'] = data.name
    if (data.cron !== undefined) row['cron'] = data.cron
    if (data.prompt !== undefined) row['prompt'] = data.prompt
    if (data.inputs !== undefined) row['inputs'] = JSON.stringify(data.inputs)
    if (data.enabled !== undefined) row['enabled'] = data.enabled ? 1 : 0
    return Db.update(TABLE, { id }, row)
  },

  /** 记录一次执行结果：页面列表直接展示最近一次状态 */
  async report(id: string, status: string, message: string) {
    return Db.update(
      TABLE,
      { id },
      {
        last_run_at: new Date().toISOString(),
        last_status: status,
        last_message: message,
        updated_at: new Date().toISOString()
      }
    )
  },

  async delete(id: string) {
    return Db.remove(TABLE, { id })
  }
}
