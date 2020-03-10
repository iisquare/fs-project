export default {
  hasPermit ($store, value) {
    if (!value) return false
    const permission = $store.state.user.data.resource
    if (!permission) return false
    return permission[value]
  }
}
