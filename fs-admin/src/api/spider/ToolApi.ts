import base from './Api'

export default {
  state (tips = {}) {
    return base.get('/tool/state', {}, tips)
  },
  parse (param: any, tips = {}) {
    return base.post('/tool/parse', param, tips)
  },
}
