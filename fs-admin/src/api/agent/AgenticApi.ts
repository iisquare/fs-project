import base from './Api'

export default {
  list (param: any, tips = {}) {
    return base.post('/agentic/list', param, tips)
  },
  info (id: any, tips = {}) {
    return base.get('/agentic/info', { id }, tips)
  },
  delete (ids: any, tips = {}) {
    return base.post('/agentic/delete', { ids }, tips)
  },
  config (tips = {}) {
    return base.post('/agentic/config', {}, tips)
  },
  save (param: any, tips = {}) {
    return base.post('/agentic/save', param, tips)
  },
  publish (param: any, tips = {}) {
    return base.post('/agentic/publish', param, tips)
  },
  run (param: any, tips = {}) {
    return base.post('/agentic/run', param, tips)
  },
  invoke (param: any, tips = {}) {
    return base.post('/agentic/invoke', param, tips)
  },
}
