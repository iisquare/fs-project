import base from './Api'

export default {
  sourceList (param: any = {}, tips = {}) {
    return base.post('/extract/sourceList', param, tips)
  },
  sourceInfo (param: any, tips = {}) {
    return base.post('/extract/sourceInfo', param, tips)
  },
  sourceSave (param: any, tips = {}) {
    return base.post('/extract/sourceSave', param, tips)
  },
  sourceDelete (param: any, tips = {}) {
    return base.post('/extract/sourceDelete', param, tips)
  },
  sourceMark (param: any, tips = {}) {
    return base.post('/extract/sourceMark', param, tips)
  },
  preview (param: any, tips = {}) {
    return base.post('/extract/preview', param, tips)
  },
  apply (param: any, tips = {}) {
    return base.post('/extract/apply', param, tips)
  },
}
