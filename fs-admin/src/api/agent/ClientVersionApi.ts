import base from './Api'

/** 智能体客户端安装包版本：上传、发布日志、最低支持版本与启用状态 */
export default {
  info (id: any, tips = {}) {
    return base.get('/clientVersion/info', { id }, tips)
  },
  list (param: any, tips = {}) {
    return base.post('/clientVersion/list', param, tips)
  },
  delete (ids: any, tips = {}) {
    return base.post('/clientVersion/delete', { ids }, tips)
  },
  config (tips = {}) {
    return base.post('/clientVersion/config', {}, tips)
  },
  save (param: any, tips = {}) {
    return base.post('/clientVersion/save', param, tips)
  },
  upload (param: any, tips = {}, config = {}) {
    return base.form('/clientVersion/upload', param, tips, config)
  },
  download (param: any) {
    return base.get('/clientVersion/download', param, {})
  },
  stats (param: any = {}, tips = {}) {
    return base.post('/clientVersion/stats', param, tips)
  }
}
