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
  },
  jobList (param, tips = {}) {
    return this.post('/job/list', param, tips)
  },
  jobSave (param, tips = {}) {
    return this.post('/job/save', param, tips)
  },
  jobCommand (param, tips = {}) {
    return this.post('/job/command', param, tips)
  },
  triggerList (param, tips = {}) {
    return this.post('/trigger/list', param, tips)
  },
  triggerSave (param, tips = {}) {
    return this.post('/trigger/save', param, tips)
  },
  triggerCommand (param, tips = {}) {
    return this.post('/trigger/command', param, tips)
  },
  flowInfo (param, tips = {}) {
    return this.post('/flow/info', param, tips)
  },
  flowList (param, tips = {}) {
    return this.post('/flow/list', param, tips)
  },
  flowSave (param, tips = {}) {
    return this.post('/flow/save', param, tips)
  },
  flowDelete (param, tips = {}) {
    return this.post('/flow/delete', param, tips)
  },
  flowLogList (param, tips = {}) {
    return this.post('/flowLog/list', param, tips)
  },
  flowLogStages (param, tips = {}) {
    return this.post('/flowLog/stages', param, tips)
  }
}
