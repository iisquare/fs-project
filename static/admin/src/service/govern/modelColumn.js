import base from './base'

export default {
  list (param, tips = {}) {
    return base.post('/modelColumn/list', param, tips)
  }
}
