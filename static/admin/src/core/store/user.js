import rbacService from '@/service/admin/rbac'

// initial state
const state = {
  ready: null,
  readyText: '载入中',
  data: null,
  appIndex: {}
}

// getters
const getters = {

}

// actions
const actions = {
  loadConfig ({ commit }) {
    rbacService.login().then((response) => {
      if (response.code === 0) {
        commit('ready', true)
        commit('data', response.data)
      } else {
        commit('ready', false)
      }
    })
  }
}

// mutations
const mutations = {
  ready (state, ready) {
    state.ready = ready
    state.readyText = ready ? '载入成功' : '载入失败'
  },
  data (state, data) {
    if (data.info && !data.info.avatar) {
      data.info.avatar = '/logo.png'
    }
    for (const key in data.menu) {
      const value = data.menu[key]
      const path = value.url.replace(/(^[/#]*)|([/#]*$)/, '')
      if (path.startsWith('[a-zA-Z]://')) continue
      const paths = path.split('/')
      if (paths.length === 2 || path === '') {
        state.appIndex['frame'] = key
      } else if (paths.length === 3) {
        state.appIndex[paths[0]] = key
      }
    }
    state.data = data
  }
}

export default {
  namespaced: true,
  state,
  getters,
  actions,
  mutations
}
