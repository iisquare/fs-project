class Item {
  constructor (key, loader, ttl = 0) {
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
  /**
   * 对异步加载函数进行缓存
   * 提示：key=this或loader=() => {}用于构造唯一标识
   * UIUtil.cache(this, func()).then(result => {})
   * UIUtil.cache(null, () => func()).then(result => {})
   */
  load: state => (key, loader, ttl) => {
    key = Item.key(key, loader)
    let item = state[key]
    if (item) return item.load(true)
    item = state[key] = new Item(key, loader, ttl)
    return item.load(false)
  },
  /**
   * 获取缓存
   * UIUtil.kv(this)
   */
  get: state => (key) => {
    const item = state[key]
    if (!item) return null
    if (item.isExpired()) return false
    return item.value
  },
  /**
   * 设置缓存
   * UIUtil.kv(this, 'value')
   */
  set: state => (key, value, ttl) => {
    const item = new Item(key, null, ttl)
    item.value = value
    state[key] = item
    return item
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
