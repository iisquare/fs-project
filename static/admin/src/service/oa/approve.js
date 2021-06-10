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
  }
}
