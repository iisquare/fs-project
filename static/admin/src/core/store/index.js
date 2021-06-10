import Vue from 'vue'
import Vuex from 'vuex'
import app from '@/core/store/app'
import user from '@/core/store/user'
import cache from '@/core/store/cache'

Vue.use(Vuex)

export default new Vuex.Store({
  modules: {
    app,
    user,
    cache
  }
})
