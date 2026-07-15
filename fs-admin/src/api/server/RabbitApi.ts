import api from '@/core/Api'

export default {
  get (url: string, params = {}, tips = {}, config = {}) {
    return api.get('/rabbit' + url, params, tips, config)
  },
  post (url: string, data = {}, tips = {}, config = {}) {
    return api.post('/rabbit' + url, data, tips, config)
  },
  containerSubmit (param: any, tips = {}) {
    return this.post('/container/submit', param, tips)
  },
  containerCreate (param: any, tips = {}) {
    return this.post('/container/create', param, tips)
  },
  containerChange (param: any, tips = {}) {
    return this.post('/container/change', param, tips)
  },
  containerRemove (param: any, tips = {}) {
    return this.post('/container/remove', param, tips)
  },
  containerStart (param: any, tips = {}) {
    return this.post('/container/start', param, tips)
  },
  containerStop (param: any, tips = {}) {
    return this.post('/container/stop', param, tips)
  },
  containerState (tips = {}) {
    return this.get('/container/state', tips)
  },
  taskTransient (param: any, tips = {}) {
    return this.get('/task/transient', param, tips)
  },
  taskNode (tips = {}) {
    return this.get('/task/node', tips)
  },
  taskNodes (param: any, tips = {}) {
    return this.get('/task/nodes', param, tips)
  },
  taskSubmit (param: any, tips = {}) {
    return this.post('/task/submit', param, tips)
  },
  taskList (param: any, tips = {}) {
    return this.post('/task/list', param, tips)
  },
  taskRemove (param: any, tips = {}) {
    return this.post('/task/remove', param, tips)
  },
  taskStart (param: any, tips = {}) {
    return this.post('/task/start', param, tips)
  },
  taskStop (param: any, tips = {}) {
    return this.post('/task/stop', param, tips)
  },
  taskRebalance (param: any, tips = {}) {
    return this.post('/task/rebalance', param, tips)
  },
  taskStartAll (param: any, tips = {}) {
    return this.post('/task/startAll', param, tips)
  },
  taskStopAll (param: any, tips = {}) {
    return this.post('/task/stopAll', param, tips)
  }
}
