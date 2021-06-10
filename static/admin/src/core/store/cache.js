class Item {
  constructor (key, loader, ttl) {
    this.key = key
    this.loader = loader
    this.ttl = ttl
    this.date = new Date()
    this.value = null
  }

  isExpired () {
    if (this.ttl === 0) return false
    return new Date().getTime() - this.date.getTime() > this.ttl
  }

  load (withExpired) {
    if (withExpired && !this.isExpired()) return Promise.resolve(this.value)
    return this.loader().then(result => {
      this.date = new Date()
      this.value = result
      return result
    })
  }

  static key (key, loader) {
    return key || loader
  }
}

// initial state
const state = {
}

// getters
const getters = {
  load: state => (key, loader, ttl) => {
    key = Item.key(key, loader)
    let item = state[key]
    if (item) return item.load(true)
    item = state[key] = new Item(key, loader, ttl)
    return item.load(false)
  }
}

// actions
const actions = {
  reload ({ commit }, key, loader, ttl) {
    key = Item.key(key, loader)
    const item = new Item(key, loader, ttl)
    item.load(false).then(() => {
      commit('save', item)
    })
  }
}

// mutations
const mutations = {
  save (state, item) {
    state[item.key] = item
  }
}

export default {
  namespaced: true,
  state,
  getters,
  actions,
  mutations
}
