import base from './base'

export default {
  jdbc (param, tips = {}) {
    return base.post('/modelCompare/jdbc', param, tips)
  }
}
