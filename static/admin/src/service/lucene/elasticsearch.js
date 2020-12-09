import axios from 'axios'
import base from '@/core/ServiceBase'

let elasticsearchURL = 'http://127.0.0.1:9200/_plugin/analysis-ik-online'

const elasticsearchService = {
  baseURL () {
    return elasticsearchURL
  },
  reloadURL () {
    if (window.localStorage && window.localStorage.elasticsearchURL) {
      elasticsearchURL = window.localStorage && window.localStorage.elasticsearchURL
    }
    return elasticsearchURL
  },
  saveURL (url) {
    elasticsearchURL = url
    $axios.baseURL = elasticsearchURL
    if (window.localStorage) {
      window.localStorage.setItem('elasticsearchURL', elasticsearchURL)
    }
    return elasticsearchURL
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
  dictDemo (param, tips = {}) {
    return this.post('/demo', param, tips)
  },
  dictIndex (param, tips = {}) {
    return this.post('/index', param, tips)
  },
  dictReload (param, tips = {}) {
    return this.post('/reload', param, tips)
  }
}

elasticsearchService.reloadURL()
const $axios = axios.create({ baseURL: elasticsearchURL })

export default elasticsearchService
