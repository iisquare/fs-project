import base from './Api'

export default {
  info(id: any, tips = {}) {
    return base.post('/diagram/info', { id }, tips)
  },
  list(params: any = {}, tips = {}) {
    return base.post('/diagram/list', params, tips)
  },
  save(data: any, tips = {}) {
    return base.post('/diagram/save', data, tips)
  },
  delete(ids: any, tips = {}) {
    return base.post('/diagram/delete', { ids }, tips)
  },
  config(tips = {}) {
    return base.post('/diagram/config', {}, tips)
  },
  dagConfig(tips = {}) {
    return base.post('/dag/config', {}, tips)
  },
  diagram(id: any, tips = {}) {
    return base.get('/dag/diagram', { id }, tips)
  },
  run(data: any, tips = {}) {
    return base.post('/dag/run', data, tips)
  },
  debug(data: any, tips = {}) {
    return base.post('/dag/debug', data, tips)
  },
  tables(data: any, tips = {}) {
    return base.post('/dag/tables', data, tips)
  },
  columns(data: any, tips = {}) {
    return base.post('/dag/columns', data, tips)
  },
  jarList(params: any = {}, tips = {}) {
    return base.post('/diagram/jar/list', params, tips)
  },
  jarInfo(id: string, tips = {}) {
    return base.get('/diagram/jar/info', { id }, tips)
  },
  jarNodes(id: string, tips = {}) {
    return base.get('/diagram/jar/nodes', { id }, tips)
  },
  uploadJar(file: File, tips = {}) {
    return base.form('/diagram/jar/upload', { file }, tips)
  },
  deleteJar(id: string, tips = {}) {
    return base.post('/diagram/jar/delete', { id }, tips)
  },
}
