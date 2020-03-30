import base from '@/core/ServiceBase'

export default {
  hasPermit ($store, value) {
    if (!value) return false
    const permission = $store.state.user.data.resource
    if (!permission) return false
    return permission[value]
  },
  login (data = {}, tips = {}, config = {}) {
    return base.post('/proxy/login', data, tips, config)
  }
}
