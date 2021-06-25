import base from './base'

export default {
  infos (param, tips = {}) {
    return base.post('/rbac/infos', param, tips)
  }
}
