import axios from 'axios'
import base from '@/core/ServiceBase'

let crawlerURL = 'http://127.0.0.1:8433'

const CrawlerService = {
  baseURL () {
    return crawlerURL
  },
  reloadURL () {
    if (window.localStorage && window.localStorage.crawlerURL) {
      crawlerURL = window.localStorage && window.localStorage.crawlerURL
    }
    return crawlerURL
  },
  saveURL (url) {
    crawlerURL = url
    $axios.baseURL = crawlerURL
    if (window.localStorage) {
      window.localStorage.setItem('crawlerURL', crawlerURL)
    }
    return crawlerURL
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
  scheduleList (tips = {}) {
    return this.get('/schedule/list/', tips)
  },
  scheduleGroup (tips = {}) {
    return this.get('/schedule/group/', tips)
  },
  scheduleHistory (tips = {}) {
    return this.get('/schedule/history/', tips)
  },
  scheduleClear (param, tips = {}) {
    return this.post('/schedule/clear/', param, tips)
  },
  scheduleChange (param, tips = {}) {
    return this.post('/schedule/change/', param, tips)
  },
  scheduleRemove (param, tips = {}) {
    return this.post('/schedule/remove/', param, tips)
  },
  scheduleSave (param, tips = {}) {
    return this.post('/schedule/save/', param, tips)
  },
  scheduleSubmit (param, tips = {}) {
    return this.post('/schedule/submit/', param, tips)
  },
  scheduleExecute (param, tips = {}) {
    return this.post('/schedule/execute/', param, tips)
  },
  scheduleParser (param, tips = {}) {
    return this.post('/schedule/parser/', param, tips)
  },
  scheduleProxy (tips = {}) {
    return this.get('/schedule/proxy/', tips)
  },
  nodesState (tips = {}) {
    return this.get('/nodes/state/', tips)
  }
}

CrawlerService.reloadURL()
const $axios = axios.create({ baseURL: crawlerURL })

export default CrawlerService
