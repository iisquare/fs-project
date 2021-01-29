import axios from 'axios'
import base from '@/core/ServiceBase'

let rabbitURL = 'http://127.0.0.1:7813'

const rabbitService = {
  baseURL () {
    return rabbitURL
  },
  reloadURL () {
    if (window.localStorage && window.localStorage.rabbitURL) {
      rabbitURL = window.localStorage && window.localStorage.rabbitURL
    }
    return rabbitURL
  },
  saveURL (url) {
    rabbitURL = url
    $axios.defaults.baseURL = rabbitURL
    if (window.localStorage) {
      window.localStorage.setItem('rabbitURL', rabbitURL)
    }
    return rabbitURL
  },
  request (tips, config) {
    return base.wrapper($axios, tips, config)
  },
  get (url, tips = {}, config = {}) {
    return this.request(tips, Object.assign(config, { method: 'get', url }))
  },
  post (url, data = null, tips = {}, config = {}) {
    return this.request(tips, Object.assign(config, { method: 'post', url, data }))
  },
  containerSubmit (param, tips = {}) {
    return this.post('/container/submit', param, tips)
  },
  containerCreate (param, tips = {}) {
    return this.post('/container/create', param, tips)
  },
  containerChange (param, tips = {}) {
    return this.post('/container/change', param, tips)
  },
  containerRemove (param, tips = {}) {
    return this.post('/container/remove', param, tips)
  },
  containerStart (param, tips = {}) {
    return this.post('/container/start', param, tips)
  },
  containerStop (param, tips = {}) {
    return this.post('/container/stop', param, tips)
  },
  containerState (tips = {}) {
    return this.get('/container/state', tips)
  },
  taskTransient ({ maxConsumer }, tips = {}) {
    return this.get('/task/transient?maxConsumer=' + maxConsumer, tips)
  },
  taskNode (param, tips = {}) {
    return this.get('/task/node', tips)
  },
  taskNodes (param, tips = {}) {
    return this.get('/task/nodes', tips)
  },
  taskSubmit (param, tips = {}) {
    return this.post('/task/submit', param, tips)
  },
  taskList (param, tips = {}) {
    return this.post('/task/list', param, tips)
  },
  taskRemove (param, tips = {}) {
    return this.post('/task/remove', param, tips)
  },
  taskStart (param, tips = {}) {
    return this.post('/task/start', param, tips)
  },
  taskStop (param, tips = {}) {
    return this.post('/task/stop', param, tips)
  },
  taskRebalance (param, tips = {}) {
    return this.post('/task/rebalance', param, tips)
  },
  taskStartAll (param, tips = {}) {
    return this.post('/task/startAll', param, tips)
  },
  taskStopAll (param, tips = {}) {
    return this.post('/task/stopAll', param, tips)
  }
}

rabbitService.reloadURL()
const $axios = axios.create({ baseURL: rabbitURL })

export default rabbitService
