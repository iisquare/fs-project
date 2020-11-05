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
    return this.post('/dict/demo/', param, tips)
  },
  dictIndex (param, tips = {}) {
    return this.post('/dict/index/', param, tips)
  },
  dictReload (param, tips = {}) {
    return this.post('/dict/reload/', param, tips)
  },
  dictList (param, tips = {}) {
    if (param.identity === '') {
      delete param.identity
    }
    return this.post('/dict/list/', param, null).then(result => {
      return {
        code: result.status === 200 ? 0 : result.status,
        message: 'wraper with service',
        data: result.data
      }
    })
  },
  dictUpdate (param, tips = {}) {
    return this.post('/dict/update/', param, tips)
  },
  dictDelete (param, tips = {}) {
    return this.post('/dict/delete/', param, tips)
  },
  dictInsert (param, tips = {}) {
    return this.post('/dict/insert/', param, tips)
  },
  dictSave (param, tips = {}) {
    if (param.id) {
      return this.dictUpdate(param, tips)
    }
    param = Object.assign({}, param, { texts: [param.text] })
    return this.dictInsert(param, tips)
  }
}

elasticsearchService.reloadURL()
const $axios = axios.create({ baseURL: elasticsearchURL })

export default elasticsearchService
