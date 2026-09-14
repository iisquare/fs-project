import base from './Api'

export default {
  info (param: any, tips = {}) {
      return base.post('/mcp/info', param, tips)
    },
  list (param: any, tips = {}) {
    return base.post('/mcp/list', param, tips)
  },
  delete (ids: any, tips = {}) {
    return base.post('/mcp/delete', { ids }, tips)
  },
  config (tips = {}) {
    return base.post('/mcp/config', {}, tips)
  },
  save (param: any, tips = {}) {
    return base.post('/mcp/save', param, tips)
  },
}
