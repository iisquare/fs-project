import base from './Api'

export default {
  agents (tips = {}) {
    return base.post('/chat/agents', {}, tips)
  },
}
