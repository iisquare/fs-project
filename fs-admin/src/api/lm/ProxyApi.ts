import base from './Api'

export default {
  agents (tips = {}) {
    return base.get('/v1/agents', {}, tips)
  },
}
