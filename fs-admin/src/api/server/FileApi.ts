import api from '@/core/Api'

export default {
  get (url: string, params = {}, tips = {}, config = {}) {
    return api.get('/file' + url, params, tips, config)
  },
  post (url: string, data = {}, tips = {}, config = {}) {
    return api.post('/file' + url, data, tips, config)
  },
  archiveList (param: any, tips = {}) {
    return this.post('/archive/list', param, tips)
  },
  archiveDelete (ids: any, tips = {}) {
    return this.post('/archive/delete', { ids }, tips)
  },
  archiveConfig (tips = {}) {
    return this.post('/archive/config', {}, tips)
  },
  archiveSave (param: any, tips = {}) {
    return this.post('/archive/save', param, tips)
  },
  archiveUpload (param: any, tips = {}) {
    return api.form('/file/archive/upload', param, tips)
  },
  archiveUrl (param: any, tips = {}) {
    return api.post('/file/archive/url', param, tips)
  }
}
