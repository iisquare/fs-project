import base from './Api'

export default {
  capabilities (param: any = {}, tips = {}) {
    return base.post('/schema/capabilities', param, tips)
  },
  show (param: any, tips = {}) {
    return base.post('/schema/show', param, tips)
  },
  precheck (param: any, tips = {}) {
    return base.post('/schema/precheck', param, tips)
  },
  create (param: any, tips = {}) {
    return base.post('/schema/create', param, tips)
  },
  drop (param: any, tips = {}) {
    return base.post('/schema/drop', param, tips)
  },
  batch (param: any, tips = {}) {
    return base.post('/schema/batch', param, tips)
  },
  plan (param: any, tips = {}) {
    return base.post('/schema/plan', param, tips)
  },
  diff (param: any, tips = {}) {
    return base.post('/schema/diff', param, tips)
  },
  apply (param: any, tips = {}) {
    return base.post('/schema/apply', param, tips)
  },
  scan (param: any = {}, tips = {}) {
    return base.post('/schema/scan', param, tips)
  },
  attach (param: any, tips = {}) {
    return base.post('/schema/attach', param, tips)
  },
}
