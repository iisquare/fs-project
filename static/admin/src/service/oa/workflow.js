import base from './base'

export default {
  info (param, tips = {}) {
    return base.post('/workflow/info', param, tips)
  },
  list (param, tips = {}) {
    return base.post('/workflow/list', param, tips)
  },
  delete (ids, tips = {}) {
    return base.post('/workflow/delete', { ids }, tips)
  },
  config (tips = {}) {
    return base.post('/workflow/config', {}, tips)
  },
  save (param, tips = {}) {
    return base.post('/workflow/save', param, tips)
  },
  publish (param, tips = {}) {
    return base.post('/workflow/publish', param, tips)
  },
  searchDeployment (param, tips = {}) {
    return base.post('/workflow/searchDeployment', param, tips)
  },
  deleteDeployment (param, tips = {}) {
    return base.post('/workflow/deleteDeployment', param, tips)
  }
}
