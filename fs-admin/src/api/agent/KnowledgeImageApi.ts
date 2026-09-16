import base from './Api'

export default {
  url (param: any, tips = {}) {
    return base.post('/knowledgeImage/url', param, tips)
  },
  upload (param: any, tips = {}) {
    return base.form('/knowledgeImage/upload', param, tips)
  },
}
