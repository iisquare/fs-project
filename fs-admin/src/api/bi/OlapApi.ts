import base from './Api'

const byName = (a: any, b: any) => (a.name || '').localeCompare(b.name || '')

export default {
  catalogs(params: any = {}, tips = {}) {
    return base.post('/olap/catalogs', params, tips).then((result: any) => {
      const data = result.data || {}
      data.catalogs = (data.catalogs || []).slice().sort(byName)
      return result
    })
  },
  schemas(params: any = {}, tips = {}) {
    return base.post('/olap/schemas', params, tips).then((result: any) => {
      const data = result.data || {}
      data.schemas = (data.schemas || []).slice().sort(byName)
      return result
    })
  },
  tables(params: any = {}, tips = {}) {
    return base.post('/olap/tables', params, tips).then((result: any) => {
      const data = result.data || {}
      data.tables = (data.tables || [])
        .filter((item: any) => !(item.name || '').startsWith('.'))
        .sort(byName)
      return result
    })
  },
  columns(params: any = {}, tips = {}) {
    return base.post('/olap/columns', params, tips).then((result: any) => {
      const data = result.data || {}
      data.columns = (data.columns || []).slice().sort(byName)
      return result
    })
  },
  config(params: any = {}, tips = {}) {
    return base.post('/olap/config', params, tips)
  },
  query(params: any = {}, tips = {}) {
    return base.form('/olap/query', params, tips)
  },
}
