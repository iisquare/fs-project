import Vue from 'vue'
import store from '@/core/store'
import rbacService from '@/service/admin/rbac'

Vue.directive('permit', {
  inserted (el, binding, vnode, oldVnode) {
    if (!rbacService.hasPermit(store, binding.value)) {
      el.remove()
    }
  }
})
