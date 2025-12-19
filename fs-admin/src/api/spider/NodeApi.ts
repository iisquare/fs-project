import base from './Api'

export default {
  stats (param: any, tips = {}) {
    return base.get('/node/stats', param, tips)
  },
  start (param: any, tips = {}) {
    return base.post('/node/start', param, tips)
  },
  stop (param: any, tips = {}) {
    return base.post('/node/stop', param, tips)
  },
  job (param: any, tips = {}) {
    return base.post('/node/job', param, tips)
  },
  execute (param: any, tips = {}) {
    return base.post('/node/execute', param, tips)
  },
  startJob (param: any, tips = {}) {
    return base.post('/node/startJob', param, tips)
  },
  pauseJob (param: any, tips = {}) {
    return base.post('/node/pauseJob', param, tips)
  },
  stopJob (param: any, tips = {}) {
    return base.post('/node/stopJob', param, tips)
  },
  clearJob (param: any, tips = {}) {
    return base.post('/node/clearJob', param, tips)
  },
  ack (param: any, tips = {}) {
    return base.get('/node/ack', param, tips)
  },
  discardAck (param: any, tips = {}) {
    return base.post('/node/discardAck', param, tips)
  },
}
