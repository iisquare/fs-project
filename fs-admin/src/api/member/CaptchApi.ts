import base from './Api'

export default {
  generate (param: any = {}, tips = {}) {
    return base.get('/captcha/generate', param, tips)
  }
}
