import api from '@/core/Api'
import axios from 'axios'

export default {
  get (url: string, data = {}, tips = {}, config = {}) {
    return api.post('/proxy/get', {
      app: 'Lucene',
      uri: url,
      data: data
    }, tips, config)
  },
  post (url: string, data = {}, tips = {}, config = {}) {
    return api.post('/proxy/post', {
      app: 'Lucene',
      uri: url,
      data: data
    }, tips, config)
  },
  esPost (baseURL: string, uri: string, data: any = {}) {
    const $axios = axios.create({ baseURL })
    return $axios.post(uri, data)
  },
  dictIndex (baseURL: string, param: any) {
    return this.esPost(baseURL, '/index', param)
  },
  dictDemo (baseURL: string, param: any) {
    return this.esPost(baseURL, '/demo', param)
  },
  dictReload (baseURL: string, param: any) {
    return this.esPost(baseURL, '/reload', param)
  },
  dictionaryList (param: any, tips = {}) {
    return this.post('/dictionary/list', param, tips)
  },
  dictionaryDelete (ids: any, tips = {}) {
    return this.post('/dictionary/delete', { ids }, tips)
  },
  dictionaryUnique (param: any, tips = {}) {
    return this.post('/dictionary/unique', param, tips)
  },
  dictionaryConfig (tips = {}) {
    return this.post('/dictionary/config', {}, tips)
  },
  dictionarySave (param: any, tips = {}) {
    return this.post('/dictionary/save', param, tips)
  }
}
