import api from '@/core/Api'

export default {
  login (data = {}, tips = {}, config = {}) {
    return api.post('/member/user/login', Object.assign({}, data, { module: 'admin' }), tips, config)
  },
  logout (data = {}, tips = {}, config = {}) {
    return api.post('/member/user/logout', Object.assign({}, data, { module: 'admin' }), tips, config)
  },
  signup (data = {}, tips = {}, config = {}) {
    return api.post('/member/user/signup', Object.assign({}, data, { module: 'admin' }), tips, config)
  },
  forgot (data = {}, tips = {}, config = {}) {
    return api.post('/member/user/forgot', Object.assign({}, data, { module: 'admin' }), tips, config)
  },
}
