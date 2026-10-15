import api from '@renderer/core/Api'

export default {
  generate (param: any = {}, tips = {}) {
    return api.get('/member/captcha/generate', param, tips)
  }
}
