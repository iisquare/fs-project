import base from './Api'

export default {
  jdbc (data: any, tips: any = {}) {
    return base.post('/modelCompare/jdbc', data, tips)
  },
}
