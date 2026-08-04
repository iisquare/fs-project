/**
 * 数据集管理 API
 */
import base from './Api'

export default {
  info(id: any, tips = {}) {
    return base.post('/dataset/info', { id }, tips)
  },
  list(params: any = {}, tips = {}) {
    return base.post('/dataset/list', params, tips)
  },
  save(data: any, tips = {}) {
    return base.post('/dataset/save', data, tips)
  },
  delete(ids: any, tips = {}) {
    return base.post('/dataset/delete', { ids }, tips)
  },
  config(tips = {}) {
    return base.post('/dataset/config', {}, tips)
  },
  /** 获取数据集 SQL 的列结构（通过 Calcite 解析） */
  sqlSchema(id: any, tips = {}) {
    return base.post('/dataset/sqlSchema', { id }, tips)
  },
  /** 获取数据集 SQL 的预览数据（通过 Calcite 执行） */
  sqlPreview(id: any, limit: number = 100, tips = {}) {
    return base.post('/dataset/sqlPreview', { id, limit }, tips)
  },
  /** 获取数据集表字段定义 */
  columns(id: any, tips = {}) {
    return base.post('/dataset/columns', { id }, tips)
  },
}

/** 构建 Dataset content JSON */
export function buildDatasetContent(opts: {
  sourceIds: number[]
  sql: string
  table?: { name: string; type: string; format: string; enabled: boolean }[]
  collection?: string
}): string {
  return JSON.stringify({
    relation: {
      items: opts.sourceIds.map(id => ({ sourceId: id })),
    },
    sql: opts.sql,
    table: opts.table || [],
    collection: opts.collection || '',
  })
}

/** 解析 Dataset content JSON */
export function parseDatasetContent(content: string) {
  const obj = typeof content === 'string' ? JSON.parse(content || '{}') : (content || {})
  return {
    sourceIds: (obj.relation?.items || []).map((item: any) => item.sourceId),
    sql: obj.sql || '',
    table: obj.table || [],
    collection: obj.collection || '',
  }
}
