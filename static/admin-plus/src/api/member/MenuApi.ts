import base from './Api'

export default {
  list (param: any, tips = {}) {
    return base.post('/menu/list', param, tips)
  },
  tree (param: any, tips = {}) {
    return base.post('/menu/tree', param, tips)
  },
  delete (ids: any, tips = {}) {
    return base.post('/menu/delete', { ids }, tips)
  },
  config (tips = {}) {
    return base.post('/menu/config', {}, tips)
  },
  save (param: any, tips = {}) {
    return base.post('/menu/save', param, tips)
  },
  target () {
    return [
      { value: '_blank', description: '在新窗口或选项卡中打开链接文档。' },
      { value: '_self', description: '在与点击相同的框架中打开链接的文档（默认）。' },
      { value: '_parent', description: '在父框架中打开链接文档。' },
      { value: '_top', description: '在窗口的整个主体中打开链接的文档。' },
      { value: 'framename', description: '在指定的 iframe 中打开链接文档。' },
    ]
  },
}
