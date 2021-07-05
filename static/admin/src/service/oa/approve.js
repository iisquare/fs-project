import base from './base'

export default {
  workflow (param, tips = {}) {
    return base.post('/approve/workflow', param, tips)
  },
  form (param, tips = {}) {
    return base.post('/approve/form', param, tips)
  },
  submit (param, tips = {}) {
    return base.post('/approve/submit', param, tips)
  },
  searchCandidate (param, tips = {}) {
    return base.post('/approve/searchCandidate', param, tips)
  },
  claim (param, tips = {}) {
    return base.post('/approve/claim', param, tips)
  },
  searchAssignee (param, tips = {}) {
    return base.post('/approve/searchAssignee', param, tips)
  },
  transact (param, tips = {}) {
    return base.post('/approve/transact', param, tips)
  },
  complete (param, tips = {}) {
    return base.post('/approve/complete', param, tips)
  },
  searchHistory (param, tips = {}) {
    return base.post('/approve/searchHistory', param, tips)
  },
  process (param, tips = {}) {
    return base.post('/approve/process', param, tips)
  },
  revocation (param, tips = {}) {
    return base.post('/approve/revocation', param, tips)
  }
}
