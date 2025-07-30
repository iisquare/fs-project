import api from '@/core/Api'

export default {
  get (url: string, data = {}, tips = {}, config = {}) {
    return api.post('/proxy/get', {
      app: 'Cron',
      uri: url,
      data: data
    }, tips, config)
  },
  post (url: string, data = {}, tips = {}, config = {}) {
    return api.post('/proxy/post', {
      app: 'Cron',
      uri: url,
      data: data
    }, tips, config)
  },
  nodeStats (param: any = {}, tips = {}) {
    return this.get('/node/stats', param, tips)
  },
  nodeStandby (param: any, tips = {}) {
    return this.get('/node/standby', param, tips)
  },
  nodeRestart (param: any, tips = {}) {
    return this.get('/node/restart', param, tips)
  },
  nodeShutdown (param: any, tips = {}) {
    return this.get('/node/shutdown', param, tips)
  },
  jobList (param: any, tips = {}) {
    return this.post('/job/list', param, tips)
  },
  jobSave (param: any, tips = {}) {
    return this.post('/job/save', param, tips)
  },
  jobCommand (param: any, tips = {}) {
    return this.post('/job/command', param, tips)
  },
  triggerList (param: any, tips = {}) {
    return this.post('/trigger/list', param, tips)
  },
  triggerSave (param: any, tips = {}) {
    return this.post('/trigger/save', param, tips)
  },
  triggerCommand (param: any, tips = {}) {
    return this.post('/trigger/command', param, tips)
  },
  flowInfo (param: any, tips = {}) {
    return this.post('/flow/info', param, tips)
  },
  flowList (param: any, tips = {}) {
    return this.post('/flow/list', param, tips)
  },
  flowSave (param: any, tips = {}) {
    return this.post('/flow/save', param, tips)
  },
  flowDelete (param: any, tips = {}) {
    return this.post('/flow/delete', param, tips)
  },
  flowConfig (tips = {}) {
    return this.post('/flow/config', {}, tips)
  },
  flowLogList (param: any, tips = {}) {
    return this.post('/flowLog/list', param, tips)
  },
  flowLogStages (param: any, tips = {}) {
    return this.post('/flowLog/stages', param, tips)
  }
}
