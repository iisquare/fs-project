import userService from '@/service/member/user'

// initial state
const state = {
  ready: false,
  data: null
}

// getters
const getters = {

}

// actions
const actions = {
  loadConfig ({ commit }) {
    userService.login().then((response) => {
      if (response.code === 0) {
        commit('ready')
        commit('data', response.data)
      }
    })
  }
}

// mutations
const mutations = {
  ready (state, ready = true) {
    state.ready = ready
  },
  data (state, data) {
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
