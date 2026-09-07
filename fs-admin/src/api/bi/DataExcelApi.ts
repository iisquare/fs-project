import base from './Api'

export default {
  info(id: any, tips = {}) {
    return base.post('/dataExcel/info', { id }, tips)
  },
  list(params: any = {}, tips = {}) {
    return base.post('/dataExcel/list', params, tips)
  },
  save(data: any, tips = {}) {
    return base.post('/dataExcel/save', data, tips)
  },
  delete(ids: any, tips = {}) {
    return base.post('/dataExcel/delete', { ids }, tips)
  },
  config(tips = {}) {
    return base.post('/dataExcel/config', {}, tips)
  },
  upload(param: any, tips = {}) {
    return base.form('/dataExcel/upload', param, tips)
  },
  dataList(params: any = {}, tips = {}) {
    return base.post('/dataExcel/dataList', params, tips)
  },
  dataDelete(id: any, _ids: any, tips = {}) {
    return base.post('/dataExcel/dataDelete', { id, _ids }, tips)
  },
}
