import base from './Api'

export default {
  list (param: any, tips = {}) {
    return base.post('/tool/list', param, tips)
  },
  delete (ids: any, tips = {}) {
    return base.post('/tool/delete', { ids }, tips)
  },
  config (tips = {}) {
    return base.post('/tool/config', {}, tips)
  },
  save (param: any, tips = {}) {
    return base.post('/tool/save', param, tips)
  },
  mcpSync (param: any, tips = {}) {
    return base.post('/tool/mcpSync', param, tips)
  },
}
