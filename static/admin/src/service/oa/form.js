import base from './base'

export default {
  frame (param, tips = {}) {
    return base.post('/form/frame', param, tips)
  },
  list (param, tips = {}) {
    return base.post('/form/list', param, tips)
  },
  delete (param, tips = {}) {
    return base.post('/form/delete', param, tips)
  },
  save (param, tips = {}) {
    return base.post('/form/save', param, tips)
  }
}
