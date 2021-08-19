import base from './base'

export default {
  load (param, tips = {}) {
    return base.post('/setting/load', param, tips)
  },
  change (param, tips = {}) {
    return base.post('/setting/change', param, tips)
  }
}
