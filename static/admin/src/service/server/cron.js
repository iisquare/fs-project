import base from '@/core/ServiceBase'

export default {
  get (url, data = {}, tips = {}, config = {}) {
    return base.post('/proxy/get', {
      app: 'Cron',
      uri: url,
      data: data
    }, tips, config)
  },
  post (url, data = {}, tips = {}, config = {}) {
    return base.post('/proxy/post', {
      app: 'Cron',
      uri: url,
      data: data
    }, tips, config)
  },
  nodeStats (param, tips = {}) {
    return this.get('/node/stats', param, tips)
  },
  nodeStandby (param, tips = {}) {
    return this.get('/node/standby', param, tips)
  },
  nodeRestart (param, tips = {}) {
    return this.get('/node/restart', param, tips)
  },
  nodeShutdown (param, tips = {}) {
    return this.get('/node/shutdown', param, tips)
  }
}
