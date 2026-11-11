import base from './Api'

export default {
  spider(data: any, tips = {}) {
    return base.post('/sync/spider', data, tips)
  },
}
