// with polyfills
import 'core-js/stable'
import 'regenerator-runtime/runtime'

import Vue from 'vue'
import App from './App.vue'
import router from './core/router'
import store from './core/store'
import './core/directive'

import bootstrap from './core/bootstrap'
import './core/lazy_use'
import './core/filter' // global filter
import './assets/global.less'

Vue.config.productionTip = false

store.dispatch('user/loadConfig')

new Vue({
  router,
  store,
  created: bootstrap,
  render: h => h(App)
}).$mount('#app')
