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
  },
  searchHistory (param, tips = {}) {
    return base.post('/workflow/searchHistory', param, tips)
  },
  process (param, tips = {}) {
    return base.post('/workflow/process', param, tips)
  },
  deleteProcessInstance (param, tips = {}) {
    return base.post('/workflow/deleteProcessInstance', param, tips)
  },
  deleteHistoricProcessInstance (param, tips = {}) {
    return base.post('/workflow/deleteHistoricProcessInstance', param, tips)
  },
  activateProcessInstance (param, tips = {}) {
    return base.post('/workflow/activateProcessInstance', param, tips)
  },
  suspendProcessInstance (param, tips = {}) {
    return base.post('/workflow/suspendProcessInstance', param, tips)
  },
  reject (param, tips = {}) {
    return base.post('/workflow/reject', param, tips)
  },
  candidateInfos (param, tips = {}) {
    return base.post('/workflow/candidateInfos', param, tips)
  }
}
