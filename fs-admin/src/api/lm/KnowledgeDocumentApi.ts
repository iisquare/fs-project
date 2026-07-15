import base from './Api'

export default {
  info (id: any, tips = {}) {
    return base.get('/knowledgeDocument/info', { id }, tips)
  },
  list (param: any, tips = {}) {
    return base.post('/knowledgeDocument/list', param, tips)
  },
  delete (ids: any, tips = {}) {
    return base.post('/knowledgeDocument/delete', { ids }, tips)
  },
  config (tips = {}) {
    return base.post('/knowledgeDocument/config', {}, tips)
  },
  save (param: any, tips = {}) {
    return base.post('/knowledgeDocument/save', param, tips)
  },
  upload (param: any, tips = {}) {
    return base.form('/knowledgeDocument/upload', param, tips)
  },
}
