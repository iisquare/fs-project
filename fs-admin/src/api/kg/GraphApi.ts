import base from './Api'

export default {
  summary (param: any, tips = {}) {
    return base.post('/graph/summary', param, tips)
  },
  search (param: any, tips = {}) {
    return base.post('/graph/search', param, tips)
  },
  info (param: any, tips = {}) {
    return base.post('/graph/info', param, tips)
  },
  save (param: any, tips = {}) {
    return base.post('/graph/save', param, tips)
  },
  batch (param: any, tips = {}) {
    return base.post('/graph/batch', param, tips)
  },
  exportData (param: any, tips = {}) {
    return base.post('/graph/export', param, tips)
  },
  exportExcel (param: any, tips = {}) {
    return base.post('/graph/exportExcel', param, tips)
  },
  importTemplate (param: any, tips = {}) {
    return base.post('/graph/importTemplate', param, tips)
  },
  importExcel (data: any, tips = {}) {
    return base.form('/graph/importExcel', data, tips)
  },
  queryList (param: any, tips = {}) {
    return base.post('/graph/queryList', param, tips)
  },
  querySave (param: any, tips = {}) {
    return base.post('/graph/querySave', param, tips)
  },
  queryDelete (param: any, tips = {}) {
    return base.post('/graph/queryDelete', param, tips)
  },
  remove (param: any, tips = {}) {
    return base.post('/graph/remove', param, tips)
  },
  aggregate (param: any, tips = {}) {
    return base.post('/graph/aggregate', param, tips)
  },
  path (param: any, tips = {}) {
    return base.post('/graph/path', param, tips)
  },
  paths (param: any, tips = {}) {
    return base.post('/graph/paths', param, tips)
  },
  inspect (param: any, tips = {}) {
    return base.post('/graph/inspect', param, tips)
  },
  logClean (param: any, tips = {}) {
    return base.post('/graph/logClean', param, tips)
  },
  relationshipSearch (param: any, tips = {}) {
    return base.post('/graph/relationshipSearch', param, tips)
  },
  relationshipSave (param: any, tips = {}) {
    return base.post('/graph/relationshipSave', param, tips)
  },
  relationshipRemove (param: any, tips = {}) {
    return base.post('/graph/relationshipRemove', param, tips)
  },
  traverse (param: any, tips = {}) {
    return base.post('/graph/traverse', param, tips)
  },
  log (param: any, tips = {}) {
    return base.post('/graph/log', param, tips)
  },
}
