import base from './Api'

export default {
  notice (param: any, tips = {}) {
    return base.post('/manage/notice', param, tips)
  },
}
