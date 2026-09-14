import base from './Api'

export default {
  run (param: any, tips = {}) {
    return base.post('/assess/run', param, tips)
  },
  history (param: any, tips = {}) {
    return base.post('/assess/history', param, tips)
  },
  detail (param: any, tips = {}) {
    return base.post('/assess/detail', param, tips)
  },
}
