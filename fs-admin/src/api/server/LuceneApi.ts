import api from '@/core/Api'

export default {
  get (url: string, params = {}, tips = {}, config = {}) {
    return api.get('/lucene' + url, params, tips, config)
  },
  post (url: string, data = {}, tips = {}, config = {}) {
    return api.post('/lucene' + url, data, tips, config)
  },
  ikIndex (param: any, tips = {}) {
    return this.post('/ik/index', param, tips)
  },
  ikDemo (param: any, tips = {}) {
    return this.post('/ik/demo', param, tips)
  },
  ikReload (param: any, tips = {}) {
    return this.post('/ik/reload', param, tips)
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
