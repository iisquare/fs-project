import base from './Api'

export default {
  notice (param: any, tips = {}) {
    return base.post('/gateway/notice', param, tips)
  },
}
