import base from './Api'

export default {
  info (param: any, tips = {}) {
    return base.post('/workflow/info', param, tips)
  },
  list (param: any, tips = {}) {
    return base.post('/workflow/list', param, tips)
  },
  delete (ids: any, tips = {}) {
    return base.post('/workflow/delete', { ids }, tips)
  },
  config (tips = {}) {
    return base.post('/workflow/config', {}, tips)
  },
  save (param: any, tips = {}) {
    return base.post('/workflow/save', param, tips)
  },
  publish (param: any, tips = {}) {
    return base.post('/workflow/publish', param, tips)
  },
  searchDeployment (param: any, tips = {}) {
    return base.post('/workflow/searchDeployment', param, tips)
  },
  deleteDeployment (param: any, tips = {}) {
    return base.post('/workflow/deleteDeployment', param, tips)
  },
  searchHistory (param: any, tips = {}) {
    return base.post('/workflow/searchHistory', param, tips)
  },
  process (param: any, tips = {}) {
    return base.post('/workflow/process', param, tips)
  },
  deleteProcessInstance (param: any, tips = {}) {
    return base.post('/workflow/deleteProcessInstance', param, tips)
  },
  deleteHistoricProcessInstance (param: any, tips = {}) {
    return base.post('/workflow/deleteHistoricProcessInstance', param, tips)
  },
  activateProcessInstance (param: any, tips = {}) {
    return base.post('/workflow/activateProcessInstance', param, tips)
  },
  suspendProcessInstance (param: any, tips = {}) {
    return base.post('/workflow/suspendProcessInstance', param, tips)
  },
  reject (param: any, tips = {}) {
    return base.post('/workflow/reject', param, tips)
  },
  candidateInfos (param: any, tips = {}) {
    return base.post('/workflow/candidateInfos', param, tips)
  }
}
