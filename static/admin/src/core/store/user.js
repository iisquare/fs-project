import userService from '@/service/member/user'

// initial state
const state = {
  ready: null,
  readyText: '载入中',
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