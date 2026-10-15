import base from './Api'

/** 知识图谱本体：桌面端只做选择，实体类型与关联定义都在服务端 */
export default {
  list(param: any, tips = {}) {
    return base.post('/ontology/list', param, tips)
  }
}
