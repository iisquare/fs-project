import base from './Api'

export default {
  ruleList (param: any, tips = {}) {
    return base.post('/fusion/ruleList', param, tips)
  },
  ruleSave (param: any, tips = {}) {
    return base.post('/fusion/ruleSave', param, tips)
  },
  ruleDelete (param: any, tips = {}) {
    return base.post('/fusion/ruleDelete', param, tips)
  },
  scan (param: any, tips = {}) {
    return base.post('/fusion/scan', param, tips)
  },
  candidateList (param: any, tips = {}) {
    return base.post('/fusion/candidateList', param, tips)
  },
  candidateDetail (param: any, tips = {}) {
    return base.post('/fusion/candidateDetail', param, tips)
  },
  reject (param: any, tips = {}) {
    return base.post('/fusion/candidateReject', param, tips)
  },
  merge (param: any, tips = {}) {
    return base.post('/fusion/merge', param, tips)
  },
  recordList (param: any, tips = {}) {
    return base.post('/fusion/recordList', param, tips)
  },
}
