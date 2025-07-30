import base from './Api'

export default {
  config (tips = {}) {
    return base.post('/dag/config', {}, tips)
  },
}
