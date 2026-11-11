import base from './Api'

export default {
  channelConfig (tips = {}) {
    return base.post('/proxy/channel/config', {}, tips)
  },
  channelList (param: any, tips = {}) {
    return base.post('/proxy/channel/list', param, tips)
  },
  channelSave (param: any, tips = {}) {
    return base.post('/proxy/channel/save', param, tips)
  },
  channelDelete (ids: any, tips = {}) {
    return base.post('/proxy/channel/delete', { ids }, tips)
  },
  channelTest (param: any, tips = {}) {
    return base.post('/proxy/channel/test', param, tips)
  },
  groupConfig (tips = {}) {
    return base.post('/proxy/group/config', {}, tips)
  },
  groupList (param: any, tips = {}) {
    return base.post('/proxy/group/list', param, tips)
  },
  groupOptions (tips = {}) {
    return base.post('/proxy/group/options', {}, tips)
  },
  groupSave (param: any, tips = {}) {
    return base.post('/proxy/group/save', param, tips)
  },
  groupDelete (ids: any, tips = {}) {
    return base.post('/proxy/group/delete', { ids }, tips)
  },
  stats (param: any, tips = {}) {
    return base.get('/proxy/stats', param, tips)
  },
}
