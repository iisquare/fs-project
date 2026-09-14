import base from './Api'

export default {
  list (param: any, tips = {}) {
    return base.post('/knowledgeChunk/list', param, tips)
  },
  delete (ids: any, tips = {}) {
    return base.post('/knowledgeChunk/delete', { ids }, tips)
  },
  config (tips = {}) {
    return base.post('/knowledgeChunk/config', {}, tips)
  },
  save (param: any, tips = {}) {
    return base.post('/knowledgeChunk/save', param, tips)
  },
}
