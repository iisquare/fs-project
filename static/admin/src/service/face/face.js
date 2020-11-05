import base from './base'

export default {
  detect (param, tips = {}) {
    return base.post('/face/detect', param, tips)
  },
  compare (param, tips = {}) {
    return base.post('/face/compare', param, tips)
  },
  search (param, tips = {}) {
    return base.post('/face/search', param, tips)
  },
  state (param, tips = {}) {
    return base.get('/face/state', param, tips)
  },
  reload (param, tips = {}) {
    return base.post('/face/reload', param, tips)
  }
}
