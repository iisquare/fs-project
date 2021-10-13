import base from './base'

export default {
  config (tips = {}) {
    return base.post('/dag/config', {}, tips)
  }
}
