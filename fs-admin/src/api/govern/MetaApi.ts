import base from './Api'

export default {
  statistic (data: any = {}, tips: any = {}) {
    return base.get('/meta/statistic', data, tips)
  },
  search (data: any, tips: any = {}) {
    return base.post('/meta/search', data, tips)
  },
  blood (data: any, tips: any = {}) {
    return base.post('/meta/blood', data, tips)
  },
  influence (data: any, tips: any = {}) {
    return base.post('/meta/influence', data, tips)
  },
}
