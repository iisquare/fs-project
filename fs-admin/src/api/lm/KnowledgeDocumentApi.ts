import base from './Api'

export default {
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
}
