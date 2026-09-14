import base from './Api'

export default {
  list (param: any, tips = {}) {
    return base.post('/knowledgeSegment/list', param, tips)
  },
  delete (ids: any, tips = {}) {
    return base.post('/knowledgeSegment/delete', { ids }, tips)
  },
  config (tips = {}) {
    return base.post('/knowledgeSegment/config', {}, tips)
  },
  save (param: any, tips = {}) {
    return base.post('/knowledgeSegment/save', param, tips)
  },
}
