import base from './Api'

export default {
  list (data: any = {}, tips: any = {}) {
    return base.post('/modelColumn/list', data, tips)
  },
}
