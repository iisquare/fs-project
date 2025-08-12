import base from './Api'

export default {
  workflow (param: any, tips = {}) {
    return base.post('/approve/workflow', param, tips)
  },
  form (param: any, tips = {}) {
    return base.post('/approve/form', param, tips)
  },
  submit (param: any, tips = {}) {
    return base.post('/approve/submit', param, tips)
  },
  searchCandidate (param: any, tips = {}) {
    return base.post('/approve/searchCandidate', param, tips)
  },
  claim (param: any, tips = {}) {
    return base.post('/approve/claim', param, tips)
  },
  searchAssignee (param: any, tips = {}) {
    return base.post('/approve/searchAssignee', param, tips)
  },
  transact (param: any, tips = {}) {
    return base.post('/approve/transact', param, tips)
  },
  complete (param: any, tips = {}) {
    return base.post('/approve/complete', param, tips)
  },
  searchHistory (param: any, tips = {}) {
    return base.post('/approve/searchHistory', param, tips)
  },
  process (param: any, tips = {}) {
    return base.post('/approve/process', param, tips)
  },
  revocation (param: any, tips = {}) {
    return base.post('/approve/revocation', param, tips)
  }
}
