import api from '@renderer/core/Api'

export default {
  login(body: any, tips = {}) {
    return api.post('/member/user/login', Object.assign({}, body, { module: 'admin' }), tips)
  },
  logout(body: any = {}, tips = {}) {
    return api.post('/member/user/logout', Object.assign({}, body, { module: 'admin' }), tips)
  },
  // 参考 fs-admin：不传账号调用登录接口即返回当前会话信息，用于会话校验与续期
  info(body: any = {}, tips = {}) {
    return api.post('/member/user/login', Object.assign({}, body, { module: 'admin' }), tips)
  },
  signup(body: any = {}, tips = {}) {
    return api.post('/member/user/signup', Object.assign({}, body, { module: 'admin' }), tips)
  },
  forgot(body: any = {}, tips = {}) {
    return api.post('/member/user/forgot', Object.assign({}, body, { module: 'admin' }), tips)
  },
}
