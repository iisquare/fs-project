import base from './base'

export default {
  statistic (param, tips = {}) {
    return base.get('/meta/statistic', param, tips)
  },
  search (param, tips = {}) {
    return base.post('/meta/search', param, tips)
  },
  blood (param, tips = {}) {
    return base.post('/meta/blood', param, tips)
  },
  influence (param, tips = {}) {
    return base.post('/meta/influence', param, tips)
  }
}
