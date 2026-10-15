import base from './Api'

/** 数据主题：桌面端只做选择，主题的数据集与字段都在服务端 */
export default {
  list(param: any, tips = {}) {
    return base.post('/dataTheme/list', param, tips)
  }
}
